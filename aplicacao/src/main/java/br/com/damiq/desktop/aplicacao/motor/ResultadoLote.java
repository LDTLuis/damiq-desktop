package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.NivelResposta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Lacuna;
import java.util.List;
import java.util.Map;

/**
 * Resposta de {@code processar_lote}.
 *
 * @param versaoConfiguracao versão da configuração aplicada (auditoria, RF-12)
 * @param statusBarragem maior severidade entre os alertas de segurança
 * @param statusDados maior severidade entre os alertas de qualidade dos dados
 * @param rejeicoesHistorico leituras do histórico recusadas (indicam dado ruim já gravado)
 * @param avisos campos da configuração que o motor não reconheceu
 */
public record ResultadoLote(
        VersaoConfiguracao versaoConfiguracao,
        List<MedicaoProcessada> medicoes,
        List<Rejeicao> rejeicoes,
        List<Rejeicao> rejeicoesHistorico,
        List<Lacuna> lacunas,
        List<Alerta> alertas,
        Severidade statusBarragem,
        Severidade statusDados,
        NivelResposta nivelResposta,
        Map<CodigoInstrumento, SituacaoInstrumento> instrumentos,
        List<MensagemMotor> avisos) {

    public ResultadoLote {
        Validacao.obrigatorio(versaoConfiguracao, "versão da configuração");
        medicoes = List.copyOf(Validacao.obrigatorio(medicoes, "medições"));
        rejeicoes = List.copyOf(Validacao.obrigatorio(rejeicoes, "rejeições"));
        rejeicoesHistorico = List.copyOf(Validacao.obrigatorio(rejeicoesHistorico, "rejeições do histórico"));
        lacunas = List.copyOf(Validacao.obrigatorio(lacunas, "lacunas"));
        alertas = List.copyOf(Validacao.obrigatorio(alertas, "alertas"));
        Validacao.obrigatorio(statusBarragem, "status da barragem");
        Validacao.obrigatorio(statusDados, "status dos dados");
        Validacao.obrigatorio(nivelResposta, "nível de resposta");
        instrumentos = Map.copyOf(Validacao.obrigatorio(instrumentos, "situação dos instrumentos"));
        avisos = List.copyOf(Validacao.obrigatorio(avisos, "avisos"));
    }
}
