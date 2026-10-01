package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exclusão lógica de uma barragem: ela deixa de aparecer e de aceitar configurações e medições, mas ela e todos
 * os seus dados continuam gravados para auditoria.
 */
public final class ExcluirBarragem {

    private static final Logger LOG = LoggerFactory.getLogger(ExcluirBarragem.class);

    private final RepositorioBarragens barragens;

    public ExcluirBarragem(RepositorioBarragens barragens) {
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
    }

    /** @throws BarragemNaoCadastradaException se a barragem não existir ou já estiver excluída */
    public void executar(BarragemId barragem) {
        if (!barragens.excluir(Validacao.obrigatorio(barragem, "barragem"))) {
            throw new BarragemNaoCadastradaException(barragem);
        }
        LOG.warn("Barragem {} excluída (exclusão lógica)", barragem);
    }
}
