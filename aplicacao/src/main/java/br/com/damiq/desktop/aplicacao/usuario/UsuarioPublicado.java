package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import java.util.Optional;

/**
 * Um usuário publicado pela Central: válido, ou recusado com o motivo.
 *
 * @param identificacao o login, se legível; senão a posição na lista (ex.: "item 3")
 * @param login o login, se legível; um cadastro recusado com login conhecido não exclui a cópia existente
 */
public record UsuarioPublicado(String identificacao, Login login, CadastroUsuario cadastro, String erro) {

    public UsuarioPublicado {
        identificacao = Validacao.textoObrigatorio(identificacao, "identificação do usuário");
        if ((cadastro == null) == (erro == null)) {
            throw new IllegalArgumentException("informe o cadastro ou o motivo da recusa");
        }
    }

    public static UsuarioPublicado valido(CadastroUsuario cadastro) {
        return new UsuarioPublicado(cadastro.login().valor(), cadastro.login(), cadastro, null);
    }

    public static UsuarioPublicado recusado(String identificacao, Login login, String erro) {
        return new UsuarioPublicado(identificacao, login, null, Validacao.textoObrigatorio(erro, "motivo"));
    }

    public Optional<CadastroUsuario> valido() {
        return Optional.ofNullable(cadastro);
    }
}
