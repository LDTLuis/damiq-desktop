package br.com.damiq.desktop.dominio.alerta;

/** Regra que disparou o alerta (contrato do motor, seção 5). */
public enum TipoAlerta {
    LIMITE,
    TAXA_VARIACAO,
    FORA_FAIXA_PLAUSIVEL,
    SENSOR_TRAVADO,
    ANOMALIA_ESTATISTICA
}
