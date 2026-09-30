package br.com.damiq.desktop.dominio.instrumento;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;

/** Ponto de medição lido pelo técnico. Cada instrumento mede um único tipo de grandeza. */
public record Instrumento(CodigoInstrumento codigo, TipoMedicao tipo) {

    public Instrumento {
        Validacao.obrigatorio(codigo, "código do instrumento");
        Validacao.obrigatorio(tipo, "tipo do instrumento");
    }
}
