package br.com.damiq.desktop.aplicacao.usuario;

import java.util.List;

/**
 * Usuários publicados pela Central de Configurações: hoje um arquivo local, depois a API da Central. A lista é
 * completa: um usuário ausente foi removido na Central.
 */
public interface FonteCadastroUsuarios {

    /**
     * @return um item por usuário publicado, válido ou recusado
     * @throws FalhaFonteUsuariosException se a publicação não puder ser lida (nada deve ser alterado)
     */
    List<UsuarioPublicado> buscar();
}
