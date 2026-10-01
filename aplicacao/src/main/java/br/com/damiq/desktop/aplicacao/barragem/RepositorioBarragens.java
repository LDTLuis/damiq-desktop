package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.util.Optional;

/** Barragens cadastradas no Desktop. */
public interface RepositorioBarragens {

    /** Cadastra a barragem ou atualiza o nome, se já existir. */
    void salvar(Barragem barragem);

    Optional<Barragem> buscar(BarragemId id);
}
