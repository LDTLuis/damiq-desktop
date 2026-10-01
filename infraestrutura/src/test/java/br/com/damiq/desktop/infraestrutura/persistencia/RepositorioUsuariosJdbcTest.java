package br.com.damiq.desktop.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.manutencao.LimparDadosTeste;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.Perfil;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.VersaoUsuario;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Migração V7 e {@link RepositorioUsuariosJdbc}. */
class RepositorioUsuariosJdbcTest {

    private static final BarragemId REAL = new BarragemId("joao-leite");
    private static final BarragemId DE_TESTE = new BarragemId("uat");
    private static final Login ANA = new Login("ana");
    private static final Login BETO = new Login("beto");
    private static final Instant AGORA = Instant.parse("2026-10-01T13:45:00Z");
    private static final String HASH = "$2a$10$xLS1W84muMQxPyNj2Sc/QOyVSo8o0GATq/lTMK42I5jWzJQ7Oy.YC";

    @TempDir
    Path diretorio;

    private DataSource banco;
    private RepositorioUsuariosJdbc usuarios;

    @BeforeEach
    void abrir() {
        banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        usuarios = new RepositorioUsuariosJdbc(banco, AutoriaDeTeste.em(AGORA));
        var barragens = new RepositorioBarragensJdbc(banco, AutoriaDeTeste.em(AGORA));
        barragens.salvar(new Barragem(REAL, "João Leite"));
        barragens.salvar(new Barragem(DE_TESTE, "Barragem de UAT", true));
    }

    private static CadastroUsuario cadastro(Login login, BarragemId barragem, String versao) {
        return new CadastroUsuario(login, new VersaoUsuario(versao), barragem, Perfil.ENGENHEIRO, "Ana Souza",
                "ana@x.com", "(62) 99999-0000", null, "CREA-GO 1/D", new SenhaHash(HASH), false, true);
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

    private void executar(String sql) throws SQLException {
        try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
            comando.executeUpdate(sql);
        }
    }

    @Test
    void gravaACopiaEHerdaATesteDaBarragem() throws SQLException {
        usuarios.salvarCadastro(cadastro(ANA, REAL, "1"));
        usuarios.salvarCadastro(cadastro(BETO, DE_TESTE, "1"));

        var credencial = usuarios.credencial(ANA).orElseThrow();
        assertEquals(HASH, credencial.senhaHash().valor());
        var ana = credencial.usuario();
        assertEquals(REAL, ana.barragem());
        assertEquals(Perfil.ENGENHEIRO, ana.perfil());
        assertEquals("CREA-GO 1/D", ana.registroProfissional());
        assertNull(ana.cargo());
        assertTrue(ana.trocarSenha());
        assertNull(ana.ultimoAcesso());
        assertEquals(List.of("ana|0", "beto|1"), linhas("SELECT login, teste FROM usuario WHERE id <> 1 ORDER BY login"));
        assertEquals(List.of(ANA, BETO), usuarios.logins());
        assertEquals(List.of(ana), usuarios.listar(REAL));
        assertEquals("1", usuarios.versaoCadastro(ANA).orElseThrow().valor());
    }

    @Test
    void falhasBloqueiamEAcessoOuDesbloqueioZeram() {
        usuarios.salvarCadastro(cadastro(ANA, REAL, "1"));
        var id = usuarios.credencial(ANA).orElseThrow().usuario().id();

        assertEquals(1, usuarios.registrarFalha(id, 3));
        usuarios.registrarAcesso(id, AGORA);
        var conectado = usuarios.buscar(id).orElseThrow();
        assertEquals(0, conectado.tentativasFalhas());
        assertEquals(AGORA, conectado.ultimoAcesso());

        usuarios.registrarFalha(id, 3);
        usuarios.registrarFalha(id, 3);
        assertFalse(usuarios.buscar(id).orElseThrow().bloqueado());
        assertEquals(3, usuarios.registrarFalha(id, 3));
        assertTrue(usuarios.buscar(id).orElseThrow().bloqueado());

        assertTrue(usuarios.desbloquear(id));
        assertFalse(usuarios.buscar(id).orElseThrow().bloqueado());
        assertEquals(0, usuarios.buscar(id).orElseThrow().tentativasFalhas());
    }

    @Test
    void trocaDeSenhaEVersaoNovaDaCentral() {
        usuarios.salvarCadastro(cadastro(ANA, REAL, "1"));
        var id = usuarios.credencial(ANA).orElseThrow().usuario().id();
        usuarios.registrarFalha(id, 5);

        usuarios.trocarSenha(id, new SenhaHash("$2a$10$novo"));
        var depois = usuarios.credencial(ANA).orElseThrow();
        assertEquals("$2a$10$novo", depois.senhaHash().valor());
        assertFalse(depois.usuario().trocarSenha());

        usuarios.salvarCadastro(cadastro(ANA, REAL, "2"));
        var central = usuarios.credencial(ANA).orElseThrow();
        assertEquals(HASH, central.senhaHash().valor());
        assertEquals(0, central.usuario().tentativasFalhas());
        assertEquals(id, central.usuario().id());
    }

    @Test
    void exclusaoLogicaEReativacaoNoMesmoRegistro() throws SQLException {
        usuarios.salvarCadastro(cadastro(ANA, REAL, "1"));
        var id = usuarios.credencial(ANA).orElseThrow().usuario().id();

        assertTrue(usuarios.excluir(ANA));
        assertFalse(usuarios.excluir(ANA));
        assertTrue(usuarios.credencial(ANA).isEmpty());
        assertTrue(usuarios.buscar(id).isEmpty());
        assertEquals("1", usuarios.versaoCadastro(ANA).orElseThrow().valor());

        usuarios.salvarCadastro(cadastro(ANA, REAL, "1"));
        assertEquals(id, usuarios.credencial(ANA).orElseThrow().usuario().id());
        assertEquals(List.of("2"), linhas("SELECT count(*) FROM usuario"));
    }

    @Test
    void sistemaNaoApareceNemEntra() throws SQLException {
        assertTrue(usuarios.logins().isEmpty());
        assertEquals(List.of("1|sistema|null|null"), linhas("SELECT id, nome, barragem_id, login FROM usuario"));
    }

    @Test
    void bancoExigeOsCamposDoUsuario() {
        var semBarragem = assertThrows(SQLException.class, () -> executar("""
                INSERT INTO usuario (nome, login, senha_hash, perfil, email, criado_em, criado_por, atualizado_em, atualizado_por)
                VALUES ('X', 'x', 'h', 'TECNICO_CAMPO', 'x@x.com', 'a', 1, 'a', 1)"""));
        assertTrue(semBarragem.getMessage().contains("obrigatórios"), semBarragem.getMessage());

        var engenheiroSemCrea = assertThrows(SQLException.class, () -> executar("""
                INSERT INTO usuario (nome, barragem_id, login, senha_hash, perfil, email, criado_em, criado_por,
                    atualizado_em, atualizado_por)
                VALUES ('X', 'joao-leite', 'x', 'h', 'ENGENHEIRO', 'x@x.com', 'a', 1, 'a', 1)"""));
        assertTrue(engenheiroSemCrea.getMessage().contains("CREA"), engenheiroSemCrea.getMessage());

        assertThrows(SQLException.class, () -> executar("UPDATE usuario SET barragem_id = 'joao-leite' WHERE id = 1"));
    }

    @Test
    void loginUnicoEntreAtivos() {
        usuarios.salvarCadastro(cadastro(ANA, REAL, "1"));
        var duplicado = assertThrows(SQLException.class, () -> executar("""
                INSERT INTO usuario (nome, barragem_id, login, senha_hash, perfil, email, criado_em, criado_por,
                    atualizado_em, atualizado_por)
                VALUES ('X', 'joao-leite', 'ana', 'h', 'TECNICO_CAMPO', 'x@x.com', 'a', 1, 'a', 1)"""));
        assertTrue(duplicado.getMessage().contains("UNIQUE"), duplicado.getMessage());
    }

    @Test
    void limpezaApagaUsuariosDeTesteMasMantemAutorDeRegistroReal() throws SQLException {
        usuarios.salvarCadastro(cadastro(ANA, DE_TESTE, "1"));
        usuarios.salvarCadastro(cadastro(BETO, DE_TESTE, "1"));
        var ana = usuarios.credencial(ANA).orElseThrow().usuario().id();
        var beto = usuarios.credencial(BETO).orElseThrow().usuario().id();
        // ana alterou a barragem de teste (some com ela); beto, conectado, alterou a barragem real
        new RepositorioBarragensJdbc(banco, AutoriaDeTeste.em(AGORA, ana)).salvar(new Barragem(DE_TESTE, "UAT 2", true));
        new RepositorioBarragensJdbc(banco, AutoriaDeTeste.em(AGORA, beto)).salvar(new Barragem(REAL, "João Leite 2"));

        var apagados = new LimparDadosTeste(new RepositorioDadosTesteJdbc(banco)).executar();

        // beto fica (autor de registro real), com ele a barragem de teste dele, e ana, que gravou essa barragem
        assertEquals(0, apagados.get("usuario"));
        assertEquals(0, apagados.get("barragem"));
        assertEquals(List.of("ana", "beto"), linhas("SELECT login FROM usuario WHERE id <> 1 ORDER BY login"));

        // sem a autoria real, tudo de teste sai
        executar("UPDATE barragem SET atualizado_por = 1 WHERE id = 'joao-leite'");
        apagados = new LimparDadosTeste(new RepositorioDadosTesteJdbc(banco)).executar();
        assertEquals(2, apagados.get("usuario"));
        assertEquals(1, apagados.get("barragem"));
        assertEquals(List.of("joao-leite"), linhas("SELECT id FROM barragem"));
        assertEquals(List.of("1"), linhas("SELECT id FROM usuario"));
    }
}
