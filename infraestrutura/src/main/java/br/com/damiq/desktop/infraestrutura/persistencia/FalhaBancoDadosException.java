package br.com.damiq.desktop.infraestrutura.persistencia;

/** Erro de acesso ao banco local (SQLite). */
public class FalhaBancoDadosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaBancoDadosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
