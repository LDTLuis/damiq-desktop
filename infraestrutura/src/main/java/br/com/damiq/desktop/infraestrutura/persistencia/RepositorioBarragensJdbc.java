package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.CampoBarragem;
import br.com.damiq.desktop.dominio.barragem.Coordenadas;
import br.com.damiq.desktop.dominio.barragem.TipoCampo;
import br.com.damiq.desktop.dominio.barragem.UnidadeFederativa;
import br.com.damiq.desktop.dominio.barragem.VersaoCadastro;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** {@link RepositorioBarragens} nas tabelas {@code barragem} e {@code barragem_campo}. */
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
                            campos(conexao, id)));
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
            gravarCampos(conexao, c, agora);
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

    private record CampoGravado(long id, CampoBarragem campo, int ordem) {}

    private static List<CampoBarragem> campos(Connection conexao, BarragemId id) throws SQLException {
        return camposGravados(conexao, id).values().stream()
                .sorted((a, b) -> Integer.compare(a.ordem(), b.ordem()))
                .map(CampoGravado::campo)
                .toList();
    }

    private static Map<String, CampoGravado> camposGravados(Connection conexao, BarragemId id) throws SQLException {
        try (var consulta = conexao.prepareStatement("""
                SELECT id, chave, rotulo, grupo, tipo, valor, unidade, ordem FROM barragem_campo
                WHERE barragem_id = ? AND excluido_em IS NULL
                """)) {
            consulta.setString(1, id.valor());
            try (var linhas = consulta.executeQuery()) {
                var campos = new HashMap<String, CampoGravado>();
                while (linhas.next()) {
                    var campo = new CampoBarragem(linhas.getString("chave"), linhas.getString("rotulo"),
                            linhas.getString("grupo"), TipoCampo.valueOf(linhas.getString("tipo")),
                            linhas.getString("valor"), linhas.getString("unidade"));
                    campos.put(campo.chave(), new CampoGravado(linhas.getLong("id"), campo, linhas.getInt("ordem")));
                }
                return campos;
            }
        }
    }

    /** Insere os campos novos, atualiza os que mudaram e aplica exclusão lógica aos que saíram. */
    private void gravarCampos(Connection conexao, CadastroBarragem c, String agora) throws SQLException {
        var gravados = camposGravados(conexao, c.id());
        try (var inserir = conexao.prepareStatement("""
                        INSERT INTO barragem_campo (barragem_id, chave, rotulo, grupo, tipo, valor, unidade, ordem,
                            criado_em, criado_por, atualizado_em, atualizado_por, teste)
                        SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, teste FROM barragem WHERE id = ?
                        """);
                var atualizar = conexao.prepareStatement("""
                        UPDATE barragem_campo SET rotulo = ?, grupo = ?, tipo = ?, valor = ?, unidade = ?, ordem = ?,
                            atualizado_em = ?, atualizado_por = ?
                        WHERE id = ?
                        """);
                var excluir = conexao.prepareStatement("""
                        UPDATE barragem_campo SET excluido_em = ?, atualizado_em = ?, atualizado_por = ? WHERE id = ?
                        """)) {
            for (int ordem = 0; ordem < c.campos().size(); ordem++) {
                var campo = c.campos().get(ordem);
                var gravado = gravados.remove(campo.chave());
                if (gravado == null) {
                    inserir.setString(1, c.id().valor());
                    inserir.setString(2, campo.chave());
                    inserir.setString(3, campo.rotulo());
                    inserir.setString(4, campo.grupo());
                    inserir.setString(5, campo.tipo().name());
                    inserir.setString(6, campo.valor());
                    inserir.setString(7, campo.unidade());
                    inserir.setInt(8, ordem);
                    inserir.setString(9, agora);
                    inserir.setLong(10, autoria.usuario());
                    inserir.setString(11, agora);
                    inserir.setLong(12, autoria.usuario());
                    inserir.setString(13, c.id().valor());
                    inserir.addBatch();
                } else if (!gravado.campo().equals(campo) || gravado.ordem() != ordem) {
                    atualizar.setString(1, campo.rotulo());
                    atualizar.setString(2, campo.grupo());
                    atualizar.setString(3, campo.tipo().name());
                    atualizar.setString(4, campo.valor());
                    atualizar.setString(5, campo.unidade());
                    atualizar.setInt(6, ordem);
                    atualizar.setString(7, agora);
                    atualizar.setLong(8, autoria.usuario());
                    atualizar.setLong(9, gravado.id());
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
}
