package br.com.damiq.desktop.infraestrutura.usuario;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import org.junit.jupiter.api.Test;

class CodificadorSenhaBcryptTest {

    /** Custo mínimo, para o teste ser rápido. */
    private final CodificadorSenhaBcrypt codificador = new CodificadorSenhaBcrypt(4);

    @Test
    void confereASenhaCodificadaComSalDiferenteACadaVez() {
        var hash = codificador.codificar("Barragem2026".toCharArray());
        assertTrue(CodificadorSenhaBcrypt.formatoValido(hash.valor()));
        assertTrue(codificador.confere("Barragem2026".toCharArray(), hash));
        assertFalse(codificador.confere("barragem2026".toCharArray(), hash));
        assertNotEquals(hash, codificador.codificar("Barragem2026".toCharArray()));
    }

    @Test
    void confereOHashPublicadoPelaCentral() {
        // do cadastro-usuarios.exemplo.json, senha "Provisoria2026"
        var hash = new SenhaHash("$2a$10$xLS1W84muMQxPyNj2Sc/QOyVSo8o0GATq/lTMK42I5jWzJQ7Oy.YC");
        assertTrue(codificador.confere("Provisoria2026".toCharArray(), hash));
    }

    @Test
    void senhaComAcentoEmUtf8() {
        var hash = codificador.codificar("Represa-ção1".toCharArray());
        assertTrue(codificador.confere("Represa-ção1".toCharArray(), hash));
        assertFalse(codificador.confere("Represa-cao1".toCharArray(), hash));
    }

    @Test
    void hashForaDoFormatoNaoConfere() {
        assertFalse(codificador.confere("x".toCharArray(), new SenhaHash("texto-puro")));
        assertFalse(CodificadorSenhaBcrypt.formatoValido("$2a$10$curto"));
        assertFalse(CodificadorSenhaBcrypt.formatoValido(null));
    }
}
