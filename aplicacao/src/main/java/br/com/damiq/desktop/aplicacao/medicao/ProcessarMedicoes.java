package br.com.damiq.desktop.aplicacao.medicao;

import br.com.damiq.desktop.aplicacao.barragem.BarragemNaoCadastradaException;
import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.configuracao.ConfiguracaoAusenteException;
import br.com.damiq.desktop.aplicacao.configuracao.RepositorioConfiguracoes;
import br.com.damiq.desktop.aplicacao.medicao.RegistroProcessamento.RejeicaoLeitura;
import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.aplicacao.motor.LoteMedicoes;
import br.com.damiq.desktop.aplicacao.motor.MedicaoProcessada;
import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.aplicacao.motor.Rejeicao;
import br.com.damiq.desktop.aplicacao.motor.ResultadoLote;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processa leituras digitadas ou importadas (RF-03/RF-04): envia ao motor com a configuração vigente e o
 * histórico da barragem e grava medições, rejeições, lacunas e alertas.
 *
 * <p>Reimportar leituras já gravadas não as duplica: elas são contadas em
 * {@link ResultadoProcessamento#jaRegistradas()}, e os episódios de alerta formados só por elas, assim como as
 * lacunas encerradas por elas, não são gravados de novo.
 */
public final class ProcessarMedicoes {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessarMedicoes.class);

    private final RepositorioBarragens barragens;
    private final RepositorioConfiguracoes configuracoes;
    private final RepositorioMedicoes medicoes;
    private final RepositorioProcessamentos processamentos;
    private final MotorCalculo motor;
    private final Clock relogio;
    private final int historicoPorInstrumento;

    /**
     * @param historicoPorInstrumento leituras anteriores enviadas ao motor por instrumento. O contrato pede no
     *     mínimo o maior entre {@code anomalia.janela_leituras} (padrão 24) e
     *     {@code sensor_travado.leituras_consecutivas} (padrão 12) da configuração
     */
    public ProcessarMedicoes(
            RepositorioBarragens barragens,
            RepositorioConfiguracoes configuracoes,
            RepositorioMedicoes medicoes,
            RepositorioProcessamentos processamentos,
            MotorCalculo motor,
            Clock relogio,
            int historicoPorInstrumento) {
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
        this.configuracoes = Validacao.obrigatorio(configuracoes, "repositório de configurações");
        this.medicoes = Validacao.obrigatorio(medicoes, "repositório de medições");
        this.processamentos = Validacao.obrigatorio(processamentos, "repositório de processamentos");
        this.motor = Validacao.obrigatorio(motor, "motor de cálculo");
        this.relogio = Validacao.obrigatorio(relogio, "relógio");
        if (historicoPorInstrumento < 1) {
            throw new IllegalArgumentException("histórico por instrumento deve ser positivo: " + historicoPorInstrumento);
        }
        this.historicoPorInstrumento = historicoPorInstrumento;
    }

    /**
     * @param arquivo nome do arquivo importado; {@code null} na digitação
     * @throws BarragemNaoCadastradaException se a barragem não estiver cadastrada
     * @throws ConfiguracaoAusenteException se a barragem não tiver configuração vigente
     * @throws FalhaMotorException se o motor falhar ou recusar o lote inteiro; nada é gravado
     */
    public ResultadoProcessamento executar(
            BarragemId barragem, List<LeituraInformada> leituras, OrigemLeituras origem, String arquivo) {
        Validacao.obrigatorio(leituras, "leituras");
        Validacao.obrigatorio(origem, "origem das leituras");
        if (leituras.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma leitura para processar");
        }
        barragens.buscar(barragem).orElseThrow(() -> new BarragemNaoCadastradaException(barragem));
        var configuracao = configuracoes.vigente(barragem).orElseThrow(() -> new ConfiguracaoAusenteException(barragem));

        var agora = relogio.instant();
        var historico = medicoes.ultimas(barragem, historicoPorInstrumento);
        var lote = motor.processarLote(
                new LoteMedicoes(barragem, configuracao, leituras, historico, agora.atOffset(ZoneOffset.UTC)));
        if (!lote.versaoConfiguracao().equals(configuracao.versao())) {
            throw new FalhaMotorException("O motor aplicou a configuração " + lote.versaoConfiguracao()
                    + ", mas a vigente é a " + configuracao.versao());
        }

        var chaves = lote.medicoes().stream().map(m -> ChaveMedicao.de(m.medicao())).toList();
        var jaGravadas = medicoes.registradas(barragem, chaves);
        var novas = lote.medicoes().stream().filter(m -> !jaGravadas.contains(ChaveMedicao.de(m.medicao()))).toList();
        var repetidas = lote.medicoes().stream().filter(m -> jaGravadas.contains(ChaveMedicao.de(m.medicao()))).toList();
        var alertas = lote.alertas().stream().filter(a -> !soDeLeiturasRepetidas(a, lote.medicoes(), jaGravadas)).toList();
        // a lacuna é detectada pela leitura que a encerra: se ela já estava gravada, a lacuna também está
        var lacunas = lote.lacunas().stream()
                .filter(l -> !jaGravadas.contains(new ChaveMedicao(l.instrumento(), l.fim().toInstant())))
                .toList();
        var rejeicoes = lote.rejeicoes().stream().map(r -> comLeitura(r, leituras)).toList();

        var id = processamentos.registrar(new RegistroProcessamento(
                barragem,
                lote.versaoConfiguracao(),
                agora,
                origem,
                arquivo,
                lote.statusBarragem(),
                lote.statusDados(),
                lote.nivelResposta().nivel(),
                leituras.size(),
                repetidas.size(),
                novas,
                rejeicoes,
                lacunas,
                alertas));

        registrarNoLog(barragem, id, lote, novas.size(), repetidas.size(), alertas);
        return new ResultadoProcessamento(id, lote, novas, repetidas, alertas);
    }

    private static RejeicaoLeitura comLeitura(Rejeicao rejeicao, List<LeituraInformada> leituras) {
        if (rejeicao.indice() >= leituras.size()) {
            throw new FalhaMotorException("O motor rejeitou a leitura " + rejeicao.indice()
                    + ", mas o lote tem " + leituras.size() + " leituras");
        }
        return new RejeicaoLeitura(rejeicao, leituras.get(rejeicao.indice()));
    }

    /** O episódio só tem leituras que já estavam gravadas: ele já foi avaliado num processamento anterior. */
    private static boolean soDeLeiturasRepetidas(
            Alerta alerta, List<MedicaoProcessada> medicoesDoLote, Set<ChaveMedicao> jaGravadas) {
        var doEpisodio = medicoesDoLote.stream()
                .map(MedicaoProcessada::medicao)
                .filter(m -> m.instrumento().equals(alerta.instrumento()))
                .filter(m -> !m.momento().isBefore(alerta.inicio()) && !m.momento().isAfter(alerta.fim()))
                .map(ChaveMedicao::de)
                .toList();
        return !doEpisodio.isEmpty() && jaGravadas.containsAll(doEpisodio);
    }

    private static void registrarNoLog(
            BarragemId barragem,
            long id,
            ResultadoLote lote,
            int gravadas,
            int repetidas,
            List<Alerta> alertas) {
        LOG.info(
                "Processamento {} da barragem {}: {} gravadas, {} já registradas, {} rejeitadas, {} alertas; "
                        + "barragem {}, dados {} (configuração {})",
                id,
                barragem,
                gravadas,
                repetidas,
                lote.rejeicoes().size(),
                alertas.size(),
                lote.statusBarragem(),
                lote.statusDados(),
                lote.versaoConfiguracao());
        lote.avisos().forEach(aviso -> LOG.warn("Configuração {}: {}", lote.versaoConfiguracao(), aviso));
        lote.rejeicoesHistorico().forEach(r -> LOG.warn(
                "Leitura gravada da barragem {} recusada pelo motor no histórico ({}): {}",
                barragem,
                r.codigo(),
                r.mensagem()));
        alertas.forEach(alerta -> LOG.warn("Alerta {} {}: {}", alerta.severidade(), alerta.tipo(), alerta.mensagem()));
        if (lote.nivelResposta().exigeAcao()) {
            LOG.warn("Barragem {} em {}: {}", barragem, lote.nivelResposta().rotulo(), lote.nivelResposta().situacao());
        }
    }
}
