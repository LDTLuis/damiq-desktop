package br.com.damiq.desktop.infraestrutura.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.configuracao.FalhaFonteConfiguracaoException;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FonteConfiguracaoArquivoTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");

    @TempDir
    Path diretorio;

    private FonteConfiguracaoArquivo fonte(String conteudo) throws IOException {
        Files.writeString(diretorio.resolve("joao-leite.json"), conteudo, StandardCharsets.UTF_8);
        return new FonteConfiguracaoArquivo(diretorio);
    }

    @Test
    void leOArquivoDaBarragem() throws IOException {
        var conteudo = "{\"versao\": 21, \"fuso_padrao\": \"-03:00\", \"observacao\": \"ação\"}";

        var configuracao = fonte(conteudo).buscar(JOAO_LEITE);

        assertEquals("21", configuracao.versao().valor());
        assertEquals(conteudo, configuracao.conteudoJson());
    }

    @Test
    void versaoEmTexto() throws IOException {
        assertEquals("2026.10-a", fonte("{\"versao\": \"2026.10-a\"}").buscar(JOAO_LEITE).versao().valor());
    }

    @Test
    void arquivoAusente() {
        var falha = assertThrows(
                FalhaFonteConfiguracaoException.class, () -> new FonteConfiguracaoArquivo(diretorio).buscar(JOAO_LEITE));

        assertTrue(falha.getMessage().contains("não encontrado"), falha.getMessage());
    }

    @Test
    void jsonInvalido() throws IOException {
        var fonte = fonte("{versao: 21");

        var falha = assertThrows(FalhaFonteConfiguracaoException.class, () -> fonte.buscar(JOAO_LEITE));

        assertTrue(falha.getMessage().contains("não é um JSON válido"), falha.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"versao\": null}", "{\"versao\": true}", "{\"versao\": 2.5}", "{\"versao\": \"  \"}", "[]"})
    void versaoAusenteOuInvalida(String conteudo) throws IOException {
        var fonte = fonte(conteudo);

        assertThrows(FalhaFonteConfiguracaoException.class, () -> fonte.buscar(JOAO_LEITE));
    }
}
