package br.com.damiq.desktop.aplicacao.importacao;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.List;

/**
 * Leituras de um arquivo CSV ou XLSX, na ordem do arquivo, sem as linhas em branco.
 *
 * @param nome nome do arquivo (sem o diretório)
 */
public record ArquivoLeituras(String nome, List<LinhaArquivo> linhas) {

    public ArquivoLeituras {
        nome = Validacao.textoObrigatorio(nome, "nome do arquivo");
        linhas = List.copyOf(Validacao.obrigatorio(linhas, "linhas do arquivo"));
    }
}
