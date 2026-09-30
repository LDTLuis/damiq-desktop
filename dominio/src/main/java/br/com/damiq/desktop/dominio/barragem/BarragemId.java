package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;

/** Identificador de uma barragem. */
public record BarragemId(String valor) {

    public BarragemId {
        valor = Validacao.textoObrigatorio(valor, "identificador da barragem");
    }

    @Override
    public String toString() {
        return valor;
    }
}
