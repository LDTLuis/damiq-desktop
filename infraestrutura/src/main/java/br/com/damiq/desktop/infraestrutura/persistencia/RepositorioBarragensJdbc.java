package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.CampoBarragem;
import br.com.damiq.desktop.dominio.barragem.ContatoBarragem;
import br.com.damiq.desktop.dominio.barragem.Coordenadas;
import br.com.damiq.desktop.dominio.barragem.GrupoCampos;
import br.com.damiq.desktop.dominio.barragem.MeioContato;
import br.com.damiq.desktop.dominio.barragem.PapelContato;
import br.com.damiq.desktop.dominio.barragem.TipoCampo;
import br.com.damiq.desktop.dominio.barragem.UnidadeFederativa;
import br.com.damiq.desktop.dominio.barragem.VersaoCadastro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.sql.DataSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link RepositorioBarragens} nas tabelas {@code barragem}, {@code barragem_grupo}, {@code barragem_campo} e
 * {@code barragem_contato}.
 */
public final class RepositorioBarragensJdbc implements RepositorioBarragens {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Jdbc jdbc;
    private final Autoria autoria;

    public RepositorioBarragensJdbc(DataSource fonte, Autoria autoria) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
        this.autoria = Validacao.obrigatorio(autoria, "autoria");
    }

    @Override
    public void salvar(Barragem barragem) {
        jdbc.executar("salvar a barragem " + barragem.id(), conexao -> {
            try (var comando = conexao.prepareStatement("""
                    INSERT INTO barragem (id, nome, teste, criado_em, criado_por, atualizado_em, atualizado_por)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        nome = excluded.nome, atualizado_em = excluded.atualizado_em,
                        atualizado_por = excluded.atualizado_por
                    """)) {
                var agora = autoria.agora();
                comando.setString(1, barragem.id().valor());
                comando.setString(2, barragem.nome());
                comando.setInt(3, barragem.teste() ? 1 : 0);
                comando.setString(4, agora);
                comando.setLong(5, autoria.usuario());
                comando.setString(6, agora);
                comando.setLong(7, autoria.usuario());
                return comando.executeUpdate();
            }
        });
    }

    @Override
    public Optional<Barragem> buscar(BarragemId id) {
        return jdbc.executar("buscar a barragem " + id, conexao -> {
            try (var consulta = conexao.prepareStatement(
                    "SELECT nome, teste FROM barragem WHERE id = ? AND excluido_em IS NULL")) {
                consulta.setString(1, id.valor());
                try (var linhas = consulta.executeQuery()) {
                    return linhas.next()
                            ? Optional.of(new Barragem(id, linhas.getString("nome"), linhas.getInt("teste") == 1))
                            : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Barragem> listar() {
        return jdbc.executar("listar as barragens", conexao -> {
            try (var consulta = conexao.prepareStatement(
                            "SELECT id, nome, teste FROM barragem WHERE excluido_em IS NULL ORDER BY nome, id");
                    var linhas = consulta.executeQuery()) {
                var barragens = new ArrayList<Barragem>();
                while (linhas.next()) {
                    barragens.add(new Barragem(
                            new BarragemId(linhas.getString("id")), linhas.getString("nome"), linhas.getInt("teste") == 1));
                }
                return barragens;
            }
        });
    }

    @Override
    public Optional<VersaoCadastro> versaoCadastro(BarragemId id) {
        return jdbc.executar("consultar a versão do cadastro da barragem " + id, conexao -> {
            try (var consulta = conexao.prepareStatement("SELECT versao_cadastro FROM barragem WHERE id = ?")) {
                consulta.setString(1, id.valor());
                try (var linhas = consulta.executeQuery()) {
                    var versao = linhas.next() ? linhas.getString(1) : null;
                    return Optional.ofNullable(versao).map(VersaoCadastro::new);
                }
            }
        });
    }

    @Override
    public Optional<CadastroBarragem> buscarCadastro(BarragemId id) {
        return jdbc.executar("buscar o cadastro da barragem " + id, conexao -> {
            try (var consulta = conexao.prepareStatement("""
                    SELECT versao_cadastro, nome, teste, empreendedor, finalidade, municipios, uf, latitude, longitude,
                           curso_dagua, tipo_macico, altura_macico_m, capacidade_total_m3
                    FROM barragem WHERE id = ? AND excluido_em IS NULL AND versao_cadastro IS NOT NULL
                    """)) {
                consulta.setString(1, id.valor());
                try (var linha = consulta.executeQuery()) {
                    if (!linha.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new CadastroBarragem(
                            id,
                            new VersaoCadastro(linha.getString("versao_cadastro")),
                            linha.getString("nome"),
                            linha.getInt("teste") == 1,
                            linha.getString("empreendedor"),
                            linha.getString("finalidade"),
                            JSON.readValue(linha.getString("municipios"), new TypeReference<List<String>>() {}),
                            UnidadeFederativa.valueOf(linha.getString("uf")),
                            new Coordenadas(linha.getDouble("latitude"), linha.getDouble("longitude")),
                            linha.getString("curso_dagua"),
                            linha.getString("tipo_macico"),
                            linha.getDouble("altura_macico_m"),
                            linha.getDouble("capacidade_total_m3"),
                            ordenados(gruposGravados(conexao, id).values(), GrupoGravado::ordem, GrupoGravado::grupo),
                            ordenados(camposGravados(conexao, id).values(), CampoGravado::ordem, CampoGravado::campo),
                            ordenados(contatosGravados(conexao, id).values(), ContatoGravado::ordem,
                                    ContatoGravado::contato)));
                }
            }
        });
    }

    @Override
    public void salvarCadastro(CadastroBarragem c) {
        jdbc.emTransacao("gravar o cadastro da barragem " + c.id(), conexao -> {
            var agora = autoria.agora();
            // teste só vale no primeiro cadastro; uma barragem excluída que volta à Central é reativada
            try (var comando = conexao.prepareStatement("""
                    INSERT INTO barragem (id, nome, teste, versao_cadastro, empreendedor, finalidade, municipios, uf,
                        latitude, longitude, curso_dagua, tipo_macico, altura_macico_m, capacidade_total_m3,
                        criado_em, criado_por, atualizado_em, atualizado_por)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        nome = excluded.nome, versao_cadastro = excluded.versao_cadastro,
                        empreendedor = excluded.empreendedor, finalidade = excluded.finalidade,
                        municipios = excluded.municipios, uf = excluded.uf, latitude = excluded.latitude,
                        longitude = excluded.longitude, curso_dagua = excluded.curso_dagua,
                        tipo_macico = excluded.tipo_macico, altura_macico_m = excluded.altura_macico_m,
                        capacidade_total_m3 = excluded.capacidade_total_m3, excluido_em = NULL,
                        atualizado_em = excluded.atualizado_em, atualizado_por = excluded.atualizado_por
                    """)) {
                comando.setString(1, c.id().valor());
                comando.setString(2, c.nome());
                comando.setInt(3, c.teste() ? 1 : 0);
                comando.setString(4, c.versao().valor());
                comando.setString(5, c.empreendedor());
                comando.setString(6, c.finalidade());
                comando.setString(7, JSON.writeValueAsString(c.municipios()));
                comando.setString(8, c.uf().name());
                comando.setDouble(9, c.coordenadas().latitude());
                comando.setDouble(10, c.coordenadas().longitude());
                comando.setString(11, c.cursoDagua());
                comando.setString(12, c.tipoMacico());
                comando.setDouble(13, c.alturaMacicoM());
                comando.setDouble(14, c.capacidadeTotalM3());
                comando.setString(15, agora);
                comando.setLong(16, autoria.usuario());
                comando.setString(17, agora);
                comando.setLong(18, autoria.usuario());
                comando.executeUpdate();
            }
            var idsGrupos = gravarGrupos(conexao, c, agora);
            gravarCampos(conexao, c, idsGrupos, agora);
            gravarContatos(conexao, c, agora);
            return null;
        });
    }

    @Override
    public boolean excluir(BarragemId id) {
        return jdbc.executar("excluir a barragem " + id, conexao -> {
            try (var comando = conexao.prepareStatement("""
                    UPDATE barragem SET excluido_em = ?, atualizado_em = ?, atualizado_por = ?
                    WHERE id = ? AND excluido_em IS NULL
                    """)) {
                var agora = autoria.agora();
                comando.setString(1, agora);
                comando.setString(2, agora);
                comando.setLong(3, autoria.usuario());
                comando.setString(4, id.valor());
                return comando.executeUpdate() == 1;
            }
        });
    }

    private record GrupoGravado(long id, GrupoCampos grupo, int ordem) {}

    private record CampoGravado(long id, CampoBarragem campo, int ordem) {}

    private record ContatoGravado(long id, ContatoBarragem contato, int ordem) {}

    private static <G, T> List<T> ordenados(Collection<G> gravados, ToIntFunction<G> ordem, Function<G, T> item) {
        return gravados.stream().sorted(Comparator.comparingInt(ordem)).map(item).toList();
    }

    private static Map<String, GrupoGravado> gruposGravados(Connection conexao, BarragemId id) throws SQLException {
        try (var consulta = conexao.prepareStatement(
                "SELECT id, chave, nome, ordem FROM barragem_grupo WHERE barragem_id = ? AND excluido_em IS NULL")) {
            consulta.setString(1, id.valor());
            try (var linhas = consulta.executeQuery()) {
                var grupos = new HashMap<String, GrupoGravado>();
                while (linhas.next()) {
                    var grupo = new GrupoCampos(linhas.getString("chave"), linhas.getString("nome"));
                    grupos.put(grupo.chave(), new GrupoGravado(linhas.getLong("id"), grupo, linhas.getInt("ordem")));
                }
                return grupos;
            }
        }
    }

    private static Map<String, CampoGravado> camposGravados(Connection conexao, BarragemId id) throws SQLException {
        try (var consulta = conexao.prepareStatement("""
                SELECT c.id, c.chave, c.rotulo, g.chave AS grupo, c.tipo, c.valor, c.unidade, c.padrao, c.ordem
                FROM barragem_campo c LEFT JOIN barragem_grupo g ON g.id = c.grupo_id
                WHERE c.barragem_id = ? AND c.excluido_em IS NULL
                """)) {
            consulta.setString(1, id.valor());
            try (var linhas = consulta.executeQuery()) {
                var campos = new HashMap<String, CampoGravado>();
                while (linhas.next()) {
                    var campo = new CampoBarragem(linhas.getString("chave"), linhas.getString("rotulo"),
                            linhas.getString("grupo"), TipoCampo.valueOf(linhas.getString("tipo")),
                            linhas.getString("valor"), linhas.getString("unidade"), linhas.getInt("padrao") == 1);
                    campos.put(campo.chave(), new CampoGravado(linhas.getLong("id"), campo, linhas.getInt("ordem")));
                }
                return campos;
            }
        }
    }

    /**
     * Insere os grupos novos, atualiza os que mudaram e aplica exclusão lógica aos que saíram.
     *
     * @return id de cada grupo ativo, pela chave
     */
    private Map<String, Long> gravarGrupos(Connection conexao, CadastroBarragem c, String agora) throws SQLException {
        var gravados = gruposGravados(conexao, c.id());
        var ids = new HashMap<String, Long>();
        try (var inserir = conexao.prepareStatement("""
                        INSERT INTO barragem_grupo (barragem_id, chave, nome, ordem,
                            criado_em, criado_por, atualizado_em, atualizado_por, teste)
                        SELECT ?, ?, ?, ?, ?, ?, ?, ?, teste FROM barragem WHERE id = ?
                        RETURNING id
                        """);
                var atualizar = conexao.prepareStatement("""
                        UPDATE barragem_grupo SET nome = ?, ordem = ?, atualizado_em = ?, atualizado_por = ? WHERE id = ?
                        """);
                var excluir = conexao.prepareStatement("""
                        UPDATE barragem_grupo SET excluido_em = ?, atualizado_em = ?, atualizado_por = ? WHERE id = ?
                        """)) {
            for (int ordem = 0; ordem < c.grupos().size(); ordem++) {
                var grupo = c.grupos().get(ordem);
                var gravado = gravados.remove(grupo.chave());
                if (gravado == null) {
                    inserir.setString(1, c.id().valor());
                    inserir.setString(2, grupo.chave());
                    inserir.setString(3, grupo.nome());
                    inserir.setInt(4, ordem);
                    inserir.setString(5, agora);
                    inserir.setLong(6, autoria.usuario());
                    inserir.setString(7, agora);
                    inserir.setLong(8, autoria.usuario());
                    inserir.setString(9, c.id().valor());
                    try (var gerado = inserir.executeQuery()) {
                        gerado.next();
                        ids.put(grupo.chave(), gerado.getLong(1));
                    }
                } else {
                    ids.put(grupo.chave(), gravado.id());
                    if (!gravado.grupo().equals(grupo) || gravado.ordem() != ordem) {
                        atualizar.setString(1, grupo.nome());
                        atualizar.setInt(2, ordem);
                        atualizar.setString(3, agora);
                        atualizar.setLong(4, autoria.usuario());
                        atualizar.setLong(5, gravado.id());
                        atualizar.addBatch();
                    }
                }
            }
            for (var saiu : gravados.values()) {
                excluir.setString(1, agora);
                excluir.setString(2, agora);
                excluir.setLong(3, autoria.usuario());
                excluir.setLong(4, saiu.id());
                excluir.addBatch();
            }
            atualizar.executeBatch();
            excluir.executeBatch();
        }
        return ids;
    }

    /** Insere os campos novos, atualiza os que mudaram e aplica exclusão lógica aos que saíram. */
    private void gravarCampos(Connection conexao, CadastroBarragem c, Map<String, Long> idsGrupos, String agora)
            throws SQLException {
        var gravados = camposGravados(conexao, c.id());
        try (var inserir = conexao.prepareStatement("""
                        INSERT INTO barragem_campo (barragem_id, grupo_id, chave, rotulo, tipo, valor, unidade, padrao,
                            ordem, criado_em, criado_por, atualizado_em, atualizado_por, teste)
                        SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, teste FROM barragem WHERE id = ?
                        """);
                var atualizar = conexao.prepareStatement("""
                        UPDATE barragem_campo SET grupo_id = ?, rotulo = ?, tipo = ?, valor = ?, unidade = ?,
                            padrao = ?, ordem = ?, atualizado_em = ?, atualizado_por = ?
                        WHERE id = ?
                        """);
                var excluir = conexao.prepareStatement("""
                        UPDATE barragem_campo SET excluido_em = ?, atualizado_em = ?, atualizado_por = ? WHERE id = ?
                        """)) {
            for (int ordem = 0; ordem < c.campos().size(); ordem++) {
                var campo = c.campos().get(ordem);
                var grupo = campo.grupo() == null ? null : idsGrupos.get(campo.grupo());
                var gravado = gravados.remove(campo.chave());
                if (gravado == null) {
                    inserir.setString(1, c.id().valor());
                    definirGrupo(inserir, 2, grupo);
                    inserir.setString(3, campo.chave());
                    inserir.setString(4, campo.rotulo());
                    inserir.setString(5, campo.tipo().name());
                    inserir.setString(6, campo.valor());
                    inserir.setString(7, campo.unidade());
                    inserir.setInt(8, campo.padrao() ? 1 : 0);
                    inserir.setInt(9, ordem);
                    inserir.setString(10, agora);
                    inserir.setLong(11, autoria.usuario());
                    inserir.setString(12, agora);
                    inserir.setLong(13, autoria.usuario());
                    inserir.setString(14, c.id().valor());
                    inserir.addBatch();
                } else if (!gravado.campo().equals(campo) || gravado.ordem() != ordem) {
                    definirGrupo(atualizar, 1, grupo);
                    atualizar.setString(2, campo.rotulo());
                    atualizar.setString(3, campo.tipo().name());
                    atualizar.setString(4, campo.valor());
                    atualizar.setString(5, campo.unidade());
                    atualizar.setInt(6, campo.padrao() ? 1 : 0);
                    atualizar.setInt(7, ordem);
                    atualizar.setString(8, agora);
                    atualizar.setLong(9, autoria.usuario());
                    atualizar.setLong(10, gravado.id());
                    atualizar.addBatch();
                }
            }
            for (var saiu : gravados.values()) {
                excluir.setString(1, agora);
                excluir.setString(2, agora);
                excluir.setLong(3, autoria.usuario());
                excluir.setLong(4, saiu.id());
                excluir.addBatch();
            }
            inserir.executeBatch();
            atualizar.executeBatch();
            excluir.executeBatch();
        }
    }

    private static void definirGrupo(PreparedStatement comando, int posicao, Long grupo) throws SQLException {
        if (grupo == null) {
            comando.setNull(posicao, Types.INTEGER);
        } else {
            comando.setLong(posicao, grupo);
        }
    }

    private static Map<String, ContatoGravado> contatosGravados(Connection conexao, BarragemId id) throws SQLException {
        try (var consulta = conexao.prepareStatement("""
                SELECT id, chave, papel, entidade, responsavel, cargo, meios, nivel_acionamento, substitui,
                       recebe_copia_pae, ordem
                FROM barragem_contato WHERE barragem_id = ? AND excluido_em IS NULL
                """)) {
            consulta.setString(1, id.valor());
            try (var linhas = consulta.executeQuery()) {
                var contatos = new HashMap<String, ContatoGravado>();
                while (linhas.next()) {
                    var lido = linhas.getInt("nivel_acionamento");
                    Integer nivel = linhas.wasNull() ? null : lido;
                    var contato = new ContatoBarragem(linhas.getString("chave"),
                            PapelContato.valueOf(linhas.getString("papel")), linhas.getString("entidade"),
                            linhas.getString("responsavel"), linhas.getString("cargo"),
                            JSON.readValue(linhas.getString("meios"), new TypeReference<List<MeioContato>>() {}),
                            nivel, linhas.getString("substitui"),
                            linhas.getInt("recebe_copia_pae") == 1);
                    contatos.put(contato.chave(), new ContatoGravado(linhas.getLong("id"), contato, linhas.getInt("ordem")));
                }
                return contatos;
            }
        }
    }

    /** Insere os contatos novos, atualiza os que mudaram e aplica exclusão lógica aos que saíram. */
    private void gravarContatos(Connection conexao, CadastroBarragem c, String agora) throws SQLException {
        var gravados = contatosGravados(conexao, c.id());
        try (var inserir = conexao.prepareStatement("""
                        INSERT INTO barragem_contato (papel, entidade, responsavel, cargo, meios, nivel_acionamento,
                            substitui, recebe_copia_pae, ordem, atualizado_em, atualizado_por,
                            barragem_id, chave, criado_em, criado_por, teste)
                        SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, teste FROM barragem WHERE id = ?
                        """);
                var atualizar = conexao.prepareStatement("""
                        UPDATE barragem_contato SET papel = ?, entidade = ?, responsavel = ?, cargo = ?, meios = ?,
                            nivel_acionamento = ?, substitui = ?, recebe_copia_pae = ?, ordem = ?,
                            atualizado_em = ?, atualizado_por = ?
                        WHERE id = ?
                        """);
                var excluir = conexao.prepareStatement("""
                        UPDATE barragem_contato SET excluido_em = ?, atualizado_em = ?, atualizado_por = ? WHERE id = ?
                        """)) {
            for (int ordem = 0; ordem < c.contatos().size(); ordem++) {
                var contato = c.contatos().get(ordem);
                var gravado = gravados.remove(contato.chave());
                if (gravado == null) {
                    definirContato(inserir, contato, ordem, agora);
                    inserir.setString(12, c.id().valor());
                    inserir.setString(13, contato.chave());
                    inserir.setString(14, agora);
                    inserir.setLong(15, autoria.usuario());
                    inserir.setString(16, c.id().valor());
                    inserir.addBatch();
                } else if (!gravado.contato().equals(contato) || gravado.ordem() != ordem) {
                    definirContato(atualizar, contato, ordem, agora);
                    atualizar.setLong(12, gravado.id());
                    atualizar.addBatch();
                }
            }
            for (var saiu : gravados.values()) {
                excluir.setString(1, agora);
                excluir.setString(2, agora);
                excluir.setLong(3, autoria.usuario());
                excluir.setLong(4, saiu.id());
                excluir.addBatch();
            }
            inserir.executeBatch();
            atualizar.executeBatch();
            excluir.executeBatch();
        }
    }

    /** Parâmetros 1 a 11, comuns à inclusão e à atualização do contato. */
    private void definirContato(PreparedStatement comando, ContatoBarragem contato, int ordem, String agora)
            throws SQLException {
        comando.setString(1, contato.papel().name());
        comando.setString(2, contato.entidade());
        comando.setString(3, contato.responsavel());
        comando.setString(4, contato.cargo());
        comando.setString(5, JSON.writeValueAsString(contato.meios()));
        if (contato.nivelAcionamento() == null) {
            comando.setNull(6, Types.INTEGER);
        } else {
            comando.setInt(6, contato.nivelAcionamento());
        }
        comando.setString(7, contato.substitui());
        comando.setInt(8, contato.recebeCopiaPae() ? 1 : 0);
        comando.setInt(9, ordem);
        comando.setString(10, agora);
        comando.setLong(11, autoria.usuario());
    }
}
