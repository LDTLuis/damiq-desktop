package br.com.damiq.desktop.inicializacao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.configuracao.AtualizarConfiguracao;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class ConfiguracaoAplicacaoTest {

    @TempDir
    Path dados;

    @Test
    void contextoSobeECriaOBancoNoDiretorioDeDados() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("teste", Map.of("damiq.dados.diretorio", dados.toString())));
            contexto.register(ConfiguracaoAplicacao.class);
            contexto.refresh();

            assertTrue(contexto.isActive());
            assertTrue(contexto.getBean(AtualizarConfiguracao.class) != null);
            assertTrue(Files.exists(dados.resolve("damiq.db")));
        }
    }
}
