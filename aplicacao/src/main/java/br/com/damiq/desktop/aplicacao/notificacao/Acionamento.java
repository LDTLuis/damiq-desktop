package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.ContatoBarragem;
import java.util.List;

/**
 * Quem acionar, e em que ordem, num nível de resposta do PAE (RF-06).
 *
 * @param nivelResposta 0 a 3; no nível 0 ninguém é acionado
 * @param verificarInstrumentoAntes a leitura que gerou o alerta está fora da faixa plausível: verificar o
 *     instrumento antes de acionar o PAE
 * @param contatos na ordem de acionamento: por nível (verde, amarelo, vermelho) e, dentro do nível, na ordem
 *     do cadastro
 * @param pendencias papéis que o PAE prevê neste nível mas que não têm contato no cadastro da Central (ex.: sem
 *     Defesa Civil no nível vermelho); vazio se o cadastro estiver completo
 */
public record Acionamento(
        BarragemId barragem,
        int nivelResposta,
        boolean verificarInstrumentoAntes,
        List<Contato> contatos,
        List<String> pendencias) {

    public Acionamento {
        Validacao.obrigatorio(barragem, "barragem");
        if (nivelResposta < 0 || nivelResposta > 3) {
            throw new IllegalArgumentException("nível de resposta deve estar entre 0 e 3: " + nivelResposta);
        }
        contatos = List.copyOf(Validacao.obrigatorio(contatos, "contatos"));
        pendencias = List.copyOf(Validacao.obrigatorio(pendencias, "pendências"));
    }

    /**
     * Um contato a acionar.
     *
     * @param substitutos a acionar, na ordem, se o titular não for encontrado
     */
    public record Contato(ContatoBarragem titular, List<ContatoBarragem> substitutos) {

        public Contato {
            Validacao.obrigatorio(titular, "contato");
            substitutos = List.copyOf(Validacao.obrigatorio(substitutos, "substitutos"));
        }
    }
}
