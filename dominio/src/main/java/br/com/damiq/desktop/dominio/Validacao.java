package br.com.damiq.desktop.dominio;

import java.util.Objects;

/** Verificações de invariantes usadas pelos construtores do domínio. */
public final class Validacao {

    private Validacao() {}

    public static <T> T obrigatorio(T valor, String campo) {
        return Objects.requireNonNull(valor, () -> campo + " é obrigatório");
    }

    /** Exige um texto não vazio e devolve-o sem espaços nas pontas. */
    public static String textoObrigatorio(String valor, String campo) {
        obrigatorio(valor, campo);
        var aparado = valor.strip();
        if (aparado.isEmpty()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }
        return aparado;
    }

    public static double finito(double valor, String campo) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException(campo + " deve ser um número finito: " + valor);
        }
        return valor;
    }
}
