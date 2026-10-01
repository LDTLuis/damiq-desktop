package br.com.damiq.desktop.inicializacao;

import java.nio.file.Path;
import java.util.Map;

/** Diretório padrão dos dados do usuário (banco local e configurações), conforme o sistema operacional. */
final class DiretorioDados {

    private DiretorioDados() {}

    static Path padrao() {
        return padrao(System.getProperty("os.name", ""), System.getenv(), Path.of(System.getProperty("user.home")));
    }

    static Path padrao(String sistema, Map<String, String> ambiente, Path home) {
        if (sistema.startsWith("Windows")) {
            var appData = ambiente.get("APPDATA");
            return (appData != null ? Path.of(appData) : home.resolve("AppData").resolve("Roaming")).resolve("DAMIQ");
        }
        var xdg = ambiente.get("XDG_DATA_HOME");
        return (xdg != null && !xdg.isBlank() ? Path.of(xdg) : home.resolve(".local").resolve("share"))
                .resolve("damiq");
    }
}
