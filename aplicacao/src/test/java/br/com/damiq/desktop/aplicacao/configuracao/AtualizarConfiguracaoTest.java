package br.com.damiq.desktop.aplicacao.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.damiq.desktop.aplicacao.barragem.BarragemNaoCadastradaException;
import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.configuracao.ResultadoAtualizacaoConfiguracao.Situacao;
import br.com.damiq.desktop.aplicacao.motor.FalhaMotorException;
import br.com.damiq.desktop.aplicacao.motor.MensagemMotor;
import br.com.damiq.desktop.aplicacao.motor.MotorCalculo;
import br.com.damiq.desktop.aplicacao.motor.ValidacaoConfiguracao;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtualizarConfiguracaoTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final Instant AGORA = Instant.parse("2026-10-01T13:45:00Z");
    private static final Configuracao V21 = configuracao("21");
    private static final Configuracao V22 = configuracao("22");
    private static final MensagemMotor AVISO =
            new MensagemMotor("CAMPO_DESCONHECIDO", "campo ignorado", "configuracao.xpto");

    @Mock
    private RepositorioBarragens barragens;

    @Mock
    private RepositorioConfiguracoes configuracoes;

    @Mock
    private FonteConfiguracao fonte;

    @Mock
    private MotorCalculo motor;

    private AtualizarConfiguracao atualizar;

    private static Configuracao configuracao(String versao) {
        return new Configuracao(new VersaoConfiguracao(versao), "{\"versao\": " + versao + "}");
    }

    @BeforeEach
    void preparar() {
        when(barragens.buscar(JOAO_LEITE)).thenReturn(Optional.of(new Barragem(JOAO_LEITE, "João Leite")));
        when(fonte.origem()).thenReturn(OrigemConfiguracao.ARQUIVO);
        when(configuracoes.vigente(JOAO_LEITE)).thenReturn(Optional.of(V21));
        atualizar = new AtualizarConfiguracao(
                barragens, configuracoes, fonte, motor, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void ativaVersaoNovaValida() {
        when(fonte.buscar(JOAO_LEITE)).thenReturn(V22);
        when(motor.validarConfiguracao(V22)).thenReturn(new ValidacaoConfiguracao(List.of(), List.of(AVISO)));

        var resultado = atualizar.executar(JOAO_LEITE);

        assertEquals(Situacao.ATIVADA, resultado.situacao());
        assertEquals(List.of(AVISO), resultado.avisos());
        verify(configuracoes).ativar(JOAO_LEITE, V22, OrigemConfiguracao.ARQUIVO, AGORA);
    }

    @Test
    void primeiraConfiguracaoDaBarragem() {
        when(configuracoes.vigente(JOAO_LEITE)).thenReturn(Optional.empty());
        when(fonte.buscar(JOAO_LEITE)).thenReturn(V21);
        when(motor.validarConfiguracao(V21)).thenReturn(new ValidacaoConfiguracao(List.of(), List.of()));

        assertEquals(Situacao.ATIVADA, atualizar.executar(JOAO_LEITE).situacao());
        verify(configuracoes).ativar(JOAO_LEITE, V21, OrigemConfiguracao.ARQUIVO, AGORA);
    }

    @Test
    void recusadaPeloMotorMantemAVigente() {
        var erro = new MensagemMotor("CONTRATO_INVALIDO", "min (5) maior que max (1)", "configuracao.sensores.PZ-01.faixa");
        when(fonte.buscar(JOAO_LEITE)).thenReturn(V22);
        when(motor.validarConfiguracao(V22)).thenReturn(new ValidacaoConfiguracao(List.of(erro), List.of()));

        var resultado = atualizar.executar(JOAO_LEITE);

        assertEquals(Situacao.RECUSADA, resultado.situacao());
        assertEquals(List.of(erro), resultado.erros());
        verify(configuracoes, never()).ativar(any(), any(), any(), any());
    }

    @Test
    void mesmaVersaoDaVigenteNaoChamaOMotor() {
        when(fonte.buscar(JOAO_LEITE)).thenReturn(V21);

        assertEquals(Situacao.JA_VIGENTE, atualizar.executar(JOAO_LEITE).situacao());
        verifyNoInteractions(motor);
        verify(configuracoes, never()).ativar(any(), any(), any(), any());
    }

    @Test
    void versaoAntigaDoHistoricoEhRecusada() {
        var v20 = configuracao("20");
        when(fonte.buscar(JOAO_LEITE)).thenReturn(v20);
        when(configuracoes.versaoRegistrada(JOAO_LEITE, v20.versao())).thenReturn(true);

        var resultado = atualizar.executar(JOAO_LEITE);

        assertEquals(Situacao.RECUSADA, resultado.situacao());
        assertEquals("VERSAO_JA_USADA", resultado.erros().getFirst().codigo());
        verifyNoInteractions(motor);
    }

    @Test
    void barragemNaoCadastrada() {
        var outra = new BarragemId("outra");
        when(barragens.buscar(outra)).thenReturn(Optional.empty());

        assertThrows(BarragemNaoCadastradaException.class, () -> atualizar.executar(outra));
        verifyNoInteractions(fonte, motor);
    }

    @Test
    void falhaDoMotorNaoAlteraNada() {
        when(fonte.buscar(JOAO_LEITE)).thenReturn(V22);
        when(motor.validarConfiguracao(V22)).thenThrow(new FalhaMotorException("motor ausente"));

        assertThrows(FalhaMotorException.class, () -> atualizar.executar(JOAO_LEITE));
        verify(configuracoes, never()).ativar(any(), any(), any(), any());
    }
}
