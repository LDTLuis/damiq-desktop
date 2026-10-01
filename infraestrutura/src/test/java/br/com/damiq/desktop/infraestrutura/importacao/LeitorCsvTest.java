package br.com.damiq.desktop.infraestrutura.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeituras;
import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeiturasInvalidoException;
import br.com.damiq.desktop.aplicacao.importacao.LinhaArquivo;
import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeitorCsvTest {

    private static ArquivoLeituras ler(String conteudo) {
        return LeitorCsv.ler("campo.csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void formatoDoExcelEmPortugues() {
        // ponto e vírgula, vírgula decimal, data brasileira, Windows-1252 e CRLF
        var conteudo = "Instrumento;Tipo;Data/Hora;Valor;Unidade\r\n"
                + "PZ-01;Pressão;29/09/2026 08:00;1,4;bar\r\n"
                + "NV-01;Nível;29/09/2026 08:00;745,2;m\r\n";

        var arquivo = LeitorCsv.ler("campo.csv", conteudo.getBytes(Charset.forName("windows-1252")));

        assertEquals("campo.csv", arquivo.nome());
        assertEquals(
                List.of(
                        new LinhaArquivo(2, new LeituraInformada("PZ-01", "Pressão", "2026-09-29T08:00:00", "1,4", "bar")),
                        new LinhaArquivo(3, new LeituraInformada("NV-01", "Nível", "2026-09-29T08:00:00", "745,2", "m"))),
                arquivo.linhas());
    }

    @Test
    void utf8ComBomEVirgula() {
        var conteudo = "﻿sensor,tipo,timestamp,valor,unidade\n"
                + "PZ-01,pressão,2026-09-29T08:00:00-03:00,140,kPa\n";

        var leitura = ler(conteudo).linhas().getFirst().leitura();

        assertEquals("PZ-01", leitura.instrumento());
        assertEquals("pressão", leitura.tipo());
        assertEquals("2026-09-29T08:00:00-03:00", leitura.momento());
    }

    @Test
    void aspasComSeparadorDentro() {
        var conteudo = "instrumento,tipo,data_hora,valor,unidade\n"
                + "\"PZ-01\",pressao,2026-09-29T08:00:00,\"1,4\",\"bar\"\n";

        assertEquals("1,4", ler(conteudo).linhas().getFirst().leitura().valor());
    }

    @Test
    void dataEHoraEmColunasSeparadasEColunasExtras() {
        var conteudo = "Data;Hora;Ponto;Tipo;Leitura;Unid.;Observação\n"
                + "29/09/2026;8:00;PZ-01;pressao;140;kPa;chuva forte\n";

        var leitura = ler(conteudo).linhas().getFirst().leitura();

        assertEquals(new LeituraInformada("PZ-01", "pressao", "2026-09-29T08:00:00", "140", "kPa"), leitura);
    }

    @Test
    void linhasEmBrancoSaoIgnoradasMasANumeracaoSegueOArquivo() {
        var conteudo = "\ninstrumento;tipo;data_hora;valor;unidade\n"
                + "PZ-01;pressao;29/09/2026 08:00;140;kPa\n"
                + ";;;;\n"
                + "\n"
                + "PZ-01;pressao;29/09/2026 09:00;141;kPa\n";

        var linhas = ler(conteudo).linhas();

        assertEquals(List.of(3, 6), linhas.stream().map(LinhaArquivo::linha).toList());
    }

    @Test
    void campoVazioViraNuloParaOMotorRecusarALinha() {
        var conteudo = "instrumento;tipo;data_hora;valor;unidade\nPZ-01;pressao;29/09/2026 08:00;;kPa\n";

        assertNull(ler(conteudo).linhas().getFirst().leitura().valor());
    }

    @Test
    void faltaColunaObrigatoria() {
        var falha = assertThrows(ArquivoLeiturasInvalidoException.class,
                () -> ler("instrumento;data_hora;valor\nPZ-01;29/09/2026 08:00;140\n"));

        assertTrue(falha.getMessage().contains("tipo"), falha.getMessage());
        assertTrue(falha.getMessage().contains("unidade"), falha.getMessage());
    }

    @Test
    void colunaRepetida() {
        assertThrows(ArquivoLeiturasInvalidoException.class,
                () -> ler("instrumento;sensor;tipo;data_hora;valor;unidade\n"));
    }

    @Test
    void arquivoVazio() {
        assertThrows(ArquivoLeiturasInvalidoException.class, () -> ler("\n\n"));
    }

    @Test
    void soTitulosNaoTemLeituras() {
        assertTrue(ler("instrumento;tipo;data_hora;valor;unidade\n").linhas().isEmpty());
    }

    @Test
    void quebraDeLinhaDentroDeAspasNaoSeparaRegistros() {
        var conteudo = "instrumento;tipo;data_hora;valor;unidade;obs\n"
                + "PZ-01;pressao;29/09/2026 08:00;140;kPa;\"duas\nlinhas\"\n"
                + "PZ-01;pressao;29/09/2026 09:00;141;kPa;\n";

        assertEquals(List.of(2, 4), ler(conteudo).linhas().stream().map(LinhaArquivo::linha).toList());
    }
}
