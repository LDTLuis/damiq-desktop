package br.com.damiq.desktop.infraestrutura.motor;

import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Resposta do motor (contrato 1.0, seção 5). Os campos de {@code processar_lote} seguem a resposta real do
 * motor 1.0.1: o {@code resposta.schema.json} da release não descreve {@code medicoes}, {@code lacunas} nem os
 * detalhes de {@code monitoramento}. Campos desconhecidos são ignorados; listas ausentes viram vazias.
 */
record RespostaMotor(
        String versaoContrato,
        String operacao,
        String status,
        JsonNode versaoConfig,
        List<Mensagem> erros,
        List<Mensagem> avisos,
        Motor motor,
        Monitoramento monitoramento,
        List<Medicao> medicoes,
        List<Alerta> alertas,
        List<Rejeicao> rejeicoes,
        List<Rejeicao> rejeicoesHistorico,
        List<Lacuna> lacunas) {

    RespostaMotor {
        erros = vazioSeNulo(erros);
        avisos = vazioSeNulo(avisos);
        medicoes = vazioSeNulo(medicoes);
        alertas = vazioSeNulo(alertas);
        rejeicoes = vazioSeNulo(rejeicoes);
        rejeicoesHistorico = vazioSeNulo(rejeicoesHistorico);
        lacunas = vazioSeNulo(lacunas);
    }

    private static <T> List<T> vazioSeNulo(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    boolean ok() {
        return "OK".equals(status);
    }

    record Mensagem(String codigo, String mensagem, String campo) {}

    record Motor(String versao, List<String> operacoes) {}

    record Monitoramento(
            String statusBarragem, String statusDados, NivelResposta nivelResposta, Map<String, Sensor> sensores) {}

    record NivelResposta(Integer nivel, String cor, String rotulo, String situacao, List<String> acoes, String fonte) {}

    record Sensor(String tipo, UltimaLeitura ultimaLeitura, String statusAtual, String statusMaximo) {}

    record UltimaLeitura(String timestamp, Double valor, String unidade) {}

    /** @param valorOriginal número ou texto, como foi informado */
    record Medicao(
            String sensor,
            String tipo,
            String timestamp,
            Double valor,
            String unidade,
            String valorOriginal,
            String unidadeOriginal,
            List<String> flags) {}

    record Alerta(
            String tipo,
            String categoria,
            String sensor,
            String severidade,
            String inicio,
            String fim,
            Integer leituras,
            Double valorExtremo,
            String unidade,
            Double limite,
            String direcao,
            Boolean leituraSuspeita,
            String mensagem) {}

    record Rejeicao(Integer indice, String codigo, String mensagem) {}

    record Lacuna(String sensor, String inicio, String fim, Double duracaoS) {}
}
