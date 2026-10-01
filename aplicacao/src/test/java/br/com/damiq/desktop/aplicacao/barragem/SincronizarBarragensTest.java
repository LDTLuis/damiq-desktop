package br.com.damiq.desktop.aplicacao.barragem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.Coordenadas;
import br.com.damiq.desktop.dominio.barragem.UnidadeFederativa;
import br.com.damiq.desktop.dominio.barragem.VersaoCadastro;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SincronizarBarragensTest {

    private static final BarragemId A = new BarragemId("a");
    private static final BarragemId B = new BarragemId("b");
    private static final BarragemId C = new BarragemId("c");

    /** Repositório em memória com cadastro e exclusão lógica. */
    private static final class Barragens implements RepositorioBarragens {

        final Map<BarragemId, Barragem> todas = new HashMap<>();
        final Map<BarragemId, CadastroBarragem> cadastros = new HashMap<>();
        final Set<BarragemId> excluidas = new HashSet<>();
        final List<BarragemId> gravacoes = new ArrayList<>();

        @Override
        public void salvar(Barragem barragem) {
            todas.put(barragem.id(), barragem);
        }

        @Override
        public Optional<Barragem> buscar(BarragemId id) {
            return excluidas.contains(id) ? Optional.empty() : Optional.ofNullable(todas.get(id));
        }

        @Override
        public List<Barragem> listar() {
            return todas.values().stream().filter(b -> !excluidas.contains(b.id()))
                    .sorted(Comparator.comparing(Barragem::nome)).toList();
        }

        @Override
        public Optional<CadastroBarragem> buscarCadastro(BarragemId id) {
            return excluidas.contains(id) ? Optional.empty() : Optional.ofNullable(cadastros.get(id));
        }

        @Override
        public Optional<VersaoCadastro> versaoCadastro(BarragemId id) {
            return Optional.ofNullable(cadastros.get(id)).map(CadastroBarragem::versao);
        }

        @Override
        public void salvarCadastro(CadastroBarragem cadastro) {
            gravacoes.add(cadastro.id());
            cadastros.put(cadastro.id(), cadastro);
            todas.put(cadastro.id(), cadastro.barragem());
            excluidas.remove(cadastro.id());
        }

        @Override
        public boolean excluir(BarragemId id) {
            return todas.containsKey(id) && excluidas.add(id);
        }
    }

    private final Barragens barragens = new Barragens();

    private static CadastroBarragem cadastro(BarragemId id, String versao) {
        return new CadastroBarragem(id, new VersaoCadastro(versao), "Barragem " + id, false, "SANEAGO", "Abastecimento",
                List.of("Goiânia"), UnidadeFederativa.GO, new Coordenadas(-16.57, -49.21), "Ribeirão", "CCR", 50,
                129_000_000, List.of(), List.of(), List.of());
    }

    private ResultadoSincronizacao sincronizar(CadastroPublicado... publicados) {
        return new SincronizarBarragens(() -> List.of(publicados), barragens).executar();
    }

    @Test
    void novasAtualizadasEInalteradas() {
        sincronizar(CadastroPublicado.valido(cadastro(A, "1")), CadastroPublicado.valido(cadastro(B, "1")));

        var resultado = sincronizar(CadastroPublicado.valido(cadastro(A, "2")), CadastroPublicado.valido(cadastro(B, "1")),
                CadastroPublicado.valido(cadastro(C, "1")));

        assertEquals(List.of(C), resultado.novas());
        assertEquals(List.of(A), resultado.atualizadas());
        assertEquals(List.of(B), resultado.inalteradas());
        assertEquals(List.of(A, B, A, C), barragens.gravacoes);
    }

    @Test
    void barragemQueSaiuDaCentralRecebeExclusaoLogica() {
        sincronizar(CadastroPublicado.valido(cadastro(A, "1")), CadastroPublicado.valido(cadastro(B, "1")));

        var resultado = sincronizar(CadastroPublicado.valido(cadastro(A, "1")));

        assertEquals(List.of(B), resultado.excluidas());
        assertTrue(barragens.buscar(B).isEmpty());
        // volta: é reativada mesmo com a mesma versão
        var volta = sincronizar(CadastroPublicado.valido(cadastro(A, "1")), CadastroPublicado.valido(cadastro(B, "1")));
        assertEquals(List.of(B), volta.atualizadas());
        assertTrue(barragens.buscar(B).isPresent());
    }

    @Test
    void cadastroRecusadoMantemACopiaAnterior() {
        sincronizar(CadastroPublicado.valido(cadastro(A, "1")), CadastroPublicado.valido(cadastro(B, "1")));

        var resultado = sincronizar(CadastroPublicado.valido(cadastro(A, "1")),
                CadastroPublicado.recusado("b", B, "altura_macico_m ausente"));

        assertEquals(1, resultado.recusados().size());
        assertTrue(resultado.excluidas().isEmpty());
        assertEquals("1", barragens.buscarCadastro(B).orElseThrow().versao().valor());
    }

    @Test
    void semNenhumCadastroValidoNadaEhExcluido() {
        sincronizar(CadastroPublicado.valido(cadastro(A, "1")));

        assertTrue(sincronizar().excluidas().isEmpty());
        assertTrue(sincronizar(CadastroPublicado.recusado("item 1", null, "id ausente")).excluidas().isEmpty());
        assertTrue(barragens.buscar(A).isPresent());
    }

    @Test
    void falhaDaFonteNaoAlteraNada() {
        sincronizar(CadastroPublicado.valido(cadastro(A, "1")));
        var comFalha = new SincronizarBarragens(() -> {
            throw new FalhaFonteCadastroException("Central fora do ar");
        }, barragens);

        assertThrows(FalhaFonteCadastroException.class, comFalha::executar);
        assertTrue(barragens.buscar(A).isPresent());
    }
}
