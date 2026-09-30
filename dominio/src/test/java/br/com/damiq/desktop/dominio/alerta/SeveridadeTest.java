package br.com.damiq.desktop.dominio.alerta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SeveridadeTest {

    @Test
    void ordemDaMenosParaAMaisGrave() {
        assertTrue(Severidade.CRITICO.maisGraveQue(Severidade.ALERTA));
        assertTrue(Severidade.ALERTA.maisGraveQue(Severidade.AVISO));
        assertTrue(Severidade.AVISO.maisGraveQue(Severidade.OK));
        assertFalse(Severidade.AVISO.maisGraveQue(Severidade.AVISO));
    }

    @Test
    void maisGraveDeUmaColecao() {
        assertEquals(
                Severidade.ALERTA,
                Severidade.maisGrave(List.of(Severidade.AVISO, Severidade.ALERTA, Severidade.OK)));
    }

    @Test
    void colecaoVaziaEhOk() {
        assertEquals(Severidade.OK, Severidade.maisGrave(List.of()));
    }
}
