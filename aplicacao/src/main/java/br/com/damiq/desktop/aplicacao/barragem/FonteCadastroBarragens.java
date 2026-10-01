package br.com.damiq.desktop.aplicacao.barragem;

import java.util.List;

/**
 * Cadastro das barragens publicado pela Central de Configurações: hoje um arquivo local, depois a API da
 * Central. A lista é completa: uma barragem ausente foi removida na Central.
 */
public interface FonteCadastroBarragens {

    /**
     * @return um item por barragem publicada, válido ou recusado
     * @throws FalhaFonteCadastroException se a publicação não puder ser lida (nada deve ser alterado)
     */
    List<CadastroPublicado> buscar();
}
