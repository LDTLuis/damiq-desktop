package br.com.damiq.desktop.aplicacao.medicao;

/**
 * Leitura como foi digitada pelo técnico ou importada de CSV/XLSX (RF-03), ainda sem validação.
 *
 * <p>Os campos são texto livre e podem vir vazios ou nulos: quem valida e normaliza é o motor, que recusa só
 * o registro com problema (com o índice da linha) sem reprovar o lote. Ex.: tipo {@code "Pressão"}, momento
 * {@code "2026-09-29T08:00:00"} (sem fuso vale o fuso da configuração), valor {@code "1,4"}, unidade
 * {@code "bar"}.
 */
public record LeituraInformada(String instrumento, String tipo, String momento, String valor, String unidade) {}
