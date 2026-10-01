package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.Validacao;

/** Evento para a interface exibir: uma notificação foi criada ou mudou. */
public record NotificacaoEmitida(Notificacao notificacao, Motivo motivo) {

    public enum Motivo {
        /** Primeiro episódio do problema (ou o primeiro depois do último reconhecimento). */
        NOVA,
        /** Novo episódio mais grave que o já notificado: destacar de novo. */
        ESCALADA,
        /** Novo episódio de mesma severidade ou menor: só soma ocorrências. */
        REPETIDA
    }

    public NotificacaoEmitida {
        Validacao.obrigatorio(notificacao, "notificação");
        Validacao.obrigatorio(motivo, "motivo");
    }
}
