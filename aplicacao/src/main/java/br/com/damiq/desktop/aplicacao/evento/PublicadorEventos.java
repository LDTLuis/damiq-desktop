package br.com.damiq.desktop.aplicacao.evento;

/**
 * Publica eventos da aplicação para quem estiver ouvindo (outros casos de uso, a interface). A entrega é
 * síncrona: o evento é tratado antes de {@code publicar} retornar.
 */
@FunctionalInterface
public interface PublicadorEventos {

    void publicar(Object evento);
}
