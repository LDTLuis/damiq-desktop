package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Notificações de alerta e os alertas que ainda não foram notificados. */
public interface RepositorioNotificacoes {

    /** Barragens com alertas gravados ainda sem notificação. */
    List<BarragemId> barragensComAlertasPendentes();

    /** Alertas da barragem ainda sem notificação, na ordem em que foram gravados. */
    List<AlertaPendente> alertasPendentes(BarragemId barragem);

    /** Notificações não reconhecidas; todas as barragens se {@code barragem} for {@code null}. */
    List<Notificacao> abertas(BarragemId barragem);

    Optional<Notificacao> buscar(long id);

    /**
     * Grava as notificações (inserindo as sem id) e vincula cada alerta à sua, numa única transação.
     *
     * @return as notificações gravadas, com id, na mesma ordem
     */
    List<Notificacao> gravar(List<Gravacao> gravacoes);

    /**
     * Registra o reconhecimento se a notificação ainda estiver aberta.
     *
     * @return {@code false} se ela não existir ou já tiver sido reconhecida
     */
    boolean reconhecer(long id, String por, String observacao, Instant em);

    /**
     * @param alertas ids dos alertas que a notificação passa a cobrir
     */
    record Gravacao(Notificacao notificacao, List<Long> alertas) {

        public Gravacao {
            Validacao.obrigatorio(notificacao, "notificação");
            alertas = List.copyOf(Validacao.obrigatorio(alertas, "alertas da notificação"));
        }
    }
}
