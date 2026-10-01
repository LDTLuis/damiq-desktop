package br.com.damiq.desktop.infraestrutura.motor;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.aplicacao.motor.LoteMedicoes;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Lote usado nos testes de {@code processar_lote}. A resposta do motor 1.0.1 para ele está gravada em
 * {@code motor/processar-lote-resposta-1.0.1.json}.
 */
public final class LoteExemplo {

    public static final String CONFIGURACAO = """
            {"versao": 21, "sensores": {"PZ-01": {"tipo": "pressao", "frequencia_esperada_s": 3600,
              "faixa": {"min": 0, "max": 500, "unidade": "kPa"},
              "limites_alerta": {"unidade": "kPa", "acima": {"aviso": 180, "alerta": 215.82, "critico": 260}}}}}
            """;

    private LoteExemplo() {}

    public static LoteMedicoes lote() {
        var pz01 = new CodigoInstrumento("PZ-01");
        return new LoteMedicoes(
                new BarragemId("joao-leite"),
                new Configuracao(new VersaoConfiguracao("21"), CONFIGURACAO),
                List.of(
                        new LeituraInformada("PZ-01", "pressao", "2026-09-29T08:00:00-03:00", "1,4", "bar"),
                        new LeituraInformada("PZ-01", "Pressão", "2026-09-29T12:00:00-03:00", "240", "kPa"),
                        new LeituraInformada("PZ-01", "pressao", "2026-09-29T13:00:00", "600", "kPa"),
                        new LeituraInformada("PZ-01", "pressao", "2026-09-29T14:00:00-03:00", "x", "kPa"),
                        new LeituraInformada("NV-01", "nivel", "2026-09-29T14:00:00-03:00", "12.5", "m")),
                List.of(
                        new Medicao(pz01, TipoMedicao.PRESSAO, OffsetDateTime.parse("2026-09-29T06:00:00-03:00"), 139),
                        new Medicao(pz01, TipoMedicao.PRESSAO, OffsetDateTime.parse("2026-09-29T07:00:00-03:00"), 139.5)),
                OffsetDateTime.parse("2026-09-29T23:00:00-03:00"));
    }
}
