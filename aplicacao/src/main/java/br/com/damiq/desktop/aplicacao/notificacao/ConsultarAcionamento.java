package br.com.damiq.desktop.aplicacao.notificacao;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.ContatoBarragem;
import br.com.damiq.desktop.dominio.barragem.PapelContato;
import java.util.Comparator;
import java.util.List;

/**
 * Quem acionar em cada nível de resposta, segundo os contatos do PAE no cadastro da barragem (RF-02 e RF-06).
 * Os contatos vêm da Central; aqui só se monta a ordem de acionamento do fluxograma de notificação.
 */
public final class ConsultarAcionamento {

    /** Papéis que o fluxograma de notificação do PAE aciona a partir de cada nível (PAE §6.1 e §7.2). */
    private static final List<PapelPrevisto> PAPEIS_PREVISTOS = List.of(
            new PapelPrevisto(PapelContato.COORDENADOR_PAE, "coordenador do PAE", 1),
            new PapelPrevisto(PapelContato.EMPREENDEDOR, "empreendedor", 1),
            new PapelPrevisto(PapelContato.ENTIDADE_FISCALIZADORA, "entidade fiscalizadora", 2),
            new PapelPrevisto(PapelContato.DEFESA_CIVIL, "Defesa Civil", 3));

    private record PapelPrevisto(PapelContato papel, String nome, int nivel) {}

    private final RepositorioBarragens barragens;

    public ConsultarAcionamento(RepositorioBarragens barragens) {
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
    }

    /** Acionamento para o nível de resposta da notificação. */
    public Acionamento para(Notificacao notificacao) {
        return montar(notificacao.barragem(), notificacao.nivelResposta(), notificacao.leituraSuspeita());
    }

    /** Acionamento num nível de resposta (0 a 3), ex.: para a tela do PAE. */
    public Acionamento para(BarragemId barragem, int nivelResposta) {
        return montar(Validacao.obrigatorio(barragem, "barragem"), nivelResposta, false);
    }

    private Acionamento montar(BarragemId barragem, int nivelResposta, boolean leituraSuspeita) {
        if (nivelResposta == 0) {
            return new Acionamento(barragem, 0, false, List.of(), List.of());
        }
        var cadastro = barragens.buscarCadastro(barragem);
        if (cadastro.isEmpty()) {
            return new Acionamento(barragem, nivelResposta, leituraSuspeita, List.of(),
                    List.of("A barragem não tem cadastro sincronizado com a Central: sem contatos do PAE"));
        }
        var contatos = contatosNoNivel(cadastro.get(), nivelResposta);
        return new Acionamento(barragem, nivelResposta, leituraSuspeita, contatos, pendencias(contatos, nivelResposta));
    }

    private static List<Acionamento.Contato> contatosNoNivel(CadastroBarragem cadastro, int nivelResposta) {
        return cadastro.contatos().stream()
                .filter(c -> c.substitui() == null && c.acionadoNo(nivelResposta))
                // ordenação estável: dentro do nível, mantém a ordem do cadastro
                .sorted(Comparator.comparingInt(ContatoBarragem::nivelAcionamento))
                .map(titular -> new Acionamento.Contato(titular, cadastro.contatos().stream()
                        .filter(c -> titular.chave().equals(c.substitui()))
                        .toList()))
                .toList();
    }

    private static List<String> pendencias(List<Acionamento.Contato> contatos, int nivelResposta) {
        return PAPEIS_PREVISTOS.stream()
                .filter(previsto -> previsto.nivel() <= nivelResposta)
                .filter(previsto -> contatos.stream().noneMatch(c -> c.titular().papel() == previsto.papel()))
                .map(previsto -> "Sem contato de " + previsto.nome() + " (o PAE o aciona a partir do nível "
                        + previsto.nivel() + "): cadastre na Central")
                .toList();
    }
}
