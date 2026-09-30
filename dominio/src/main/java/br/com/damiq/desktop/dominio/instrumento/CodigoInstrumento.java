package br.com.damiq.desktop.dominio.instrumento;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Código de um instrumento (ex.: {@code PZ-01}). No contrato do motor é o campo {@code sensor}, que pode
 * vir como texto ou inteiro; aqui é sempre texto.
 */
public record CodigoInstrumento(String valor) {

    public CodigoInstrumento {
        valor = Validacao.textoObrigatorio(valor, "código do instrumento");
    }

    @Override
    public String toString() {
        return valor;
    }
}
