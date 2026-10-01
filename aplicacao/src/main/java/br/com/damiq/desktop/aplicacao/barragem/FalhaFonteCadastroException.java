package br.com.damiq.desktop.aplicacao.barragem;

/** Não foi possível ler o cadastro publicado pela Central (arquivo ausente, JSON ilegível, Central fora). */
public class FalhaFonteCadastroException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaFonteCadastroException(String mensagem) {
        super(mensagem);
    }

    public FalhaFonteCadastroException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
