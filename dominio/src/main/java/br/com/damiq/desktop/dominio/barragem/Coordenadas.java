package br.com.damiq.desktop.dominio.barragem;

/** Posição em graus decimais (SIRGAS 2000 / WGS 84); no Brasil, latitude e longitude são em geral negativas. */
public record Coordenadas(double latitude, double longitude) {

    public Coordenadas {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude deve estar entre -90 e 90: " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude deve estar entre -180 e 180: " + longitude);
        }
    }
}
