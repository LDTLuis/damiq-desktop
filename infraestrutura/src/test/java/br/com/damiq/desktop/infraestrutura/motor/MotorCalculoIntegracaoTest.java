package br.com.damiq.desktop.infraestrutura.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Executa o motor real, instalado a partir da release (ver README). Sem o motor, os testes são pulados, a
 * menos que {@code DAMIQ_MOTOR_OBRIGATORIO=true} (CI).
 */
class MotorCalculoIntegracaoTest {

    private static MotorCalculoProcessBuilder motor;

    @BeforeAll
    static void localizarMotor() {
        var variavel = System.getenv("DAMIQ_MOTOR_PYTHON");
        var python = variavel != null && !variavel.isBlank()
                ? Path.of(variavel)
                : ConfiguracaoMotor.pythonDoAmbienteLocal(Path.of(System.getProperty("damiq.raiz", "..")));
        var instalado = Files.isExecutable(python);
        if (!instalado && Boolean.parseBoolean(System.getenv("DAMIQ_MOTOR_OBRIGATORIO"))) {
            throw new IllegalStateException("Motor de cálculo não encontrado em " + python);
        }
        assumeTrue(instalado, "Motor de cálculo não instalado em " + python);
        motor = new MotorCalculoProcessBuilder(ConfiguracaoMotor.python(python, Duration.ofSeconds(60)));
    }

    private static Configuracao configuracao(String json) {
        return new Configuracao(new VersaoConfiguracao("21"), json);
    }

    @Test
    void info() {
        var info = motor.info();

        assertTrue(info.versaoMotor().startsWith("1."), info.versaoMotor());
        assertEquals(MotorCalculoProcessBuilder.VERSAO_CONTRATO, info.versaoContrato());
        assertTrue(info.operacoes().contains("validar_configuracao"), info.operacoes().toString());
    }

    @Test
    void configuracaoValida() {
        var validacao = motor.validarConfiguracao(configuracao("""
                {"versao": 21, "fuso_padrao": "-03:00",
                 "sensores": {"PZ-01": {"tipo": "pressao", "frequencia_esperada_s": 3600}}}
                """));

        assertTrue(validacao.valida(), validacao.erros().toString());
    }

    @Test
    void configuracaoInvalidaApontaOCampo() {
        var validacao = motor.validarConfiguracao(configuracao("""
                {"versao": 21, "fuso_padrao": "America/Sao_Paulo"}
                """));

        assertFalse(validacao.valida());
        assertEquals("configuracao.fuso_padrao", validacao.erros().getFirst().campo());
    }

    @Test
    void campoDesconhecidoViraAviso() {
        var validacao = motor.validarConfiguracao(configuracao("""
                {"versao": 21, "campo_inventado": true}
                """));

        assertTrue(validacao.valida(), validacao.erros().toString());
        assertFalse(validacao.avisos().isEmpty());
    }
}
