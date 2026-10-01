package br.com.damiq.desktop.infraestrutura.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.aplicacao.motor.LoteMedicoes;
import br.com.damiq.desktop.aplicacao.motor.RequisicaoRecusadaException;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;
import br.com.damiq.desktop.infraestrutura.motor.ExecutorMotor.ExecucaoMotor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** {@code processar_lote} com a resposta real do motor 1.0.1 gravada (sem executar o motor). */
class ProcessarLoteTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final List<String> requisicoes = new ArrayList<>();

    private static String respostaGravada() {
        try (var entrada = Objects.requireNonNull(
                ProcessarLoteTest.class.getResourceAsStream("/motor/processar-lote-resposta-1.0.1.json"))) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private MotorCalculoProcessBuilder motor(int codigo, String resposta) {
        return new MotorCalculoProcessBuilder(requisicao -> {
            requisicoes.add(requisicao);
            return new ExecucaoMotor(codigo, resposta, "");
        });
    }

    @Test
    void requisicaoSegueOContrato() {
        motor(0, respostaGravada()).processarLote(LoteExemplo.lote());

        JsonNode requisicao = JSON.readTree(requisicoes.getFirst());
        assertEquals("processar_lote", requisicao.get("operacao").asString());
        assertEquals("joao-leite", requisicao.at("/barragem/id").asString());
        assertEquals(215.82, requisicao.at("/configuracao/sensores/PZ-01/limites_alerta/acima/alerta").asDouble());
        assertEquals("2026-09-29T23:00:00-03:00", requisicao.at("/opcoes/agora").asString());

        // leituras vão como informadas, em texto
        assertEquals("1,4", requisicao.at("/medicoes/0/valor").asString());
        assertEquals("Pressão", requisicao.at("/medicoes/1/tipo").asString());
        // histórico vai normalizado, com número e unidade canônica
        var historico = requisicao.at("/historico/1");
        assertEquals("pressao", historico.get("tipo").asString());
        assertEquals("2026-09-29T07:00:00-03:00", historico.get("timestamp").asString());
        assertTrue(historico.get("valor").isNumber());
        assertEquals("kPa", historico.get("unidade").asString());
    }

    @Test
    void campoNuloDaLeituraNaoEhEnviado() {
        var lote = LoteExemplo.lote();
        var comNulo = new LoteMedicoes(
                lote.barragem(),
                lote.configuracao(),
                List.of(new LeituraInformada(
                        "PZ-01", "pressao", "2026-09-29T08:00:00-03:00", null, "kPa")),
                List.of(),
                lote.agora());

        motor(0, respostaGravada()).processarLote(comNulo);

        assertFalse(JSON.readTree(requisicoes.getFirst()).at("/medicoes/0").has("valor"));
    }

    @Test
    void traduzARespostaDoMotor() {
        var resultado = motor(0, respostaGravada()).processarLote(LoteExemplo.lote());

        assertEquals("21", resultado.versaoConfiguracao().valor());
        assertEquals(Severidade.CRITICO, resultado.statusBarragem());
        assertEquals(Severidade.AVISO, resultado.statusDados());
        assertEquals(3, resultado.nivelResposta().nivel());
        assertEquals("vermelho", resultado.nivelResposta().cor());
        assertFalse(resultado.nivelResposta().acoes().isEmpty());

        // medições normalizadas: 1,4 bar → 140 kPa, valor original preservado
        assertEquals(4, resultado.medicoes().size());
        var convertida = resultado.medicoes().stream()
                .filter(m -> m.medicao().momento().equals(OffsetDateTime.parse("2026-09-29T08:00:00-03:00")))
                .findFirst()
                .orElseThrow();
        assertEquals(140.0, convertida.medicao().valor());
        assertEquals(TipoMedicao.PRESSAO, convertida.medicao().tipo());
        assertEquals("1.4", convertida.valorOriginal());
        assertEquals("bar", convertida.unidadeOriginal());
        assertTrue(resultado.medicoes().stream().anyMatch(m -> m.flags().contains("FORA_FAIXA_PLAUSIVEL")));

        // leitura "x" (índice 3) recusada sem reprovar o lote
        assertEquals(1, resultado.rejeicoes().size());
        assertEquals(3, resultado.rejeicoes().getFirst().indice());
        assertEquals("VALOR_INVALIDO", resultado.rejeicoes().getFirst().codigo());

        var lacuna = resultado.lacunas().getFirst();
        assertEquals(new CodigoInstrumento("PZ-01"), lacuna.instrumento());
        assertEquals(Duration.ofHours(4), lacuna.duracao());

        assertEquals(2, resultado.alertas().size());
        var limite = resultado.alertas().getFirst();
        assertEquals(TipoAlerta.LIMITE, limite.tipo());
        assertEquals(CategoriaAlerta.SEGURANCA, limite.categoria());
        assertEquals(Severidade.CRITICO, limite.severidade());
        assertEquals(2, limite.leituras());
        assertEquals(260.0, limite.limite());
        assertTrue(limite.leituraSuspeita());
        assertEquals("21", limite.versaoConfiguracao().valor());
        assertNull(resultado.alertas().get(1).limite());

        var pz01 = resultado.instrumentos().get(new CodigoInstrumento("PZ-01"));
        assertEquals(Severidade.CRITICO, pz01.statusAtual());
        assertEquals(600.0, pz01.ultimaLeitura().valor());
        assertEquals(2, resultado.instrumentos().size());
    }

    @Test
    void loteRecusadoPorInteiro() {
        var resposta = """
                {"status": "ERRO", "erros": [{"codigo": "CONTRATO_INVALIDO",
                 "campo": "configuracao.sensores.PZ-01.limites_alerta", "mensagem": "precisa de 'acima' e/ou 'abaixo'"}]}
                """;

        var falha = assertThrows(
                RequisicaoRecusadaException.class, () -> motor(1, resposta).processarLote(LoteExemplo.lote()));

        assertEquals("configuracao.sensores.PZ-01.limites_alerta", falha.erros().getFirst().campo());
    }

    @Test
    void respostaForaDoContratoEhFalha() {
        // unidade que não é a canônica do tipo
        var resposta = respostaGravada().replace("\"unidade\": \"kPa\", \"valor_original\"", "\"unidade\": \"bar\", \"valor_original\"");

        var falha = assertThrows(
                FalhaMotorException.class, () -> motor(0, resposta).processarLote(LoteExemplo.lote()));

        assertTrue(falha.getMessage().contains("fora do contrato"), falha.getMessage());
    }

    @Test
    void configuracaoGravadaQueNaoEhJson() {
        var lote = LoteExemplo.lote();
        var corrompido = new LoteMedicoes(
                lote.barragem(),
                new Configuracao(lote.configuracao().versao(), "{corrompida"),
                lote.leituras(),
                lote.historico(),
                lote.agora());

        assertThrows(FalhaMotorException.class, () -> motor(0, respostaGravada()).processarLote(corrompido));
        assertTrue(requisicoes.isEmpty());
    }
}
