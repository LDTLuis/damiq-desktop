package br.com.damiq.desktop.aplicacao.importacao;

import br.com.damiq.desktop.aplicacao.medicao.ResultadoProcessamento;
import br.com.damiq.desktop.aplicacao.motor.Rejeicao;
import br.com.damiq.desktop.dominio.Validacao;
import java.util.List;

/**
 * Resultado de {@link ImportarMedicoes}.
 *
 * @param rejeicoes leituras recusadas, com a linha do arquivo, para o técnico corrigir
 */
public record ResultadoImportacao(
        String arquivo, ResultadoProcessamento processamento, List<RejeicaoNaLinha> rejeicoes) {

    public ResultadoImportacao {
        arquivo = Validacao.textoObrigatorio(arquivo, "nome do arquivo");
        Validacao.obrigatorio(processamento, "processamento");
        rejeicoes = List.copyOf(Validacao.obrigatorio(rejeicoes, "rejeições"));
    }

    public record RejeicaoNaLinha(int linha, Rejeicao rejeicao) {

        public RejeicaoNaLinha {
            Validacao.obrigatorio(rejeicao, "rejeição");
        }

        @Override
        public String toString() {
            return "linha " + linha + ": " + rejeicao.mensagem() + " (" + rejeicao.codigo() + ")";
        }
    }
}
