package br.com.damiq.desktop.aplicacao.medicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.damiq.desktop.aplicacao.barragem.BarragemNaoCadastradaException;
import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.configuracao.ConfiguracaoAusenteException;
import br.com.damiq.desktop.aplicacao.configuracao.RepositorioConfiguracoes;
import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.aplicacao.motor.LoteMedicoes;
import br.com.damiq.desktop.aplicacao.motor.MedicaoProcessada;
import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.aplicacao.motor.Rejeicao;
import br.com.damiq.desktop.aplicacao.motor.ResultadoLote;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.NivelResposta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Lacuna;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessarMedicoesTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final CodigoInstrumento PZ_01 = new CodigoInstrumento("PZ-01");
    private static final VersaoConfiguracao V21 = new VersaoConfiguracao("21");
    private static final Configuracao CONFIGURACAO = new Configuracao(V21, "{\"versao\": 21}");
    private static final Instant AGORA = Instant.parse("2026-09-30T02:00:00Z");
    private static final OffsetDateTime T08 = OffsetDateTime.parse("2026-09-29T08:00:00-03:00");
    private static final OffsetDateTime T12 = OffsetDateTime.parse("2026-09-29T12:00:00-03:00");
    private static final OffsetDateTime T13 = OffsetDateTime.parse("2026-09-29T13:00:00-03:00");

    private static final List<LeituraInformada> LEITURAS = List.of(
            new LeituraInformada("PZ-01", "pressao", "2026-09-29T08:00:00-03:00", "1,4", "bar"),
            new LeituraInformada("PZ-01", "pressao", "2026-09-29T12:00:00-03:00", "240", "kPa"),
            new LeituraInformada("PZ-01", "pressao", "2026-09-29T13:00:00-03:00", "250", "kPa"),
            new LeituraInformada("PZ-01", "pressao", "2026-09-29T14:00:00-03:00", "x", "kPa"));

    @Mock
    private RepositorioBarragens barragens;

    @Mock
    private RepositorioConfiguracoes configuracoes;

    @Mock
    private RepositorioMedicoes medicoes;

    @Mock
    private RepositorioProcessamentos processamentos;

    @Mock
    private MotorCalculo motor;

    @Captor
    private ArgumentCaptor<RegistroProcessamento> registro;

    @Captor
    private ArgumentCaptor<LoteMedicoes> lote;

    private ProcessarMedicoes processar;

    private static MedicaoProcessada medicao(OffsetDateTime momento, double valor) {
        return new MedicaoProcessada(new Medicao(PZ_01, TipoMedicao.PRESSAO, momento, valor), "x", "kPa", List.of());
    }

    private static Alerta alerta(OffsetDateTime inicio, OffsetDateTime fim) {
        return new Alerta(TipoAlerta.LIMITE, CategoriaAlerta.SEGURANCA, PZ_01, Severidade.ALERTA, inicio, fim, 2,
                250, "kPa", 215.82, "acima", false, "PZ-01 acima do limite", V21);
    }

    private static ResultadoLote resultado(VersaoConfiguracao versao) {
        return new ResultadoLote(
                versao,
                List.of(medicao(T08, 140), medicao(T12, 240), medicao(T13, 250)),
                List.of(new Rejeicao(3, "VALOR_INVALIDO", "Valor não numérico: 'x'")),
                List.of(),
                List.of(new Lacuna(PZ_01, T08, T12, Duration.ofHours(4))),
                List.of(alerta(T12, T13)),
                Severidade.ALERTA,
                Severidade.OK,
                new NivelResposta(2, "amarelo", "Nível 2 – amarelo", "situação", List.of("ação"), null),
                Map.of(),
                List.of());
    }

    @BeforeEach
    void preparar() {
        when(barragens.buscar(JOAO_LEITE)).thenReturn(Optional.of(new Barragem(JOAO_LEITE, "João Leite")));
        when(configuracoes.vigente(JOAO_LEITE)).thenReturn(Optional.of(CONFIGURACAO));
        when(medicoes.ultimas(JOAO_LEITE, 48)).thenReturn(List.of());
        when(medicoes.registradas(any(), anyCollection())).thenReturn(Set.of());
        when(motor.processarLote(any())).thenReturn(resultado(V21));
        when(processamentos.registrar(any())).thenReturn(7L);
        processar = new ProcessarMedicoes(barragens, configuracoes, medicoes, processamentos, motor, eventos::add,
                Clock.fixed(AGORA, ZoneOffset.UTC), 48);
    }

    private final List<Object> eventos = new ArrayList<>();

    @Test
    void publicaMedicoesProcessadasDepoisDeGravar() {
        processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.DIGITACAO, null);

        assertEquals(List.of(new MedicoesProcessadas(JOAO_LEITE, 7L, 1)), eventos);
    }

    @Test
    void falhaDeQuemOuveNaoDesfazOProcessamento() {
        var comOuvinteComDefeito = new ProcessarMedicoes(barragens, configuracoes, medicoes, processamentos, motor,
                evento -> {
                    throw new IllegalStateException("ouvinte com defeito");
                },
                Clock.fixed(AGORA, ZoneOffset.UTC), 48);

        var resultado = comOuvinteComDefeito.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.DIGITACAO, null);

        assertEquals(7L, resultado.processamento());
    }

    @Test
    void enviaAoMotorComConfiguracaoVigenteEHistorico() {
        var historico = List.of(new Medicao(PZ_01, TipoMedicao.PRESSAO, T08.minusHours(1), 139));
        when(medicoes.ultimas(JOAO_LEITE, 48)).thenReturn(historico);

        processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.DIGITACAO, null);

        verify(motor).processarLote(lote.capture());
        assertEquals(CONFIGURACAO, lote.getValue().configuracao());
        assertEquals(LEITURAS, lote.getValue().leituras());
        assertEquals(historico, lote.getValue().historico());
        assertEquals(AGORA, lote.getValue().agora().toInstant());
    }

    @Test
    void gravaTudoNumProcessamento() {
        var resultado = processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.IMPORTACAO, "campo-setembro.csv");

        verify(processamentos).registrar(registro.capture());
        var gravado = registro.getValue();
        assertEquals(7L, resultado.processamento());
        assertEquals(V21, gravado.versaoConfiguracao());
        assertEquals("campo-setembro.csv", gravado.arquivo());
        assertEquals(4, gravado.recebidas());
        assertEquals(3, gravado.medicoes().size());
        assertEquals(0, gravado.jaRegistradas());
        assertEquals(1, gravado.lacunas().size());
        assertEquals(1, gravado.alertas().size());
        assertEquals(2, gravado.nivelResposta());
        // a rejeição leva a leitura como foi informada
        assertEquals("x", gravado.rejeicoes().getFirst().leitura().valor());
    }

    @Test
    void reimportacaoNaoDuplicaMedicoesAlertasNemLacunas() {
        var todas = Set.of(
                new ChaveMedicao(PZ_01, T08.toInstant()),
                new ChaveMedicao(PZ_01, T12.toInstant()),
                new ChaveMedicao(PZ_01, T13.toInstant()));
        when(medicoes.registradas(any(), anyCollection())).thenReturn(todas);

        var resultado = processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.IMPORTACAO, "campo-setembro.csv");

        verify(processamentos).registrar(registro.capture());
        assertEquals(0, registro.getValue().medicoes().size());
        assertEquals(3, registro.getValue().jaRegistradas());
        assertTrue(registro.getValue().alertas().isEmpty());
        assertTrue(registro.getValue().lacunas().isEmpty());
        assertEquals(3, resultado.jaRegistradas().size());
    }

    @Test
    void alertaComLeituraNovaEhGravado() {
        // só a leitura das 12h já estava gravada; o episódio 12h–13h tem a das 13h, que é nova
        when(medicoes.registradas(any(), anyCollection())).thenReturn(Set.of(new ChaveMedicao(PZ_01, T12.toInstant())));

        processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.DIGITACAO, null);

        verify(processamentos).registrar(registro.capture());
        assertEquals(2, registro.getValue().medicoes().size());
        assertEquals(1, registro.getValue().alertas().size());
        // a lacuna 8h–12h é encerrada pela leitura das 12h, que já estava gravada
        assertTrue(registro.getValue().lacunas().isEmpty());
    }

    @Test
    void semConfiguracaoVigente() {
        when(configuracoes.vigente(JOAO_LEITE)).thenReturn(Optional.empty());

        assertThrows(ConfiguracaoAusenteException.class,
                () -> processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.DIGITACAO, null));
        verifyNoInteractions(motor, processamentos);
    }

    @Test
    void barragemNaoCadastrada() {
        var outra = new BarragemId("outra");

        assertThrows(BarragemNaoCadastradaException.class,
                () -> processar.executar(outra, LEITURAS, OrigemLeituras.DIGITACAO, null));
        verifyNoInteractions(motor, processamentos);
    }

    @Test
    void semLeituras() {
        assertThrows(IllegalArgumentException.class,
                () -> processar.executar(JOAO_LEITE, List.of(), OrigemLeituras.DIGITACAO, null));
        verifyNoInteractions(motor);
    }

    @Test
    void falhaDoMotorNaoGravaNada() {
        when(motor.processarLote(any())).thenThrow(new FalhaMotorException("motor ausente"));

        assertThrows(FalhaMotorException.class,
                () -> processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.DIGITACAO, null));
        verify(processamentos, never()).registrar(any());
    }

    @Test
    void motorComOutraVersaoDaConfiguracao() {
        when(motor.processarLote(any())).thenReturn(resultado(new VersaoConfiguracao("20")));

        assertThrows(FalhaMotorException.class,
                () -> processar.executar(JOAO_LEITE, LEITURAS, OrigemLeituras.DIGITACAO, null));
        verify(processamentos, never()).registrar(any());
    }

    @Test
    void historicoPorInstrumentoDeveSerPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessarMedicoes(barragens, configuracoes, medicoes,
                processamentos, motor, evento -> {}, Clock.systemUTC(), 0));
        verify(medicoes, never()).ultimas(any(), anyInt());
    }
}
