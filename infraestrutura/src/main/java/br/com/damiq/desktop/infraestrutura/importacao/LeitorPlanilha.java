package br.com.damiq.desktop.infraestrutura.importacao;

import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeituras;
import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeiturasInvalidoException;
import br.com.damiq.desktop.aplicacao.importacao.LinhaArquivo;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Planilha Excel ({@code .xlsx} ou {@code .xls}) via Apache POI: lê a primeira aba. Células de data do Excel
 * viram ISO-8601; números viram texto com ponto decimal; fórmulas valem pelo último resultado calculado.
 */
final class LeitorPlanilha {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("uuuu-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private LeitorPlanilha() {}

    static ArquivoLeituras ler(String nome, Path arquivo) {
        try (var livro = WorkbookFactory.create(arquivo.toFile(), null, true)) {
            if (livro.getNumberOfSheets() == 0) {
                throw new ArquivoLeiturasInvalidoException("A planilha " + nome + " não tem abas");
            }
            var aba = livro.getSheetAt(0);

            Cabecalho cabecalho = null;
            var linhas = new ArrayList<LinhaArquivo>();
            for (var linha : aba) {
                if (cabecalho == null) {
                    var titulos = titulos(linha);
                    if (titulos.stream().anyMatch(t -> !t.isBlank())) {
                        cabecalho = Cabecalho.de(titulos, nome);
                    }
                    continue;
                }
                var colunas = cabecalho;
                IntFunction<String> celula = i -> texto(linha.getCell(i), colunas.papelDaColuna(i));
                if (!cabecalho.vazia(celula)) {
                    linhas.add(new LinhaArquivo(linha.getRowNum() + 1, cabecalho.leitura(celula)));
                }
            }
            if (cabecalho == null) {
                throw new ArquivoLeiturasInvalidoException("A planilha " + nome + " está vazia");
            }
            return new ArquivoLeituras(nome, linhas);
        } catch (ArquivoLeiturasInvalidoException e) {
            throw e;
        } catch (EncryptedDocumentException e) {
            throw new ArquivoLeiturasInvalidoException("A planilha " + nome + " é protegida por senha", e);
        } catch (IOException | RuntimeException e) {
            // POI sinaliza arquivo corrompido ou que não é planilha com exceções não verificadas
            throw new ArquivoLeiturasInvalidoException(
                    "Não foi possível ler a planilha " + nome + ": " + e.getMessage(), e);
        }
    }

    private static List<String> titulos(Row linha) {
        var titulos = new ArrayList<String>();
        for (int i = 0; i < Math.max(linha.getLastCellNum(), 0); i++) {
            titulos.add(texto(linha.getCell(i), null));
        }
        return titulos;
    }

    /** Texto da célula; datas no formato que o papel da coluna espera. */
    static String texto(Cell celula, Papel papel) {
        if (celula == null) {
            return "";
        }
        var tipo = celula.getCellType() == CellType.FORMULA ? celula.getCachedFormulaResultType() : celula.getCellType();
        return switch (tipo) {
            case STRING -> celula.getStringCellValue().strip();
            case BOOLEAN -> String.valueOf(celula.getBooleanCellValue());
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(celula)) {
                    var momento = celula.getLocalDateTimeCellValue();
                    yield switch (papel == null ? Papel.DATA_HORA : papel) {
                        case DATA -> DATA.format(momento);
                        case HORA -> HORA.format(momento);
                        default -> DATA_HORA.format(momento);
                    };
                }
                yield BigDecimal.valueOf(celula.getNumericCellValue()).stripTrailingZeros().toPlainString();
            }
            default -> "";
        };
    }
}
