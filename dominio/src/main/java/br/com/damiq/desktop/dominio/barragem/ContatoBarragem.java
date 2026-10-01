package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Contato do PAE da barragem (RF-02), com o nível de resposta a partir do qual ele é acionado (RF-06).
 *
 * <p>No fluxograma de notificação do PAE, cada nível soma contatos aos do nível anterior: verde (1) aciona a
 * equipe técnica, o coordenador do PAE e o empreendedor; amarelo (2) acrescenta a entidade fiscalizadora;
 * vermelho (3) acrescenta a Defesa Civil, que alerta a população da ZAS.
 *
 * @param chave identificador estável do contato na barragem (minúsculas, números e {@code _}), ex.:
 *     {@code coordenador_pae}
 * @param entidade órgão ou empresa, ex.: "Defesa Civil de Goiânia"
 * @param responsavel pessoa a contatar; o PAE pede contato direto com o responsável. {@code null} se só houver o
 *     contato da entidade
 * @param cargo ex.: "Coordenador"; {@code null} se não informado
 * @param meios na ordem de preferência; ao menos um se o contato for acionado
 * @param nivelAcionamento nível de resposta (1 verde, 2 amarelo, 3 vermelho) a partir do qual o contato é
 *     acionado; {@code null} se não fizer parte do fluxo de notificação (ex.: só recebe cópia do PAE). Num
 *     substituto, é o do titular
 * @param substitui chave do contato titular que este substitui quando o titular não for encontrado (ex.: o
 *     coordenador substituto do PAE); {@code null} se não for substituto
 * @param recebeCopiaPae entidade que recebe cópia do PAE (§12)
 */
public record ContatoBarragem(
        String chave,
        PapelContato papel,
        String entidade,
        String responsavel,
        String cargo,
        List<MeioContato> meios,
        Integer nivelAcionamento,
        String substitui,
        boolean recebeCopiaPae) {

    private static final Pattern CHAVE = Pattern.compile("[a-z][a-z0-9_]{0,63}");

    public ContatoBarragem {
        chave = Validacao.textoObrigatorio(chave, "chave do contato");
        if (!CHAVE.matcher(chave).matches()) {
            throw new IllegalArgumentException(
                    "chave do contato deve ter só minúsculas, números e _ (até 64, começando por letra): " + chave);
        }
        Validacao.obrigatorio(papel, "papel do contato " + chave);
        entidade = Validacao.textoObrigatorio(entidade, "entidade do contato " + chave);
        responsavel = opcional(responsavel);
        cargo = opcional(cargo);
        meios = List.copyOf(Validacao.obrigatorio(meios, "meios do contato " + chave));
        if (nivelAcionamento != null && (nivelAcionamento < 1 || nivelAcionamento > 3)) {
            throw new IllegalArgumentException(
                    "contato " + chave + ": nível de acionamento deve ser 1, 2 ou 3: " + nivelAcionamento);
        }
        substitui = opcional(substitui);
        if (chave.equals(substitui)) {
            throw new IllegalArgumentException("contato " + chave + " não pode substituir a si mesmo");
        }
        if (nivelAcionamento != null && meios.isEmpty()) {
            throw new IllegalArgumentException("contato " + chave + ": informe ao menos um meio de contato");
        }
    }

    /** Faz parte do fluxo de notificação no nível de resposta informado (0 a 3). */
    public boolean acionadoNo(int nivelResposta) {
        return nivelAcionamento != null && nivelAcionamento <= nivelResposta;
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
