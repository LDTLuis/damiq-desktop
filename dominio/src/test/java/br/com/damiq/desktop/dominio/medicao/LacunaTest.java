package br.com.damiq.desktop.dominio.medicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class LacunaTest {

    private static final CodigoInstrumento PZ_01 = new CodigoInstrumento("PZ-01");
    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-09-29T08:00:00-03:00");

    @Test
    void lacunaValida() {
        var lacuna = new Lacuna(PZ_01, INICIO, INICIO.plusHours(4), Duration.ofHours(4));

        assertEquals(Duration.ofHours(4), lacuna.duracao());
    }

    @Test
    void fimDeveSerDepoisDoInicio() {
        assertThrows(IllegalArgumentException.class, () -> new Lacuna(PZ_01, INICIO, INICIO, Duration.ofHours(1)));
    }

    @Test
    void duracaoDeveSerPositiva() {
        assertThrows(
                IllegalArgumentException.class, () -> new Lacuna(PZ_01, INICIO, INICIO.plusHours(1), Duration.ZERO));
    }
}
