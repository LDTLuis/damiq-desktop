package br.com.damiq.desktop.dominio.alerta;

/**
 * {@link #SEGURANCA}: comportamento da barragem, compõe o status da barragem. {@link #QUALIDADE}: problema no
 * dado ou no instrumento, compõe o status dos dados.
 */
public enum CategoriaAlerta {
    SEGURANCA,
    QUALIDADE
}
