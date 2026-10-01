package br.com.damiq.desktop.aplicacao.manutencao;

import java.util.Map;

/** Apaga fisicamente os registros marcados como teste ({@code teste = 1}). */
public interface RepositorioDadosTeste {

    /**
     * Apaga todos os registros de teste numa única transação.
     *
     * @return quantos registros foram apagados em cada tabela, na ordem em que foram apagados
     */
    Map<String, Integer> apagarDadosDeTeste();
}
