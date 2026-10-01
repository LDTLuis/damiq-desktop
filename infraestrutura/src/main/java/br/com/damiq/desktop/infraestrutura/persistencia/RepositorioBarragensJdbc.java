package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.util.Optional;
import javax.sql.DataSource;

/** {@link RepositorioBarragens} na tabela {@code barragem}. */
public final class RepositorioBarragensJdbc implements RepositorioBarragens {

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
}
