package br.com.damiq.desktop.infraestrutura.motor;

import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.aplicacao.motor.InfoMotor;
import br.com.damiq.desktop.aplicacao.motor.LoteMedicoes;
import br.com.damiq.desktop.aplicacao.motor.MensagemMotor;
import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.aplicacao.motor.RequisicaoRecusadaException;
import br.com.damiq.desktop.aplicacao.motor.ResultadoLote;
import br.com.damiq.desktop.aplicacao.motor.ValidacaoConfiguracao;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adapter do {@link MotorCalculo}: chama o motor Python como processo filho.
 *
 * <p>Códigos de saída do motor: 0 sucesso, 1 entrada recusada (motivos em {@code erros[]}), 2 erro interno
 * (traceback no stderr, que vai para o log).
 */
public final class MotorCalculoProcessBuilder implements MotorCalculo {

    static final String VERSAO_CONTRATO = "1.0";

    private static final Logger LOG = LoggerFactory.getLogger(MotorCalculoProcessBuilder.class);

    // Versões 1.x do contrato podem acrescentar campos: os desconhecidos são ignorados
    private static final JsonMapper JSON = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final ExecutorMotor executor;

    public MotorCalculoProcessBuilder(ConfiguracaoMotor configuracao) {
        this(new ExecutorMotorProcesso(configuracao));
    }

    MotorCalculoProcessBuilder(ExecutorMotor executor) {
        this.executor = Validacao.obrigatorio(executor, "executor do motor");
    }

    @Override
    public InfoMotor info() {
        var resposta = chamarExigindoSucesso(RequisicaoMotor.de("info"));
        if (resposta.motor() == null) {
            throw new FalhaMotorException("Resposta de info sem o campo 'motor'");
        }
        return new InfoMotor(resposta.motor().versao(), resposta.versaoContrato(), resposta.motor().operacoes());
    }

    @Override
    public ValidacaoConfiguracao validarConfiguracao(Configuracao configuracao) {
        JsonNode conteudo;
        try {
            conteudo = JSON.readTree(configuracao.conteudoJson());
        } catch (JacksonException e) {
            var erro = new MensagemMotor(
                    "JSON_INVALIDO", "A configuração não é um JSON válido: " + e.getOriginalMessage(), "configuracao");
            return new ValidacaoConfiguracao(List.of(erro), List.of());
        }
        var resposta = chamar(new RequisicaoMotor(
                VERSAO_CONTRATO, "validar_configuracao", null, conteudo, null, null, null));
        if (!resposta.ok() && resposta.erros().isEmpty()) {
            throw new FalhaMotorException("O motor recusou a configuração sem informar o motivo");
        }
        return new ValidacaoConfiguracao(
                TraducaoContrato.mensagens(resposta.erros()), TraducaoContrato.mensagens(resposta.avisos()));
    }

    @Override
    public ResultadoLote processarLote(LoteMedicoes lote) {
        JsonNode configuracao;
        try {
            configuracao = JSON.readTree(lote.configuracao().conteudoJson());
        } catch (JacksonException e) {
            throw new FalhaMotorException("A configuração " + lote.configuracao().versao()
                    + " gravada não é um JSON válido: " + e.getOriginalMessage(), e);
        }
        var requisicao = new RequisicaoMotor(
                VERSAO_CONTRATO,
                "processar_lote",
                Map.of("id", lote.barragem().valor()),
                configuracao,
                new RequisicaoMotor.Opcoes(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(lote.agora())),
                lote.leituras().stream().map(TraducaoContrato::leitura).toList(),
                lote.historico().stream().map(TraducaoContrato::leitura).toList());

        return TraducaoContrato.resultadoLote(chamarExigindoSucesso(requisicao));
    }

    /** A resposta de uma operação que não pode ser recusada pelo motor (saída 1 vira exceção). */
    private RespostaMotor chamarExigindoSucesso(RequisicaoMotor requisicao) {
        var resposta = chamar(requisicao);
        if (!resposta.ok()) {
            throw new RequisicaoRecusadaException(
                    requisicao.operacao(), TraducaoContrato.mensagens(resposta.erros()));
        }
        return resposta;
    }

    /** Executa a requisição e devolve a resposta de uma saída 0 ou 1; as demais viram {@link FalhaMotorException}. */
    private RespostaMotor chamar(RequisicaoMotor requisicao) {
        var operacao = requisicao.operacao();
        var execucao = executor.executar(JSON.writeValueAsString(requisicao));
        var codigo = execucao.codigoSaida();

        if (codigo == 2) {
            LOG.error("Erro interno do motor na operação {}:\n{}", operacao, execucao.stderr());
            throw new FalhaMotorException("Erro interno do motor na operação " + operacao + " (detalhes no log)");
        }
        if (codigo != 0 && codigo != 1) {
            LOG.error("Motor terminou com código {} na operação {}:\n{}", codigo, operacao, execucao.stderr());
            throw new FalhaMotorException("O motor terminou com o código inesperado " + codigo);
        }
        if (!execucao.stderr().isBlank()) {
            LOG.warn("Saída de erro do motor na operação {}:\n{}", operacao, execucao.stderr());
        }

        if (execucao.resposta().isBlank()) {
            throw new FalhaMotorException("O motor não escreveu resposta na operação " + operacao);
        }
        RespostaMotor resposta;
        try {
            resposta = JSON.readValue(execucao.resposta(), RespostaMotor.class);
        } catch (JacksonException e) {
            throw new FalhaMotorException(
                    "Resposta ilegível do motor na operação " + operacao + ": " + e.getOriginalMessage(), e);
        }
        if (resposta.ok() != (codigo == 0)) {
            throw new FalhaMotorException(
                    "Resposta inconsistente do motor: status " + resposta.status() + " com código de saída " + codigo);
        }
        return resposta;
    }
}
