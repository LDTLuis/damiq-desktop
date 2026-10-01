package br.com.damiq.desktop.dominio.alerta;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.List;

/**
 * Nível de resposta do PAE correspondente ao status da barragem (0 a 3), com as ações recomendadas.
 *
 * @param cor {@code verde}, {@code amarelo} ou {@code vermelho}; {@code null} no nível 0
 * @param fonte referência normativa do nível; {@code null} se o motor não informar
 */
public record NivelResposta(int nivel, String cor, String rotulo, String situacao, List<String> acoes, String fonte) {

    public NivelResposta {
        if (nivel < 0 || nivel > 3) {
            throw new IllegalArgumentException("nível de resposta deve estar entre 0 e 3: " + nivel);
        }
        rotulo = Validacao.textoObrigatorio(rotulo, "rótulo do nível de resposta");
        situacao = Validacao.textoObrigatorio(situacao, "situação do nível de resposta");
        acoes = List.copyOf(Validacao.obrigatorio(acoes, "ações do nível de resposta"));
    }

    /** Níveis 1 a 3 exigem os procedimentos do PAE; o nível 0 é a situação normal. */
    public boolean exigeAcao() {
        return nivel > 0;
    }
}
