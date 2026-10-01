package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.aplicacao.evento.PublicadorEventos;
import br.com.damiq.desktop.aplicacao.notificacao.NotificacaoEmitida.Motivo;
import br.com.damiq.desktop.aplicacao.notificacao.RepositorioNotificacoes.Gravacao;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gera as notificações dos alertas gravados que ainda não foram notificados (RF-06).
 *
 * <p>Regra: enquanto houver notificação aberta para o mesmo problema (barragem, instrumento e tipo de
 * alerta), novos episódios entram nela como ocorrências; se forem mais graves, ela é escalada. Depois de
 * reconhecida, o próximo episódio abre uma notificação nova. Assim um problema que persiste por vários
 * lotes não gera uma notificação por lote, mas um agravamento nunca passa despercebido.
 *
 * <p>É idempotente: só trata alertas sem notificação. Roda depois de cada processamento e na inicialização,
 * para recuperar alertas gravados sem notificação (ex.: o app fechou no meio).
 */
public final class NotificarAlertas {

    private static final Logger LOG = LoggerFactory.getLogger(NotificarAlertas.class);

    private final RepositorioNotificacoes notificacoes;
    private final PublicadorEventos eventos;
    private final Clock relogio;

    public NotificarAlertas(RepositorioNotificacoes notificacoes, PublicadorEventos eventos, Clock relogio) {
        this.notificacoes = Validacao.obrigatorio(notificacoes, "repositório de notificações");
        this.eventos = Validacao.obrigatorio(eventos, "publicador de eventos");
        this.relogio = Validacao.obrigatorio(relogio, "relógio");
    }

    /** Notifica os alertas pendentes de todas as barragens. */
    public List<NotificacaoEmitida> executarPendentes() {
        var emitidas = new ArrayList<NotificacaoEmitida>();
        notificacoes.barragensComAlertasPendentes().forEach(barragem -> emitidas.addAll(executar(barragem)));
        return emitidas;
    }

    /** Notifica os alertas pendentes da barragem e publica um {@link NotificacaoEmitida} por notificação. */
    public List<NotificacaoEmitida> executar(BarragemId barragem) {
        var pendentes = notificacoes.alertasPendentes(barragem);
        if (pendentes.isEmpty()) {
            return List.of();
        }
        var agora = relogio.instant();

        var abertas = new HashMap<Chave, Notificacao>();
        notificacoes.abertas(barragem).forEach(n -> abertas.put(new Chave(n.instrumento(), n.tipoAlerta()), n));

        // estado final de cada notificação tocada neste lote, na ordem do primeiro alerta
        var alteradas = new LinkedHashMap<Chave, Alteracao>();
        for (var pendente : pendentes) {
            var alerta = pendente.alerta();
            var chave = new Chave(alerta.instrumento(), alerta.tipo());
            var anterior = alteradas.get(chave);
            var atual = anterior != null ? anterior.notificacao() : abertas.get(chave);

            Alteracao alteracao;
            if (atual == null) {
                alteracao = new Alteracao(
                        Notificacao.de(barragem, alerta, pendente.nivelResposta(), agora), new ArrayList<>(), Motivo.NOVA);
            } else {
                var escalou = alerta.severidade().maisGraveQue(atual.severidade());
                var motivo = anterior != null && (anterior.motivo() == Motivo.NOVA || !escalou)
                        ? anterior.motivo()
                        : escalou ? Motivo.ESCALADA : Motivo.REPETIDA;
                alteracao = new Alteracao(atual.comOcorrencia(alerta, pendente.nivelResposta(), agora),
                        anterior != null ? anterior.alertas() : new ArrayList<>(), motivo);
            }
            alteracao.alertas().add(pendente.id());
            alteradas.put(chave, alteracao);
        }

        var gravadas = notificacoes.gravar(alteradas.values().stream()
                .map(a -> new Gravacao(a.notificacao(), a.alertas()))
                .toList());

        var motivos = alteradas.values().stream().map(Alteracao::motivo).toList();
        var emitidas = new ArrayList<NotificacaoEmitida>();
        for (int i = 0; i < gravadas.size(); i++) {
            var emitida = new NotificacaoEmitida(gravadas.get(i), motivos.get(i));
            registrarNoLog(emitida);
            eventos.publicar(emitida);
            emitidas.add(emitida);
        }
        return emitidas;
    }

    private static void registrarNoLog(NotificacaoEmitida emitida) {
        var n = emitida.notificacao();
        LOG.warn("Notificação {} ({}) da barragem {}: {} {} em {} — {}{}",
                n.id(), emitida.motivo(), n.barragem(), n.severidade(), n.tipoAlerta(), n.instrumento(),
                n.mensagem(), n.leituraSuspeita() ? " [leitura suspeita: verificar o instrumento]" : "");
    }

    private record Chave(CodigoInstrumento instrumento, TipoAlerta tipo) {}

    private record Alteracao(Notificacao notificacao, List<Long> alertas, Motivo motivo) {}
}
