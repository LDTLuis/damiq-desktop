package br.com.damiq.desktop.infraestrutura.configuracao;

import br.com.damiq.desktop.infraestrutura.persistencia.AutoriaDeTeste;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.damiq.desktop.aplicacao.configuracao.AtualizarConfiguracao;
import br.com.damiq.desktop.aplicacao.configuracao.ResultadoAtualizacaoConfiguracao.Situacao;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.infraestrutura.motor.MotorInstalado;
import br.com.damiq.desktop.infraestrutura.persistencia.BancoDados;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioBarragensJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioConfiguracoesJdbc;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Fluxo completo com SQLite, arquivo de configuração e motor real. */
class AtualizarConfiguracaoIntegracaoTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");

    @TempDir
    Path diretorio;

    private RepositorioConfiguracoesJdbc configuracoes;
    private AtualizarConfiguracao atualizar;

    @BeforeEach
    void montar() {
        var motor = MotorInstalado.exigir();
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        var barragens = new RepositorioBarragensJdbc(banco, AutoriaDeTeste.SISTEMA);
        barragens.salvar(new Barragem(JOAO_LEITE, "João Leite"));
        configuracoes = new RepositorioConfiguracoesJdbc(banco, AutoriaDeTeste.SISTEMA);
        atualizar = new AtualizarConfiguracao(
                barragens, configuracoes, new FonteConfiguracaoArquivo(diretorio), motor, Clock.systemUTC());
    }

    private void publicar(String json) throws IOException {
        Files.writeString(diretorio.resolve("joao-leite.json"), json, StandardCharsets.UTF_8);
    }

    private String versaoVigente() {
        return configuracoes.vigente(JOAO_LEITE).orElseThrow().versao().valor();
    }

    @Test
    void ativaValidaEMantemAVigenteQuandoAProximaEhInvalida() throws IOException {
        publicar("""
                {"versao": 21, "sensores": {"PZ-01": {"tipo": "pressao", "frequencia_esperada_s": 3600}}}
                """);
        assertEquals(Situacao.ATIVADA, atualizar.executar(JOAO_LEITE).situacao());
        assertEquals(Situacao.JA_VIGENTE, atualizar.executar(JOAO_LEITE).situacao());

        publicar("""
                {"versao": 22, "fuso_padrao": "America/Sao_Paulo"}
                """);
        var recusada = atualizar.executar(JOAO_LEITE);

        assertEquals(Situacao.RECUSADA, recusada.situacao());
        assertEquals("configuracao.fuso_padrao", recusada.erros().getFirst().campo());
        assertEquals("21", versaoVigente());

        publicar("""
                {"versao": 23, "fuso_padrao": "-03:00"}
                """);
        assertEquals(Situacao.ATIVADA, atualizar.executar(JOAO_LEITE).situacao());
        assertEquals("23", versaoVigente());
    }
}
