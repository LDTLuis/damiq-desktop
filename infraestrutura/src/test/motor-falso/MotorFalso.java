import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Substituto do motor para os testes do executor, executado com {@code java MotorFalso.java <modo> --entrada
 * <arquivo> --saida <arquivo>}.
 */
public class MotorFalso {

    public static void main(String[] args) throws Exception {
        var modo = args[0];
        var entrada = Path.of(args[2]);
        var saida = Path.of(args[4]);
        switch (modo) {
            case "eco" -> Files.writeString(saida, Files.readString(entrada, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            case "erro-interno" -> {
                Files.writeString(saida, "{\"status\": \"ERRO\"}", StandardCharsets.UTF_8);
                System.err.println("Traceback: falha simulada na medição de pressão");
                System.exit(2);
            }
            case "dorme" -> Thread.sleep(60_000);
            case "sem-resposta" -> { }
            default -> throw new IllegalArgumentException(modo);
        }
    }
}
