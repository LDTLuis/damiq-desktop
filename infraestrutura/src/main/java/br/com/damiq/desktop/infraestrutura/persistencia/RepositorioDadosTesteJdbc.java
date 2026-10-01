package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.manutencao.RepositorioDadosTeste;
import br.com.damiq.desktop.dominio.Validacao;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/** {@link RepositorioDadosTeste}: apaga os registros com {@code teste = 1}, das tabelas filhas para as mães. */
public final class RepositorioDadosTesteJdbc implements RepositorioDadosTeste {

    /**
     * Ordem que respeita as chaves estrangeiras. Usuários de teste ficam de fora: eles podem ter alterado
     * registros reais ({@code criado_por}/{@code atualizado_por}) e entram quando houver autenticação (RF-01).
     */
    private static final List<String> TABELAS = List.of(
            "alerta", "notificacao", "rejeicao", "lacuna", "medicao", "processamento", "configuracao",
            "barragem_campo", "barragem_grupo", "barragem");

    private final Jdbc jdbc;

    public RepositorioDadosTesteJdbc(DataSource fonte) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
    }

    @Override
    public Map<String, Integer> apagarDadosDeTeste() {
        return jdbc.emTransacao("apagar os dados de teste", conexao -> {
            var apagados = new LinkedHashMap<String, Integer>();
            try (var comando = conexao.createStatement()) {
                for (var tabela : TABELAS) {
                    // nome da tabela vem da lista fixa acima, nunca de entrada externa
                    apagados.put(tabela, comando.executeUpdate("DELETE FROM " + tabela + " WHERE teste = 1"));
                }
            }
            return apagados;
        });
    }
}
