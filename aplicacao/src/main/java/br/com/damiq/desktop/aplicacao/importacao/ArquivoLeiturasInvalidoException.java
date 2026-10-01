package br.com.damiq.desktop.aplicacao.importacao;

/**
 * O arquivo não pôde ser lido como uma lista de leituras: formato não suportado, colunas obrigatórias
 * ausentes, arquivo corrompido ou sem leituras. Problemas em linhas isoladas não causam isso: elas vão ao
 * motor e voltam como rejeições.
 */
public class ArquivoLeiturasInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ArquivoLeiturasInvalidoException(String mensagem) {
        super(mensagem);
    }

    public ArquivoLeiturasInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
