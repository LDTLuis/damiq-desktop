package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.notificacao.AlertaPendente;
import br.com.damiq.desktop.aplicacao.notificacao.Notificacao;
import br.com.damiq.desktop.aplicacao.notificacao.RepositorioNotificacoes;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** {@link RepositorioNotificacoes} nas tabelas {@code notificacao} e {@code alerta}. */
public final class RepositorioNotificacoesJdbc implements RepositorioNotificacoes {

    private static final String COLUNAS_NOTIFICACAO = """
            id, barragem_id, instrumento, tipo_alerta, categoria, severidade, mensagem, leitura_suspeita,
            nivel_resposta, ocorrencias, criada_em, atualizada_em, reconhecida_em, reconhecida_por, observacao
            """;

    private final Jdbc jdbc;

    public RepositorioNotificacoesJdbc(DataSource fonte) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
    }

    @Override
    public List<BarragemId> barragensComAlertasPendentes() {
        return jdbc.executar("listar barragens com alertas pendentes", conexao -> {
            try (var consulta = conexao.prepareStatement(
                            "SELECT DISTINCT barragem_id FROM alerta WHERE notificacao_id IS NULL ORDER BY barragem_id");
                    var linhas = consulta.executeQuery()) {
                var barragens = new ArrayList<BarragemId>();
                while (linhas.next()) {
                    barragens.add(new BarragemId(linhas.getString(1)));
                }
                return barragens;
            }
        });
    }

    @Override
    public List<AlertaPendente> alertasPendentes(BarragemId barragem) {
        return jdbc.executar("buscar alertas pendentes da barragem " + barragem, conexao -> {
            try (var consulta = conexao.prepareStatement("""
                    SELECT a.id, a.instrumento, a.tipo, a.categoria, a.severidade, a.inicio, a.fim, a.fuso,
                           a.leituras, a.valor_extremo, a.unidade, a.limite, a.direcao, a.leitura_suspeita,
                           a.mensagem, c.versao, p.nivel_resposta
                    FROM alerta a
                    JOIN processamento p ON p.id = a.processamento_id
                    JOIN configuracao c ON c.id = p.configuracao_id
                    WHERE a.barragem_id = ? AND a.notificacao_id IS NULL
                    ORDER BY a.id
                    """)) {
                consulta.setString(1, barragem.valor());
                try (var linhas = consulta.executeQuery()) {
                    var pendentes = new ArrayList<AlertaPendente>();
                    while (linhas.next()) {
                        pendentes.add(new AlertaPendente(linhas.getLong("id"), alerta(linhas), linhas.getInt("nivel_resposta")));
                    }
                    return pendentes;
                }
            }
        });
    }

    @Override
    public List<Notificacao> abertas(BarragemId barragem) {
        return jdbc.executar("listar notificações abertas", conexao -> {
            var sql = "SELECT " + COLUNAS_NOTIFICACAO + " FROM notificacao WHERE reconhecida_em IS NULL"
                    + (barragem == null ? "" : " AND barragem_id = ?") + " ORDER BY id";
            try (var consulta = conexao.prepareStatement(sql)) {
                if (barragem != null) {
                    consulta.setString(1, barragem.valor());
                }
                try (var linhas = consulta.executeQuery()) {
                    var abertas = new ArrayList<Notificacao>();
                    while (linhas.next()) {
                        abertas.add(notificacao(linhas));
                    }
                    return abertas;
                }
            }
        });
    }

    @Override
    public Optional<Notificacao> buscar(long id) {
        return jdbc.executar("buscar a notificação " + id, conexao -> buscar(conexao, id));
    }

    @Override
    public List<Notificacao> gravar(List<Gravacao> gravacoes) {
        return jdbc.emTransacao("gravar notificações", conexao -> {
            var gravadas = new ArrayList<Notificacao>();
            for (var gravacao : gravacoes) {
                var id = gravacao.notificacao().id() == null
                        ? inserir(conexao, gravacao.notificacao())
                        : atualizar(conexao, gravacao.notificacao());
                vincular(conexao, id, gravacao.alertas());
                gravadas.add(buscar(conexao, id).orElseThrow());
            }
            return gravadas;
        });
    }

    @Override
    public boolean reconhecer(long id, String por, String observacao, Instant em) {
        return jdbc.executar("reconhecer a notificação " + id, conexao -> {
            try (var comando = conexao.prepareStatement("""
                    UPDATE notificacao SET reconhecida_em = ?, reconhecida_por = ?, observacao = ?
                    WHERE id = ? AND reconhecida_em IS NULL
                    """)) {
                comando.setString(1, BancoDados.data(em));
                comando.setString(2, por);
                comando.setString(3, observacao);
                comando.setLong(4, id);
                return comando.executeUpdate() == 1;
            }
        });
    }

    private static long inserir(Connection conexao, Notificacao n) throws SQLException {
        try (var comando = conexao.prepareStatement("""
                INSERT INTO notificacao (barragem_id, instrumento, tipo_alerta, categoria, severidade, mensagem,
                    leitura_suspeita, nivel_resposta, ocorrencias, criada_em, atualizada_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """)) {
            comando.setString(1, n.barragem().valor());
            comando.setString(2, n.instrumento().valor());
            comando.setString(3, n.tipoAlerta().name());
            comando.setString(4, n.categoria().name());
            comando.setString(5, n.severidade().name());
            comando.setString(6, n.mensagem());
            comando.setInt(7, n.leituraSuspeita() ? 1 : 0);
            comando.setInt(8, n.nivelResposta());
            comando.setInt(9, n.ocorrencias());
            comando.setString(10, BancoDados.data(n.criadaEm()));
            comando.setString(11, BancoDados.data(n.atualizadaEm()));
            try (var gerado = comando.executeQuery()) {
                gerado.next();
                return gerado.getLong(1);
            }
        }
    }

    private static long atualizar(Connection conexao, Notificacao n) throws SQLException {
        try (var comando = conexao.prepareStatement("""
                UPDATE notificacao SET severidade = ?, mensagem = ?, leitura_suspeita = ?, nivel_resposta = ?,
                    ocorrencias = ?, atualizada_em = ?
                WHERE id = ? AND reconhecida_em IS NULL
                """)) {
            comando.setString(1, n.severidade().name());
            comando.setString(2, n.mensagem());
            comando.setInt(3, n.leituraSuspeita() ? 1 : 0);
            comando.setInt(4, n.nivelResposta());
            comando.setInt(5, n.ocorrencias());
            comando.setString(6, BancoDados.data(n.atualizadaEm()));
            comando.setLong(7, n.id());
            if (comando.executeUpdate() != 1) {
                // reconhecida entre a leitura e a gravação: desfaz tudo; a próxima execução abre uma nova
                throw new SQLException("A notificação " + n.id() + " não está mais aberta");
            }
            return n.id();
        }
    }

    private static void vincular(Connection conexao, long notificacao, List<Long> alertas) throws SQLException {
        try (var comando = conexao.prepareStatement(
                "UPDATE alerta SET notificacao_id = ? WHERE id = ? AND notificacao_id IS NULL")) {
            for (var alerta : alertas) {
                comando.setLong(1, notificacao);
                comando.setLong(2, alerta);
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static Optional<Notificacao> buscar(Connection conexao, long id) throws SQLException {
        try (var consulta = conexao.prepareStatement("SELECT " + COLUNAS_NOTIFICACAO + " FROM notificacao WHERE id = ?")) {
            consulta.setLong(1, id);
            try (var linhas = consulta.executeQuery()) {
                return linhas.next() ? Optional.of(notificacao(linhas)) : Optional.empty();
            }
        }
    }

    private static Notificacao notificacao(ResultSet linha) throws SQLException {
        var reconhecidaEm = linha.getString("reconhecida_em");
        return new Notificacao(
                linha.getLong("id"),
                new BarragemId(linha.getString("barragem_id")),
                new CodigoInstrumento(linha.getString("instrumento")),
                TipoAlerta.valueOf(linha.getString("tipo_alerta")),
                CategoriaAlerta.valueOf(linha.getString("categoria")),
                Severidade.valueOf(linha.getString("severidade")),
                linha.getString("mensagem"),
                linha.getInt("leitura_suspeita") == 1,
                linha.getInt("nivel_resposta"),
                linha.getInt("ocorrencias"),
                Instant.parse(linha.getString("criada_em")),
                Instant.parse(linha.getString("atualizada_em")),
                reconhecidaEm == null
                        ? null
                        : new Notificacao.Reconhecimento(
                                Instant.parse(reconhecidaEm), linha.getString("reconhecida_por"), linha.getString("observacao")));
    }

    private static Alerta alerta(ResultSet linha) throws SQLException {
        var fuso = ZoneOffset.of(linha.getString("fuso"));
        Double limite = linha.getObject("limite") == null ? null : linha.getDouble("limite");
        return new Alerta(
                TipoAlerta.valueOf(linha.getString("tipo")),
                CategoriaAlerta.valueOf(linha.getString("categoria")),
                new CodigoInstrumento(linha.getString("instrumento")),
                Severidade.valueOf(linha.getString("severidade")),
                momento(linha.getString("inicio"), fuso),
                momento(linha.getString("fim"), fuso),
                linha.getInt("leituras"),
                linha.getDouble("valor_extremo"),
                linha.getString("unidade"),
                limite,
                linha.getString("direcao"),
                linha.getInt("leitura_suspeita") == 1,
                linha.getString("mensagem"),
                new VersaoConfiguracao(linha.getString("versao")));
    }

    private static OffsetDateTime momento(String utc, ZoneOffset fuso) {
        return Instant.parse(utc).atOffset(fuso);
    }
}
