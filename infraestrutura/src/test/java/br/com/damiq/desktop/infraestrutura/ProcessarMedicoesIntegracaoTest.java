package br.com.damiq.desktop.infraestrutura;

import br.com.damiq.desktop.infraestrutura.persistencia.AutoriaDeTeste;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.aplicacao.medicao.OrigemLeituras;
import br.com.damiq.desktop.aplicacao.medicao.ProcessarMedicoes;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.infraestrutura.motor.LoteExemplo;
import br.com.damiq.desktop.infraestrutura.motor.MotorInstalado;
import br.com.damiq.desktop.infraestrutura.persistencia.BancoDados;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioBarragensJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioConfiguracoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioMedicoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioProcessamentosJdbc;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Fluxo completo de {@link ProcessarMedicoes} com SQLite e motor real. */
class ProcessarMedicoesIntegracaoTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");

    @TempDir
    Path diretorio;

    private RepositorioMedicoesJdbc medicoes;
    private ProcessarMedicoes processar;

    @BeforeEach
    void montar() {
        var motor = MotorInstalado.exigir();
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        var barragens = new RepositorioBarragensJdbc(banco, AutoriaDeTeste.SISTEMA);
        barragens.salvar(new Barragem(JOAO_LEITE, "João Leite"));
        var configuracoes = new RepositorioConfiguracoesJdbc(banco, AutoriaDeTeste.SISTEMA);
        configuracoes.ativar(JOAO_LEITE, new Configuracao(new VersaoConfiguracao("21"), LoteExemplo.CONFIGURACAO),
                OrigemConfiguracao.ARQUIVO, Instant.now());
        medicoes = new RepositorioMedicoesJdbc(banco);
        // relógio depois das leituras, para nenhuma ser recusada como futura
        var relogio = Clock.fixed(Instant.parse("2026-09-30T02:00:00Z"), ZoneOffset.UTC);
        processar = new ProcessarMedicoes(barragens, configuracoes, medicoes, new RepositorioProcessamentosJdbc(banco, AutoriaDeTeste.SISTEMA),
                motor, evento -> {}, relogio, 48);
    }

    @Test
    void processaGravaEReimportaSemDuplicar() {
        var leituras = LoteExemplo.lote().leituras();

        var primeiro = processar.executar(JOAO_LEITE, leituras, OrigemLeituras.IMPORTACAO, "campo.csv");

        assertEquals(4, primeiro.gravadas().size());
        assertEquals(1, primeiro.lote().rejeicoes().size());
        assertEquals(2, primeiro.alertas().size());
        assertEquals(Severidade.CRITICO, primeiro.lote().statusBarragem());
        assertEquals(4, medicoes.ultimas(JOAO_LEITE, 48).size());

        var reimportado = processar.executar(JOAO_LEITE, leituras, OrigemLeituras.IMPORTACAO, "campo.csv");

        assertEquals(0, reimportado.gravadas().size());
        assertEquals(4, reimportado.jaRegistradas().size());
        assertTrue(reimportado.alertas().isEmpty());
        assertEquals(4, medicoes.ultimas(JOAO_LEITE, 48).size());
    }

    @Test
    void historicoDaBarragemDaContextoAoLoteSeguinte() {
        processar.executar(JOAO_LEITE, LoteExemplo.lote().leituras(), OrigemLeituras.DIGITACAO, null);

        // 250 kPa às 15h fica entre o alerta (215,82) e o crítico (260): o lote seguinte sai em ALERTA
        var seguinte = processar.executar(JOAO_LEITE,
                List.of(new LeituraInformada("PZ-01", "pressao", "2026-09-29T15:00:00-03:00", "250", "kPa")),
                OrigemLeituras.DIGITACAO, null);

        assertEquals(1, seguinte.gravadas().size());
        assertEquals(Severidade.ALERTA, seguinte.lote().statusBarragem());
        assertEquals(5, medicoes.ultimas(JOAO_LEITE, 48).size());
    }
}
