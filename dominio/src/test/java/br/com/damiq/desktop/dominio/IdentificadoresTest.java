package br.com.damiq.desktop.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdentificadoresTest {

    @Test
    void removeEspacosNasPontas() {
        assertEquals("PZ-01", new CodigoInstrumento("  PZ-01 ").valor());
        assertEquals("12", new VersaoConfiguracao(" 12 ").valor());
        assertEquals("joao-leite", new BarragemId("joao-leite ").valor());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejeitaTextoVazio(String vazio) {
        assertThrows(IllegalArgumentException.class, () -> new CodigoInstrumento(vazio));
        assertThrows(IllegalArgumentException.class, () -> new VersaoConfiguracao(vazio));
        assertThrows(IllegalArgumentException.class, () -> new BarragemId(vazio));
        assertThrows(IllegalArgumentException.class, () -> new Barragem(new BarragemId("b"), vazio));
        assertThrows(
                IllegalArgumentException.class, () -> new Configuracao(new VersaoConfiguracao("1"), vazio));
    }

    @Test
    void rejeitaNulo() {
        assertThrows(NullPointerException.class, () -> new CodigoInstrumento(null));
        assertThrows(NullPointerException.class, () -> new Barragem(null, "João Leite"));
    }
}
