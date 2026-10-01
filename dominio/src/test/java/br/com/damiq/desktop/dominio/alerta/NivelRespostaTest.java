package br.com.damiq.desktop.dominio.alerta;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NivelRespostaTest {

    private static NivelResposta nivel(int nivel) {
        return new NivelResposta(nivel, null, "Nível " + nivel, "situação", List.of(), null);
    }

    @Test
    void nivelZeroNaoExigeAcao() {
        assertFalse(nivel(0).exigeAcao());
        assertTrue(nivel(1).exigeAcao());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4})
    void nivelForaDaEscalaDoPae(int invalido) {
        assertThrows(IllegalArgumentException.class, () -> nivel(invalido));
    }
}
