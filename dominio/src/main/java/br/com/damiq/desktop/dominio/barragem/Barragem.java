package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Barragem monitorada: o cadastro principal, ao qual pertencem todos os dados. Por enquanto só identificação;
 * os metadados do cadastro (RF-02) entram depois.
 *
 * @param teste barragem de teste (UAT, RF-13): todos os seus dados nascem como teste e podem ser apagados
 *     fisicamente; não muda depois do cadastro
 */
public record Barragem(BarragemId id, String nome, boolean teste) {

    public Barragem {
        Validacao.obrigatorio(id, "identificador da barragem");
        nome = Validacao.textoObrigatorio(nome, "nome da barragem");
    }

    /** Barragem real (não de teste). */
    public Barragem(BarragemId id, String nome) {
        this(id, nome, false);
    }
}
