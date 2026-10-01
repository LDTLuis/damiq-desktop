package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.configuracao.RepositorioConfiguracoes;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;

/** {@link RepositorioConfiguracoes} na tabela {@code configuracao}. */
public final class RepositorioConfiguracoesJdbc implements RepositorioConfiguracoes {

    private final Jdbc jdbc;
    private final Autoria autoria;

    public RepositorioConfiguracoesJdbc(DataSource fonte, Autoria autoria) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
        this.autoria = Validacao.obrigatorio(autoria, "autoria");
    }

    @Override
    public Optional<Configuracao> vigente(BarragemId barragem) {
        return jdbc.executar("buscar a configuração vigente da barragem " + barragem, conexao -> {
            try (var consulta = conexao.prepareStatement("""
                    SELECT versao, conteudo_json FROM configuracao
                    WHERE barragem_id = ? AND vigente = 1 AND excluido_em IS NULL
                    """)) {
                consulta.setString(1, barragem.valor());
                try (var linhas = consulta.executeQuery()) {
                    if (!linhas.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new Configuracao(
                            new VersaoConfiguracao(linhas.getString("versao")), linhas.getString("conteudo_json")));
                }
            }
        });
    }

    @Override
    public boolean versaoRegistrada(BarragemId barragem, VersaoConfiguracao versao) {
        return jdbc.executar("consultar a versão " + versao + " da barragem " + barragem, conexao -> {
            try (var consulta = conexao.prepareStatement(
                    "SELECT 1 FROM configuracao WHERE barragem_id = ? AND versao = ? AND excluido_em IS NULL")) {
                consulta.setString(1, barragem.valor());
                consulta.setString(2, versao.valor());
                try (var linhas = consulta.executeQuery()) {
                    return linhas.next();
                }
            }
        });
    }

    @Override
    public void ativar(
            BarragemId barragem, Configuracao configuracao, OrigemConfiguracao origem, Instant recebidaEm) {
        jdbc.emTransacao("ativar a configuração " + configuracao.versao() + " da barragem " + barragem, conexao -> {
            var agora = autoria.agora();
            try (var desativar = conexao.prepareStatement("""
                    UPDATE configuracao SET vigente = 0, atualizado_em = ?, atualizado_por = ?
                    WHERE barragem_id = ? AND vigente = 1 AND excluido_em IS NULL
                    """)) {
                desativar.setString(1, agora);
                desativar.setLong(2, autoria.usuario());
                desativar.setString(3, barragem.valor());
                desativar.executeUpdate();
            }
            // a marcação de teste vem da barragem
            try (var inserir = conexao.prepareStatement("""
                    INSERT INTO configuracao (barragem_id, versao, conteudo_json, origem, vigente,
                        criado_em, criado_por, atualizado_em, atualizado_por, teste)
                    SELECT ?, ?, ?, ?, 1, ?, ?, ?, ?, teste FROM barragem WHERE id = ? AND excluido_em IS NULL
                    """)) {
                inserir.setString(1, barragem.valor());
                inserir.setString(2, configuracao.versao().valor());
                inserir.setString(3, configuracao.conteudoJson());
                inserir.setString(4, origem.name());
                inserir.setString(5, BancoDados.data(recebidaEm));
                inserir.setLong(6, autoria.usuario());
                inserir.setString(7, agora);
                inserir.setLong(8, autoria.usuario());
                inserir.setString(9, barragem.valor());
                if (inserir.executeUpdate() != 1) {
                    throw new SQLException("barragem não cadastrada: " + barragem);
                }
                return 1;
            }
        });
    }
}
