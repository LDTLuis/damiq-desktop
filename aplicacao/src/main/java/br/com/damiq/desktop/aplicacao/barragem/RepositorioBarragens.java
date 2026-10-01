package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.util.Optional;

/** Barragens cadastradas no Desktop. Barragens excluídas (exclusão lógica) não aparecem nas consultas. */
public interface RepositorioBarragens {

    /** Cadastra a barragem ou atualiza o nome, se já existir. A marcação de teste não muda depois do cadastro. */
    void salvar(Barragem barragem);

    Optional<Barragem> buscar(BarragemId id);

    /**
     * Exclusão lógica: a barragem some das consultas, mas ela e os seus dados continuam gravados.
     *
     * @return {@code false} se ela não existir ou já estiver excluída
     */
    boolean excluir(BarragemId id);
}
