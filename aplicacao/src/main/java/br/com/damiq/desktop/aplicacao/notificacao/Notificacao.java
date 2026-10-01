package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.Instant;
import java.util.Optional;

/**
 * Notificação de alerta no app (RF-06). Enquanto aberta (não reconhecida), agrupa os episódios do mesmo
 * problema: mesma barragem, instrumento e tipo de alerta.
 *
 * @param id {@code null} enquanto não gravada
 * @param severidade a maior severidade entre as ocorrências
 * @param mensagem mensagem da ocorrência mais grave (a mais recente, em caso de empate)
 * @param leituraSuspeita a ocorrência mais grave tem leitura fora da faixa plausível: verificar o instrumento
 *     antes de acionar o PAE
 * @param nivelResposta maior nível de resposta do PAE (0 a 3) nos processamentos das ocorrências
 */
public record Notificacao(
        Long id,
        BarragemId barragem,
        CodigoInstrumento instrumento,
        TipoAlerta tipoAlerta,
        CategoriaAlerta categoria,
        Severidade severidade,
        String mensagem,
        boolean leituraSuspeita,
        int nivelResposta,
        int ocorrencias,
        Instant criadaEm,
        Instant atualizadaEm,
        Reconhecimento reconhecimento) {

    public Notificacao {
        Validacao.obrigatorio(barragem, "barragem");
        Validacao.obrigatorio(instrumento, "instrumento");
        Validacao.obrigatorio(tipoAlerta, "tipo do alerta");
        Validacao.obrigatorio(categoria, "categoria");
        Validacao.obrigatorio(severidade, "severidade");
        mensagem = Validacao.textoObrigatorio(mensagem, "mensagem");
        if (ocorrencias < 1) {
            throw new IllegalArgumentException("notificação deve ter ao menos 1 ocorrência: " + ocorrencias);
        }
        Validacao.obrigatorio(criadaEm, "criação");
        Validacao.obrigatorio(atualizadaEm, "atualização");
    }

    /** Reconhecimento por um responsável, que encerra a notificação. */
    public record Reconhecimento(Instant em, String por, String observacao) {

        public Reconhecimento {
            Validacao.obrigatorio(em, "momento do reconhecimento");
            por = Validacao.textoObrigatorio(por, "responsável pelo reconhecimento");
        }
    }

    /** Nova notificação para o primeiro episódio de um problema. */
    static Notificacao de(BarragemId barragem, Alerta alerta, int nivelResposta, Instant agora) {
        return new Notificacao(null, barragem, alerta.instrumento(), alerta.tipo(), alerta.categoria(),
                alerta.severidade(), alerta.mensagem(), alerta.leituraSuspeita(), nivelResposta, 1, agora, agora, null);
    }

    /** Soma um novo episódio; a severidade e a mensagem passam a ser as da ocorrência mais grave. */
    Notificacao comOcorrencia(Alerta alerta, int nivelResposta, Instant agora) {
        var maisGrave = !severidade.maisGraveQue(alerta.severidade());
        return new Notificacao(id, barragem, instrumento, tipoAlerta, categoria,
                maisGrave ? alerta.severidade() : severidade,
                maisGrave ? alerta.mensagem() : mensagem,
                maisGrave ? alerta.leituraSuspeita() : leituraSuspeita,
                Math.max(this.nivelResposta, nivelResposta),
                ocorrencias + 1, criadaEm, agora, reconhecimento);
    }

    public boolean aberta() {
        return reconhecimento == null;
    }

    public Optional<Reconhecimento> reconhecida() {
        return Optional.ofNullable(reconhecimento);
    }
}
