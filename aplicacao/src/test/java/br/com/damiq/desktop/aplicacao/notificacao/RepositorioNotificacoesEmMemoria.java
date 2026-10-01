package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Repositório em memória para testar a regra de notificação ao longo de vários processamentos. */
class RepositorioNotificacoesEmMemoria implements RepositorioNotificacoes {

    private record AlertaGravado(long id, BarragemId barragem, Alerta alerta, int nivel, Long notificacao) {}

    private final Map<Long, AlertaGravado> alertas = new LinkedHashMap<>();
    private final Map<Long, Notificacao> notificacoes = new LinkedHashMap<>();
    private long proximoAlerta = 1;
    private long proximaNotificacao = 1;

    long gravarAlerta(BarragemId barragem, Alerta alerta, int nivelResposta) {
        var id = proximoAlerta++;
        alertas.put(id, new AlertaGravado(id, barragem, alerta, nivelResposta, null));
        return id;
    }

    Long notificacaoDoAlerta(long alerta) {
        return alertas.get(alerta).notificacao();
    }

    List<Notificacao> todas() {
        return List.copyOf(notificacoes.values());
    }

    @Override
    public List<BarragemId> barragensComAlertasPendentes() {
        return alertas.values().stream().filter(a -> a.notificacao() == null).map(AlertaGravado::barragem).distinct().toList();
    }

    @Override
    public List<AlertaPendente> alertasPendentes(BarragemId barragem) {
        return alertas.values().stream()
                .filter(a -> a.barragem().equals(barragem) && a.notificacao() == null)
                .map(a -> new AlertaPendente(a.id(), a.alerta(), a.nivel()))
                .toList();
    }

    @Override
    public List<Notificacao> abertas(BarragemId barragem) {
        return notificacoes.values().stream()
                .filter(n -> n.aberta() && (barragem == null || n.barragem().equals(barragem)))
                .toList();
    }

    @Override
    public Optional<Notificacao> buscar(long id) {
        return Optional.ofNullable(notificacoes.get(id));
    }

    @Override
    public List<Notificacao> gravar(List<Gravacao> gravacoes) {
        var gravadas = new ArrayList<Notificacao>();
        for (var gravacao : gravacoes) {
            var n = gravacao.notificacao();
            var id = n.id() != null ? n.id() : proximaNotificacao++;
            var comId = new Notificacao(id, n.barragem(), n.instrumento(), n.tipoAlerta(), n.categoria(), n.severidade(),
                    n.mensagem(), n.leituraSuspeita(), n.nivelResposta(), n.ocorrencias(), n.criadaEm(), n.atualizadaEm(),
                    n.reconhecimento());
            notificacoes.put(id, comId);
            for (var alerta : gravacao.alertas()) {
                var gravado = Objects.requireNonNull(alertas.get(alerta));
                alertas.put(alerta, new AlertaGravado(alerta, gravado.barragem(), gravado.alerta(), gravado.nivel(), id));
            }
            gravadas.add(comId);
        }
        return gravadas;
    }

    @Override
    public boolean reconhecer(long id, String por, String observacao, Instant em) {
        var n = notificacoes.get(id);
        if (n == null || !n.aberta()) {
            return false;
        }
        notificacoes.put(id, new Notificacao(id, n.barragem(), n.instrumento(), n.tipoAlerta(), n.categoria(),
                n.severidade(), n.mensagem(), n.leituraSuspeita(), n.nivelResposta(), n.ocorrencias(), n.criadaEm(),
                n.atualizadaEm(), new Notificacao.Reconhecimento(em, por, observacao)));
        return true;
    }
}
