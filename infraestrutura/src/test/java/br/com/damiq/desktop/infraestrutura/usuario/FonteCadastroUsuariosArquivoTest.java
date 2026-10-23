package br.com.damiq.desktop.infraestrutura.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.usuario.FalhaFonteUsuariosException;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.Perfil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FonteCadastroUsuariosArquivoTest {

    private static final String HASH = "$2a$10$xLS1W84muMQxPyNj2Sc/QOyVSo8o0GATq/lTMK42I5jWzJQ7Oy.YC";

    @TempDir
    Path diretorio;

    private FonteCadastroUsuariosArquivo fonte(String json) throws IOException {
        return new FonteCadastroUsuariosArquivo(
                Files.writeString(diretorio.resolve("usuarios.json"), json, StandardCharsets.UTF_8));
    }

    @Test
    void leOExemploDoContrato() {
        var exemplo = Path.of(System.getProperty("damiq.raiz", ".."), "docs", "central", "cadastro-usuarios.exemplo.json");
        var publicados = new FonteCadastroUsuariosArquivo(exemplo).buscar();

        assertEquals(3, publicados.size());
        publicados.forEach(p -> assertTrue(p.valido().isPresent(), p.identificacao() + ": " + p.erro()));
        var engenheira = publicados.get(1).valido().orElseThrow();
        assertEquals(new Login("eng.joaoleite"), engenheira.login());
        assertEquals("4", engenheira.versao().valor());
        assertEquals(new BarragemId("joao-leite"), engenheira.barragem());
        assertEquals(Perfil.ENGENHEIRO, engenheira.perfil());
        assertEquals("CREA-GO 000000/D", engenheira.registroProfissional());
        assertFalse(engenheira.trocarSenha());
        var admin = publicados.getFirst().valido().orElseThrow();
        assertTrue(admin.trocarSenha());
        assertFalse(admin.bloqueado());
        assertNull(admin.telefone());
        assertEquals("2", publicados.get(2).valido().orElseThrow().versao().valor());
    }

    @Test
    void recusaCadastrosInvalidosUmAUm() throws IOException {
        var publicados = fonte("""
                {"usuarios": [
                  {"versao": 1},
                  {"login": "sem.hash", "versao": 1, "barragem": "b", "perfil": "TECNICO_CAMPO", "nome": "X",
                   "email": "x@x.com", "senha_hash": "123456"},
                  {"login": "perfil.errado", "versao": 1, "barragem": "b", "perfil": "GERENTE", "nome": "X",
                   "email": "x@x.com", "senha_hash": "%1$s"},
                  {"login": "eng.sem.crea", "versao": 1, "barragem": "b", "perfil": "engenheiro", "nome": "X",
                   "email": "x@x.com", "senha_hash": "%1$s"},
                  {"login": "Valido", "versao": 1, "barragem": "b", "perfil": "tecnico_campo", "nome": "X",
                   "email": "x@x.com", "senha_hash": "%1$s"}
                ]}
                """.formatted(HASH)).buscar();

        assertEquals("item 1", publicados.get(0).identificacao());
        assertNull(publicados.get(0).login());
        assertTrue(publicados.get(1).erro().contains("bcrypt"));
        assertEquals(new Login("sem.hash"), publicados.get(1).login());
        assertTrue(publicados.get(2).erro().contains("GERENTE"));
        assertTrue(publicados.get(3).erro().contains("CREA"));
        assertEquals(new Login("valido"), publicados.get(4).valido().orElseThrow().login());
    }

    @Test
    void falhaSeOArquivoNaoPuderSerLido() throws IOException {
        assertThrows(FalhaFonteUsuariosException.class,
                () -> new FonteCadastroUsuariosArquivo(diretorio.resolve("ausente.json")).buscar());
        assertThrows(FalhaFonteUsuariosException.class, () -> fonte("{").buscar());
        assertThrows(FalhaFonteUsuariosException.class, () -> fonte("{\"barragens\": []}").buscar());
    }
}
