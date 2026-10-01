package br.com.damiq.desktop.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositorioConfiguracoesJdbcTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final BarragemId OUTRA = new BarragemId("outra");
    private static final Instant AGORA = Instant.parse("2026-10-01T13:45:00Z");

    @TempDir
    Path diretorio;

    private DataSource banco;
    private RepositorioConfiguracoesJdbc repositorio;

    @BeforeEach
    void criarBanco() {
        banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        var barragens = new RepositorioBarragensJdbc(banco, Clock.fixed(AGORA, ZoneOffset.UTC));
        barragens.salvar(new Barragem(JOAO_LEITE, "João Leite"));
        barragens.salvar(new Barragem(OUTRA, "Outra"));
        repositorio = new RepositorioConfiguracoesJdbc(banco);
    }

    private static Configuracao configuracao(String versao) {
        return new Configuracao(new VersaoConfiguracao(versao), "{\"versao\": " + versao + "}");
    }

    private void ativar(BarragemId barragem, String versao) {
        repositorio.ativar(barragem, configuracao(versao), OrigemConfiguracao.ARQUIVO, AGORA);
    }

    private List<String> linhas(String sql) throws SQLException {
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

    @Test
    void semConfiguracaoNaoHaVigente() {
        assertTrue(repositorio.vigente(JOAO_LEITE).isEmpty());
    }

    @Test
    void ativarGravaETornaVigente() throws SQLException {
        ativar(JOAO_LEITE, "21");

        assertEquals(configuracao("21"), repositorio.vigente(JOAO_LEITE).orElseThrow());
        assertEquals(
                List.of("joao-leite|21|ARQUIVO|2026-10-01T13:45:00.000Z|1"),
                linhas("SELECT barragem_id, versao, origem, recebida_em, vigente FROM configuracao"));
    }

    @Test
    void novaVersaoPassaAAnteriorParaOHistorico() throws SQLException {
        ativar(JOAO_LEITE, "21");
        ativar(JOAO_LEITE, "22");

        assertEquals("22", repositorio.vigente(JOAO_LEITE).orElseThrow().versao().valor());
        assertEquals(List.of("21|0", "22|1"), linhas("SELECT versao, vigente FROM configuracao ORDER BY versao"));
        assertTrue(repositorio.versaoRegistrada(JOAO_LEITE, new VersaoConfiguracao("21")));
        assertFalse(repositorio.versaoRegistrada(JOAO_LEITE, new VersaoConfiguracao("23")));
    }

    @Test
    void cadaBarragemTemSuaVigente() {
        ativar(JOAO_LEITE, "21");
        ativar(OUTRA, "5");

        assertEquals("21", repositorio.vigente(JOAO_LEITE).orElseThrow().versao().valor());
        assertEquals("5", repositorio.vigente(OUTRA).orElseThrow().versao().valor());
        assertFalse(repositorio.versaoRegistrada(OUTRA, new VersaoConfiguracao("21")));
    }

    @Test
    void falhaNaTransacaoMantemAVigenteAnterior() {
        ativar(JOAO_LEITE, "21");

        // versão repetida viola UNIQUE (barragem_id, versao) depois de desativar a 21
        assertThrows(FalhaBancoDadosException.class, () -> ativar(JOAO_LEITE, "21"));

        assertEquals("21", repositorio.vigente(JOAO_LEITE).orElseThrow().versao().valor());
    }

    @Test
    void barragemPrecisaEstarCadastrada() {
        assertThrows(FalhaBancoDadosException.class, () -> ativar(new BarragemId("inexistente"), "1"));
    }

    @Test
    void reabrirOBancoNaoRepeteAsMigracoes() throws SQLException {
        ativar(JOAO_LEITE, "21");
        var historico = "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank";
        var antes = linhas(historico);

        var reaberto = new RepositorioConfiguracoesJdbc(BancoDados.abrir(diretorio.resolve("damiq.db")));

        assertEquals("21", reaberto.vigente(JOAO_LEITE).orElseThrow().versao().valor());
        assertEquals(antes, linhas(historico));
        assertTrue(antes.stream().allMatch(linha -> linha.endsWith("|1")), antes.toString());
    }
}
