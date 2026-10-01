package br.com.damiq.desktop.aplicacao.manutencao;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove do banco os dados de teste (UAT, RF-13): as barragens de teste e tudo o que pertence a elas. Dados
 * reais nunca são apagados fisicamente; o banco recusa ({@code tg_*_exclusao_fisica}).
 */
public final class LimparDadosTeste {

    private static final Logger LOG = LoggerFactory.getLogger(LimparDadosTeste.class);

    private final RepositorioDadosTeste dados;

    public LimparDadosTeste(RepositorioDadosTeste dados) {
        this.dados = Validacao.obrigatorio(dados, "repositório de dados de teste");
    }

    /** @return quantos registros foram apagados em cada tabela */
    public Map<String, Integer> executar() {
        var apagados = dados.apagarDadosDeTeste();
        var total = apagados.values().stream().mapToInt(Integer::intValue).sum();
        LOG.warn("Dados de teste apagados: {} registros {}", total, apagados);
        return apagados;
    }
}
