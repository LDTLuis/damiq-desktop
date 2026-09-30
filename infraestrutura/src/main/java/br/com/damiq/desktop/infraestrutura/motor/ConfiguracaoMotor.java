package br.com.damiq.desktop.infraestrutura.motor;

import br.com.damiq.desktop.dominio.Validacao;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Como executar o motor.
 *
 * @param comando comando que inicia o motor, sem os argumentos {@code --entrada}/{@code --saida} (ex.:
 *     {@code [python, -m, damiq_calc]}). Com o empacotamento (issue #14 do motor), passa a ser o executável
 * @param tempoLimite tempo máximo de uma chamada; depois disso o processo é encerrado
 */
public record ConfiguracaoMotor(List<String> comando, Duration tempoLimite) {

    public ConfiguracaoMotor {
        comando = List.copyOf(Validacao.obrigatorio(comando, "comando do motor"));
        if (comando.isEmpty()) {
            throw new IllegalArgumentException("comando do motor não pode ser vazio");
        }
        Validacao.obrigatorio(tempoLimite, "tempo limite do motor");
        if (tempoLimite.isNegative() || tempoLimite.isZero()) {
            throw new IllegalArgumentException("tempo limite do motor deve ser positivo: " + tempoLimite);
        }
    }

    /** Motor instalado como pacote Python ({@code python -m damiq_calc}). */
    public static ConfiguracaoMotor python(Path python, Duration tempoLimite) {
        return new ConfiguracaoMotor(List.of(python.toString(), "-m", "damiq_calc"), tempoLimite);
    }

    /** Python do ambiente {@code .motor} criado na raiz do repositório (ver README). */
    public static Path pythonDoAmbienteLocal(Path raiz) {
        var windows = System.getProperty("os.name", "").startsWith("Windows");
        return windows
                ? raiz.resolve(".motor").resolve("Scripts").resolve("python.exe")
                : raiz.resolve(".motor").resolve("bin").resolve("python");
    }
}
