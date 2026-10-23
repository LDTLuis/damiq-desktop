package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.Permissao;
import br.com.damiq.desktop.dominio.usuario.Usuario;

/**
 * Confere, antes de cada operação, se o usuário conectado pode fazê-la (RF-01): alguém conectado, sem troca de
 * senha pendente, com a permissão no perfil e, quando a operação é sobre uma barragem, que seja a dele.
 */
public final class ControleAcesso {

    private final Sessao sessao;

    public ControleAcesso(Sessao sessao) {
        this.sessao = Validacao.obrigatorio(sessao, "sessão");
    }

    /**
     * @return o usuário conectado
     * @throws AcessoNegadoException se ele não puder fazer a operação
     */
    public Usuario exigir(Permissao permissao) {
        var usuario = sessao.exigirUsuario();
        if (usuario.trocarSenha()) {
            throw new AcessoNegadoException("Troque a senha antes de continuar");
        }
        if (!usuario.permite(permissao)) {
            throw new AcessoNegadoException(
                    "O perfil " + usuario.perfil() + " de " + usuario.login() + " não permite " + permissao);
        }
        return usuario;
    }

    /** Como {@link #exigir(Permissao)}, e a barragem tem de ser a do usuário. */
    public Usuario exigir(Permissao permissao, BarragemId barragem) {
        var usuario = exigir(permissao);
        if (!usuario.barragem().equals(barragem)) {
            throw new AcessoNegadoException(
                    "O usuário " + usuario.login() + " não tem acesso à barragem " + barragem);
        }
        return usuario;
    }
}
