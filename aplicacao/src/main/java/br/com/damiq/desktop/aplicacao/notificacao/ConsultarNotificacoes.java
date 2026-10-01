package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.util.Comparator;
import java.util.List;

/** Notificações abertas para a interface exibir (RF-06). */
public final class ConsultarNotificacoes {

    private static final Comparator<Notificacao> MAIS_URGENTES_PRIMEIRO = Comparator
            .comparing(Notificacao::severidade).reversed()
            .thenComparing(Notificacao::atualizadaEm, Comparator.reverseOrder());

    private final RepositorioNotificacoes notificacoes;

    public ConsultarNotificacoes(RepositorioNotificacoes notificacoes) {
        this.notificacoes = Validacao.obrigatorio(notificacoes, "repositório de notificações");
    }

    /** Abertas da barragem (ou de todas, se {@code null}), mais graves e mais recentes primeiro. */
    public List<Notificacao> abertas(BarragemId barragem) {
        return notificacoes.abertas(barragem).stream().sorted(MAIS_URGENTES_PRIMEIRO).toList();
    }
}
