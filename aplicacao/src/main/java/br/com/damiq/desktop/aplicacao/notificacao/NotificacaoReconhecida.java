package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.Validacao;

/** Evento: uma notificação foi reconhecida e sai da lista de abertas. */
public record NotificacaoReconhecida(Notificacao notificacao) {

    public NotificacaoReconhecida {
        Validacao.obrigatorio(notificacao, "notificação");
    }
}
