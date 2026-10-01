package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.configuracao.RepositorioConfiguracoes;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;

/** {@link RepositorioConfiguracoes} na tabela {@code configuracao}. */
public final class RepositorioConfiguracoesJdbc implements RepositorioConfiguracoes {

    private final Jdbc jdbc;

    public RepositorioConfiguracoesJdbc(DataSource fonte) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
    }

    @Override
    public Optional<Configuracao> vigente(BarragemId barragem) {
        return jdbc.executar("buscar a configuração vigente da barragem " + barragem, conexao -> {
            try (var consulta = conexao.prepareStatement(
                    "SELECT versao, conteudo_json FROM configuracao WHERE barragem_id = ? AND vigente = 1")) {
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
                    "SELECT 1 FROM configuracao WHERE barragem_id = ? AND versao = ?")) {
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
            try (var desativar = conexao.prepareStatement(
                    "UPDATE configuracao SET vigente = 0 WHERE barragem_id = ? AND vigente = 1")) {
                desativar.setString(1, barragem.valor());
                desativar.executeUpdate();
            }
            try (var inserir = conexao.prepareStatement("""
                    INSERT INTO configuracao (barragem_id, versao, conteudo_json, origem, recebida_em, vigente)
                    VALUES (?, ?, ?, ?, ?, 1)
                    """)) {
                inserir.setString(1, barragem.valor());
                inserir.setString(2, configuracao.versao().valor());
                inserir.setString(3, configuracao.conteudoJson());
                inserir.setString(4, origem.name());
                inserir.setString(5, BancoDados.data(recebidaEm));
                return inserir.executeUpdate();
            }
        });
    }
}
