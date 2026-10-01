package br.com.damiq.desktop.inicializacao;

import br.com.damiq.desktop.aplicacao.medicao.MedicoesProcessadas;
import br.com.damiq.desktop.aplicacao.notificacao.NotificarAlertas;
import org.springframework.context.event.EventListener;

/** Gera as notificações logo depois de cada processamento de medições. */
public class OuvinteNotificacoes {

    private final NotificarAlertas notificarAlertas;

    OuvinteNotificacoes(NotificarAlertas notificarAlertas) {
        this.notificarAlertas = notificarAlertas;
    }

    @EventListener
    public void aoProcessar(MedicoesProcessadas evento) {
        if (evento.alertas() > 0) {
            notificarAlertas.executar(evento.barragem());
        }
    }
}
