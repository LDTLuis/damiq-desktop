package br.com.damiq.desktop.infraestrutura.motor;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Motor real para testes de integração: o Python de {@code DAMIQ_MOTOR_PYTHON} ou do {@code .motor} na raiz.
 * Sem o motor, o teste é pulado, a menos que {@code DAMIQ_MOTOR_OBRIGATORIO=true} (CI).
 */
public final class MotorInstalado {

    private MotorInstalado() {}

    public static MotorCalculoProcessBuilder exigir() {
        var variavel = System.getenv("DAMIQ_MOTOR_PYTHON");
        var python = variavel != null && !variavel.isBlank()
                ? Path.of(variavel)
                : ConfiguracaoMotor.pythonDoAmbienteLocal(Path.of(System.getProperty("damiq.raiz", "..")));
        var instalado = Files.isExecutable(python);
        if (!instalado && Boolean.parseBoolean(System.getenv("DAMIQ_MOTOR_OBRIGATORIO"))) {
            throw new IllegalStateException("Motor de cálculo não encontrado em " + python);
        }
        assumeTrue(instalado, "Motor de cálculo não instalado em " + python);
        return new MotorCalculoProcessBuilder(ConfiguracaoMotor.python(python, Duration.ofSeconds(60)));
    }
}
