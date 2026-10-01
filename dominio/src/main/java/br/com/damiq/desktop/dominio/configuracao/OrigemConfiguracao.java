package br.com.damiq.desktop.dominio.configuracao;

/** De onde veio uma configuração: arquivo local (enquanto a Central não existe) ou a API da Central. */
public enum OrigemConfiguracao {
    ARQUIVO,
    CENTRAL
}
