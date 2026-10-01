package br.com.damiq.desktop.infraestrutura.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.infraestrutura.motor.ExecutorMotor.ExecucaoMotor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Testa o mapeamento entre a porta e o JSON do motor, com um executor que devolve respostas prontas. */
class MotorCalculoProcessBuilderTest {

    private static final String INFO_OK = """
            {"versao_contrato": "1.0", "operacao": "info", "status": "OK", "versao_config": null,
             "avisos": [], "erros": [], "campo_de_uma_versao_futura": {"x": 1},
             "motor": {"versao": "1.0.1", "operacoes": ["info", "validar_configuracao"]}}
            """;

    private final List<String> requisicoes = new ArrayList<>();

    private MotorCalculoProcessBuilder motor(int codigoSaida, String resposta) {
        return motor(new ExecucaoMotor(codigoSaida, resposta, ""));
    }

    private MotorCalculoProcessBuilder motor(ExecucaoMotor execucao) {
        return new MotorCalculoProcessBuilder(requisicao -> {
            requisicoes.add(requisicao);
            return execucao;
        });
    }

    private static Configuracao configuracao(String json) {
        return new Configuracao(new VersaoConfiguracao("21"), json);
    }

    @Test
    void infoIgnoraCamposDesconhecidos() {
        var info = motor(0, INFO_OK).info();

        assertEquals("1.0.1", info.versaoMotor());
        assertEquals("1.0", info.versaoContrato());
        assertEquals(List.of("info", "validar_configuracao"), info.operacoes());
        assertEquals("{\"versao_contrato\":\"1.0\",\"operacao\":\"info\"}", requisicoes.getFirst());
    }

    @Test
    void validarConfiguracaoEnviaAConfiguracaoInteira() {
        var resposta = """
                {"versao_contrato": "1.0", "status": "OK", "versao_config": 21, "erros": [],
                 "avisos": [{"codigo": "CAMPO_DESCONHECIDO", "campo": "configuracao.xpto", "mensagem": "ignorado"}]}
                """;

        var validacao = motor(0, resposta).validarConfiguracao(configuracao("{\"versao\": 21, \"xpto\": true}"));

        assertTrue(validacao.valida());
        assertEquals("configuracao.xpto", validacao.avisos().getFirst().campo());
        assertEquals(
                "{\"versao_contrato\":\"1.0\",\"operacao\":\"validar_configuracao\","
                        + "\"configuracao\":{\"versao\":21,\"xpto\":true}}",
                requisicoes.getFirst());
    }

    @Test
    void configuracaoRecusadaTrazOCampo() {
        var resposta = """
                {"status": "ERRO", "erros": [{"codigo": "CONTRATO_INVALIDO",
                 "campo": "configuracao.sensores.PZ-01.faixa", "mensagem": "min (5) maior que max (1)"}]}
                """;

        var validacao = motor(1, resposta).validarConfiguracao(configuracao("{\"versao\": 21}"));

        assertFalse(validacao.valida());
        assertEquals("configuracao.sensores.PZ-01.faixa", validacao.erros().getFirst().campo());
    }

    @Test
    void configuracaoQueNaoEhJsonNemChegaAoMotor() {
        var motor = new MotorCalculoProcessBuilder(requisicao -> fail("o motor não deveria ser chamado"));

        var validacao = motor.validarConfiguracao(configuracao("{versao: 21"));

        assertFalse(validacao.valida());
        assertEquals("JSON_INVALIDO", validacao.erros().getFirst().codigo());
        assertEquals("configuracao", validacao.erros().getFirst().campo());
    }

    @Test
    void erroNaoLigadoACampoTemCampoNulo() {
        var resposta = """
                {"status": "ERRO", "erros": [{"codigo": "VERSAO_INCOMPATIVEL", "campo": null, "mensagem": "2.0"}]}
                """;

        var validacao = motor(1, resposta).validarConfiguracao(configuracao("{\"versao\": 21}"));

        assertNull(validacao.erros().getFirst().campo());
    }

    @Test
    void infoRecusadaEhFalha() {
        var resposta = """
                {"status": "ERRO", "erros": [{"codigo": "VERSAO_INCOMPATIVEL", "campo": null, "mensagem": "2.0"}]}
                """;

        var falha = assertThrows(FalhaMotorException.class, () -> motor(1, resposta).info());

        assertTrue(falha.getMessage().contains("VERSAO_INCOMPATIVEL"), falha.getMessage());
    }

    @Test
    void erroInternoEhFalha() {
        var execucao = new ExecucaoMotor(2, "{\"status\": \"ERRO\"}", "Traceback (most recent call last): ...");

        var falha = assertThrows(FalhaMotorException.class, () -> motor(execucao).info());

        assertTrue(falha.getMessage().contains("Erro interno"), falha.getMessage());
    }

    @Test
    void codigoDeSaidaInesperadoEhFalha() {
        assertThrows(FalhaMotorException.class, () -> motor(137, "").info());
    }

    @Test
    void respostaVaziaEhFalha() {
        var falha = assertThrows(FalhaMotorException.class, () -> motor(0, "").info());

        assertTrue(falha.getMessage().contains("não escreveu resposta"), falha.getMessage());
    }

    @Test
    void respostaIlegivelEhFalha() {
        var falha = assertThrows(FalhaMotorException.class, () -> motor(0, "Traceback").info());

        assertTrue(falha.getMessage().contains("ilegível"), falha.getMessage());
    }

    @Test
    void statusIncoerenteComOCodigoDeSaidaEhFalha() {
        assertThrows(FalhaMotorException.class, () -> motor(1, INFO_OK).info());
    }

    @Test
    void recusaSemMotivoEhFalha() {
        var resposta = "{\"status\": \"ERRO\", \"erros\": []}";

        assertThrows(
                FalhaMotorException.class,
                () -> motor(1, resposta).validarConfiguracao(configuracao("{\"versao\": 21}")));
    }
}
