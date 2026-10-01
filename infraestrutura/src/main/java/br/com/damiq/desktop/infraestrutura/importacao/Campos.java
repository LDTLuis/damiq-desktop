package br.com.damiq.desktop.infraestrutura.importacao;

import java.util.regex.Pattern;

/**
 * Converte datas e horas no formato brasileiro para o ISO-8601 que o motor espera. O que não for reconhecido
 * segue como está, e o motor recusa a linha com {@code TIMESTAMP_INVALIDO}. Sem fuso, vale o
 * {@code fuso_padrao} da configuração.
 */
final class Campos {

    private static final Pattern DATA_BR = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern HORA = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern DATA_HORA_BR =
            Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})[ T]+(\\d{1,2}:\\d{2}(?::\\d{2})?)");
    private static final Pattern DATA_HORA_ISO_COM_ESPACO =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2}) +(\\d{2}:\\d{2}.*)");

    private Campos() {}

    /** Data e hora numa coluna só: {@code 29/09/2026 08:00} → {@code 2026-09-29T08:00:00}. */
    static String dataHora(String texto) {
        if (texto == null) {
            return null;
        }
        var br = DATA_HORA_BR.matcher(texto);
        if (br.matches()) {
            return data(br.group(1)) + "T" + hora(br.group(2));
        }
        var isoComEspaco = DATA_HORA_ISO_COM_ESPACO.matcher(texto);
        if (isoComEspaco.matches()) {
            return isoComEspaco.group(1) + "T" + isoComEspaco.group(2);
        }
        if (DATA_BR.matcher(texto).matches()) {
            return data(texto) + "T00:00:00";
        }
        return texto;
    }

    /** Data e hora em colunas separadas; sem hora, vale 00:00. */
    static String dataHora(String data, String hora) {
        if (data == null) {
            return null;
        }
        return data(data) + "T" + (hora == null ? "00:00:00" : hora(hora));
    }

    /** {@code 29/09/2026} → {@code 2026-09-29}; outros formatos seguem como estão. */
    static String data(String texto) {
        var br = DATA_BR.matcher(texto);
        if (!br.matches()) {
            return texto;
        }
        return "%s-%02d-%02d".formatted(br.group(3), Integer.parseInt(br.group(2)), Integer.parseInt(br.group(1)));
    }

    /** {@code 8:00} → {@code 08:00:00}; outros formatos seguem como estão. */
    static String hora(String texto) {
        var hora = HORA.matcher(texto);
        if (!hora.matches()) {
            return texto;
        }
        var segundos = hora.group(3) == null ? 0 : Integer.parseInt(hora.group(3));
        return "%02d:%s:%02d".formatted(Integer.parseInt(hora.group(1)), hora.group(2), segundos);
    }
}
