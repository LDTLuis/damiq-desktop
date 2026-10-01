package br.com.damiq.desktop.aplicacao.importacao;

import java.nio.file.Path;

/** Lê as leituras de um arquivo fornecido pela equipe de campo (RF-03). */
public interface LeitorArquivoLeituras {

    /**
     * Lê o arquivo sem validar os valores; quem valida cada leitura é o motor.
     *
     * @throws ArquivoLeiturasInvalidoException se o arquivo não puder ser lido como uma lista de leituras
     */
    ArquivoLeituras ler(Path arquivo);
}
