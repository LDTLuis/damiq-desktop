package br.com.damiq.desktop.dominio.barragem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Tipo do valor de um {@link CampoBarragem}. O valor é guardado como texto no formato canônico do tipo. */
public enum TipoCampo {
    /** Número com ponto decimal (ex.: {@code 752.5}); guardado sem zeros à direita ({@code 752.50} → {@code 752.5}). */
    NUMERO,
    /** Texto livre. */
    TEXTO,
    /** Data ISO-8601 (ex.: {@code 2008-12-18}). */
    DATA,
    /** {@code true} ou {@code false}. */
    BOOLEANO;

    /**
     * @return o valor no formato canônico
     * @throws IllegalArgumentException se o valor não for desse tipo
     */
    String canonico(String valor) {
        var texto = valor.strip();
        return switch (this) {
            case TEXTO -> texto;
            case NUMERO -> {
                try {
                    yield new BigDecimal(texto).stripTrailingZeros().toPlainString();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("não é um número (use ponto decimal): '" + valor + "'");
                }
            }
            case DATA -> {
                try {
                    yield LocalDate.parse(texto).toString();
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("não é uma data AAAA-MM-DD: '" + valor + "'");
                }
            }
            case BOOLEANO -> {
                if (!texto.equals("true") && !texto.equals("false")) {
                    throw new IllegalArgumentException("não é true nem false: '" + valor + "'");
                }
                yield texto;
            }
        };
    }
}
