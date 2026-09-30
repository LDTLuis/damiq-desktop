package br.com.damiq.desktop.dominio.medicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class MedicaoTest {

    private static final CodigoInstrumento PZ_01 = new CodigoInstrumento("PZ-01");
    private static final OffsetDateTime MOMENTO = OffsetDateTime.parse("2026-09-29T08:00:00-03:00");

    @Test
    void unidadeEhACanonicaDoTipo() {
        assertEquals("kPa", new Medicao(PZ_01, TipoMedicao.PRESSAO, MOMENTO, 140.0).unidade());
        assertEquals("m3/s", new Medicao(PZ_01, TipoMedicao.VAZAO, MOMENTO, 0.5).unidade());
    }

    @Test
    void valorDeveSerFinito() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Medicao(PZ_01, TipoMedicao.PRESSAO, MOMENTO, Double.POSITIVE_INFINITY));
    }

    @Test
    void momentoEhObrigatorio() {
        assertThrows(NullPointerException.class, () -> new Medicao(PZ_01, TipoMedicao.PRESSAO, null, 1.0));
    }
}
