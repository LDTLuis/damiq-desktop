package br.com.damiq.desktop.dominio.usuario;

import br.com.damiq.desktop.dominio.Validacao;

/** Revisão do cadastro do usuário na Central; muda a cada edição publicada (inclusive troca de senha pelo administrador). */
public record VersaoUsuario(String valor) {

    public VersaoUsuario {
        valor = Validacao.textoObrigatorio(valor, "versão do cadastro do usuário");
    }

    @Override
    public String toString() {
        return valor;
    }
}
