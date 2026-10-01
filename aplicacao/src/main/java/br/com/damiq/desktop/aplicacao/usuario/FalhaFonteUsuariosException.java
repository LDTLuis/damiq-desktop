package br.com.damiq.desktop.aplicacao.usuario;

/** Não foi possível ler os usuários publicados pela Central (arquivo ausente, JSON ilegível, Central fora). */
public class FalhaFonteUsuariosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaFonteUsuariosException(String mensagem) {
        super(mensagem);
    }

    public FalhaFonteUsuariosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
