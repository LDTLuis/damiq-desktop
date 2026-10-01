package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Meio de contato. Os meios de um contato vêm na ordem de preferência: o PAE recomenda o celular, de viva voz,
 * e o e-mail como complemento para detalhes (§6.1.2.2).
 *
 * @param valor número, endereço de e-mail, canal de rádio…
 */
public record MeioContato(Tipo tipo, String valor) {

    public enum Tipo {
        CELULAR,
        TELEFONE,
        EMAIL,
        RADIO,
        OUTRO
    }

    public MeioContato {
        Validacao.obrigatorio(tipo, "tipo do meio de contato");
        valor = Validacao.textoObrigatorio(valor, "meio de contato");
        if (tipo == Tipo.EMAIL && !valor.matches("[^@\\s]+@[^@\\s]+")) {
            throw new IllegalArgumentException("e-mail inválido: " + valor);
        }
        if ((tipo == Tipo.CELULAR || tipo == Tipo.TELEFONE) && !valor.matches("[0-9 ()+\\-.]*[0-9][0-9 ()+\\-.]*")) {
            throw new IllegalArgumentException("telefone inválido: " + valor);
        }
    }
}
