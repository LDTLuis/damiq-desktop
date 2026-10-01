package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.usuario.Credencial;
import br.com.damiq.desktop.aplicacao.usuario.RepositorioUsuarios;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.Perfil;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.Usuario;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import br.com.damiq.desktop.dominio.usuario.VersaoUsuario;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.sql.DataSource;

/** {@link RepositorioUsuarios} na tabela {@code usuario}. O usuário 1, "sistema", nunca aparece. */
public final class RepositorioUsuariosJdbc implements RepositorioUsuarios {

    private static final String COLUNAS = """
            id, barragem_id, login, perfil, nome, email, telefone, cargo, registro_profissional, bloqueado,
            tentativas_falhas, trocar_senha, ultimo_acesso, senha_hash""";

    private static final String ATIVO = "id <> 1 AND excluido_em IS NULL";

    private final Jdbc jdbc;
    private final Autoria autoria;

    public RepositorioUsuariosJdbc(DataSource fonte, Autoria autoria) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
        this.autoria = Validacao.obrigatorio(autoria, "autoria");
    }

    @Override
    public Optional<Credencial> credencial(Login login) {
        return jdbc.executar("buscar o usuário " + login, conexao -> {
            try (var consulta = conexao.prepareStatement(
                    "SELECT " + COLUNAS + " FROM usuario WHERE login = ? AND " + ATIVO)) {
                consulta.setString(1, login.valor());
                try (var linha = consulta.executeQuery()) {
                    return linha.next()
                            ? Optional.of(new Credencial(usuario(linha), new SenhaHash(linha.getString("senha_hash"))))
                            : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<Usuario> buscar(UsuarioId id) {
        return jdbc.executar("buscar o usuário " + id, conexao -> {
            try (var consulta = conexao.prepareStatement(
                    "SELECT " + COLUNAS + " FROM usuario WHERE id = ? AND " + ATIVO)) {
                consulta.setLong(1, id.valor());
                try (var linha = consulta.executeQuery()) {
                    return linha.next() ? Optional.of(usuario(linha)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Usuario> listar(BarragemId barragem) {
        return jdbc.executar("listar os usuários da barragem " + barragem, conexao -> {
            try (var consulta = conexao.prepareStatement(
                    "SELECT " + COLUNAS + " FROM usuario WHERE barragem_id = ? AND " + ATIVO + " ORDER BY nome, login")) {
                consulta.setString(1, barragem.valor());
                try (var linha = consulta.executeQuery()) {
                    var usuarios = new ArrayList<Usuario>();
                    while (linha.next()) {
                        usuarios.add(usuario(linha));
                    }
                    return usuarios;
                }
            }
        });
    }

    @Override
    public List<Login> logins() {
        return jdbc.executar("listar os logins", conexao -> {
            try (var consulta = conexao.prepareStatement("SELECT login FROM usuario WHERE " + ATIVO + " ORDER BY login");
                    var linha = consulta.executeQuery()) {
                var logins = new ArrayList<Login>();
                while (linha.next()) {
                    logins.add(new Login(linha.getString(1)));
                }
                return logins;
            }
        });
    }

    @Override
    public Optional<VersaoUsuario> versaoCadastro(Login login) {
        return jdbc.executar("consultar a versão do cadastro do usuário " + login, conexao -> {
            try (var consulta = conexao.prepareStatement(
                    "SELECT versao_cadastro FROM usuario WHERE login = ? AND id <> 1 ORDER BY excluido_em IS NULL DESC, id DESC LIMIT 1")) {
                consulta.setString(1, login.valor());
                try (var linha = consulta.executeQuery()) {
                    var versao = linha.next() ? linha.getString(1) : null;
                    return Optional.ofNullable(versao).map(VersaoUsuario::new);
                }
            }
        });
    }

    @Override
    public void salvarCadastro(CadastroUsuario c) {
        jdbc.emTransacao("gravar o cadastro do usuário " + c.login(), conexao -> {
            var agora = autoria.agora();
            Long existente = null;
            // o mais recente com esse login, ativo ou excluído: um usuário que volta à Central é reativado
            try (var consulta = conexao.prepareStatement(
                    "SELECT id FROM usuario WHERE login = ? AND id <> 1 ORDER BY excluido_em IS NULL DESC, id DESC LIMIT 1")) {
                consulta.setString(1, c.login().valor());
                try (var linha = consulta.executeQuery()) {
                    if (linha.next()) {
                        existente = linha.getLong(1);
                    }
                }
            }
            if (existente == null) {
                // teste herdado da barragem, só no primeiro cadastro
                try (var comando = conexao.prepareStatement("""
                        INSERT INTO usuario (barragem_id, login, versao_cadastro, senha_hash, perfil, nome, email,
                            telefone, cargo, registro_profissional, bloqueado, trocar_senha, teste,
                            criado_em, criado_por, atualizado_em, atualizado_por)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT teste FROM barragem WHERE id = ?), ?, ?, ?, ?)
                        """)) {
                    int i = preencherCadastro(comando, c);
                    comando.setString(i++, c.barragem().valor());
                    comando.setString(i++, agora);
                    comando.setLong(i++, autoria.usuario());
                    comando.setString(i++, agora);
                    comando.setLong(i, autoria.usuario());
                    comando.executeUpdate();
                }
            } else {
                try (var comando = conexao.prepareStatement("""
                        UPDATE usuario SET barragem_id = ?, login = ?, versao_cadastro = ?, senha_hash = ?, perfil = ?,
                            nome = ?, email = ?, telefone = ?, cargo = ?, registro_profissional = ?, bloqueado = ?,
                            trocar_senha = ?, tentativas_falhas = 0, excluido_em = NULL, atualizado_em = ?,
                            atualizado_por = ?
                        WHERE id = ?
                        """)) {
                    int i = preencherCadastro(comando, c);
                    comando.setString(i++, agora);
                    comando.setLong(i++, autoria.usuario());
                    comando.setLong(i, existente);
                    comando.executeUpdate();
                }
            }
            return null;
        });
    }

    /** Preenche os parâmetros 1 a 12 (de barragem_id a trocar_senha); devolve o próximo. */
    private static int preencherCadastro(PreparedStatement comando, CadastroUsuario c) throws SQLException {
        int i = 1;
        comando.setString(i++, c.barragem().valor());
        comando.setString(i++, c.login().valor());
        comando.setString(i++, c.versao().valor());
        comando.setString(i++, c.senhaHash().valor());
        comando.setString(i++, c.perfil().name());
        comando.setString(i++, c.nome());
        comando.setString(i++, c.email());
        comando.setString(i++, c.telefone());
        comando.setString(i++, c.cargo());
        comando.setString(i++, c.registroProfissional());
        comando.setInt(i++, c.bloqueado() ? 1 : 0);
        comando.setInt(i++, c.trocarSenha() ? 1 : 0);
        return i;
    }

    @Override
    public boolean excluir(Login login) {
        return atualizar("excluir o usuário " + login,
                "UPDATE usuario SET excluido_em = ?, atualizado_em = ?, atualizado_por = ? WHERE login = ? AND " + ATIVO,
                agora -> new Object[] {agora, agora, autoria.usuario(), login.valor()}) > 0;
    }

    @Override
    public int registrarFalha(UsuarioId id, int limite) {
        return jdbc.emTransacao("registrar tentativa malsucedida do usuário " + id, conexao -> {
            var agora = autoria.agora();
            try (var comando = conexao.prepareStatement("""
                    UPDATE usuario SET tentativas_falhas = tentativas_falhas + 1,
                        bloqueado = CASE WHEN tentativas_falhas + 1 >= ? THEN 1 ELSE bloqueado END,
                        atualizado_em = ?, atualizado_por = ?
                    WHERE id = ? AND id <> 1
                    """)) {
                comando.setInt(1, limite);
                comando.setString(2, agora);
                comando.setLong(3, autoria.usuario());
                comando.setLong(4, id.valor());
                comando.executeUpdate();
            }
            try (var consulta = conexao.prepareStatement("SELECT tentativas_falhas FROM usuario WHERE id = ?")) {
                consulta.setLong(1, id.valor());
                try (var linha = consulta.executeQuery()) {
                    return linha.next() ? linha.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public void registrarAcesso(UsuarioId id, Instant momento) {
        atualizar("registrar o acesso do usuário " + id, """
                UPDATE usuario SET ultimo_acesso = ?, tentativas_falhas = 0, atualizado_em = ?, atualizado_por = ?
                WHERE id = ? AND id <> 1
                """, agora -> new Object[] {BancoDados.data(momento), agora, autoria.usuario(), id.valor()});
    }

    @Override
    public void trocarSenha(UsuarioId id, SenhaHash senhaHash) {
        atualizar("trocar a senha do usuário " + id, """
                UPDATE usuario SET senha_hash = ?, trocar_senha = 0, atualizado_em = ?, atualizado_por = ?
                WHERE id = ? AND id <> 1
                """, agora -> new Object[] {senhaHash.valor(), agora, autoria.usuario(), id.valor()});
    }

    @Override
    public boolean desbloquear(UsuarioId id) {
        return atualizar("desbloquear o usuário " + id,
                "UPDATE usuario SET bloqueado = 0, tentativas_falhas = 0, atualizado_em = ?, atualizado_por = ?"
                        + " WHERE id = ? AND " + ATIVO,
                agora -> new Object[] {agora, autoria.usuario(), id.valor()}) > 0;
    }

    private int atualizar(String descricao, String sql, Function<String, Object[]> parametros) {
        return jdbc.executar(descricao, conexao -> {
            try (var comando = conexao.prepareStatement(sql)) {
                var valores = parametros.apply(autoria.agora());
                for (int i = 0; i < valores.length; i++) {
                    comando.setObject(i + 1, valores[i]);
                }
                return comando.executeUpdate();
            }
        });
    }

    private static Usuario usuario(ResultSet linha) throws SQLException {
        var ultimoAcesso = linha.getString("ultimo_acesso");
        return new Usuario(
                new UsuarioId(linha.getLong("id")),
                new BarragemId(linha.getString("barragem_id")),
                new Login(linha.getString("login")),
                Perfil.valueOf(linha.getString("perfil")),
                linha.getString("nome"),
                linha.getString("email"),
                linha.getString("telefone"),
                linha.getString("cargo"),
                linha.getString("registro_profissional"),
                linha.getInt("bloqueado") == 1,
                linha.getInt("tentativas_falhas"),
                linha.getInt("trocar_senha") == 1,
                ultimoAcesso != null ? Instant.parse(ultimoAcesso) : null);
    }
}
