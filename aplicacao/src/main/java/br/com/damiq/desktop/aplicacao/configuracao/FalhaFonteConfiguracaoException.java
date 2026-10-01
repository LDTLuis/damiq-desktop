package br.com.damiq.desktop.aplicacao.configuracao;

/** Não foi possível obter a configuração da fonte (arquivo ausente, JSON ilegível, sem versão, Central fora). */
public class FalhaFonteConfiguracaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaFonteConfiguracaoException(String mensagem) {
        super(mensagem);
    }

    public FalhaFonteConfiguracaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
