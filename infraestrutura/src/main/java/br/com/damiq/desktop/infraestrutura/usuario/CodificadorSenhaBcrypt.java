package br.com.damiq.desktop.infraestrutura.usuario;

import br.com.damiq.desktop.aplicacao.usuario.CodificadorSenha;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * {@link CodificadorSenha} com bcrypt (custo 12 por padrão, ≈ 0,25 s por conferência): o mesmo formato
 * ({@code $2a$}, {@code $2b$} ou {@code $2y$}) que a Central publica. A senha vai ao bcrypt como bytes UTF-8, que
 * são apagados logo depois.
 */
public final class CodificadorSenhaBcrypt implements CodificadorSenha {

    public static final int CUSTO_PADRAO = 12;

    private static final Pattern FORMATO = Pattern.compile("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}");

    private final int custo;

    public CodificadorSenhaBcrypt(int custo) {
        if (custo < 4 || custo > 31) {
            throw new IllegalArgumentException("custo do bcrypt deve estar entre 4 e 31: " + custo);
        }
        this.custo = custo;
    }

    public CodificadorSenhaBcrypt() {
        this(CUSTO_PADRAO);
    }

    /** Se o texto é um hash bcrypt bem formado. */
    public static boolean formatoValido(String hash) {
        return hash != null && FORMATO.matcher(hash).matches();
    }

    @Override
    public SenhaHash codificar(char[] senha) {
        var bytes = utf8(Validacao.obrigatorio(senha, "senha"));
        try {
            return new SenhaHash(BCrypt.hashpw(bytes, BCrypt.gensalt(custo)));
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    @Override
    public boolean confere(char[] senha, SenhaHash hash) {
        if (senha == null || hash == null || !formatoValido(hash.valor())) {
            return false;
        }
        var bytes = utf8(senha);
        try {
            return BCrypt.checkpw(bytes, hash.valor());
        } catch (IllegalArgumentException e) {
            return false; // ex.: senha acima de 72 bytes
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    private static byte[] utf8(char[] senha) {
        var buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(senha));
        var bytes = Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit());
        Arrays.fill(buffer.array(), (byte) 0);
        return bytes;
    }
}
