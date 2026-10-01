package br.com.damiq.desktop.aplicacao.medicao;

/** Gravação dos processamentos de medições. */
public interface RepositorioProcessamentos {

    /**
     * Grava o processamento com medições, rejeições, lacunas e alertas, numa única transação.
     *
     * @return identificador do processamento
     */
    long registrar(RegistroProcessamento registro);
}
