package br.com.damiq.desktop.infraestrutura.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.motor.LoteMedicoes;
import br.com.damiq.desktop.aplicacao.motor.RequisicaoRecusadaException;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Executa o motor real, instalado a partir da release (ver {@link MotorInstalado} e o README). */
class MotorCalculoIntegracaoTest {

    private static MotorCalculoProcessBuilder motor;

    @BeforeAll
    static void localizarMotor() {
        motor = MotorInstalado.exigir();
    }

    private static Configuracao configuracao(String json) {
        return new Configuracao(new VersaoConfiguracao("21"), json);
    }

    @Test
    void info() {
        var info = motor.info();

        assertTrue(info.versaoMotor().startsWith("1."), info.versaoMotor());
        assertEquals(MotorCalculoProcessBuilder.VERSAO_CONTRATO, info.versaoContrato());
        assertTrue(info.operacoes().contains("validar_configuracao"), info.operacoes().toString());
    }

    @Test
    void configuracaoValida() {
        var validacao = motor.validarConfiguracao(configuracao("""
                {"versao": 21, "fuso_padrao": "-03:00",
                 "sensores": {"PZ-01": {"tipo": "pressao", "frequencia_esperada_s": 3600}}}
                """));

        assertTrue(validacao.valida(), validacao.erros().toString());
    }

    @Test
    void configuracaoInvalidaApontaOCampo() {
        var validacao = motor.validarConfiguracao(configuracao("""
                {"versao": 21, "fuso_padrao": "America/Sao_Paulo"}
                """));

        assertFalse(validacao.valida());
        assertEquals("configuracao.fuso_padrao", validacao.erros().getFirst().campo());
    }

    @Test
    void campoDesconhecidoViraAviso() {
        var validacao = motor.validarConfiguracao(configuracao("""
                {"versao": 21, "campo_inventado": true}
                """));

        assertTrue(validacao.valida(), validacao.erros().toString());
        assertFalse(validacao.avisos().isEmpty());
    }

    @Test
    void processarLote() {
        var resultado = motor.processarLote(LoteExemplo.lote());

        assertEquals("21", resultado.versaoConfiguracao().valor());
        assertEquals(Severidade.CRITICO, resultado.statusBarragem());
        assertEquals(4, resultado.medicoes().size());
        assertEquals(3, resultado.rejeicoes().getFirst().indice());
        assertEquals(1, resultado.lacunas().size());
        assertEquals(2, resultado.alertas().size());
        assertTrue(resultado.alertas().stream().anyMatch(Alerta::leituraSuspeita));
    }

    @Test
    void configuracaoInvalidaRecusaOLoteInteiro() {
        var lote = LoteExemplo.lote();
        var invalida = new LoteMedicoes(
                lote.barragem(),
                configuracao("{\"versao\": 21, \"fuso_padrao\": \"America/Sao_Paulo\"}"),
                lote.leituras(),
                lote.historico(),
                lote.agora());

        var falha = assertThrows(RequisicaoRecusadaException.class, () -> motor.processarLote(invalida));

        assertEquals("configuracao.fuso_padrao", falha.erros().getFirst().campo());
    }
}
