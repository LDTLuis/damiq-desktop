package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;

/** Revisão do cadastro da barragem na Central; muda a cada edição publicada. */
public record VersaoCadastro(String valor) {

    public VersaoCadastro {
        valor = Validacao.textoObrigatorio(valor, "versão do cadastro");
    }

    @Override
    public String toString() {
        return valor;
    }
}
