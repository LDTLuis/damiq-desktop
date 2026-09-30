package br.com.damiq.desktop.dominio.configuracao;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Configuração de monitoramento definida na Central. O conteúdo é o JSON da seção {@code configuracao} do
 * contrato, repassado inteiro ao motor em toda chamada; o Desktop não interpreta as regras.
 */
public record Configuracao(VersaoConfiguracao versao, String conteudoJson) {

    public Configuracao {
        Validacao.obrigatorio(versao, "versão da configuração");
        conteudoJson = Validacao.textoObrigatorio(conteudoJson, "conteúdo da configuração");
    }
}
