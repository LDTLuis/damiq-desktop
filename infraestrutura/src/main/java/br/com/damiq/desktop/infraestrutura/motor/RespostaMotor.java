package br.com.damiq.desktop.infraestrutura.motor;

import java.util.List;

/**
 * Campos comuns da resposta do motor e os de {@code info}. Os DTOs completos, gerados dos JSON Schemas,
 * substituem este tipo quando entrar {@code processar_lote}.
 */
record RespostaMotor(
        String versaoContrato,
        String operacao,
        String status,
        List<Mensagem> erros,
        List<Mensagem> avisos,
        Motor motor) {

    RespostaMotor {
        erros = erros == null ? List.of() : List.copyOf(erros);
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }

    boolean ok() {
        return "OK".equals(status);
    }

    record Mensagem(String codigo, String mensagem, String campo) {}

    record Motor(String versao, List<String> operacoes) {}
}
