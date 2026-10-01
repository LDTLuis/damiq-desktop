package br.com.damiq.desktop.aplicacao.configuracao;

import br.com.damiq.desktop.dominio.barragem.BarragemId;

/** A barragem ainda não tem configuração vigente: é preciso atualizá-la antes de processar medições. */
public class ConfiguracaoAusenteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfiguracaoAusenteException(BarragemId barragem) {
        super("A barragem " + barragem + " não tem configuração vigente; atualize a configuração antes");
    }
}
