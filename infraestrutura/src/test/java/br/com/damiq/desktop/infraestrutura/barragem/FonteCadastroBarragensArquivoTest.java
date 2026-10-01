package br.com.damiq.desktop.infraestrutura.barragem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.barragem.FalhaFonteCadastroException;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.GrupoCampos;
import br.com.damiq.desktop.dominio.barragem.TipoCampo;
import br.com.damiq.desktop.dominio.barragem.UnidadeFederativa;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FonteCadastroBarragensArquivoTest {

    @TempDir
    Path diretorio;

    private FonteCadastroBarragensArquivo fonte(String json) throws IOException {
        var arquivo = Files.writeString(diretorio.resolve("barragens.json"), json, StandardCharsets.UTF_8);
        return new FonteCadastroBarragensArquivo(arquivo);
    }

    private static String barragem(String id, String extra) {
        return """
                {"id": "%s", "versao": 3, "nome": "Barragem %s", "empreendedor": "SANEAGO",
                 "finalidade": "Abastecimento", "municipios": ["Goiânia"], "uf": "go", "latitude": -16.57,
                 "longitude": -49.21, "curso_dagua": "Ribeirão", "tipo_macico": "CCR", "altura_macico_m": 50,
                 "capacidade_total_m3": 129000000 %s}
                """.formatted(id, id, extra);
    }

    @Test
    void exemploDaDocumentacao() {
        var exemplo = Path.of(System.getProperty("damiq.raiz", ".."), "docs", "central", "cadastro-barragens.exemplo.json");

        var publicados = new FonteCadastroBarragensArquivo(exemplo).buscar();

        assertEquals(1, publicados.size());
        var cadastro = publicados.getFirst().valido().orElseThrow(() -> new AssertionError(publicados.getFirst().erro()));
        assertEquals(new BarragemId("joao-leite"), cadastro.id());
        assertEquals("3", cadastro.versao().valor());
        assertEquals(UnidadeFederativa.GO, cadastro.uf());
        assertEquals(129_000_000, cadastro.capacidadeTotalM3());
        assertEquals(8, cadastro.grupos().size());
        assertEquals(new GrupoCampos("macico", "Maciço"), cadastro.grupos().getFirst());
        assertEquals(13, cadastro.campos().size());
        var cotaCrista = cadastro.campos().get(1);
        assertEquals("cota_crista", cotaCrista.chave());
        assertEquals(752.5, cotaCrista.numero());
        assertEquals("macico", cotaCrista.grupo());
        assertTrue(cotaCrista.padrao());
        assertEquals(TipoCampo.BOOLEANO, cadastro.campos().get(9).tipo());
        assertEquals("true", cadastro.campos().get(9).valor());
        var proprio = cadastro.campos().getLast();
        assertEquals("descarga_ecologica", proprio.chave());
        assertEquals("abastecimento", proprio.grupo());
        assertEquals("0.9", proprio.valor());
        assertFalse(proprio.padrao());
    }

    @Test
    void cadastroInvalidoEhRecusadoSemAfetarOsOutros() throws IOException {
        var publicados = fonte("{\"barragens\": [%s, %s, {\"nome\": \"sem id\"}]}".formatted(
                barragem("valida", ""), barragem("sem-altura", ", \"altura_macico_m\": null").replace(
                        "\"altura_macico_m\": 50,", ""))).buscar();

        assertEquals(3, publicados.size());
        assertTrue(publicados.get(0).valido().isPresent());
        var semAltura = publicados.get(1);
        assertEquals(new BarragemId("sem-altura"), semAltura.barragem());
        assertTrue(semAltura.erro().contains("altura_macico_m"), semAltura.erro());
        assertNull(publicados.get(2).barragem());
        assertEquals("item 3", publicados.get(2).identificacao());
    }

    @Test
    void campoComTipoOuValorInvalido() throws IOException {
        var publicados = fonte("{\"barragens\": [%s, %s]}".formatted(
                barragem("a", ", \"campos\": [{\"chave\": \"cota\", \"rotulo\": \"Cota\", \"tipo\": \"MOEDA\", \"valor\": 1}]"),
                barragem("b", ", \"campos\": [{\"chave\": \"cota\", \"rotulo\": \"Cota\", \"tipo\": \"NUMERO\", \"valor\": \"752,5\"}]")))
                .buscar();

        assertTrue(publicados.get(0).erro().contains("MOEDA"), publicados.get(0).erro());
        assertTrue(publicados.get(1).erro().contains("ponto decimal"), publicados.get(1).erro());
    }

    @Test
    void campoEmGrupoInexistenteECampoSemPadrao() throws IOException {
        var publicados = fonte("{\"barragens\": [%s, %s]}".formatted(
                barragem("a", ", \"grupos\": [{\"chave\": \"macico\", \"nome\": \"Maciço\"}], \"campos\": "
                        + "[{\"chave\": \"cota\", \"rotulo\": \"Cota\", \"grupo\": \"vertedouro\", \"tipo\": \"NUMERO\", \"valor\": 1}]"),
                barragem("b", ", \"campos\": [{\"chave\": \"cota\", \"rotulo\": \"Cota\", \"tipo\": \"NUMERO\", \"valor\": 1}]")))
                .buscar();

        assertTrue(publicados.get(0).erro().contains("vertedouro"), publicados.get(0).erro());
        var semGrupo = publicados.get(1).valido().orElseThrow().campos().getFirst();
        assertNull(semGrupo.grupo());
        assertFalse(semGrupo.padrao());
    }

    @Test
    void ufInvalida() throws IOException {
        var publicados = fonte("{\"barragens\": [%s]}".formatted(barragem("a", "").replace("\"go\"", "\"XX\""))).buscar();

        assertTrue(publicados.getFirst().erro().contains("UF inválida"), publicados.getFirst().erro());
    }

    @Test
    void arquivoAusente() {
        assertThrows(FalhaFonteCadastroException.class,
                () -> new FonteCadastroBarragensArquivo(diretorio.resolve("nao-existe.json")).buscar());
    }

    @Test
    void jsonInvalidoOuSemLista() throws IOException {
        assertThrows(FalhaFonteCadastroException.class, () -> fonte("{barragens: ").buscar());
        assertThrows(FalhaFonteCadastroException.class, () -> fonte("{\"barragem\": []}").buscar());
    }
}
