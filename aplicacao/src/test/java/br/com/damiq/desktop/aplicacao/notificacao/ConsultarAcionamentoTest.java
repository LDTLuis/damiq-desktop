package br.com.damiq.desktop.aplicacao.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.ContatoBarragem;
import br.com.damiq.desktop.dominio.barragem.Coordenadas;
import br.com.damiq.desktop.dominio.barragem.MeioContato;
import br.com.damiq.desktop.dominio.barragem.PapelContato;
import br.com.damiq.desktop.dominio.barragem.UnidadeFederativa;
import br.com.damiq.desktop.dominio.barragem.VersaoCadastro;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Ordem de acionamento do fluxograma de notificação do PAE, por nível de resposta. */
class ConsultarAcionamentoTest {

    private static final BarragemId BARRAGEM = new BarragemId("joao-leite");
    private static final List<MeioContato> CELULAR = List.of(new MeioContato(MeioContato.Tipo.CELULAR, "(62) 99999-0000"));

    private static final ContatoBarragem EQUIPE = contato("equipe_operacao", PapelContato.EQUIPE_TECNICA, 1, null);
    private static final ContatoBarragem COORDENADOR = contato("coordenador_pae", PapelContato.COORDENADOR_PAE, 1, null);
    private static final ContatoBarragem SUBSTITUTO =
            contato("coordenador_substituto", PapelContato.COORDENADOR_PAE, null, "coordenador_pae");
    private static final ContatoBarragem EMPREENDEDOR = contato("empreendedor", PapelContato.EMPREENDEDOR, 1, null);
    private static final ContatoBarragem FISCALIZADORA =
            contato("semad", PapelContato.ENTIDADE_FISCALIZADORA, 2, null);
    private static final ContatoBarragem DEFESA_CIVIL_MUNICIPAL =
            contato("defesa_civil_municipal", PapelContato.DEFESA_CIVIL, 3, null);
    private static final ContatoBarragem DEFESA_CIVIL_ESTADUAL =
            contato("defesa_civil_estadual", PapelContato.DEFESA_CIVIL, 3, null);
    private static final ContatoBarragem PREFEITURA = contato("prefeitura", PapelContato.OUTRO, null, null);

    private final RepositorioBarragens barragens = mock(RepositorioBarragens.class);
    private final ConsultarAcionamento consultar = new ConsultarAcionamento(barragens);

    private static ContatoBarragem contato(String chave, PapelContato papel, Integer nivel, String substitui) {
        return new ContatoBarragem(chave, papel, "Entidade " + chave, null, null, CELULAR, nivel, substitui, false);
    }

    private void cadastrar(ContatoBarragem... contatos) {
        when(barragens.buscarCadastro(BARRAGEM)).thenReturn(Optional.of(new CadastroBarragem(BARRAGEM,
                new VersaoCadastro("1"), "João Leite", false, "Empreendedor", "Abastecimento", List.of("Goiânia"),
                UnidadeFederativa.GO, new Coordenadas(-16.57, -49.21), "Ribeirão", "CCR", 50, 129_000_000, List.of(),
                List.of(), List.of(contatos))));
    }

    private static List<String> titulares(Acionamento acionamento) {
        return acionamento.contatos().stream().map(c -> c.titular().chave()).toList();
    }

    @Test
    void cadaNivelSomaContatosAosDoNivelAnteriorNaOrdemDoFluxograma() {
        // fora de ordem no cadastro: o acionamento ordena por nível e mantém a ordem do cadastro dentro do nível
        cadastrar(DEFESA_CIVIL_MUNICIPAL, FISCALIZADORA, EQUIPE, COORDENADOR, SUBSTITUTO, EMPREENDEDOR,
                DEFESA_CIVIL_ESTADUAL, PREFEITURA);

        assertEquals(List.of("equipe_operacao", "coordenador_pae", "empreendedor"),
                titulares(consultar.para(BARRAGEM, 1)));
        assertEquals(List.of("equipe_operacao", "coordenador_pae", "empreendedor", "semad"),
                titulares(consultar.para(BARRAGEM, 2)));
        var vermelho = consultar.para(BARRAGEM, 3);
        assertEquals(List.of("equipe_operacao", "coordenador_pae", "empreendedor", "semad", "defesa_civil_municipal",
                "defesa_civil_estadual"), titulares(vermelho));
        assertEquals(List.of(), vermelho.pendencias());
    }

    @Test
    void substitutoAcompanhaOTitular() {
        cadastrar(COORDENADOR, SUBSTITUTO, EMPREENDEDOR);

        var coordenador = consultar.para(BARRAGEM, 1).contatos().getFirst();

        assertEquals(COORDENADOR, coordenador.titular());
        assertEquals(List.of(SUBSTITUTO), coordenador.substitutos());
    }

    @Test
    void nivelZeroNaoAcionaNinguem() {
        var acionamento = consultar.para(BARRAGEM, 0);

        assertTrue(acionamento.contatos().isEmpty());
        assertTrue(acionamento.pendencias().isEmpty());
    }

    @Test
    void apontaOsPapeisDoPaeSemContatoNoCadastro() {
        cadastrar(COORDENADOR, EMPREENDEDOR);

        assertEquals(List.of(), consultar.para(BARRAGEM, 1).pendencias());
        var pendencias = consultar.para(BARRAGEM, 3).pendencias();
        assertEquals(2, pendencias.size());
        assertTrue(pendencias.get(0).contains("entidade fiscalizadora"), pendencias.get(0));
        assertTrue(pendencias.get(1).contains("Defesa Civil"), pendencias.get(1));
    }

    @Test
    void barragemSemCadastroSincronizado() {
        when(barragens.buscarCadastro(BARRAGEM)).thenReturn(Optional.empty());

        var acionamento = consultar.para(BARRAGEM, 2);

        assertTrue(acionamento.contatos().isEmpty());
        assertEquals(1, acionamento.pendencias().size());
    }

    @Test
    void notificacaoUsaOMaiorNivelEAvisaDaLeituraSuspeita() {
        cadastrar(COORDENADOR, EMPREENDEDOR, FISCALIZADORA);
        var agora = Instant.parse("2026-10-01T12:00:00Z");
        var suspeita = new Notificacao(1L, BARRAGEM, new CodigoInstrumento("PZ-01"), TipoAlerta.LIMITE,
                CategoriaAlerta.SEGURANCA, Severidade.ALERTA, "Acima do limite", true, 2, 1, agora, agora, null);

        var acionamento = consultar.para(suspeita);

        assertEquals(2, acionamento.nivelResposta());
        assertTrue(acionamento.verificarInstrumentoAntes());
        assertEquals(List.of("coordenador_pae", "empreendedor", "semad"), titulares(acionamento));
        assertFalse(consultar.para(BARRAGEM, 2).verificarInstrumentoAntes());
    }
}
