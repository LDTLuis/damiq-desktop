package br.com.damiq.desktop.infraestrutura.motor;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.aplicacao.motor.MedicaoProcessada;
import br.com.damiq.desktop.aplicacao.motor.MensagemMotor;
import br.com.damiq.desktop.aplicacao.motor.Rejeicao;
import br.com.damiq.desktop.aplicacao.motor.ResultadoLote;
import br.com.damiq.desktop.aplicacao.motor.SituacaoInstrumento;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.NivelResposta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Lacuna;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Tradução entre os tipos da porta e os DTOs do contrato JSON. */
final class TraducaoContrato {

    private TraducaoContrato() {}

    static RequisicaoMotor.Leitura leitura(LeituraInformada leitura) {
        return new RequisicaoMotor.Leitura(
                leitura.instrumento(), leitura.tipo(), leitura.momento(), leitura.valor(), leitura.unidade());
    }

    static RequisicaoMotor.Leitura leitura(Medicao medicao) {
        return new RequisicaoMotor.Leitura(
                medicao.instrumento().valor(),
                medicao.tipo().name().toLowerCase(Locale.ROOT),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(medicao.momento()),
                medicao.valor(),
                medicao.unidade());
    }

    static List<MensagemMotor> mensagens(List<RespostaMotor.Mensagem> mensagens) {
        return mensagens.stream()
                .map(m -> new MensagemMotor(m.codigo(), m.mensagem(), m.campo()))
                .toList();
    }

    /**
     * @throws FalhaMotorException se a resposta não respeitar o contrato (campo ausente, valor fora do domínio)
     */
    static ResultadoLote resultadoLote(RespostaMotor resposta) {
        try {
            var versao = versao(resposta);
            var monitoramento = obrigatorio(resposta.monitoramento(), "monitoramento");

            var instrumentos = new LinkedHashMap<CodigoInstrumento, SituacaoInstrumento>();
            if (monitoramento.sensores() != null) {
                monitoramento.sensores().forEach((codigo, sensor) -> instrumentos.put(
                        new CodigoInstrumento(codigo), situacao(codigo, sensor)));
            }
            return new ResultadoLote(
                    versao,
                    resposta.medicoes().stream().map(TraducaoContrato::medicao).toList(),
                    resposta.rejeicoes().stream().map(TraducaoContrato::rejeicao).toList(),
                    resposta.rejeicoesHistorico().stream().map(TraducaoContrato::rejeicao).toList(),
                    resposta.lacunas().stream().map(TraducaoContrato::lacuna).toList(),
                    resposta.alertas().stream().map(a -> alerta(a, versao)).toList(),
                    severidade(monitoramento.statusBarragem()),
                    severidade(monitoramento.statusDados()),
                    nivelResposta(obrigatorio(monitoramento.nivelResposta(), "monitoramento.nivel_resposta")),
                    instrumentos,
                    mensagens(resposta.avisos()));
        } catch (IllegalArgumentException | NullPointerException | DateTimeParseException e) {
            throw new FalhaMotorException("Resposta de processar_lote fora do contrato: " + e.getMessage(), e);
        }
    }

    private static VersaoConfiguracao versao(RespostaMotor resposta) {
        var versao = resposta.versaoConfig();
        if (versao == null || versao.isNull() || versao.isMissingNode()) {
            throw new IllegalArgumentException("versao_config ausente");
        }
        return new VersaoConfiguracao(versao.asString());
    }

    private static MedicaoProcessada medicao(RespostaMotor.Medicao m) {
        var medicao = medicao(m.sensor(), m.tipo(), m.timestamp(), m.valor(), m.unidade());
        return new MedicaoProcessada(
                medicao, m.valorOriginal(), m.unidadeOriginal(), m.flags() == null ? List.of() : m.flags());
    }

    /** Medição normalizada; a unidade precisa ser a canônica do tipo. */
    private static Medicao medicao(String sensor, String tipo, String timestamp, Double valor, String unidade) {
        var tipoMedicao = tipoMedicao(tipo);
        if (!tipoMedicao.unidadeCanonica().equals(unidade)) {
            throw new IllegalArgumentException("medição de " + sensor + " em '" + unidade + "', esperado '"
                    + tipoMedicao.unidadeCanonica() + "'");
        }
        return new Medicao(
                new CodigoInstrumento(sensor), tipoMedicao, momento(timestamp), obrigatorio(valor, "valor"));
    }

    private static SituacaoInstrumento situacao(String codigo, RespostaMotor.Sensor sensor) {
        var ultima = obrigatorio(sensor.ultimaLeitura(), "ultima_leitura de " + codigo);
        return new SituacaoInstrumento(
                medicao(codigo, sensor.tipo(), ultima.timestamp(), ultima.valor(), ultima.unidade()),
                severidade(sensor.statusAtual()),
                severidade(sensor.statusMaximo()));
    }

    private static Alerta alerta(RespostaMotor.Alerta a, VersaoConfiguracao versao) {
        return new Alerta(
                TipoAlerta.valueOf(obrigatorio(a.tipo(), "tipo do alerta")),
                CategoriaAlerta.valueOf(obrigatorio(a.categoria(), "categoria do alerta")),
                new CodigoInstrumento(a.sensor()),
                severidade(a.severidade()),
                momento(a.inicio()),
                momento(a.fim()),
                obrigatorio(a.leituras(), "leituras do alerta"),
                obrigatorio(a.valorExtremo(), "valor_extremo do alerta"),
                a.unidade(),
                a.limite(),
                a.direcao(),
                Boolean.TRUE.equals(a.leituraSuspeita()),
                a.mensagem(),
                versao);
    }

    private static Rejeicao rejeicao(RespostaMotor.Rejeicao r) {
        return new Rejeicao(obrigatorio(r.indice(), "índice da rejeição"), r.codigo(), r.mensagem());
    }

    private static Lacuna lacuna(RespostaMotor.Lacuna l) {
        var segundos = obrigatorio(l.duracaoS(), "duracao_s da lacuna");
        return new Lacuna(
                new CodigoInstrumento(l.sensor()),
                momento(l.inicio()),
                momento(l.fim()),
                Duration.ofMillis(Math.round(segundos * 1000)));
    }

    private static NivelResposta nivelResposta(RespostaMotor.NivelResposta n) {
        return new NivelResposta(
                obrigatorio(n.nivel(), "nível de resposta"),
                n.cor(),
                n.rotulo(),
                n.situacao(),
                n.acoes() == null ? List.of() : n.acoes(),
                n.fonte());
    }

    private static TipoMedicao tipoMedicao(String tipo) {
        return TipoMedicao.valueOf(obrigatorio(tipo, "tipo da medição").toUpperCase(Locale.ROOT));
    }

    private static Severidade severidade(String severidade) {
        return Severidade.valueOf(obrigatorio(severidade, "severidade"));
    }

    private static OffsetDateTime momento(String timestamp) {
        return OffsetDateTime.parse(obrigatorio(timestamp, "timestamp"));
    }

    private static <T> T obrigatorio(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " ausente");
        }
        return valor;
    }
}
