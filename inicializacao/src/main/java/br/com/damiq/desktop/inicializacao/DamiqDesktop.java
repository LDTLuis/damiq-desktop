package br.com.damiq.desktop.inicializacao;

import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.aplicacao.motor.VerificarCompatibilidadeMotor;
import br.com.damiq.desktop.aplicacao.notificacao.NotificarAlertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Ponto de entrada do Desktop DAMIQ: sobe o Spring Context com a montagem dos módulos. */
public final class DamiqDesktop {

    private static final Logger LOG = LoggerFactory.getLogger(DamiqDesktop.class);

    private DamiqDesktop() {}

    public static void main(String[] args) {
        try (var contexto = new AnnotationConfigApplicationContext(ConfiguracaoAplicacao.class)) {
            LOG.info("DAMIQ Desktop iniciado ({} beans)", contexto.getBeanDefinitionCount());
            try {
                contexto.getBean(VerificarCompatibilidadeMotor.class).executar();
            } catch (FalhaMotorException e) {
                LOG.error("Motor de cálculo indisponível: {}", e.getMessage());
            }
            // alertas gravados sem notificação (ex.: o app fechou logo depois de um processamento)
            var recuperadas = contexto.getBean(NotificarAlertas.class).executarPendentes();
            if (!recuperadas.isEmpty()) {
                LOG.warn("{} notificações geradas para alertas pendentes", recuperadas.size());
            }
        }
    }
}
