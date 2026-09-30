package br.com.damiq.desktop.dominio.medicao;

/** Grandeza medida por um instrumento, com a unidade canônica em que o motor devolve as medições. */
public enum TipoMedicao {
    NIVEL("m"),
    PRESSAO("kPa"),
    VAZAO("m3/s"),
    DESLOCAMENTO("mm");

    private final String unidadeCanonica;

    TipoMedicao(String unidadeCanonica) {
        this.unidadeCanonica = unidadeCanonica;
    }

    public String unidadeCanonica() {
        return unidadeCanonica;
    }
}
