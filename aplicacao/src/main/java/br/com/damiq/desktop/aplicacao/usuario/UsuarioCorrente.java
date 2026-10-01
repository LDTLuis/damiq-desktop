package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.usuario.UsuarioId;

/**
 * Quem está operando o Desktop, para registrar {@code criado_por} e {@code atualizado_por}: o usuário conectado
 * ({@link Sessao}) ou, sem ninguém conectado, {@link UsuarioId#SISTEMA}.
 */
@FunctionalInterface
public interface UsuarioCorrente {

    UsuarioId id();
}
