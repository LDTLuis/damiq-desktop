package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caso de uso da inicialização: confere se o motor instalado fala a versão maior do contrato que o Desktop
 * usa. Versões 1.x do contrato só acrescentam campos, então qualquer 1.x é compatível.
 */
public final class VerificarCompatibilidadeMotor {

    static final String VERSAO_MAIOR_CONTRATO = "1";

    private static final Logger LOG = LoggerFactory.getLogger(VerificarCompatibilidadeMotor.class);

    private final MotorCalculo motor;

    public VerificarCompatibilidadeMotor(MotorCalculo motor) {
        this.motor = Validacao.obrigatorio(motor, "motor de cálculo");
    }

    /**
     * @throws FalhaMotorException se o motor não responder ou usar outra versão maior do contrato
     */
    public InfoMotor executar() {
        var info = motor.info();
        var versaoMaior = info.versaoContrato().split("\\.", 2)[0];
        if (!VERSAO_MAIOR_CONTRATO.equals(versaoMaior)) {
            throw new FalhaMotorException(
                    "Motor de cálculo " + info.versaoMotor() + " usa o contrato " + info.versaoContrato()
                            + "; o Desktop requer o contrato " + VERSAO_MAIOR_CONTRATO + ".x");
        }
        LOG.info("Motor de cálculo {} (contrato {})", info.versaoMotor(), info.versaoContrato());
        return info;
    }
}
