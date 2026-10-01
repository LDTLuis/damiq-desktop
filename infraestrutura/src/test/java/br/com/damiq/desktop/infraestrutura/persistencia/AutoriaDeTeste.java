package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** {@link Autoria} para os testes: usuário "sistema" e o relógio informado. */
public final class AutoriaDeTeste {

    public static final Autoria SISTEMA = new Autoria(Clock.systemUTC(), () -> UsuarioId.SISTEMA);

    private AutoriaDeTeste() {}

    public static Autoria em(Instant instante) {
        return new Autoria(Clock.fixed(instante, ZoneOffset.UTC), () -> UsuarioId.SISTEMA);
    }

    public static Autoria em(Instant instante, UsuarioId usuario) {
        return new Autoria(Clock.fixed(instante, ZoneOffset.UTC), () -> usuario);
    }
}
