package br.com.damiq.desktop.infraestrutura.importacao;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * O que cada coluna do arquivo contém. O título é reconhecido sem diferenciar maiúsculas, acentos, espaços e
 * pontuação: "Data/Hora", "data_hora" e "DATA HORA" valem o mesmo.
 */
enum Papel {
    INSTRUMENTO("instrumento", "sensor", "codigo", "codigoinstrumento", "ponto", "pontodemedicao"),
    TIPO("tipo", "tipomedicao", "tipodemedicao", "grandeza"),
    DATA_HORA("datahora", "dataehora", "timestamp", "momento"),
    DATA("data", "dia"),
    HORA("hora", "horario"),
    VALOR("valor", "leitura", "medicao"),
    UNIDADE("unidade", "unid", "un");

    private final Set<String> titulos;

    Papel(String... titulos) {
        this.titulos = Set.of(titulos);
    }

    Set<String> titulos() {
        return titulos;
    }

    static Optional<Papel> doTitulo(String titulo) {
        var normalizado = normalizar(titulo);
        for (var papel : values()) {
            if (papel.titulos.contains(normalizado)) {
                return Optional.of(papel);
            }
        }
        return Optional.empty();
    }

    /** Minúsculas, sem acentos e só letras e números. */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
