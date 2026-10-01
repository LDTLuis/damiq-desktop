package br.com.damiq.desktop.inicializacao;

import br.com.damiq.desktop.aplicacao.barragem.ConsultarBarragens;
import br.com.damiq.desktop.aplicacao.barragem.ExcluirBarragem;
import br.com.damiq.desktop.aplicacao.barragem.FonteCadastroBarragens;
import br.com.damiq.desktop.aplicacao.barragem.SincronizarBarragens;
import br.com.damiq.desktop.infraestrutura.barragem.FonteCadastroBarragensArquivo;
import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.manutencao.LimparDadosTeste;
import br.com.damiq.desktop.aplicacao.usuario.UsuarioCorrente;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import br.com.damiq.desktop.infraestrutura.persistencia.Autoria;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioDadosTesteJdbc;
import br.com.damiq.desktop.aplicacao.configuracao.AtualizarConfiguracao;
import br.com.damiq.desktop.aplicacao.configuracao.FonteConfiguracao;
import br.com.damiq.desktop.aplicacao.configuracao.RepositorioConfiguracoes;
import br.com.damiq.desktop.aplicacao.evento.PublicadorEventos;
import br.com.damiq.desktop.aplicacao.importacao.ImportarMedicoes;
import br.com.damiq.desktop.aplicacao.importacao.LeitorArquivoLeituras;
import br.com.damiq.desktop.aplicacao.medicao.ProcessarMedicoes;
import br.com.damiq.desktop.aplicacao.medicao.RepositorioMedicoes;
import br.com.damiq.desktop.aplicacao.medicao.RepositorioProcessamentos;
import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.aplicacao.notificacao.ConsultarAcionamento;
import br.com.damiq.desktop.aplicacao.notificacao.ConsultarNotificacoes;
import br.com.damiq.desktop.aplicacao.notificacao.NotificarAlertas;
import br.com.damiq.desktop.aplicacao.notificacao.ReconhecerNotificacao;
import br.com.damiq.desktop.aplicacao.notificacao.RepositorioNotificacoes;
import br.com.damiq.desktop.aplicacao.motor.VerificarCompatibilidadeMotor;
import br.com.damiq.desktop.infraestrutura.configuracao.FonteConfiguracaoArquivo;
import br.com.damiq.desktop.infraestrutura.importacao.LeitorArquivoLeiturasPadrao;
import br.com.damiq.desktop.infraestrutura.motor.ConfiguracaoMotor;
import br.com.damiq.desktop.infraestrutura.motor.MotorCalculoProcessBuilder;
import br.com.damiq.desktop.infraestrutura.persistencia.BancoDados;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioBarragensJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioConfiguracoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioMedicoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioNotificacoesJdbc;
import br.com.damiq.desktop.infraestrutura.persistencia.RepositorioProcessamentosJdbc;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.context.ApplicationEventPublisher;
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
 *   <li>{@code damiq.cadastro.arquivo}: cadastro das barragens publicado pela Central, até a API existir
 *       (padrão: {@code <dados>/cadastro/barragens.json});
 *   <li>{@code damiq.configuracao.diretorio}: arquivos {@code <id da barragem>.json} com a configuração, até a
 *       Central existir (padrão: {@code <dados>/configuracoes});
 *   <li>{@code damiq.motor.historico-por-instrumento}: leituras anteriores enviadas ao motor por instrumento;
 *       precisa cobrir {@code anomalia.janela_leituras} e {@code sensor_travado.leituras_consecutivas} da
 *       configuração (padrão: 48).
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoAplicacao {

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

    /** Eventos da aplicação pelo Spring Events; a interface (JavaFX) os ouve com {@code @EventListener}. */
    @Bean
    PublicadorEventos publicadorEventos(ApplicationEventPublisher publicador) {
        return publicador::publishEvent;
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

    /** Até a autenticação (RF-01), toda alteração é do usuário "sistema". */
    @Bean
    UsuarioCorrente usuarioCorrente() {
        return () -> UsuarioId.SISTEMA;
    }

    @Bean
    Autoria autoria(Clock relogio, UsuarioCorrente usuarioCorrente) {
        return new Autoria(relogio, usuarioCorrente);
    }

    @Bean
    RepositorioBarragens repositorioBarragens(DataSource bancoDados, Autoria autoria) {
        return new RepositorioBarragensJdbc(bancoDados, autoria);
    }

    @Bean
    RepositorioConfiguracoes repositorioConfiguracoes(DataSource bancoDados, Autoria autoria) {
        return new RepositorioConfiguracoesJdbc(bancoDados, autoria);
    }

    @Bean
    ExcluirBarragem excluirBarragem(RepositorioBarragens repositorioBarragens) {
        return new ExcluirBarragem(repositorioBarragens);
    }

    @Bean
    FonteCadastroBarragens fonteCadastroBarragens(Environment ambiente) {
        var arquivo = ambiente.getProperty("damiq.cadastro.arquivo");
        return new FonteCadastroBarragensArquivo(
                arquivo != null ? Path.of(arquivo) : diretorioDados(ambiente).resolve("cadastro").resolve("barragens.json"));
    }

    @Bean
    SincronizarBarragens sincronizarBarragens(
            FonteCadastroBarragens fonteCadastroBarragens, RepositorioBarragens repositorioBarragens) {
        return new SincronizarBarragens(fonteCadastroBarragens, repositorioBarragens);
    }

    @Bean
    ConsultarBarragens consultarBarragens(RepositorioBarragens repositorioBarragens) {
        return new ConsultarBarragens(repositorioBarragens);
    }

    @Bean
    LimparDadosTeste limparDadosTeste(DataSource bancoDados) {
        return new LimparDadosTeste(new RepositorioDadosTesteJdbc(bancoDados));
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

    @Bean
    RepositorioMedicoes repositorioMedicoes(DataSource bancoDados) {
        return new RepositorioMedicoesJdbc(bancoDados);
    }

    @Bean
    RepositorioProcessamentos repositorioProcessamentos(DataSource bancoDados, Autoria autoria) {
        return new RepositorioProcessamentosJdbc(bancoDados, autoria);
    }

    @Bean
    ProcessarMedicoes processarMedicoes(
            RepositorioBarragens repositorioBarragens,
            RepositorioConfiguracoes repositorioConfiguracoes,
            RepositorioMedicoes repositorioMedicoes,
            RepositorioProcessamentos repositorioProcessamentos,
            MotorCalculo motorCalculo,
            PublicadorEventos publicadorEventos,
            Clock relogio,
            Environment ambiente) {
        return new ProcessarMedicoes(
                repositorioBarragens,
                repositorioConfiguracoes,
                repositorioMedicoes,
                repositorioProcessamentos,
                motorCalculo,
                publicadorEventos,
                relogio,
                ambiente.getProperty("damiq.motor.historico-por-instrumento", Integer.class, 48));
    }

    @Bean
    LeitorArquivoLeituras leitorArquivoLeituras() {
        return new LeitorArquivoLeiturasPadrao();
    }

    @Bean
    ImportarMedicoes importarMedicoes(LeitorArquivoLeituras leitorArquivoLeituras, ProcessarMedicoes processarMedicoes) {
        return new ImportarMedicoes(leitorArquivoLeituras, processarMedicoes);
    }

    @Bean
    RepositorioNotificacoes repositorioNotificacoes(DataSource bancoDados, Autoria autoria) {
        return new RepositorioNotificacoesJdbc(bancoDados, autoria);
    }

    @Bean
    NotificarAlertas notificarAlertas(
            RepositorioNotificacoes repositorioNotificacoes, PublicadorEventos publicadorEventos, Clock relogio) {
        return new NotificarAlertas(repositorioNotificacoes, publicadorEventos, relogio);
    }

    @Bean
    ConsultarNotificacoes consultarNotificacoes(RepositorioNotificacoes repositorioNotificacoes) {
        return new ConsultarNotificacoes(repositorioNotificacoes);
    }

    @Bean
    ConsultarAcionamento consultarAcionamento(RepositorioBarragens repositorioBarragens) {
        return new ConsultarAcionamento(repositorioBarragens);
    }

    @Bean
    ReconhecerNotificacao reconhecerNotificacao(
            RepositorioNotificacoes repositorioNotificacoes, PublicadorEventos publicadorEventos, Clock relogio) {
        return new ReconhecerNotificacao(repositorioNotificacoes, publicadorEventos, relogio);
    }

    @Bean
    OuvinteNotificacoes ouvinteNotificacoes(NotificarAlertas notificarAlertas) {
        return new OuvinteNotificacoes(notificarAlertas);
    }

    private static Path diretorioDados(Environment ambiente) {
        var diretorio = ambiente.getProperty("damiq.dados.diretorio");
        return diretorio != null ? Path.of(diretorio) : DiretorioDados.padrao();
    }
}
