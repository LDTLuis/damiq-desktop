package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.Usuario;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Usuário conectado neste aparelho (um por vez). A barragem dele é o contexto de todas as telas: ela vem daqui,
 * nunca de uma escolha na tela. Sem ninguém conectado (inicialização, tarefas automáticas), as alterações são
 * registradas em nome do usuário "sistema".
 */
public final class Sessao implements UsuarioCorrente {

    private final AtomicReference<Usuario> conectado = new AtomicReference<>();

    void iniciar(Usuario usuario) {
        conectado.set(Validacao.obrigatorio(usuario, "usuário"));
    }

    /** Sai do sistema; não faz nada se ninguém estiver conectado. */
    public void encerrar() {
        conectado.set(null);
    }

    public Optional<Usuario> usuario() {
        return Optional.ofNullable(conectado.get());
    }

    /** @throws AcessoNegadoException se ninguém estiver conectado */
    public Usuario exigirUsuario() {
        return usuario().orElseThrow(() -> new AcessoNegadoException("Nenhum usuário conectado"));
    }

    /** Barragem do usuário conectado. */
    public BarragemId barragem() {
        return exigirUsuario().barragem();
    }

    @Override
    public UsuarioId id() {
        var usuario = conectado.get();
        return usuario != null ? usuario.id() : UsuarioId.SISTEMA;
    }
}
