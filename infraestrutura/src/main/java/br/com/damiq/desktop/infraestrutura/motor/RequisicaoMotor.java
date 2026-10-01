package br.com.damiq.desktop.infraestrutura.motor;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Requisição ao motor ({@code requisicao.schema.json}, contrato 1.0). Campos nulos não são enviados.
 *
 * @param configuracao a configuração da Central, repassada sem interpretação
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record RequisicaoMotor(
        String versaoContrato,
        String operacao,
        Map<String, String> barragem,
        JsonNode configuracao,
        Opcoes opcoes,
        List<Leitura> medicoes,
        List<Leitura> historico) {

    static RequisicaoMotor de(String operacao) {
        return new RequisicaoMotor(MotorCalculoProcessBuilder.VERSAO_CONTRATO, operacao, null, null, null, null, null);
    }

    /** @param agora ISO-8601 com fuso */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Opcoes(String agora) {}

    /**
     * Leitura no formato do contrato; um campo nulo é omitido e o motor recusa o registro com
     * {@code CAMPO_AUSENTE}.
     *
     * @param valor número, ou texto como informado (aceita vírgula decimal)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Leitura(String sensor, String tipo, String timestamp, Object valor, String unidade) {}
}
