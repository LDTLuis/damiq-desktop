package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.regex.Pattern;

/**
 * Grupo de campos próprios de uma barragem (ex.: Maciço, Vertedouro, Reservatório). Cada barragem tem os seus
 * grupos, na ordem de exibição.
 *
 * @param chave identificador estável do grupo na barragem (minúsculas, números e {@code _}), ex.: {@code macico}
 * @param nome nome exibido, ex.: "Maciço"
 */
public record GrupoCampos(String chave, String nome) {

    private static final Pattern CHAVE = Pattern.compile("[a-z][a-z0-9_]{0,63}");

    public GrupoCampos {
        chave = Validacao.textoObrigatorio(chave, "chave do grupo");
        if (!CHAVE.matcher(chave).matches()) {
            throw new IllegalArgumentException(
                    "chave do grupo deve ter só minúsculas, números e _ (até 64, começando por letra): " + chave);
        }
        nome = Validacao.textoObrigatorio(nome, "nome do grupo " + chave);
    }
}
