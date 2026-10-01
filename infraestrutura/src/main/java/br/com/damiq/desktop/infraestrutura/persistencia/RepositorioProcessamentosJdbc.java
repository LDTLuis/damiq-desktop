package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.medicao.RegistroProcessamento;
import br.com.damiq.desktop.aplicacao.medicao.RepositorioProcessamentos;
import br.com.damiq.desktop.dominio.Validacao;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import javax.sql.DataSource;

/** {@link RepositorioProcessamentos} nas tabelas {@code processamento}, {@code medicao}, {@code rejeicao}, {@code lacuna} e {@code alerta}. */
public final class RepositorioProcessamentosJdbc implements RepositorioProcessamentos {

    private final Jdbc jdbc;

    public RepositorioProcessamentosJdbc(DataSource fonte) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
    }

    @Override
    public long registrar(RegistroProcessamento registro) {
        return jdbc.emTransacao("gravar o processamento da barragem " + registro.barragem(), conexao -> {
            var id = inserirProcessamento(conexao, registro);
            inserirMedicoes(conexao, id, registro);
            inserirRejeicoes(conexao, id, registro);
            inserirLacunas(conexao, id, registro);
            inserirAlertas(conexao, id, registro);
            return id;
        });
    }

    private static long inserirProcessamento(Connection conexao, RegistroProcessamento r) throws SQLException {
        try (var comando = conexao.prepareStatement("""
                INSERT INTO processamento (barragem_id, configuracao_id, executado_em, origem, arquivo,
                    status_barragem, status_dados, nivel_resposta, recebidas, gravadas, ja_registradas, rejeitadas)
                VALUES (?, (SELECT id FROM configuracao WHERE barragem_id = ? AND versao = ?),
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """)) {
            comando.setString(1, r.barragem().valor());
            comando.setString(2, r.barragem().valor());
            comando.setString(3, r.versaoConfiguracao().valor());
            comando.setString(4, BancoDados.data(r.executadoEm()));
            comando.setString(5, r.origem().name());
            comando.setString(6, r.arquivo());
            comando.setString(7, r.statusBarragem().name());
            comando.setString(8, r.statusDados().name());
            comando.setInt(9, r.nivelResposta());
            comando.setInt(10, r.recebidas());
            comando.setInt(11, r.medicoes().size());
            comando.setInt(12, r.jaRegistradas());
            comando.setInt(13, r.rejeicoes().size());
            try (var gerado = comando.executeQuery()) {
                gerado.next();
                return gerado.getLong(1);
            }
        }
    }

    private static void inserirMedicoes(Connection conexao, long id, RegistroProcessamento r) throws SQLException {
        try (var comando = conexao.prepareStatement("""
                INSERT INTO medicao (processamento_id, barragem_id, instrumento, tipo, momento, fuso, valor,
                    valor_original, unidade_original, flags)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (var processada : r.medicoes()) {
                var m = processada.medicao();
                comando.setLong(1, id);
                comando.setString(2, r.barragem().valor());
                comando.setString(3, m.instrumento().valor());
                comando.setString(4, m.tipo().name());
                comando.setString(5, data(m.momento()));
                comando.setString(6, m.momento().getOffset().getId());
                comando.setDouble(7, m.valor());
                comando.setString(8, processada.valorOriginal());
                comando.setString(9, processada.unidadeOriginal());
                comando.setString(10, String.join(",", processada.flags()));
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static void inserirRejeicoes(Connection conexao, long id, RegistroProcessamento r) throws SQLException {
        try (var comando = conexao.prepareStatement("""
                INSERT INTO rejeicao (processamento_id, indice, codigo, mensagem, instrumento, tipo, momento, valor, unidade)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (var rejeitada : r.rejeicoes()) {
                var leitura = rejeitada.leitura();
                comando.setLong(1, id);
                comando.setInt(2, rejeitada.rejeicao().indice());
                comando.setString(3, rejeitada.rejeicao().codigo());
                comando.setString(4, rejeitada.rejeicao().mensagem());
                comando.setString(5, leitura.instrumento());
                comando.setString(6, leitura.tipo());
                comando.setString(7, leitura.momento());
                comando.setString(8, leitura.valor());
                comando.setString(9, leitura.unidade());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static void inserirLacunas(Connection conexao, long id, RegistroProcessamento r) throws SQLException {
        try (var comando = conexao.prepareStatement("""
                INSERT INTO lacuna (processamento_id, barragem_id, instrumento, inicio, fim, fuso, duracao_s)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (var lacuna : r.lacunas()) {
                comando.setLong(1, id);
                comando.setString(2, r.barragem().valor());
                comando.setString(3, lacuna.instrumento().valor());
                comando.setString(4, data(lacuna.inicio()));
                comando.setString(5, data(lacuna.fim()));
                comando.setString(6, lacuna.inicio().getOffset().getId());
                comando.setLong(7, lacuna.duracao().toSeconds());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static void inserirAlertas(Connection conexao, long id, RegistroProcessamento r) throws SQLException {
        try (var comando = conexao.prepareStatement("""
                INSERT INTO alerta (processamento_id, barragem_id, instrumento, tipo, categoria, severidade, inicio, fim,
                    fuso, leituras, valor_extremo, unidade, limite, direcao, leitura_suspeita, mensagem)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (var a : r.alertas()) {
                comando.setLong(1, id);
                comando.setString(2, r.barragem().valor());
                comando.setString(3, a.instrumento().valor());
                comando.setString(4, a.tipo().name());
                comando.setString(5, a.categoria().name());
                comando.setString(6, a.severidade().name());
                comando.setString(7, data(a.inicio()));
                comando.setString(8, data(a.fim()));
                comando.setString(9, a.inicio().getOffset().getId());
                comando.setInt(10, a.leituras());
                comando.setDouble(11, a.valorExtremo());
                comando.setString(12, a.unidade());
                if (a.limite() == null) {
                    comando.setNull(13, Types.REAL);
                } else {
                    comando.setDouble(13, a.limite());
                }
                comando.setString(14, a.direcao());
                comando.setInt(15, a.leituraSuspeita() ? 1 : 0);
                comando.setString(16, a.mensagem());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static String data(OffsetDateTime momento) {
        return BancoDados.data(momento.toInstant());
    }
}
