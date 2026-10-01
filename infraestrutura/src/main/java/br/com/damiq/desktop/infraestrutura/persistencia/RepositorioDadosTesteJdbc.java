package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.manutencao.RepositorioDadosTeste;
import br.com.damiq.desktop.dominio.Validacao;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * {@link RepositorioDadosTeste}: apaga os registros com {@code teste = 1}, das tabelas filhas para as mães, e os
 * usuários de teste.
 *
 * <p>Um usuário de teste que é autor de algum registro real (ex.: estava conectado quando o cadastro de uma
 * barragem real foi sincronizado) continua, para não perder a autoria; com ele continuam a barragem dele e os
 * usuários de teste que gravaram essa barragem ou ele próprio. Usuários e barragens se referenciam nos dois
 * sentidos, por isso as chaves estrangeiras só são conferidas no fim da transação.
 */
public final class RepositorioDadosTesteJdbc implements RepositorioDadosTeste {

    /** Ordem que respeita as chaves estrangeiras; a barragem por último. */
    private static final List<String> TABELAS = List.of(
            "alerta", "notificacao", "rejeicao", "lacuna", "medicao", "processamento", "configuracao",
            "barragem_contato", "barragem_campo", "barragem_grupo", "barragem");

    private static final String AUTOR = "(r.criado_por = u.id OR r.atualizado_por = u.id)";
    private static final String USUARIO_DE_TESTE = "SELECT u.id FROM usuario u WHERE u.teste = 1 AND u.id <> 1";

    /** Usuários de teste autores de algum registro real. */
    private static final String AUTORES_DE_REGISTRO_REAL = autoresDeRegistroReal();

    /** Usuários de teste que gravaram a barragem ou outro usuário que já ficam. */
    private static final String AUTORES_DO_QUE_FICA = USUARIO_DE_TESTE + " AND u.id NOT IN (SELECT id FROM mantido)"
            + " AND (EXISTS (SELECT 1 FROM barragem r WHERE r.id IN"
            + " (SELECT m.barragem_id FROM usuario m WHERE m.id IN (SELECT id FROM mantido)) AND " + AUTOR + ")"
            + " OR EXISTS (SELECT 1 FROM usuario r WHERE r.id IN (SELECT id FROM mantido) AND r.id <> u.id AND "
            + AUTOR + "))";

    private static String autoresDeRegistroReal() {
        var sql = new StringBuilder(USUARIO_DE_TESTE).append(" AND (");
        for (var tabela : TABELAS) {
            sql.append("EXISTS (SELECT 1 FROM ").append(tabela).append(" r WHERE r.teste = 0 AND ").append(AUTOR)
                    .append(") OR ");
        }
        return sql.append("EXISTS (SELECT 1 FROM usuario r WHERE r.teste = 0 AND r.id <> u.id AND ").append(AUTOR)
                .append("))").toString();
    }

    private final Jdbc jdbc;

    public RepositorioDadosTesteJdbc(DataSource fonte) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
    }

    @Override
    public Map<String, Integer> apagarDadosDeTeste() {
        return jdbc.emTransacao("apagar os dados de teste", conexao -> {
            var apagados = new LinkedHashMap<String, Integer>();
            try (var comando = conexao.createStatement()) {
                comando.execute("PRAGMA defer_foreign_keys = ON"); // vale até o fim desta transação
                comando.execute("CREATE TEMP TABLE mantido (id INTEGER PRIMARY KEY)");
                try {
                    comando.executeUpdate("INSERT INTO mantido " + AUTORES_DE_REGISTRO_REAL);
                    while (comando.executeUpdate("INSERT INTO mantido " + AUTORES_DO_QUE_FICA) > 0) {
                        // até não sobrar autor de algo que fica
                    }
                    for (var tabela : TABELAS) {
                        // nome da tabela vem da lista fixa acima, nunca de entrada externa
                        var condicao = tabela.equals("barragem")
                                ? " AND id NOT IN (SELECT barragem_id FROM usuario WHERE id IN (SELECT id FROM mantido))"
                                : "";
                        apagados.put(tabela,
                                comando.executeUpdate("DELETE FROM " + tabela + " WHERE teste = 1" + condicao));
                    }
                    apagados.put("usuario", comando.executeUpdate(
                            "DELETE FROM usuario WHERE teste = 1 AND id <> 1 AND id NOT IN (SELECT id FROM mantido)"));
                } finally {
                    comando.execute("DROP TABLE IF EXISTS temp.mantido");
                }
            }
            return apagados;
        });
    }
}
