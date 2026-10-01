package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.medicao.Medicao;

/**
 * Situação de um instrumento depois do lote, para o dashboard.
 *
 * @param statusAtual limite atingido na última leitura do lote
 * @param statusMaximo pior alerta de segurança do instrumento no lote
 */
public record SituacaoInstrumento(Medicao ultimaLeitura, Severidade statusAtual, Severidade statusMaximo) {

    public SituacaoInstrumento {
        Validacao.obrigatorio(ultimaLeitura, "última leitura do instrumento");
        Validacao.obrigatorio(statusAtual, "status atual do instrumento");
        Validacao.obrigatorio(statusMaximo, "status máximo do instrumento");
    }
}
