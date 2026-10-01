package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.aplicacao.evento.PublicadorEventos;
import br.com.damiq.desktop.dominio.Validacao;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Um responsável reconhece a notificação: ela sai da lista de abertas, e quem, quando e a observação ficam
 * registrados para auditoria (RF-06/RF-12). Um novo episódio do mesmo problema abre outra notificação.
 */
public final class ReconhecerNotificacao {

    private static final Logger LOG = LoggerFactory.getLogger(ReconhecerNotificacao.class);

    private final RepositorioNotificacoes notificacoes;
    private final PublicadorEventos eventos;
    private final Clock relogio;

    public ReconhecerNotificacao(RepositorioNotificacoes notificacoes, PublicadorEventos eventos, Clock relogio) {
        this.notificacoes = Validacao.obrigatorio(notificacoes, "repositório de notificações");
        this.eventos = Validacao.obrigatorio(eventos, "publicador de eventos");
        this.relogio = Validacao.obrigatorio(relogio, "relógio");
    }

    /**
     * @param responsavel quem reconhece (até a autenticação, RF-01, um nome informado)
     * @param observacao providência tomada; opcional
     * @throws IllegalArgumentException se a notificação não existir
     * @throws IllegalStateException se ela já tiver sido reconhecida
     */
    public Notificacao executar(long id, String responsavel, String observacao) {
        var por = Validacao.textoObrigatorio(responsavel, "responsável pelo reconhecimento");
        var texto = observacao == null || observacao.isBlank() ? null : observacao.strip();

        if (!notificacoes.reconhecer(id, por, texto, relogio.instant())) {
            var existente = notificacoes.buscar(id)
                    .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada: " + id));
            throw new IllegalStateException("A notificação " + id + " já foi reconhecida por "
                    + existente.reconhecida().map(Notificacao.Reconhecimento::por).orElse("?"));
        }
        var reconhecida = notificacoes.buscar(id).orElseThrow();
        LOG.info("Notificação {} da barragem {} ({} {} em {}) reconhecida por {}{}",
                id, reconhecida.barragem(), reconhecida.severidade(), reconhecida.tipoAlerta(),
                reconhecida.instrumento(), por, texto == null ? "" : ": " + texto);
        eventos.publicar(new NotificacaoReconhecida(reconhecida));
        return reconhecida;
    }
}
