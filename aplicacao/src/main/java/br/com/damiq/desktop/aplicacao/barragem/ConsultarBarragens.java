package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import java.util.List;

/** Barragens para a interface: a lista (ponto de partida de toda tela) e o cadastro de cada uma. */
public final class ConsultarBarragens {

    private final RepositorioBarragens barragens;

    public ConsultarBarragens(RepositorioBarragens barragens) {
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
    }

    /** Barragens ativas, pelo nome. */
    public List<Barragem> listar() {
        return barragens.listar();
    }

    /**
     * @throws BarragemNaoCadastradaException se a barragem não existir ou não tiver cadastro sincronizado
     */
    public CadastroBarragem cadastro(BarragemId barragem) {
        return barragens.buscarCadastro(barragem).orElseThrow(() -> new BarragemNaoCadastradaException(barragem));
    }
}
