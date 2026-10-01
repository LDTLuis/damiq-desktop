package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.time.Clock;
import java.util.Optional;
import javax.sql.DataSource;

/** {@link RepositorioBarragens} na tabela {@code barragem}. */
public final class RepositorioBarragensJdbc implements RepositorioBarragens {

    private final Jdbc jdbc;
    private final Clock relogio;

    public RepositorioBarragensJdbc(DataSource fonte, Clock relogio) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
        this.relogio = Validacao.obrigatorio(relogio, "relógio");
    }

    @Override
    public void salvar(Barragem barragem) {
        jdbc.executar("salvar a barragem " + barragem.id(), conexao -> {
            try (var comando = conexao.prepareStatement("""
                    INSERT INTO barragem (id, nome, criada_em) VALUES (?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET nome = excluded.nome
                    """)) {
                comando.setString(1, barragem.id().valor());
                comando.setString(2, barragem.nome());
                comando.setString(3, BancoDados.data(relogio.instant()));
                return comando.executeUpdate();
            }
        });
    }

    @Override
    public Optional<Barragem> buscar(BarragemId id) {
        return jdbc.executar("buscar a barragem " + id, conexao -> {
            try (var consulta = conexao.prepareStatement("SELECT nome FROM barragem WHERE id = ?")) {
                consulta.setString(1, id.valor());
                try (var linhas = consulta.executeQuery()) {
                    return linhas.next() ? Optional.of(new Barragem(id, linhas.getString("nome"))) : Optional.empty();
                }
            }
        });
    }
}
