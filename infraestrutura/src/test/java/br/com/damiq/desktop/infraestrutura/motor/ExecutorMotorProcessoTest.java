package br.com.damiq.desktop.infraestrutura.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Testa o executor com um motor falso em Java, para não depender do Python. */
class ExecutorMotorProcessoTest {

    private static final String JAVA = ProcessHandle.current().info().command().orElseThrow();
    private static final String MOTOR_FALSO = Path.of("src/test/motor-falso/MotorFalso.java").toAbsolutePath().toString();

    private static ExecutorMotorProcesso executor(String modo, Duration tempoLimite) {
        var comando = List.of(JAVA, "-Dstderr.encoding=UTF-8", MOTOR_FALSO, modo);
        return new ExecutorMotorProcesso(new ConfiguracaoMotor(comando, tempoLimite));
    }

    @Test
    void enviaERecebeJsonEmUtf8() {
        var requisicao = "{\"barragem\": \"João Leite\", \"unidade\": \"m³/s\"}";

        var execucao = executor("eco", Duration.ofSeconds(60)).executar(requisicao);

        assertEquals(0, execucao.codigoSaida());
        assertEquals(requisicao, execucao.resposta());
    }

    @Test
    void erroInternoDevolveCodigo2EStderr() {
        var execucao = executor("erro-interno", Duration.ofSeconds(60)).executar("{}");

        assertEquals(2, execucao.codigoSaida());
        assertTrue(execucao.stderr().contains("falha simulada na medição de pressão"), execucao.stderr());
    }

    @Test
    void semArquivoDeSaidaARespostaFicaVazia() {
        assertEquals("", executor("sem-resposta", Duration.ofSeconds(60)).executar("{}").resposta());
    }

    @Test
    void encerraOProcessoAoPassarDoTempoLimite() {
        var inicio = System.nanoTime();

        var falha = assertThrows(
                FalhaMotorException.class, () -> executor("dorme", Duration.ofSeconds(3)).executar("{}"));

        assertTrue(falha.getMessage().contains("não respondeu"), falha.getMessage());
        assertTrue(Duration.ofNanos(System.nanoTime() - inicio).toSeconds() < 30);
    }

    @Test
    void comandoInexistente() {
        var configuracao = new ConfiguracaoMotor(List.of("comando-que-nao-existe-damiq"), Duration.ofSeconds(5));

        var falha = assertThrows(
                FalhaMotorException.class, () -> new ExecutorMotorProcesso(configuracao).executar("{}"));

        assertTrue(falha.getMessage().startsWith("Não foi possível iniciar o motor"), falha.getMessage());
    }
}
