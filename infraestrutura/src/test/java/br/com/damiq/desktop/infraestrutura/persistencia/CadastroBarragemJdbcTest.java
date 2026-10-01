package br.com.damiq.desktop.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.CampoBarragem;
import br.com.damiq.desktop.dominio.barragem.Coordenadas;
import br.com.damiq.desktop.dominio.barragem.TipoCampo;
import br.com.damiq.desktop.dominio.barragem.UnidadeFederativa;
import br.com.damiq.desktop.dominio.barragem.VersaoCadastro;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Cópia do cadastro de barragens: dados obrigatórios e campos próprios. */
class CadastroBarragemJdbcTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final CampoBarragem COTA = new CampoBarragem("cota_crista", "Cota da crista", "Maciço",
            TipoCampo.NUMERO, "752.5", "m");
    private static final CampoBarragem LARGURA = new CampoBarragem("largura_vertedouro", "Largura do vertedouro",
            "Vertedouro", TipoCampo.NUMERO, "50", "m");
    private static final CampoBarragem OUTORGA = new CampoBarragem("outorga", "Outorga", null, TipoCampo.TEXTO,
            "Portaria nº 946/2009", null);

    @TempDir
    Path diretorio;

    private DataSource banco;
    private RepositorioBarragensJdbc barragens;

    @BeforeEach
    void criarBanco() {
        banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        barragens = new RepositorioBarragensJdbc(banco, AutoriaDeTeste.SISTEMA);
    }

    private static CadastroBarragem cadastro(String versao, String nome, boolean teste, List<CampoBarragem> campos) {
        return new CadastroBarragem(JOAO_LEITE, new VersaoCadastro(versao), nome, teste, "SANEAGO",
                "Abastecimento público", List.of("Goiânia", "Nerópolis"), UnidadeFederativa.GO,
                new Coordenadas(-16.5701, -49.2151), "Ribeirão João Leite", "CCR", 50, 129_000_000, campos);
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
    void gravaELeOCadastroCompleto() {
        var cadastro = cadastro("3", "João Leite", false, List.of(COTA, OUTORGA));

        barragens.salvarCadastro(cadastro);

        assertEquals(cadastro, barragens.buscarCadastro(JOAO_LEITE).orElseThrow());
        assertEquals("3", barragens.versaoCadastro(JOAO_LEITE).orElseThrow().valor());
        assertEquals(List.of(new Barragem(JOAO_LEITE, "João Leite")), barragens.listar());
    }

    @Test
    void novaVersaoAtualizaCamposEExcluiLogicamenteOsQueSairam() throws SQLException {
        barragens.salvarCadastro(cadastro("3", "João Leite", false, List.of(COTA, OUTORGA)));
        var cotaCorrigida = new CampoBarragem("cota_crista", "Cota da crista", "Maciço", TipoCampo.NUMERO, "752.6", "m");

        barragens.salvarCadastro(cadastro("4", "Barragem do Ribeirão João Leite", false, List.of(LARGURA, cotaCorrigida)));

        var lido = barragens.buscarCadastro(JOAO_LEITE).orElseThrow();
        assertEquals("Barragem do Ribeirão João Leite", lido.nome());
        assertEquals(List.of(LARGURA, cotaCorrigida), lido.campos());
        assertEquals(
                List.of("cota_crista|752.6|1|null", "largura_vertedouro|50|0|null", "outorga|Portaria nº 946/2009|1|excluido"),
                linhas("""
                        SELECT chave, valor, ordem, CASE WHEN excluido_em IS NULL THEN 'null' ELSE 'excluido' END
                        FROM barragem_campo ORDER BY chave"""));
    }

    @Test
    void barragemSemCadastroSincronizado() {
        barragens.salvar(new Barragem(JOAO_LEITE, "João Leite"));

        assertTrue(barragens.buscar(JOAO_LEITE).isPresent());
        assertTrue(barragens.buscarCadastro(JOAO_LEITE).isEmpty());
        assertTrue(barragens.versaoCadastro(JOAO_LEITE).isEmpty());
    }

    @Test
    void barragemExcluidaVoltaAoSerCadastradaDeNovo() {
        barragens.salvarCadastro(cadastro("3", "João Leite", false, List.of()));
        barragens.excluir(JOAO_LEITE);
        assertTrue(barragens.listar().isEmpty());
        assertTrue(barragens.buscarCadastro(JOAO_LEITE).isEmpty());
        assertEquals("3", barragens.versaoCadastro(JOAO_LEITE).orElseThrow().valor());

        barragens.salvarCadastro(cadastro("4", "João Leite", false, List.of()));

        assertEquals(1, barragens.listar().size());
    }

    @Test
    void marcacaoDeTesteSoValeNoPrimeiroCadastroEPassaAosCampos() throws SQLException {
        barragens.salvarCadastro(cadastro("1", "UAT", true, List.of(COTA)));

        barragens.salvarCadastro(cadastro("2", "UAT", false, List.of(COTA, LARGURA)));

        assertTrue(barragens.buscar(JOAO_LEITE).orElseThrow().teste());
        assertEquals(List.of("1", "1"), linhas("SELECT teste FROM barragem_campo"));
        assertEquals(1, new RepositorioDadosTesteJdbc(banco).apagarDadosDeTeste().get("barragem"));
        assertFalse(barragens.versaoCadastro(JOAO_LEITE).isPresent());
    }
}
