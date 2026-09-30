package br.com.damiq.desktop.dominio.alerta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class AlertaTest {

    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-09-29T10:00:00-03:00");
    private static final OffsetDateTime FIM = OffsetDateTime.parse("2026-09-29T11:00:00-03:00");

    private static Alerta alerta(CategoriaAlerta categoria, OffsetDateTime fim, int leituras, Double limite) {
        return new Alerta(
                TipoAlerta.LIMITE,
                categoria,
                new CodigoInstrumento("PZ-01"),
                Severidade.ALERTA,
                INICIO,
                fim,
                leituras,
                240.0,
                "kPa",
                limite,
                "acima",
                false,
                "PZ-01: pressao 240 kPa acima do limite de alerta (215.82 kPa) em 2 leituras",
                new VersaoConfiguracao("12"));
    }

    @Test
    void episodioValido() {
        var alerta = alerta(CategoriaAlerta.SEGURANCA, FIM, 2, 215.82);

        assertEquals(2, alerta.leituras());
        assertEquals(215.82, alerta.limite());
        assertTrue(alerta.afetaSegurancaDaBarragem());
    }

    @Test
    void alertaDeQualidadeNaoAfetaASegurancaDaBarragem() {
        assertFalse(alerta(CategoriaAlerta.QUALIDADE, FIM, 1, null).afetaSegurancaDaBarragem());
    }

    @Test
    void limiteEhOpcional() {
        assertEquals(null, alerta(CategoriaAlerta.QUALIDADE, FIM, 1, null).limite());
    }

    @Test
    void fimNaoPodeSerAntesDoInicio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> alerta(CategoriaAlerta.SEGURANCA, INICIO.minusMinutes(1), 1, 215.82));
    }

    @Test
    void episodioDeUmaLeituraTemInicioIgualAoFim() {
        assertEquals(INICIO, alerta(CategoriaAlerta.SEGURANCA, INICIO, 1, 215.82).fim());
    }

    @Test
    void exigeAoMenosUmaLeitura() {
        assertThrows(IllegalArgumentException.class, () -> alerta(CategoriaAlerta.SEGURANCA, FIM, 0, 215.82));
    }

    @Test
    void limiteDeveSerFinito() {
        assertThrows(
                IllegalArgumentException.class,
                () -> alerta(CategoriaAlerta.SEGURANCA, FIM, 1, Double.NaN));
    }
}
