package br.com.damiq.desktop.dominio.medicao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.OffsetDateTime;

/**
 * Medição já validada e normalizada pelo motor: o valor está sempre na unidade canônica do tipo.
 */
public record Medicao(CodigoInstrumento instrumento, TipoMedicao tipo, OffsetDateTime momento, double valor) {

    public Medicao {
        Validacao.obrigatorio(instrumento, "instrumento da medição");
        Validacao.obrigatorio(tipo, "tipo da medição");
        Validacao.obrigatorio(momento, "momento da medição");
        Validacao.finito(valor, "valor da medição");
    }

    public String unidade() {
        return tipo.unidadeCanonica();
    }
}
