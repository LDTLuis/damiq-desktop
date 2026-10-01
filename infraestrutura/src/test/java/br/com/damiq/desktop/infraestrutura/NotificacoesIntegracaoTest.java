package br.com.damiq.desktop.infraestrutura;

import br.com.damiq.desktop.infraestrutura.persistencia.AutoriaDeTeste;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.aplicacao.medicao.MedicoesProcessadas;
import br.com.damiq.desktop.aplicacao.medicao.OrigemLeituras;
import br.com.damiq.desktop.aplicacao.medicao.ProcessarMedicoes;
import br.com.damiq.desktop.aplicacao.notificacao.ConsultarNotificacoes;
import br.com.damiq.desktop.aplicacao.notificacao.Notificacao;
import br.com.damiq.desktop.aplicacao.notificacao.NotificacaoEmitida.Motivo;
import br.com.damiq.desktop.aplicacao.notificacao.NotificarAlertas;
import br.com.damiq.desktop.aplicacao.notificacao.ReconhecerNotificacao;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.infraestrutura.motor.LoteExemplo;
import br.com.damiq.desktop.infraestrutura.motor.MotorCalculoProcessBuilder;
import br.com.damiq.desktop.infraestrutura.motor.MotorInstalado;
import br.com.damiq.desktop.infraestrutura.persistencia.BancoDados;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioBarragensJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioConfiguracoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioMedicoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioNotificacoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioProcessamentosJdbc;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Processamento → notificações → reconhecimento, com SQLite e motor real. */
class NotificacoesIntegracaoTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");

    @TempDir
    Path diretorio;

    private final Clock relogio = Clock.fixed(Instant.parse("2026-09-30T02:00:00Z"), ZoneOffset.UTC);
    private MotorCalculoProcessBuilder motor;
    private DataSource banco;
    private RepositorioBarragensJdbc barragens;
    private RepositorioConfiguracoesJdbc configuracoes;
    private RepositorioNotificacoesJdbc repositorio;
    private NotificarAlertas notificar;
    private ProcessarMedicoes processar;
    private ConsultarNotificacoes consultar;
    private ReconhecerNotificacao reconhecer;

    @BeforeEach
    void montar() {
        motor = MotorInstalado.exigir();
        banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        barragens = new RepositorioBarragensJdbc(banco, AutoriaDeTeste.SISTEMA);
        barragens.salvar(new Barragem(JOAO_LEITE, "João Leite"));
        configuracoes = new RepositorioConfiguracoesJdbc(banco, AutoriaDeTeste.SISTEMA);
        configuracoes.ativar(JOAO_LEITE, new Configuracao(new VersaoConfiguracao("21"), LoteExemplo.CONFIGURACAO),
                OrigemConfiguracao.ARQUIVO, Instant.now());

        repositorio = new RepositorioNotificacoesJdbc(banco, AutoriaDeTeste.SISTEMA);
        notificar = new NotificarAlertas(repositorio, evento -> {}, relogio);
        consultar = new ConsultarNotificacoes(repositorio);
        reconhecer = new ReconhecerNotificacao(repositorio, evento -> {}, relogio);
        // como o OuvinteNotificacoes faz no app
        processar = new ProcessarMedicoes(barragens, configuracoes, new RepositorioMedicoesJdbc(banco),
                new RepositorioProcessamentosJdbc(banco, AutoriaDeTeste.SISTEMA), motor,
                evento -> notificar.executar(((MedicoesProcessadas) evento).barragem()), relogio, 48);
    }

    private static LeituraInformada pz01(String hora, String kPa) {
        return new LeituraInformada("PZ-01", "pressao", "2026-09-29T" + hora + ":00-03:00", kPa, "kPa");
    }

    @Test
    void processamentoNotificaEReconhecimentoEncerra() {
        processar.executar(JOAO_LEITE, LoteExemplo.lote().leituras(), OrigemLeituras.IMPORTACAO, "campo.csv");

        var abertas = consultar.abertas(JOAO_LEITE);
        assertEquals(2, abertas.size());
        var limite = abertas.getFirst();
        assertEquals(TipoAlerta.LIMITE, limite.tipoAlerta());
        assertEquals(Severidade.CRITICO, limite.severidade());
        assertEquals(3, limite.nivelResposta());
        assertTrue(limite.leituraSuspeita());
        assertTrue(repositorio.barragensComAlertasPendentes().isEmpty());

        reconhecer.executar(limite.id(), "Eng. Ana", "Instrumento verificado; leitura de 600 kPa descartada");

        assertEquals(List.of(TipoAlerta.FORA_FAIXA_PLAUSIVEL),
                consultar.abertas(JOAO_LEITE).stream().map(Notificacao::tipoAlerta).toList());
        var auditada = repositorio.buscar(limite.id()).orElseThrow().reconhecida().orElseThrow();
        assertEquals("Eng. Ana", auditada.por());
        assertEquals(Instant.parse("2026-09-30T02:00:00Z"), auditada.em());
    }

    @Test
    void persistenciaEEscalada() {
        // 230 kPa: acima do alerta (215,82), abaixo do crítico (260)
        processar.executar(JOAO_LEITE, List.of(pz01("08", "140"), pz01("09", "230")), OrigemLeituras.DIGITACAO, null);
        var primeira = consultar.abertas(JOAO_LEITE).getFirst();
        assertEquals(Severidade.ALERTA, primeira.severidade());

        processar.executar(JOAO_LEITE, List.of(pz01("10", "232")), OrigemLeituras.DIGITACAO, null);
        var repetida = repositorio.buscar(primeira.id()).orElseThrow();
        assertEquals(2, repetida.ocorrencias());
        assertEquals(Severidade.ALERTA, repetida.severidade());

        processar.executar(JOAO_LEITE, List.of(pz01("11", "270")), OrigemLeituras.DIGITACAO, null);
        var escalada = repositorio.buscar(primeira.id()).orElseThrow();
        assertEquals(Severidade.CRITICO, escalada.severidade());
        assertEquals(3, escalada.ocorrencias());
        assertEquals(1, consultar.abertas(JOAO_LEITE).size());
    }

    @Test
    void alertasSemNotificacaoSaoRecuperados() {
        // como se o app fechasse entre gravar o processamento e gerar as notificações
        var semNotificar = new ProcessarMedicoes(barragens, configuracoes, new RepositorioMedicoesJdbc(banco),
                new RepositorioProcessamentosJdbc(banco, AutoriaDeTeste.SISTEMA), motor, evento -> {}, relogio, 48);
        semNotificar.executar(JOAO_LEITE, List.of(pz01("08", "230")), OrigemLeituras.DIGITACAO, null);
        assertEquals(List.of(JOAO_LEITE), repositorio.barragensComAlertasPendentes());

        var recuperadas = notificar.executarPendentes();

        assertEquals(1, recuperadas.size());
        assertEquals(Motivo.NOVA, recuperadas.getFirst().motivo());
        assertTrue(repositorio.barragensComAlertasPendentes().isEmpty());
    }
}
