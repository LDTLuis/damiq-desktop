package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Barragem monitorada. Por enquanto só identificação; os metadados do cadastro (RF-02) entram depois.
 */
public record Barragem(BarragemId id, String nome) {

    public Barragem {
        Validacao.obrigatorio(id, "identificador da barragem");
        nome = Validacao.textoObrigatorio(nome, "nome da barragem");
    }
}
