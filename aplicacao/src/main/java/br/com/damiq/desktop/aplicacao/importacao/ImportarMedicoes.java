package br.com.damiq.desktop.aplicacao.importacao;

import br.com.damiq.desktop.aplicacao.importacao.ResultadoImportacao.RejeicaoNaLinha;
import br.com.damiq.desktop.aplicacao.medicao.OrigemLeituras;
import br.com.damiq.desktop.aplicacao.medicao.ProcessarMedicoes;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importa um arquivo CSV ou XLSX de leituras de campo (RF-03): lê, processa com {@link ProcessarMedicoes} e
 * devolve as rejeições com a linha do arquivo.
 */
public final class ImportarMedicoes {

    private static final Logger LOG = LoggerFactory.getLogger(ImportarMedicoes.class);

    private final LeitorArquivoLeituras leitor;
    private final ProcessarMedicoes processar;

    public ImportarMedicoes(LeitorArquivoLeituras leitor, ProcessarMedicoes processar) {
        this.leitor = Validacao.obrigatorio(leitor, "leitor de arquivos");
        this.processar = Validacao.obrigatorio(processar, "processamento de medições");
    }

    /**
     * @throws ArquivoLeiturasInvalidoException se o arquivo não puder ser lido ou não tiver leituras
     * @see ProcessarMedicoes#executar as demais exceções
     */
    public ResultadoImportacao executar(BarragemId barragem, Path arquivo) {
        var lido = leitor.ler(Validacao.obrigatorio(arquivo, "arquivo"));
        if (lido.linhas().isEmpty()) {
            throw new ArquivoLeiturasInvalidoException("O arquivo " + lido.nome() + " não tem leituras");
        }

        var leituras = lido.linhas().stream().map(LinhaArquivo::leitura).toList();
        var processamento = processar.executar(barragem, leituras, OrigemLeituras.IMPORTACAO, lido.nome());

        var rejeicoes = processamento.lote().rejeicoes().stream()
                .map(r -> new RejeicaoNaLinha(lido.linhas().get(r.indice()).linha(), r))
                .toList();
        rejeicoes.forEach(r -> LOG.info("{}: {}", lido.nome(), r));
        return new ResultadoImportacao(lido.nome(), processamento, rejeicoes);
    }
}
