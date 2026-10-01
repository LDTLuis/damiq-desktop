package br.com.damiq.desktop.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.CampoBarragem;
import br.com.damiq.desktop.dominio.barragem.Coordenadas;
import br.com.damiq.desktop.dominio.barragem.GrupoCampos;
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

/** Cópia do cadastro de barragens: dados obrigatórios, grupos e campos próprios. */
class CadastroBarragemJdbcTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final GrupoCampos MACICO = new GrupoCampos("macico", "Maciço");
    private static final GrupoCampos VERTEDOURO = new GrupoCampos("vertedouro", "Vertedouro");
    private static final CampoBarragem COTA =
            new CampoBarragem("cota_crista", "Cota da crista", "macico", TipoCampo.NUMERO, "752.5", "m", true);
    private static final CampoBarragem LARGURA = new CampoBarragem(
            "largura_vertedouro", "Largura do vertedouro", "vertedouro", TipoCampo.NUMERO, "50", "m", true);
    private static final CampoBarragem OUTORGA =
            new CampoBarragem("outorga", "Outorga", null, TipoCampo.TEXTO, "Portaria nº 946/2009", null, false);

    @TempDir
    Path diretorio;

    private DataSource banco;
    private RepositorioBarragensJdbc barragens;

    @BeforeEach
    void criarBanco() {
        banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        barragens = new RepositorioBarragensJdbc(banco, AutoriaDeTeste.SISTEMA);
    }

    private static CadastroBarragem cadastro(
            String versao, String nome, boolean teste, List<GrupoCampos> grupos, List<CampoBarragem> campos) {
        return new CadastroBarragem(JOAO_LEITE, new VersaoCadastro(versao), nome, teste, "SANEAGO",
                "Abastecimento público", List.of("Goiânia", "Nerópolis"), UnidadeFederativa.GO,
                new Coordenadas(-16.5701, -49.2151), "Ribeirão João Leite", "CCR", 50, 129_000_000, grupos, campos);
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
    void gravaELeOCadastroCompleto() throws SQLException {
        var cadastro = cadastro("3", "João Leite", false, List.of(MACICO, VERTEDOURO), List.of(COTA, LARGURA, OUTORGA));

        barragens.salvarCadastro(cadastro);

        assertEquals(cadastro, barragens.buscarCadastro(JOAO_LEITE).orElseThrow());
        assertEquals("3", barragens.versaoCadastro(JOAO_LEITE).orElseThrow().valor());
        assertEquals(List.of(new Barragem(JOAO_LEITE, "João Leite")), barragens.listar());
        assertEquals(List.of("cota_crista|macico|1", "largura_vertedouro|vertedouro|1", "outorga|null|0"), linhas("""
                SELECT c.chave, g.chave, c.padrao FROM barragem_campo c LEFT JOIN barragem_grupo g ON g.id = c.grupo_id
                ORDER BY c.ordem"""));
    }

    @Test
    void novaVersaoAtualizaGruposECamposEExcluiLogicamenteOsQueSairam() throws SQLException {
        barragens.salvarCadastro(cadastro("3", "João Leite", false, List.of(MACICO, VERTEDOURO), List.of(COTA, LARGURA, OUTORGA)));
        // vertedouro sai, maciço é renomeado e vai para o fim, um grupo novo entra; a cota muda de valor e de grupo
        var estrutura = new GrupoCampos("estrutura", "Estrutura");
        var macicoRenomeado = new GrupoCampos("macico", "Maciço principal");
        var cotaCorrigida =
                new CampoBarragem("cota_crista", "Cota da crista", "estrutura", TipoCampo.NUMERO, "752.6", "m", true);

        barragens.salvarCadastro(cadastro("4", "Barragem do Ribeirão João Leite", false,
                List.of(estrutura, macicoRenomeado), List.of(cotaCorrigida, OUTORGA)));

        var lido = barragens.buscarCadastro(JOAO_LEITE).orElseThrow();
        assertEquals("Barragem do Ribeirão João Leite", lido.nome());
        assertEquals(List.of(estrutura, macicoRenomeado), lido.grupos());
        assertEquals(List.of(cotaCorrigida, OUTORGA), lido.campos());
        assertEquals(List.of("estrutura|Estrutura|0|ativo", "macico|Maciço principal|1|ativo", "vertedouro|Vertedouro|1|excluido"),
                linhas("""
                        SELECT chave, nome, ordem, CASE WHEN excluido_em IS NULL THEN 'ativo' ELSE 'excluido' END
                        FROM barragem_grupo ORDER BY chave"""));
        assertEquals(List.of("cota_crista|752.6|ativo", "largura_vertedouro|50|excluido", "outorga|Portaria nº 946/2009|ativo"),
                linhas("""
                        SELECT chave, valor, CASE WHEN excluido_em IS NULL THEN 'ativo' ELSE 'excluido' END
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
        barragens.salvarCadastro(cadastro("3", "João Leite", false, List.of(), List.of()));
        barragens.excluir(JOAO_LEITE);
        assertTrue(barragens.listar().isEmpty());
        assertTrue(barragens.buscarCadastro(JOAO_LEITE).isEmpty());
        assertEquals("3", barragens.versaoCadastro(JOAO_LEITE).orElseThrow().valor());

        barragens.salvarCadastro(cadastro("4", "João Leite", false, List.of(), List.of()));

        assertEquals(1, barragens.listar().size());
    }

    @Test
    void marcacaoDeTesteSoValeNoPrimeiroCadastroEPassaAGruposECampos() throws SQLException {
        barragens.salvarCadastro(cadastro("1", "UAT", true, List.of(MACICO), List.of(COTA)));

        barragens.salvarCadastro(cadastro("2", "UAT", false, List.of(MACICO, VERTEDOURO), List.of(COTA, LARGURA)));

        assertTrue(barragens.buscar(JOAO_LEITE).orElseThrow().teste());
        assertEquals(List.of("1", "1"), linhas("SELECT teste FROM barragem_grupo"));
        assertEquals(List.of("1", "1"), linhas("SELECT teste FROM barragem_campo"));
        var apagados = new RepositorioDadosTesteJdbc(banco).apagarDadosDeTeste();
        assertEquals(2, apagados.get("barragem_grupo"));
        assertEquals(1, apagados.get("barragem"));
        assertFalse(barragens.versaoCadastro(JOAO_LEITE).isPresent());
    }
}
