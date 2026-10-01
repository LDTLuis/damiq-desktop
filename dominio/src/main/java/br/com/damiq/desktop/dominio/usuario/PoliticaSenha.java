package br.com.damiq.desktop.dominio.usuario;

/**
 * Regras de senha forte (RNF-03) para a senha escolhida pelo usuário: de 10 a 64 caracteres, com pelo menos uma
 * letra e um número, sem conter o login. O limite superior vem do bcrypt, que só considera os primeiros 72 bytes.
 */
public final class PoliticaSenha {

    public static final int TAMANHO_MINIMO = 10;
    public static final int TAMANHO_MAXIMO = 64;

    private PoliticaSenha() {}

    /** @throws IllegalArgumentException com o motivo, se a senha não atender às regras */
    public static void verificar(char[] senha, Login login) {
        if (senha == null || senha.length < TAMANHO_MINIMO) {
            throw new IllegalArgumentException("a senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres");
        }
        if (senha.length > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("a senha deve ter no máximo " + TAMANHO_MAXIMO + " caracteres");
        }
        boolean letra = false;
        boolean numero = false;
        for (char c : senha) {
            letra |= Character.isLetter(c);
            numero |= Character.isDigit(c);
        }
        if (!letra || !numero) {
            throw new IllegalArgumentException("a senha deve ter letras e números");
        }
        if (login != null && contem(senha, login.valor())) {
            throw new IllegalArgumentException("a senha não pode conter o login");
        }
    }

    /** Busca sem diferenciar maiúsculas, sem criar uma {@code String} com a senha. */
    private static boolean contem(char[] senha, String trecho) {
        for (int inicio = 0; inicio + trecho.length() <= senha.length; inicio++) {
            int i = 0;
            while (i < trecho.length() && Character.toLowerCase(senha[inicio + i]) == trecho.charAt(i)) {
                i++;
            }
            if (i == trecho.length()) {
                return true;
            }
        }
        return false;
    }
}
