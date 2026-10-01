package br.com.damiq.desktop.aplicacao.configuracao;

import br.com.damiq.desktop.aplicacao.barragem.BarragemNaoCadastradaException;
import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.configuracao.ResultadoAtualizacaoConfiguracao.Situacao;
import br.com.damiq.desktop.aplicacao.motor.MensagemMotor;
import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Busca a configuração publicada para uma barragem, valida no motor e, se válida, passa a usá-la. Se inválida,
 * a configuração vigente é mantida e o erro vai para o log (contrato do motor, seção 7).
 */
public final class AtualizarConfiguracao {

    private static final Logger LOG = LoggerFactory.getLogger(AtualizarConfiguracao.class);

    private final RepositorioBarragens barragens;
    private final RepositorioConfiguracoes configuracoes;
    private final FonteConfiguracao fonte;
    private final MotorCalculo motor;
    private final Clock relogio;

    public AtualizarConfiguracao(
            RepositorioBarragens barragens,
            RepositorioConfiguracoes configuracoes,
            FonteConfiguracao fonte,
            MotorCalculo motor,
            Clock relogio) {
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
        this.configuracoes = Validacao.obrigatorio(configuracoes, "repositório de configurações");
        this.fonte = Validacao.obrigatorio(fonte, "fonte de configuração");
        this.motor = Validacao.obrigatorio(motor, "motor de cálculo");
        this.relogio = Validacao.obrigatorio(relogio, "relógio");
    }

    /**
     * @throws BarragemNaoCadastradaException se a barragem não estiver cadastrada
     * @throws FalhaFonteConfiguracaoException se a configuração não puder ser obtida
     * @throws br.com.damiq.desktop.aplicacao.motor.FalhaMotorException se o motor não puder validar
     */
    public ResultadoAtualizacaoConfiguracao executar(BarragemId barragem) {
        barragens.buscar(barragem).orElseThrow(() -> new BarragemNaoCadastradaException(barragem));
        var nova = fonte.buscar(barragem);
        var versao = nova.versao();

        var vigente = configuracoes.vigente(barragem);
        if (vigente.isPresent() && vigente.get().versao().equals(versao)) {
            LOG.debug("Configuração {} da barragem {} já está em uso", versao, barragem);
            return new ResultadoAtualizacaoConfiguracao(Situacao.JA_VIGENTE, versao, List.of(), List.of());
        }
        if (configuracoes.versaoRegistrada(barragem, versao)) {
            // a Central gera uma versão nova a cada edição: reaparecer uma antiga indica erro na origem
            var erro = new MensagemMotor(
                    "VERSAO_JA_USADA", "A versão " + versao + " já foi usada antes por esta barragem", "configuracao.versao");
            LOG.error("Configuração {} da barragem {} recusada: {}", versao, barragem, erro.mensagem());
            return new ResultadoAtualizacaoConfiguracao(Situacao.RECUSADA, versao, List.of(erro), List.of());
        }

        var validacao = motor.validarConfiguracao(nova);
        validacao.avisos().forEach(aviso -> LOG.warn("Configuração {} da barragem {}: {}", versao, barragem, aviso));
        if (!validacao.valida()) {
            validacao.erros().forEach(erro -> LOG.error(
                    "Configuração {} da barragem {} recusada pelo motor: {}", versao, barragem, erro));
            LOG.error(
                    "Barragem {} continua com a configuração {}",
                    barragem,
                    vigente.map(c -> c.versao().valor()).orElse("(nenhuma)"));
            return new ResultadoAtualizacaoConfiguracao(
                    Situacao.RECUSADA, versao, validacao.erros(), validacao.avisos());
        }

        configuracoes.ativar(barragem, nova, fonte.origem(), relogio.instant());
        LOG.info("Barragem {} passou a usar a configuração {} ({})", barragem, versao, fonte.origem());
        return new ResultadoAtualizacaoConfiguracao(Situacao.ATIVADA, versao, List.of(), validacao.avisos());
    }
}
