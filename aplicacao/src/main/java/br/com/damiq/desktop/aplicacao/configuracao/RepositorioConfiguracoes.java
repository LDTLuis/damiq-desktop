package br.com.damiq.desktop.aplicacao.configuracao;

import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.time.Instant;
import java.util.Optional;

/** Configurações validadas de cada barragem, com o histórico das versões anteriores. */
public interface RepositorioConfiguracoes {

    /** A configuração em uso pela barragem, se houver. */
    Optional<Configuracao> vigente(BarragemId barragem);

    /** Se essa versão já foi registrada para a barragem (vigente ou no histórico). */
    boolean versaoRegistrada(BarragemId barragem, VersaoConfiguracao versao);

    /** Grava a configuração e a torna vigente, passando a anterior para o histórico, numa única transação. */
    void ativar(BarragemId barragem, Configuracao configuracao, OrigemConfiguracao origem, Instant recebidaEm);
}
