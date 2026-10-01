package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Leitura recusada pelo motor. Não reprova o lote: o técnico vê a linha e o motivo.
 *
 * @param indice posição da leitura na lista enviada (a partir de 0)
 * @param codigo ex.: {@code VALOR_INVALIDO}, {@code UNIDADE_DESCONHECIDA}, {@code TIMESTAMP_FUTURO}
 */
public record Rejeicao(int indice, String codigo, String mensagem) {

    public Rejeicao {
        if (indice < 0) {
            throw new IllegalArgumentException("índice da rejeição não pode ser negativo: " + indice);
        }
        codigo = Validacao.textoObrigatorio(codigo, "código da rejeição");
        mensagem = Validacao.textoObrigatorio(mensagem, "mensagem da rejeição");
    }
}
