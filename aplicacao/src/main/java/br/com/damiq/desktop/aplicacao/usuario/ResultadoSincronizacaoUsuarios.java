package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.Login;
import java.util.List;

/**
 * Resultado de {@link SincronizarUsuarios}.
 *
 * @param recusados cadastros inválidos; a cópia anterior desses usuários, se houver, foi mantida
 * @param excluidos usuários que saíram da Central e receberam exclusão lógica
 */
public record ResultadoSincronizacaoUsuarios(
        List<Login> novos,
        List<Login> atualizados,
        List<Login> inalterados,
        List<Login> excluidos,
        List<UsuarioPublicado> recusados) {

    public ResultadoSincronizacaoUsuarios {
        novos = List.copyOf(Validacao.obrigatorio(novos, "novos"));
        atualizados = List.copyOf(Validacao.obrigatorio(atualizados, "atualizados"));
        inalterados = List.copyOf(Validacao.obrigatorio(inalterados, "inalterados"));
        excluidos = List.copyOf(Validacao.obrigatorio(excluidos, "excluídos"));
        recusados = List.copyOf(Validacao.obrigatorio(recusados, "recusados"));
    }
}
