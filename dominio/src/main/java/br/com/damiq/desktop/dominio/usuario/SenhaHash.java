package br.com.damiq.desktop.dominio.usuario;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Hash da senha (bcrypt), gerado na Central ou na troca de senha. A senha em si nunca é guardada; o formato do
 * hash é conferido por quem o calcula.
 */
public record SenhaHash(String valor) {

    public SenhaHash {
        valor = Validacao.textoObrigatorio(valor, "hash da senha");
    }

    /** Não expõe o hash em logs. */
    @Override
    public String toString() {
        return "SenhaHash[***]";
    }
}
