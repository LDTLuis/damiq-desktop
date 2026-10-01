package br.com.damiq.desktop.infraestrutura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.damiq.desktop.aplicacao.importacao.ImportarMedicoes;
import br.com.damiq.desktop.aplicacao.medicao.ProcessarMedicoes;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.infraestrutura.importacao.LeitorArquivoLeiturasPadrao;
import br.com.damiq.desktop.infraestrutura.motor.LoteExemplo;
import br.com.damiq.desktop.infraestrutura.motor.MotorInstalado;
import br.com.damiq.desktop.infraestrutura.persistencia.BancoDados;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioBarragensJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioConfiguracoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioMedicoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioProcessamentosJdbc;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Importação de um CSV de campo com SQLite e motor real. */
class ImportarMedicoesIntegracaoTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");

    @TempDir
    Path diretorio;

    @Test
    void importaCsvDoExcelEApontaALinhaRejeitada() throws IOException {
        var motor = MotorInstalado.exigir();
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        var barragens = new RepositorioBarragensJdbc(banco, Clock.systemUTC());
        barragens.salvar(new Barragem(JOAO_LEITE, "João Leite"));
        var configuracoes = new RepositorioConfiguracoesJdbc(banco);
        configuracoes.ativar(JOAO_LEITE, new Configuracao(new VersaoConfiguracao("21"), LoteExemplo.CONFIGURACAO),
                OrigemConfiguracao.ARQUIVO, Instant.now());
        var processar = new ProcessarMedicoes(barragens, configuracoes, new RepositorioMedicoesJdbc(banco),
                new RepositorioProcessamentosJdbc(banco), motor,
                Clock.fixed(Instant.parse("2026-09-30T02:00:00Z"), ZoneOffset.UTC), 48);
        var importar = new ImportarMedicoes(new LeitorArquivoLeiturasPadrao(), processar);

        var csv = Files.writeString(diretorio.resolve("campo-setembro.csv"), """
                Instrumento;Tipo;Data/Hora;Valor;Unidade;Observação
                PZ-01;Pressão;29/09/2026 08:00;1,4;bar;
                PZ-01;Pressão;29/09/2026 12:00;240;kPa;chuva

                PZ-01;Pressão;29/09/2026 13:00;sem leitura;kPa;equipamento travado
                NV-01;Nível;29/09/2026 14:00;12,5;m;
                """, Charset.forName("windows-1252"));

        var resultado = importar.executar(JOAO_LEITE, csv);

        assertEquals("campo-setembro.csv", resultado.arquivo());
        assertEquals(3, resultado.processamento().gravadas().size());
        assertEquals(1, resultado.rejeicoes().size());
        assertEquals(5, resultado.rejeicoes().getFirst().linha());
        assertEquals("VALOR_INVALIDO", resultado.rejeicoes().getFirst().rejeicao().codigo());
        assertEquals(Severidade.ALERTA, resultado.processamento().lote().statusBarragem());
    }
}
