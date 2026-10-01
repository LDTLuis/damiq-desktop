package br.com.damiq.desktop.dominio.usuario;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.barragem.BarragemId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UsuarioTest {

    private static final Login ANA = new Login("ana.souza");
    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final SenhaHash HASH = new SenhaHash("$2a$10$hash");

    private static CadastroUsuario cadastro(Perfil perfil, String email, String registro) {
        return new CadastroUsuario(ANA, new VersaoUsuario("1"), JOAO_LEITE, perfil, "Ana Souza", email, null, " ",
                registro, HASH, false, true);
    }

    @Test
    void loginEmMinusculasESemEspacos() {
        assertEquals("ana.souza", new Login("  Ana.Souza ").valor());
        assertEquals(new Login("ANA.SOUZA"), new Login("ana.souza"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"an", "ana souza", "joão", ".ana", "ana;drop"})
    void rejeitaLoginForaDoFormato(String login) {
        assertThrows(IllegalArgumentException.class, () -> new Login(login));
    }

    @Test
    void engenheiroPrecisaDeRegistroProfissional() {
        var erro = assertThrows(IllegalArgumentException.class, () -> cadastro(Perfil.ENGENHEIRO, "ana@x.com", " "));
        assertTrue(erro.getMessage().contains("CREA"));
        assertEquals("CREA-GO 1234/D", cadastro(Perfil.ENGENHEIRO, "ana@x.com", " CREA-GO 1234/D ").registroProfissional());
        assertNull(cadastro(Perfil.TECNICO_CAMPO, "ana@x.com", null).registroProfissional());
    }

    @Test
    void opcionaisEmBrancoViramNulo() {
        assertNull(cadastro(Perfil.TECNICO_CAMPO, "ana@x.com", null).cargo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "@x.com", "ana@", "ana@@x.com", "ana souza@x.com"})
    void rejeitaEmailInvalido(String email) {
        assertThrows(IllegalArgumentException.class, () -> cadastro(Perfil.TECNICO_CAMPO, email, null));
    }

    @Test
    void hashNaoApareceNoTexto() {
        assertFalse(HASH.toString().contains("$2a$"));
    }

    @Test
    void permissoesPorPerfil() {
        assertTrue(Perfil.TECNICO_CAMPO.permite(Permissao.REGISTRAR_LEITURAS));
        assertFalse(Perfil.TECNICO_CAMPO.permite(Permissao.RECONHECER_NOTIFICACOES));
        assertTrue(Perfil.ENGENHEIRO.permite(Permissao.RECONHECER_NOTIFICACOES));
        assertFalse(Perfil.ENGENHEIRO.permite(Permissao.DESBLOQUEAR_USUARIOS));
        assertTrue(Perfil.ADMINISTRADOR.permite(Permissao.DESBLOQUEAR_USUARIOS));
        assertFalse(Perfil.ADMINISTRADOR.permite(Permissao.REGISTRAR_LEITURAS));
        for (var perfil : Perfil.values()) {
            assertTrue(perfil.permite(Permissao.CONSULTAR_DADOS), perfil.name());
        }
    }

    @Test
    void senhaForte() {
        assertDoesNotThrow(() -> PoliticaSenha.verificar("barragem2026".toCharArray(), ANA));
        assertThrows(IllegalArgumentException.class, () -> PoliticaSenha.verificar("curta1".toCharArray(), ANA));
        assertThrows(IllegalArgumentException.class, () -> PoliticaSenha.verificar("semnumeros!".toCharArray(), ANA));
        assertThrows(IllegalArgumentException.class, () -> PoliticaSenha.verificar("1234567890".toCharArray(), ANA));
        assertThrows(IllegalArgumentException.class, () -> PoliticaSenha.verificar("x".repeat(64).concat("1").toCharArray(), ANA));
        var comLogin = assertThrows(IllegalArgumentException.class,
                () -> PoliticaSenha.verificar("2026ANA.Souza".toCharArray(), ANA));
        assertTrue(comLogin.getMessage().contains("login"));
        assertThrows(IllegalArgumentException.class, () -> PoliticaSenha.verificar(null, ANA));
    }
}
