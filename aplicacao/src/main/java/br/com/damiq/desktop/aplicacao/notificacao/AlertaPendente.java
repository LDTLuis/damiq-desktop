package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;

/**
 * Alerta gravado que ainda não gerou notificação.
 *
 * @param nivelResposta nível de resposta do PAE do processamento que gerou o alerta
 */
public record AlertaPendente(long id, Alerta alerta, int nivelResposta) {

    public AlertaPendente {
        Validacao.obrigatorio(alerta, "alerta");
    }
}
