package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.VersaoCadastro;
import java.util.List;
import java.util.Optional;

/** Barragens no Desktop. Barragens excluídas (exclusão lógica) não aparecem nas consultas. */
public interface RepositorioBarragens {

    /**
     * Cadastra só a identificação da barragem, ou atualiza o nome se já existir. A marcação de teste não muda
     * depois do cadastro. O cadastro completo vem da Central ({@link #salvarCadastro}).
     */
    void salvar(Barragem barragem);

    Optional<Barragem> buscar(BarragemId id);

    /** Barragens ativas, pelo nome. */
    List<Barragem> listar();

    /** Cópia do cadastro da Central; vazio se a barragem não existir ou ainda não tiver cadastro sincronizado. */
    Optional<CadastroBarragem> buscarCadastro(BarragemId id);

    /** Versão do cadastro guardada, inclusive de barragem excluída; vazio se nunca sincronizada. */
    Optional<VersaoCadastro> versaoCadastro(BarragemId id);

    /**
     * Grava a cópia do cadastro numa transação: cria a barragem ou atualiza os dados (reativando-a, se estava
     * excluída); campos próprios que saíram do cadastro recebem exclusão lógica.
     */
    void salvarCadastro(CadastroBarragem cadastro);

    /**
     * Exclusão lógica: a barragem some das consultas, mas ela e os seus dados continuam gravados.
     *
     * @return {@code false} se ela não existir ou já estiver excluída
     */
    boolean excluir(BarragemId id);
}
