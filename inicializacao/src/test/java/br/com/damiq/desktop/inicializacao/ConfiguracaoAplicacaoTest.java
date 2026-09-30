package br.com.damiq.desktop.inicializacao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ConfiguracaoAplicacaoTest {

    @Test
    void contextoSobe() {
        try (var contexto = new AnnotationConfigApplicationContext(ConfiguracaoAplicacao.class)) {
            assertTrue(contexto.isActive());
        }
    }
}
