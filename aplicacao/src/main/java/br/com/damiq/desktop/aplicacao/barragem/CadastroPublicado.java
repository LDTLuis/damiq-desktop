package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import java.util.Optional;

/**
 * Um cadastro publicado pela Central: válido, ou recusado com o motivo (ex.: campo obrigatório ausente).
 *
 * @param identificacao o id da barragem, se legível; senão a posição na lista (ex.: "item 3")
 * @param barragem o id, se legível; um cadastro recusado com id conhecido não exclui a cópia existente
 */
public record CadastroPublicado(String identificacao, BarragemId barragem, CadastroBarragem cadastro, String erro) {

    public CadastroPublicado {
        identificacao = Validacao.textoObrigatorio(identificacao, "identificação do cadastro");
        if ((cadastro == null) == (erro == null)) {
            throw new IllegalArgumentException("informe o cadastro ou o motivo da recusa");
        }
    }

    public static CadastroPublicado valido(CadastroBarragem cadastro) {
        return new CadastroPublicado(cadastro.id().valor(), cadastro.id(), cadastro, null);
    }

    public static CadastroPublicado recusado(String identificacao, BarragemId barragem, String erro) {
        return new CadastroPublicado(identificacao, barragem, null, Validacao.textoObrigatorio(erro, "motivo"));
    }

    public Optional<CadastroBarragem> valido() {
        return Optional.ofNullable(cadastro);
    }
}
