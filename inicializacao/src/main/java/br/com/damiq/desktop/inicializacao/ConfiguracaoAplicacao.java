package br.com.damiq.desktop.inicializacao;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.configuracao.AtualizarConfiguracao;
import br.com.damiq.desktop.aplicacao.configuracao.FonteConfiguracao;
import br.com.damiq.desktop.aplicacao.configuracao.RepositorioConfiguracoes;
import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.aplicacao.motor.VerificarCompatibilidadeMotor;
import br.com.damiq.desktop.infraestrutura.configuracao.FonteConfiguracaoArquivo;
import br.com.damiq.desktop.infraestrutura.motor.ConfiguracaoMotor;
import br.com.damiq.desktop.infraestrutura.motor.MotorCalculoProcessBuilder;
import br.com.damiq.desktop.infraestrutura.persistencia.BancoDados;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioBarragensJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioConfiguracoesJdbc;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuração do Spring Context: registra os adapters e os casos de uso.
 *
 * <p>Propriedades (via {@code -D}):
 *
 * <ul>
 *   <li>{@code damiq.motor.python}: Python com o motor instalado (padrão: {@code .motor} no diretório de
 *       trabalho);
 *   <li>{@code damiq.motor.tempo-limite-s}: tempo máximo de uma chamada ao motor (padrão: 60);
 *   <li>{@code damiq.dados.diretorio}: dados do usuário (padrão: {@code %APPDATA%\DAMIQ} no Windows,
 *       {@code ~/.local/share/damiq} no Linux);
 *   <li>{@code damiq.banco.arquivo}: banco SQLite (padrão: {@code <dados>/damiq.db});
 *   <li>{@code damiq.configuracao.diretorio}: arquivos {@code <id da barragem>.json} com a configuração, até a
 *       Central existir (padrão: {@code <dados>/configuracoes}).
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoAplicacao {

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

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
    DataSource bancoDados(Environment ambiente) {
        var arquivo = ambiente.getProperty("damiq.banco.arquivo");
        return BancoDados.abrir(arquivo != null ? Path.of(arquivo) : diretorioDados(ambiente).resolve("damiq.db"));
    }

    @Bean
    RepositorioBarragens repositorioBarragens(DataSource bancoDados, Clock relogio) {
        return new RepositorioBarragensJdbc(bancoDados, relogio);
    }

    @Bean
    RepositorioConfiguracoes repositorioConfiguracoes(DataSource bancoDados) {
        return new RepositorioConfiguracoesJdbc(bancoDados);
    }

    @Bean
    FonteConfiguracao fonteConfiguracao(Environment ambiente) {
        var diretorio = ambiente.getProperty("damiq.configuracao.diretorio");
        return new FonteConfiguracaoArquivo(
                diretorio != null ? Path.of(diretorio) : diretorioDados(ambiente).resolve("configuracoes"));
    }

    @Bean
    VerificarCompatibilidadeMotor verificarCompatibilidadeMotor(MotorCalculo motorCalculo) {
        return new VerificarCompatibilidadeMotor(motorCalculo);
    }

    @Bean
    AtualizarConfiguracao atualizarConfiguracao(
            RepositorioBarragens repositorioBarragens,
            RepositorioConfiguracoes repositorioConfiguracoes,
            FonteConfiguracao fonteConfiguracao,
            MotorCalculo motorCalculo,
            Clock relogio) {
        return new AtualizarConfiguracao(
                repositorioBarragens, repositorioConfiguracoes, fonteConfiguracao, motorCalculo, relogio);
    }

    private static Path diretorioDados(Environment ambiente) {
        var diretorio = ambiente.getProperty("damiq.dados.diretorio");
        return diretorio != null ? Path.of(diretorio) : DiretorioDados.padrao();
    }
}
