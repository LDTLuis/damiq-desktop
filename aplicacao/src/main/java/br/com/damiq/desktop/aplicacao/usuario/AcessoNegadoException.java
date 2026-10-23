package br.com.damiq.desktop.aplicacao.usuario;

/**
 * O usuário conectado não pode fazer a operação: ninguém conectado, troca de senha pendente, perfil sem a
 * permissão ou dado de outra barragem.
 */
public class AcessoNegadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
