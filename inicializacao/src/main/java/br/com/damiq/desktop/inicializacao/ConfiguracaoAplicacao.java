package br.com.damiq.desktop.inicializacao;

import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.aplicacao.motor.VerificarCompatibilidadeMotor;
import br.com.damiq.desktop.infraestrutura.motor.ConfiguracaoMotor;
import br.com.damiq.desktop.infraestrutura.motor.MotorCalculoProcessBuilder;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuração do Spring Context: registra os adapters e os casos de uso.
 *
 * <p>Propriedades (via {@code -D}): {@code damiq.motor.python}, caminho do Python com o motor instalado
 * (padrão: {@code .motor} no diretório de trabalho); {@code damiq.motor.tempo-limite-s}, tempo máximo de uma
 * chamada (padrão: 60).
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoAplicacao {

    @Bean
    ConfiguracaoMotor configuracaoMotor(Environment ambiente) {
        var python = ambiente.getProperty("damiq.motor.python");
        var caminho = python != null ? Path.of(python) : ConfiguracaoMotor.pythonDoAmbienteLocal(Path.of(""));
        var tempoLimite = ambiente.getProperty("damiq.motor.tempo-limite-s", Long.class, 60L);
        return ConfiguracaoMotor.python(caminho, Duration.ofSeconds(tempoLimite));
    }

    @Bean
    MotorCalculo motorCalculo(ConfiguracaoMotor configuracaoMotor) {
        return new MotorCalculoProcessBuilder(configuracaoMotor);
    }

    @Bean
    VerificarCompatibilidadeMotor verificarCompatibilidadeMotor(MotorCalculo motorCalculo) {
        return new VerificarCompatibilidadeMotor(motorCalculo);
    }
}
