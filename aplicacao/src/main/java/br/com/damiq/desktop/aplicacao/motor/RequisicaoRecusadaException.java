package br.com.damiq.desktop.aplicacao.motor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * O motor recusou a requisição inteira (código de saída 1): configuração inválida, versão do contrato
 * incompatível etc. Leituras com problema não causam isso; elas voltam como {@link Rejeicao}.
 */
public class RequisicaoRecusadaException extends FalhaMotorException {

    private static final long serialVersionUID = 1L;

    private final transient List<MensagemMotor> erros;

    public RequisicaoRecusadaException(String operacao, List<MensagemMotor> erros) {
        super("O motor recusou a operação " + operacao + ": "
                + erros.stream().map(MensagemMotor::toString).collect(Collectors.joining("; ")));
        this.erros = List.copyOf(erros);
    }

    public List<MensagemMotor> erros() {
        return erros;
    }
}
