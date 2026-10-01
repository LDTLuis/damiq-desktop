package br.com.damiq.desktop.infraestrutura.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeiturasInvalidoException;
import br.com.damiq.desktop.aplicacao.importacao.LinhaArquivo;
import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeitorPlanilhaTest {

    @TempDir
    Path diretorio;

    private final LeitorArquivoLeiturasPadrao leitor = new LeitorArquivoLeiturasPadrao();

    /** Planilha como a equipe de campo monta: data como data do Excel, valor como número. */
    private Path planilha(Workbook livro, String nome) throws IOException {
        try (livro) {
            var formatoData = livro.createCellStyle();
            formatoData.setDataFormat(livro.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            var aba = livro.createSheet("Leituras");
            aba.createRow(0); // linha em branco antes dos títulos
            var titulos = aba.createRow(1);
            var nomes = List.of("Instrumento", "Tipo", "Data/Hora", "Valor", "Unidade", "Valor x 2");
            for (int i = 0; i < nomes.size(); i++) {
                titulos.createCell(i).setCellValue(nomes.get(i));
            }
            var linha = aba.createRow(2);
            linha.createCell(0).setCellValue("PZ-01");
            linha.createCell(1).setCellValue("Pressão");
            var data = linha.createCell(2);
            data.setCellValue(LocalDateTime.of(2026, 9, 29, 8, 0));
            data.setCellStyle(formatoData);
            linha.createCell(3).setCellValue(1.4);
            linha.createCell(4).setCellValue("bar");
            linha.createCell(5).setCellFormula("D3*2");

            var codigoNumerico = aba.createRow(4);
            codigoNumerico.createCell(0).setCellValue(12);
            codigoNumerico.createCell(1).setCellValue("nivel");
            codigoNumerico.createCell(2).setCellValue("29/09/2026 09:00");
            codigoNumerico.createCell(3).setCellFormula("700+45.25");
            codigoNumerico.createCell(4).setCellValue("m");
            livro.getCreationHelper().createFormulaEvaluator().evaluateAll();

            var arquivo = diretorio.resolve(nome);
            try (OutputStream saida = Files.newOutputStream(arquivo)) {
                livro.write(saida);
            }
            return arquivo;
        }
    }

    private static final List<LinhaArquivo> ESPERADAS = List.of(
            new LinhaArquivo(3, new LeituraInformada("PZ-01", "Pressão", "2026-09-29T08:00:00", "1.4", "bar")),
            new LinhaArquivo(5, new LeituraInformada("12", "nivel", "2026-09-29T09:00:00", "745.25", "m")));

    @Test
    void xlsx() throws IOException {
        var arquivo = leitor.ler(planilha(new XSSFWorkbook(), "campo.xlsx"));

        assertEquals("campo.xlsx", arquivo.nome());
        assertEquals(ESPERADAS, arquivo.linhas());
    }

    @Test
    void xlsAntigo() throws IOException {
        assertEquals(ESPERADAS, leitor.ler(planilha(new HSSFWorkbook(), "campo.xls")).linhas());
    }

    @Test
    void arquivoQueNaoEhPlanilha() throws IOException {
        var arquivo = Files.writeString(diretorio.resolve("campo.xlsx"), "isto não é uma planilha");

        var falha = assertThrows(ArquivoLeiturasInvalidoException.class, () -> leitor.ler(arquivo));

        assertTrue(falha.getMessage().contains("campo.xlsx"), falha.getMessage());
    }

    @Test
    void formatoNaoSuportado() throws IOException {
        var arquivo = Files.writeString(diretorio.resolve("campo.ods"), "");

        var falha = assertThrows(ArquivoLeiturasInvalidoException.class, () -> leitor.ler(arquivo));

        assertTrue(falha.getMessage().contains("Formato não suportado"), falha.getMessage());
    }

    @Test
    void arquivoInexistente() {
        assertThrows(ArquivoLeiturasInvalidoException.class, () -> leitor.ler(diretorio.resolve("nao-existe.csv")));
    }
}
