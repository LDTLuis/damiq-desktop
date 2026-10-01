package br.com.damiq.desktop.aplicacao.medicao;

import br.com.damiq.desktop.aplicacao.motor.MedicaoProcessada;
import br.com.damiq.desktop.aplicacao.motor.Rejeicao;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.medicao.Lacuna;
import java.time.Instant;
import java.util.List;

/**
 * O que um processamento grava, numa única transação.
 *
 * @param arquivo nome do arquivo importado; {@code null} na digitação
 * @param medicoes só as medições novas (sem as já registradas)
 * @param jaRegistradas quantas leituras válidas já estavam gravadas
 */
public record RegistroProcessamento(
        BarragemId barragem,
        VersaoConfiguracao versaoConfiguracao,
        Instant executadoEm,
        OrigemLeituras origem,
        String arquivo,
        Severidade statusBarragem,
        Severidade statusDados,
        int nivelResposta,
        int recebidas,
        int jaRegistradas,
        List<MedicaoProcessada> medicoes,
        List<RejeicaoLeitura> rejeicoes,
        List<Lacuna> lacunas,
        List<Alerta> alertas) {

    public RegistroProcessamento {
        Validacao.obrigatorio(barragem, "barragem");
        Validacao.obrigatorio(versaoConfiguracao, "versão da configuração");
        Validacao.obrigatorio(executadoEm, "momento do processamento");
        Validacao.obrigatorio(origem, "origem das leituras");
        Validacao.obrigatorio(statusBarragem, "status da barragem");
        Validacao.obrigatorio(statusDados, "status dos dados");
        medicoes = List.copyOf(Validacao.obrigatorio(medicoes, "medições"));
        rejeicoes = List.copyOf(Validacao.obrigatorio(rejeicoes, "rejeições"));
        lacunas = List.copyOf(Validacao.obrigatorio(lacunas, "lacunas"));
        alertas = List.copyOf(Validacao.obrigatorio(alertas, "alertas"));
    }

    /** Rejeição com a leitura como foi informada, para o técnico corrigir. */
    public record RejeicaoLeitura(Rejeicao rejeicao, LeituraInformada leitura) {

        public RejeicaoLeitura {
            Validacao.obrigatorio(rejeicao, "rejeição");
            Validacao.obrigatorio(leitura, "leitura rejeitada");
        }
    }
}
