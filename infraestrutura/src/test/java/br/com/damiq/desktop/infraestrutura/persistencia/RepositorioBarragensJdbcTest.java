package br.com.damiq.desktop.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositorioBarragensJdbcTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");

    @TempDir
    Path diretorio;

    private RepositorioBarragensJdbc repositorio;

    @BeforeEach
    void criarBanco() {
        repositorio = new RepositorioBarragensJdbc(BancoDados.abrir(diretorio.resolve("damiq.db")), AutoriaDeTeste.SISTEMA);
    }

    @Test
    void salvaEBusca() {
        repositorio.salvar(new Barragem(JOAO_LEITE, "João Leite"));

        assertEquals(new Barragem(JOAO_LEITE, "João Leite"), repositorio.buscar(JOAO_LEITE).orElseThrow());
    }

    @Test
    void salvarDeNovoAtualizaONome() {
        repositorio.salvar(new Barragem(JOAO_LEITE, "João Leite"));
        repositorio.salvar(new Barragem(JOAO_LEITE, "Barragem do Ribeirão João Leite"));

        assertEquals("Barragem do Ribeirão João Leite", repositorio.buscar(JOAO_LEITE).orElseThrow().nome());
    }

    @Test
    void barragemInexistente() {
        assertTrue(repositorio.buscar(JOAO_LEITE).isEmpty());
    }
}
