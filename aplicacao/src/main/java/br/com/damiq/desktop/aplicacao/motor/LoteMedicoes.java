package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Entrada de {@code processar_lote}.
 *
 * @param leituras leituras novas; as rejeições voltam com o índice nesta lista
 * @param historico leituras já processadas antes, só como contexto (taxa de variação, referência
 *     estatística, lacunas na transição); por instrumento, no mínimo 24
 * @param agora relógio de referência para recusar leituras no futuro
 */
public record LoteMedicoes(
        BarragemId barragem,
        Configuracao configuracao,
        List<LeituraInformada> leituras,
        List<Medicao> historico,
        OffsetDateTime agora) {

    public LoteMedicoes {
        Validacao.obrigatorio(barragem, "barragem do lote");
        Validacao.obrigatorio(configuracao, "configuração do lote");
        leituras = List.copyOf(Validacao.obrigatorio(leituras, "leituras do lote"));
        historico = List.copyOf(Validacao.obrigatorio(historico, "histórico do lote"));
        Validacao.obrigatorio(agora, "relógio de referência do lote");
    }
}
