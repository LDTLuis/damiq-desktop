package br.com.damiq.desktop.aplicacao.motor;

/**
 * O motor não pôde ser executado ou não devolveu uma resposta utilizável: executável ausente, tempo esgotado,
 * erro interno (código de saída 2), resposta ilegível ou versão incompatível.
 */
public class FalhaMotorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaMotorException(String mensagem) {
        super(mensagem);
    }

    public FalhaMotorException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
