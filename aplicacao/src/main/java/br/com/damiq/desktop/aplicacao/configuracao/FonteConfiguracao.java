package br.com.damiq.desktop.aplicacao.configuracao;

import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;

/** De onde vêm as configurações publicadas: hoje um arquivo local, depois a API da Central. */
public interface FonteConfiguracao {

    /**
     * A última configuração publicada para a barragem.
     *
     * @throws FalhaFonteConfiguracaoException se a configuração não puder ser obtida ou não tiver versão
     */
    Configuracao buscar(BarragemId barragem);

    OrigemConfiguracao origem();
}
