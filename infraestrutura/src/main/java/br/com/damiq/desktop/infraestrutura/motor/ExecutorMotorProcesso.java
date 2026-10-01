package br.com.damiq.desktop.infraestrutura.motor;

import static java.nio.charset.StandardCharsets.UTF_8;

import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.dominio.Validacao;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executa o motor como processo filho. Requisição, resposta e stderr passam por arquivos UTF-8 num diretório
 * temporário, apagado ao final; assim nenhum pipe enche e bloqueia o processo.
 */
final class ExecutorMotorProcesso implements ExecutorMotor {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorMotorProcesso.class);

    private final ConfiguracaoMotor configuracao;

    ExecutorMotorProcesso(ConfiguracaoMotor configuracao) {
        this.configuracao = Validacao.obrigatorio(configuracao, "configuração do motor");
    }

    @Override
    public ExecucaoMotor executar(String requisicaoJson) {
        Path diretorio = null;
        try {
            diretorio = Files.createTempDirectory("damiq-motor-");
            var entrada = diretorio.resolve("requisicao.json");
            var saida = diretorio.resolve("resposta.json");
            var erros = diretorio.resolve("stderr.txt");
            Files.writeString(entrada, requisicaoJson, UTF_8);

            var comando = new ArrayList<>(configuracao.comando());
            comando.addAll(List.of("--entrada", entrada.toString(), "--saida", saida.toString()));
            var construtor = new ProcessBuilder(comando)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(erros.toFile());
            // traceback do Python em UTF-8, qualquer que seja a página de código do sistema
            construtor.environment().put("PYTHONUTF8", "1");
            construtor.environment().put("PYTHONIOENCODING", "utf-8");

            var codigo = aguardar(iniciar(construtor), comando);
            var resposta = Files.exists(saida) ? Files.readString(saida, UTF_8) : "";
            return new ExecucaoMotor(codigo, resposta, new String(Files.readAllBytes(erros), UTF_8));
        } catch (IOException e) {
            throw new FalhaMotorException("Falha de E/S ao executar o motor: " + e.getMessage(), e);
        } finally {
            apagar(diretorio);
        }
    }

    private static Process iniciar(ProcessBuilder construtor) {
        try {
            var processo = construtor.start();
            processo.getOutputStream().close(); // o motor lê de --entrada, não do stdin
            return processo;
        } catch (IOException e) {
            throw new FalhaMotorException(
                    "Não foi possível iniciar o motor de cálculo (" + String.join(" ", construtor.command())
                            + "): " + e.getMessage(),
                    e);
        }
    }

    private int aguardar(Process processo, List<String> comando) {
        var limite = configuracao.tempoLimite();
        try {
            if (processo.waitFor(limite.toMillis(), TimeUnit.MILLISECONDS)) {
                return processo.exitValue();
            }
            encerrar(processo);
            throw new FalhaMotorException("O motor de cálculo não respondeu em " + limite.toSeconds() + " s");
        } catch (InterruptedException e) {
            encerrar(processo);
            Thread.currentThread().interrupt();
            throw new FalhaMotorException("Chamada ao motor interrompida: " + String.join(" ", comando), e);
        }
    }

    /** Encerra o processo e os filhos (no Windows, o python do venv inicia outro processo). */
    private static void encerrar(Process processo) {
        processo.descendants().forEach(ProcessHandle::destroyForcibly);
        processo.destroyForcibly();
        try {
            processo.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void apagar(Path diretorio) {
        if (diretorio == null) {
            return;
        }
        try (var caminhos = Files.walk(diretorio)) {
            for (var caminho : caminhos.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(caminho);
            }
        } catch (IOException e) {
            LOG.warn("Não foi possível apagar o diretório temporário do motor {}: {}", diretorio, e.getMessage());
        }
    }
}
