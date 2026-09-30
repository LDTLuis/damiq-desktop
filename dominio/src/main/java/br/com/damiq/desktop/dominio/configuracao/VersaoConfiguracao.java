package br.com.damiq.desktop.dominio.configuracao;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Revisão da configuração vinda da Central ({@code configuracao.versao}, devolvida pelo motor em
 * {@code versao_config}). No contrato pode ser texto ou inteiro; aqui é sempre texto.
 */
public record VersaoConfiguracao(String valor) {

    public VersaoConfiguracao {
        valor = Validacao.textoObrigatorio(valor, "versão da configuração");
    }

    @Override
    public String toString() {
        return valor;
    }
}
