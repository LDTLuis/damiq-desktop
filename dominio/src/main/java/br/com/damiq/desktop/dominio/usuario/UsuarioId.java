package br.com.damiq.desktop.dominio.usuario;

/** Identificador de um usuário do Desktop. */
public record UsuarioId(long valor) {

    /** Usuário das operações automáticas e de tudo o que é feito antes da autenticação (RF-01). */
    public static final UsuarioId SISTEMA = new UsuarioId(1);

    public UsuarioId {
        if (valor < 1) {
            throw new IllegalArgumentException("identificador de usuário deve ser positivo: " + valor);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(valor);
    }
}
