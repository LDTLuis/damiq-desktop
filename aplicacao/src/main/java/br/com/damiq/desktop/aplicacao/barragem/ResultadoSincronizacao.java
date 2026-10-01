package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.util.List;

/**
 * Resultado de {@link SincronizarBarragens}.
 *
 * @param recusados cadastros inválidos; a cópia anterior dessas barragens, se houver, foi mantida
 * @param excluidas barragens que saíram da Central e receberam exclusão lógica
 */
public record ResultadoSincronizacao(
        List<BarragemId> novas,
        List<BarragemId> atualizadas,
        List<BarragemId> inalteradas,
        List<BarragemId> excluidas,
        List<CadastroPublicado> recusados) {

    public ResultadoSincronizacao {
        novas = List.copyOf(Validacao.obrigatorio(novas, "novas"));
        atualizadas = List.copyOf(Validacao.obrigatorio(atualizadas, "atualizadas"));
        inalteradas = List.copyOf(Validacao.obrigatorio(inalteradas, "inalteradas"));
        excluidas = List.copyOf(Validacao.obrigatorio(excluidas, "excluídas"));
        recusados = List.copyOf(Validacao.obrigatorio(recusados, "recusados"));
    }
}
