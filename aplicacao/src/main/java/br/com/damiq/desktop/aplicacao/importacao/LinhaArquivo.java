package br.com.damiq.desktop.aplicacao.importacao;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.dominio.Validacao;

/**
 * Leitura lida de um arquivo, com a linha onde está.
 *
 * @param linha número da linha como o técnico vê no editor ou na planilha (a primeira é 1)
 */
public record LinhaArquivo(int linha, LeituraInformada leitura) {

    public LinhaArquivo {
        if (linha < 1) {
            throw new IllegalArgumentException("número da linha deve ser positivo: " + linha);
        }
        Validacao.obrigatorio(leitura, "leitura");
    }
}
