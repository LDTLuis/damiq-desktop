package br.com.damiq.desktop.infraestrutura.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CamposTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "29/09/2026 08:00|2026-09-29T08:00:00",
        "29/09/2026 08:00:30|2026-09-29T08:00:30",
        "1/9/2026 8:05|2026-09-01T08:05:00",
        "29/09/2026|2026-09-29T00:00:00",
        "2026-09-29 08:00:00|2026-09-29T08:00:00",
        "2026-09-29T08:00:00-03:00|2026-09-29T08:00:00-03:00",
        "ontem de manhã|ontem de manhã"
    })
    void dataHoraNumaColuna(String informado, String esperado) {
        assertEquals(esperado, Campos.dataHora(informado));
    }

    @Test
    void dataEHoraEmColunasSeparadas() {
        assertEquals("2026-09-29T08:00:00", Campos.dataHora("29/09/2026", "8:00"));
        assertEquals("2026-09-29T14:30:15", Campos.dataHora("2026-09-29", "14:30:15"));
        assertEquals("2026-09-29T00:00:00", Campos.dataHora("29/09/2026", null));
    }

    @Test
    void semData() {
        assertNull(Campos.dataHora(null));
        assertNull(Campos.dataHora(null, "08:00"));
    }
}
