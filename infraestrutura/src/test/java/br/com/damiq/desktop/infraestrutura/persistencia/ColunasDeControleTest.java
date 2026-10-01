package br.com.damiq.desktop.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.manutencao.LimparDadosTeste;
import br.com.damiq.desktop.aplicacao.medicao.OrigemLeituras;
import br.com.damiq.desktop.aplicacao.medicao.RegistroProcessamento;
import br.com.damiq.desktop.aplicacao.motor.MedicaoProcessada;
import br.com.damiq.desktop.aplicacao.notificacao.Notificacao;
import br.com.damiq.desktop.aplicacao.notificacao.RepositorioNotificacoes.Gravacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/** Migração V4 e as regras das colunas de controle: autoria, exclusão lógica e dados de teste. */
class ColunasDeControleTest {

    private static final BarragemId REAL = new BarragemId("joao-leite");
    private static final BarragemId DE_TESTE = new BarragemId("uat");
    private static final VersaoConfiguracao V21 = new VersaoConfiguracao("21");
    private static final CodigoInstrumento PZ_01 = new CodigoInstrumento("PZ-01");
    private static final OffsetDateTime T08 = OffsetDateTime.parse("2026-09-29T08:00:00-03:00");
    private static final Instant AGORA = Instant.parse("2026-10-01T13:45:00Z");

    @TempDir
    Path diretorio;

    private static List<String> linhas(DataSource banco, String sql) throws SQLException {
        try (var conexao = banco.getConnection();
                var consulta = conexao.createStatement();
                var resultado = consulta.executeQuery(sql)) {
            var linhas = new ArrayList<String>();
            while (resultado.next()) {
                var colunas = new ArrayList<String>();
                for (int i = 1; i <= resultado.getMetaData().getColumnCount(); i++) {
                    colunas.add(resultado.getString(i));
                }
                linhas.add(String.join("|", colunas));
            }
            return linhas;
        }
    }

    private static void executar(DataSource banco, String sql) throws SQLException {
        try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
            comando.executeUpdate(sql);
        }
    }

    private static RegistroProcessamento processamento(BarragemId barragem, OffsetDateTime momento) {
        var medicao = new MedicaoProcessada(new Medicao(PZ_01, TipoMedicao.PRESSAO, momento, 240), "240", "kPa", List.of());
        var alerta = new Alerta(TipoAlerta.LIMITE, CategoriaAlerta.SEGURANCA, PZ_01, Severidade.ALERTA, momento, momento,
                1, 240, "kPa", 215.82, "acima", false, "PZ-01 acima do alerta", V21);
        return new RegistroProcessamento(barragem, V21, AGORA, OrigemLeituras.DIGITACAO, null, Severidade.ALERTA,
                Severidade.OK, 2, 1, 0, List.of(medicao), List.of(), List.of(), List.of(alerta));
    }

    /** Barragem com configuração, um processamento e uma notificação. */
    private static void popular(DataSource banco, Autoria autoria, Barragem barragem) {
        new RepositorioBarragensJdbc(banco, autoria).salvar(barragem);
        new RepositorioConfiguracoesJdbc(banco, autoria).ativar(barragem.id(), new Configuracao(V21, "{\"versao\": 21}"),
                OrigemConfiguracao.ARQUIVO, AGORA);
        new RepositorioProcessamentosJdbc(banco, autoria).registrar(processamento(barragem.id(), T08));
        var notificacoes = new RepositorioNotificacoesJdbc(banco, autoria);
        var pendente = notificacoes.alertasPendentes(barragem.id()).getFirst();
        notificacoes.gravar(List.of(new Gravacao(
                new Notificacao(null, barragem.id(), PZ_01, TipoAlerta.LIMITE, CategoriaAlerta.SEGURANCA,
                        Severidade.ALERTA, "PZ-01 acima do alerta", false, 2, 1, AGORA, AGORA, null),
                List.of(pendente.id()))));
    }

    @Test
    void usuarioSistemaECamposDeAutoria() throws SQLException {
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        assertEquals(List.of("1|sistema|0"), linhas(banco, "SELECT id, nome, teste FROM usuario"));

        new RepositorioBarragensJdbc(banco, AutoriaDeTeste.em(AGORA)).salvar(new Barragem(REAL, "João Leite"));
        executar(banco, """
                INSERT INTO usuario (id, nome, barragem_id, login, senha_hash, perfil, email, registro_profissional,
                    criado_em, criado_por, atualizado_em, atualizado_por)
                VALUES (2, 'Eng. Ana', 'joao-leite', 'ana', '$2a$10$x', 'ENGENHEIRO', 'ana@x.com', 'CREA-GO 1/D',
                    '2026-10-01T00:00:00.000Z', 1, '2026-10-01T00:00:00.000Z', 1)""");
        var depois = AGORA.plusSeconds(60);
        new RepositorioBarragensJdbc(banco, AutoriaDeTeste.em(depois, new UsuarioId(2)))
                .salvar(new Barragem(REAL, "Barragem do Ribeirão João Leite"));

        assertEquals(
                List.of("Barragem do Ribeirão João Leite|2026-10-01T13:45:00.000Z|1|2026-10-01T13:46:00.000Z|2|null|0"),
                linhas(banco, """
                        SELECT nome, criado_em, criado_por, atualizado_em, atualizado_por, excluido_em, teste
                        FROM barragem"""));
    }

    @Test
    void registroRealNaoPodeSerApagadoFisicamente() throws SQLException {
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        popular(banco, AutoriaDeTeste.SISTEMA, new Barragem(REAL, "João Leite"));

        for (var tabela : List.of("alerta", "notificacao", "medicao", "processamento", "configuracao", "barragem",
                "usuario")) {
            var falha = assertThrows(SQLException.class, () -> executar(banco, "DELETE FROM " + tabela));
            assertTrue(falha.getMessage().contains("exclusão lógica"), tabela + ": " + falha.getMessage());
        }
    }

    @Test
    void dadosDaBarragemDeTesteHerdamAMarcacaoESaoApagados() throws SQLException {
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        popular(banco, AutoriaDeTeste.SISTEMA, new Barragem(REAL, "João Leite"));
        popular(banco, AutoriaDeTeste.SISTEMA, new Barragem(DE_TESTE, "Barragem de UAT", true));

        for (var tabela : List.of("configuracao", "processamento", "medicao", "alerta", "notificacao")) {
            assertEquals(List.of("joao-leite|0", "uat|1"),
                    linhas(banco, "SELECT barragem_id, teste FROM " + tabela + " ORDER BY barragem_id"), tabela);
        }

        var apagados = new LimparDadosTeste(new RepositorioDadosTesteJdbc(banco)).executar();

        assertEquals(1, apagados.get("barragem"));
        assertEquals(1, apagados.get("medicao"));
        assertEquals(List.of("joao-leite"), linhas(banco, "SELECT id FROM barragem"));
        assertEquals(List.of("joao-leite"), linhas(banco, "SELECT DISTINCT barragem_id FROM alerta"));
    }

    @Test
    void exclusaoLogicaDaBarragem() throws SQLException {
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        var barragens = new RepositorioBarragensJdbc(banco, AutoriaDeTeste.em(AGORA));
        barragens.salvar(new Barragem(REAL, "João Leite"));

        assertTrue(barragens.excluir(REAL));

        assertTrue(barragens.buscar(REAL).isEmpty());
        assertFalse(barragens.excluir(REAL));
        assertEquals(List.of("2026-10-01T13:45:00.000Z"), linhas(banco, "SELECT excluido_em FROM barragem"));
    }

    @Test
    void medicaoExcluidaNaoImpedeRegistrarDeNovo() throws SQLException {
        var banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        popular(banco, AutoriaDeTeste.SISTEMA, new Barragem(REAL, "João Leite"));
        var processamentos = new RepositorioProcessamentosJdbc(banco, AutoriaDeTeste.SISTEMA);
        assertThrows(FalhaBancoDadosException.class, () -> processamentos.registrar(processamento(REAL, T08)));

        executar(banco, "UPDATE medicao SET excluido_em = '2026-10-01T14:00:00.000Z'");
        var medicoes = new RepositorioMedicoesJdbc(banco);
        assertTrue(medicoes.ultimas(REAL, 48).isEmpty());

        processamentos.registrar(processamento(REAL, T08));

        assertEquals(1, medicoes.ultimas(REAL, 48).size());
        assertEquals(List.of("2"), linhas(banco, "SELECT count(*) FROM medicao"));
    }

    @Test
    void migracaoPreservaOsDadosDeUmBancoV3() throws SQLException {
        var configuracao = new SQLiteConfig();
        configuracao.enforceForeignKeys(true);
        var banco = new SQLiteDataSource(configuracao);
        banco.setUrl("jdbc:sqlite:" + diretorio.resolve("damiq.db").toAbsolutePath());
        Flyway.configure().dataSource(banco).locations("classpath:db/migracao").target("3").load().migrate();

        executar(banco, "INSERT INTO barragem VALUES ('joao-leite', 'João Leite', '2026-09-01T10:00:00.000Z')");
        executar(banco, """
                INSERT INTO configuracao VALUES
                    (1, 'joao-leite', '21', '{"versao": 21}', 'ARQUIVO', '2026-09-02T10:00:00.000Z', 1)""");
        executar(banco, """
                INSERT INTO processamento VALUES
                    (1, 'joao-leite', 1, '2026-09-03T10:00:00.000Z', 'IMPORTACAO', 'campo.csv', 'ALERTA', 'OK', 2,
                     2, 1, 0, 1)""");
        executar(banco, """
                INSERT INTO medicao VALUES
                    (1, 1, 'joao-leite', 'PZ-01', 'PRESSAO', '2026-09-03T11:00:00.000Z', '-03:00', 240, '240', 'kPa', '')""");
        executar(banco, """
                INSERT INTO rejeicao VALUES (1, 1, 1, 'VALOR_INVALIDO', 'x', 'PZ-01', 'pressao', null, 'x', 'kPa')""");
        executar(banco, """
                INSERT INTO lacuna VALUES
                    (1, 1, 'joao-leite', 'PZ-01', '2026-09-03T07:00:00.000Z', '2026-09-03T11:00:00.000Z', '-03:00', 14400)""");
        executar(banco, """
                INSERT INTO notificacao VALUES
                    (1, 'joao-leite', 'PZ-01', 'LIMITE', 'SEGURANCA', 'ALERTA', 'PZ-01 acima', 0, 2, 1,
                     '2026-09-03T10:00:01.000Z', '2026-09-03T10:00:02.000Z', null, null, null)""");
        executar(banco, """
                INSERT INTO alerta VALUES
                    (1, 1, 'joao-leite', 'PZ-01', 'LIMITE', 'SEGURANCA', 'ALERTA', '2026-09-03T11:00:00.000Z',
                     '2026-09-03T11:00:00.000Z', '-03:00', 1, 240, 'kPa', 215.82, 'acima', 0, 'PZ-01 acima', 1)""");

        var migrado = BancoDados.abrir(diretorio.resolve("damiq.db"));

        assertEquals(List.of("João Leite|2026-09-01T10:00:00.000Z|1|0"),
                linhas(migrado, "SELECT nome, criado_em, criado_por, teste FROM barragem"));
        assertEquals(List.of("21|1|2026-09-02T10:00:00.000Z"),
                linhas(migrado, "SELECT versao, vigente, criado_em FROM configuracao"));
        assertEquals(List.of("campo.csv|2026-09-03T10:00:00.000Z"),
                linhas(migrado, "SELECT arquivo, criado_em FROM processamento"));
        for (var tabela : List.of("medicao", "rejeicao", "lacuna", "alerta")) {
            assertEquals(List.of("2026-09-03T10:00:00.000Z|1|null|0"),
                    linhas(migrado, "SELECT criado_em, atualizado_por, excluido_em, teste FROM " + tabela), tabela);
        }
        assertEquals(List.of("2026-09-03T10:00:01.000Z|2026-09-03T10:00:02.000Z|1"),
                linhas(migrado, "SELECT criado_em, atualizado_em, ocorrencias FROM notificacao"));
        assertEquals(List.of("1"), linhas(migrado, "SELECT notificacao_id FROM alerta"));
        // chaves estrangeiras íntegras e apontando para as tabelas renomeadas
        assertTrue(linhas(migrado, "PRAGMA foreign_key_check").isEmpty());
        assertTrue(linhas(migrado, "SELECT sql FROM sqlite_schema WHERE sql LIKE '%_novo%'").isEmpty());
        // o repositório lê os dados migrados
        assertEquals("21", new RepositorioConfiguracoesJdbc(migrado, AutoriaDeTeste.SISTEMA)
                .vigente(new BarragemId("joao-leite")).orElseThrow().versao().valor());
    }
}
