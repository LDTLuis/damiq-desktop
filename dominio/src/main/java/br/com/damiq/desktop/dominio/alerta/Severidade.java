package br.com.damiq.desktop.dominio.alerta;

import java.util.Collection;

/** Severidade de um alerta ou de um status, da menos para a mais grave. */
public enum Severidade {
    OK,
    AVISO,
    ALERTA,
    CRITICO;

    public boolean maisGraveQue(Severidade outra) {
        return compareTo(outra) > 0;
    }

    /** A mais grave das severidades; {@link #OK} se a coleção estiver vazia. */
    public static Severidade maisGrave(Collection<Severidade> severidades) {
        var resultado = OK;
        for (var severidade : severidades) {
            if (severidade.maisGraveQue(resultado)) {
                resultado = severidade;
            }
        }
        return resultado;
    }
}
