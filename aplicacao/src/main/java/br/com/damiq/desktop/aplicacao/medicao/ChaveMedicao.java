package br.com.damiq.desktop.aplicacao.medicao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import java.time.Instant;

/** Identifica uma leitura dentro da barragem: um instrumento só tem uma leitura por instante. */
public record ChaveMedicao(CodigoInstrumento instrumento, Instant instante) {

    public ChaveMedicao {
        Validacao.obrigatorio(instrumento, "instrumento");
        Validacao.obrigatorio(instante, "instante");
    }

    public static ChaveMedicao de(Medicao medicao) {
        return new ChaveMedicao(medicao.instrumento(), medicao.momento().toInstant());
    }
}
