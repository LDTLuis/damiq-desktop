package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.Usuario;

/** Um usuário com o hash da senha, só para conferir o acesso; não sai da camada de aplicação. */
public record Credencial(Usuario usuario, SenhaHash senhaHash) {

    public Credencial {
        Validacao.obrigatorio(usuario, "usuário");
        Validacao.obrigatorio(senhaHash, "hash da senha");
    }
}
