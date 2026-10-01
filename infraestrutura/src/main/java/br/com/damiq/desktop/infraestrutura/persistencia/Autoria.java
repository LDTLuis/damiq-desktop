package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.usuario.UsuarioCorrente;
import br.com.damiq.desktop.dominio.Validacao;
import java.time.Clock;

/**
 * Valores das colunas de controle ({@code criado_em/por}, {@code atualizado_em/por}): o instante atual e o
 * usuário corrente.
 */
public final class Autoria {

    private final Clock relogio;
    private final UsuarioCorrente usuario;

    public Autoria(Clock relogio, UsuarioCorrente usuario) {
        this.relogio = Validacao.obrigatorio(relogio, "relógio");
        this.usuario = Validacao.obrigatorio(usuario, "usuário corrente");
    }

    /** Instante atual no formato do banco. */
    String agora() {
        return BancoDados.data(relogio.instant());
    }

    long usuario() {
        return usuario.id().valor();
    }
}
