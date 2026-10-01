package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.barragem.BarragemId;

/** A operação se refere a uma barragem que não está cadastrada no Desktop. */
public class BarragemNaoCadastradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BarragemNaoCadastradaException(BarragemId id) {
        super("Barragem não cadastrada: " + id);
    }
}
