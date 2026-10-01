package br.com.damiq.desktop.dominio.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identificação com que o usuário entra no sistema; única entre os usuários ativos. Guardado em minúsculas, para
 * que "Ana.Souza" e "ana.souza" sejam o mesmo usuário.
 */
public record Login(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[a-z0-9][a-z0-9._@-]{2,99}");

    public Login {
        valor = Validacao.textoObrigatorio(valor, "login").toLowerCase(Locale.ROOT);
        if (!FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "login inválido: 3 a 100 caracteres entre letras sem acento, números, '.', '_', '@' e '-'");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
