package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.Permissao;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O administrador da barragem desbloqueia, neste aparelho, um usuário dela bloqueado por tentativas
 * malsucedidas, sem depender da rede. Na Central, o mesmo efeito vem de republicar o cadastro do usuário.
 */
public final class DesbloquearUsuario {

    private static final Logger LOG = LoggerFactory.getLogger(DesbloquearUsuario.class);

    private final RepositorioUsuarios usuarios;
    private final ControleAcesso controle;

    public DesbloquearUsuario(RepositorioUsuarios usuarios, ControleAcesso controle) {
        this.usuarios = Validacao.obrigatorio(usuarios, "repositório de usuários");
        this.controle = Validacao.obrigatorio(controle, "controle de acesso");
    }

    /**
     * @throws AcessoNegadoException se o conectado não for administrador ou o usuário for de outra barragem
     * @throws IllegalArgumentException se o usuário não existir
     */
    public void executar(UsuarioId id) {
        var administrador = controle.exigir(Permissao.DESBLOQUEAR_USUARIOS);
        var usuario = usuarios.buscar(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
        controle.exigir(Permissao.DESBLOQUEAR_USUARIOS, usuario.barragem());
        usuarios.desbloquear(id);
        LOG.warn("Usuário {} desbloqueado por {}", usuario.login(), administrador.login());
    }
}
