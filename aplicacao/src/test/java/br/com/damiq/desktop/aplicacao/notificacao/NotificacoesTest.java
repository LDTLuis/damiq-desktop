package br.com.damiq.desktop.aplicacao.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.notificacao.NotificacaoEmitida.Motivo;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link NotificarAlertas}, {@link ReconhecerNotificacao} e {@link ConsultarNotificacoes} juntos. */
class NotificacoesTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final BarragemId OUTRA = new BarragemId("outra");
    private static final CodigoInstrumento PZ_01 = new CodigoInstrumento("PZ-01");
    private static final CodigoInstrumento PZ_02 = new CodigoInstrumento("PZ-02");
    private static final OffsetDateTime T08 = OffsetDateTime.parse("2026-09-29T08:00:00-03:00");

    private final RepositorioNotificacoesEmMemoria repositorio = new RepositorioNotificacoesEmMemoria();
    private final List<Object> eventos = new ArrayList<>();
    private final Clock relogio = Clock.fixed(Instant.parse("2026-09-30T02:00:00Z"), ZoneOffset.UTC);
    private final NotificarAlertas notificar = new NotificarAlertas(repositorio, eventos::add, relogio);
    private final ReconhecerNotificacao reconhecer = new ReconhecerNotificacao(repositorio, eventos::add, relogio);
    private final ConsultarNotificacoes consultar = new ConsultarNotificacoes(repositorio);

    private static Alerta alerta(CodigoInstrumento instrumento, TipoAlerta tipo, Severidade severidade, boolean suspeita) {
        var categoria = tipo == TipoAlerta.LIMITE ? CategoriaAlerta.SEGURANCA : CategoriaAlerta.QUALIDADE;
        return new Alerta(tipo, categoria, instrumento, severidade, T08, T08.plusHours(1), 1, 250, "kPa", null, null,
                suspeita, instrumento + " " + severidade, new VersaoConfiguracao("21"));
    }

    private long gravar(CodigoInstrumento instrumento, Severidade severidade) {
        return repositorio.gravarAlerta(JOAO_LEITE, alerta(instrumento, TipoAlerta.LIMITE, severidade, false), 2);
    }

    @Test
    void primeiroAlertaAbreNotificacao() {
        var alerta = gravar(PZ_01, Severidade.ALERTA);

        var emitidas = notificar.executar(JOAO_LEITE);

        assertEquals(1, emitidas.size());
        assertEquals(Motivo.NOVA, emitidas.getFirst().motivo());
        var notificacao = emitidas.getFirst().notificacao();
        assertEquals(Severidade.ALERTA, notificacao.severidade());
        assertEquals(2, notificacao.nivelResposta());
        assertEquals(notificacao.id(), repositorio.notificacaoDoAlerta(alerta));
        assertEquals(emitidas, eventos);
    }

    @Test
    void problemaQuePersisteNaoGeraOutraNotificacao() {
        gravar(PZ_01, Severidade.ALERTA);
        notificar.executar(JOAO_LEITE);

        gravar(PZ_01, Severidade.AVISO);
        var emitidas = notificar.executar(JOAO_LEITE);

        assertEquals(Motivo.REPETIDA, emitidas.getFirst().motivo());
        assertEquals(1, repositorio.todas().size());
        var notificacao = repositorio.todas().getFirst();
        assertEquals(2, notificacao.ocorrencias());
        // continua com a severidade mais grave
        assertEquals(Severidade.ALERTA, notificacao.severidade());
    }

    @Test
    void agravamentoEscalaANotificacaoAberta() {
        gravar(PZ_01, Severidade.AVISO);
        notificar.executar(JOAO_LEITE);

        gravar(PZ_01, Severidade.CRITICO);
        var emitidas = notificar.executar(JOAO_LEITE);

        assertEquals(Motivo.ESCALADA, emitidas.getFirst().motivo());
        assertEquals(Severidade.CRITICO, emitidas.getFirst().notificacao().severidade());
        assertEquals("PZ-01 CRITICO", emitidas.getFirst().notificacao().mensagem());
    }

    @Test
    void variosEpisodiosNoMesmoLoteViramUmaNotificacao() {
        gravar(PZ_01, Severidade.AVISO);
        gravar(PZ_01, Severidade.CRITICO);
        gravar(PZ_02, Severidade.ALERTA);

        var emitidas = notificar.executar(JOAO_LEITE);

        assertEquals(2, emitidas.size());
        var pz01 = emitidas.getFirst();
        assertEquals(Motivo.NOVA, pz01.motivo());
        assertEquals(2, pz01.notificacao().ocorrencias());
        assertEquals(Severidade.CRITICO, pz01.notificacao().severidade());
    }

    @Test
    void tiposDeAlertaDiferentesNotificamSeparado() {
        gravar(PZ_01, Severidade.ALERTA);
        repositorio.gravarAlerta(JOAO_LEITE, alerta(PZ_01, TipoAlerta.FORA_FAIXA_PLAUSIVEL, Severidade.AVISO, true), 2);

        assertEquals(2, notificar.executar(JOAO_LEITE).size());
    }

    @Test
    void depoisDeReconhecidaONovoEpisodioAbreOutra() {
        gravar(PZ_01, Severidade.ALERTA);
        var primeira = notificar.executar(JOAO_LEITE).getFirst().notificacao();

        var reconhecida = reconhecer.executar(primeira.id(), "Eng. Ana", "  Inspeção agendada  ");

        assertFalse(reconhecida.aberta());
        assertEquals("Eng. Ana", reconhecida.reconhecida().orElseThrow().por());
        assertEquals("Inspeção agendada", reconhecida.reconhecida().orElseThrow().observacao());
        assertTrue(eventos.contains(new NotificacaoReconhecida(reconhecida)));
        assertTrue(consultar.abertas(JOAO_LEITE).isEmpty());

        gravar(PZ_01, Severidade.ALERTA);
        var nova = notificar.executar(JOAO_LEITE).getFirst();

        assertEquals(Motivo.NOVA, nova.motivo());
        assertNotEquals(primeira.id(), nova.notificacao().id());
    }

    @Test
    void reconhecerDuasVezes() {
        gravar(PZ_01, Severidade.ALERTA);
        var id = notificar.executar(JOAO_LEITE).getFirst().notificacao().id();
        reconhecer.executar(id, "Eng. Ana", null);

        var falha = assertThrows(IllegalStateException.class, () -> reconhecer.executar(id, "Téc. Bruno", null));

        assertTrue(falha.getMessage().contains("Eng. Ana"), falha.getMessage());
    }

    @Test
    void reconhecerExigeResponsavelENotificacaoExistente() {
        assertThrows(IllegalArgumentException.class, () -> reconhecer.executar(99, "Eng. Ana", null));
        assertThrows(IllegalArgumentException.class, () -> reconhecer.executar(1, " ", null));
    }

    @Test
    void semAlertasPendentesNaoFazNada() {
        assertTrue(notificar.executar(JOAO_LEITE).isEmpty());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void recuperaPendentesDeTodasAsBarragens() {
        gravar(PZ_01, Severidade.ALERTA);
        repositorio.gravarAlerta(OUTRA, alerta(PZ_01, TipoAlerta.LIMITE, Severidade.AVISO, false), 1);

        assertEquals(2, notificar.executarPendentes().size());
        assertTrue(notificar.executarPendentes().isEmpty());
    }

    @Test
    void abertasMaisGravesPrimeiro() {
        gravar(PZ_01, Severidade.AVISO);
        gravar(PZ_02, Severidade.CRITICO);
        notificar.executar(JOAO_LEITE);

        assertEquals(List.of(Severidade.CRITICO, Severidade.AVISO),
                consultar.abertas(null).stream().map(Notificacao::severidade).toList());
    }
}
