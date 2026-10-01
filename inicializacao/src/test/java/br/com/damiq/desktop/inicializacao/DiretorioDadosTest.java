package br.com.damiq.desktop.inicializacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiretorioDadosTest {

    private static final Path HOME = Path.of("home", "tecnico");

    @Test
    void windowsUsaAppData() {
        var appData = Path.of("C:", "Users", "tecnico", "AppData", "Roaming");

        assertEquals(
                appData.resolve("DAMIQ"),
                DiretorioDados.padrao("Windows 11", Map.of("APPDATA", appData.toString()), HOME));
    }

    @Test
    void linuxUsaXdgDataHome() {
        assertEquals(
                Path.of("dados", "damiq"),
                DiretorioDados.padrao("Linux", Map.of("XDG_DATA_HOME", Path.of("dados").toString()), HOME));
    }

    @Test
    void linuxSemXdgUsaLocalShare() {
        assertEquals(HOME.resolve(".local").resolve("share").resolve("damiq"), DiretorioDados.padrao("Linux", Map.of(), HOME));
    }
}
