package br.com.damiq.desktop.infraestrutura.persistencia;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Execução de JDBC com conversão de {@link SQLException} e transação explícita. */
final class Jdbc {

    @FunctionalInterface
    interface Operacao<T> {
        T executar(Connection conexao) throws SQLException;
    }

    private final DataSource fonte;

    Jdbc(DataSource fonte) {
        this.fonte = fonte;
    }

    /** Executa com auto-commit (uma instrução por vez). */
    <T> T executar(String descricao, Operacao<T> operacao) {
        try (var conexao = fonte.getConnection()) {
            return operacao.executar(conexao);
        } catch (SQLException e) {
            throw new FalhaBancoDadosException("Falha ao " + descricao + ": " + e.getMessage(), e);
        }
    }

    /** Executa numa transação: confirma no fim ou desfaz tudo se algo falhar. */
    <T> T emTransacao(String descricao, Operacao<T> operacao) {
        return executar(descricao, conexao -> {
            conexao.setAutoCommit(false);
            try {
                var resultado = operacao.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                conexao.rollback();
                throw e;
            }
        });
    }
}
