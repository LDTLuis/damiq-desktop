package br.com.damiq.desktop.infraestrutura.importacao;

import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeituras;
import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeiturasInvalidoException;
import br.com.damiq.desktop.aplicacao.importacao.LeitorArquivoLeituras;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Lê CSV ({@code .csv}, {@code .txt}) ou planilha Excel ({@code .xlsx}, {@code .xls}), escolhido pela extensão.
 *
 * <p>Formato esperado: uma leitura por linha, com uma linha de títulos. Colunas obrigatórias: instrumento,
 * tipo, data_hora (ou data e hora em colunas separadas), valor e unidade. Os títulos aceitam variações
 * (ver {@link Papel}) e colunas extras são ignoradas.
 */
public final class LeitorArquivoLeiturasPadrao implements LeitorArquivoLeituras {

    @Override
    public ArquivoLeituras ler(Path arquivo) {
        var nome = arquivo.getFileName().toString();
        if (!Files.isRegularFile(arquivo)) {
            throw new ArquivoLeiturasInvalidoException("Arquivo não encontrado: " + arquivo);
        }
        var extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        return switch (extensao) {
            case "csv", "txt" -> LeitorCsv.ler(nome, bytes(arquivo));
            case "xlsx", "xls" -> LeitorPlanilha.ler(nome, arquivo);
            default -> throw new ArquivoLeiturasInvalidoException(
                    "Formato não suportado: " + nome + ". Use CSV (.csv) ou planilha Excel (.xlsx, .xls)");
        };
    }

    private static byte[] bytes(Path arquivo) {
        try {
            return Files.readAllBytes(arquivo);
        } catch (NoSuchFileException e) {
            throw new ArquivoLeiturasInvalidoException("Arquivo não encontrado: " + arquivo, e);
        } catch (IOException e) {
            throw new ArquivoLeiturasInvalidoException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        }
    }
}
