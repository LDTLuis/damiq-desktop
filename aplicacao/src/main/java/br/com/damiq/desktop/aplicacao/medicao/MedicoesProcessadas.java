package br.com.damiq.desktop.aplicacao.medicao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;

/** Evento: um processamento de medições foi gravado. */
public record MedicoesProcessadas(BarragemId barragem, long processamento, int alertas) {

    public MedicoesProcessadas {
        Validacao.obrigatorio(barragem, "barragem");
    }
}
