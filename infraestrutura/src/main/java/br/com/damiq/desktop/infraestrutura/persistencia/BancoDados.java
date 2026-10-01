package br.com.damiq.desktop.infraestrutura.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/** Banco local do Desktop: abre o arquivo SQLite e aplica as migrações de {@code db/migracao}. */
public final class BancoDados {

    /** Formato das datas no banco: ISO-8601 em UTC com milissegundos, que ordena cronologicamente. */
    static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private BancoDados() {}

    /** Abre (ou cria) o banco no arquivo e o deixa na versão mais recente do esquema. */
    public static DataSource abrir(Path arquivo) {
        try {
            var diretorio = arquivo.toAbsolutePath().getParent();
            if (diretorio != null) {
                Files.createDirectories(diretorio);
            }
        } catch (IOException e) {
            throw new FalhaBancoDadosException("Não foi possível criar o diretório do banco " + arquivo, e);
        }

        var configuracao = new SQLiteConfig();
        configuracao.enforceForeignKeys(true);
        configuracao.setJournalMode(SQLiteConfig.JournalMode.WAL);
        configuracao.setBusyTimeout(5_000);
        var fonte = new SQLiteDataSource(configuracao);
        fonte.setUrl("jdbc:sqlite:" + arquivo.toAbsolutePath());

        try {
            Flyway.configure()
                    .dataSource(fonte)
                    .locations("classpath:db/migracao")
                    .load()
                    .migrate();
        } catch (RuntimeException e) {
            throw new FalhaBancoDadosException("Falha ao migrar o banco " + arquivo + ": " + e.getMessage(), e);
        }
        return fonte;
    }

    static String data(Instant instante) {
        return FORMATO_DATA.format(instante);
    }
}
