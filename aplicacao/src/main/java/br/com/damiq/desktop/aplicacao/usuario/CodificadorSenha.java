package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.usuario.SenhaHash;

/**
 * Calcula e confere o hash das senhas (bcrypt), sempre neste aparelho, para que o acesso funcione sem rede. As
 * senhas chegam como {@code char[]} para que quem as digitou possa apagá-las da memória depois.
 */
public interface CodificadorSenha {

    SenhaHash codificar(char[] senha);

    /** @return {@code false} também quando o hash está num formato desconhecido */
    boolean confere(char[] senha, SenhaHash hash);
}
