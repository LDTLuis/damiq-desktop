package br.com.damiq.desktop.aplicacao.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.aplicacao.medicao.OrigemLeituras;
import br.com.damiq.desktop.aplicacao.medicao.ProcessarMedicoes;
import br.com.damiq.desktop.aplicacao.medicao.ResultadoProcessamento;
import br.com.damiq.desktop.aplicacao.motor.Rejeicao;
import br.com.damiq.desktop.aplicacao.motor.ResultadoLote;
import br.com.damiq.desktop.dominio.alerta.NivelResposta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportarMedicoesTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final Path ARQUIVO = Path.of("campo.csv");

    @Mock
    private LeitorArquivoLeituras leitor;

    @Mock
    private ProcessarMedicoes processar;

    private static LeituraInformada leitura(String valor) {
        return new LeituraInformada("PZ-01", "pressao", "2026-09-29T08:00:00", valor, "kPa");
    }

    private static ResultadoProcessamento comRejeicao(int indice) {
        var lote = new ResultadoLote(new VersaoConfiguracao("21"), List.of(),
                List.of(new Rejeicao(indice, "VALOR_INVALIDO", "Valor não numérico: 'x'")), List.of(), List.of(),
                List.of(), Severidade.OK, Severidade.OK,
                new NivelResposta(0, null, "Nível 0", "normal", List.of(), null), Map.of(), List.of());
        return new ResultadoProcessamento(1, lote, List.of(), List.of(), List.of());
    }

    @Test
    void rejeicaoVoltaComALinhaDoArquivo() {
        var leituras = List.of(leitura("140"), leitura("x"));
        when(leitor.ler(ARQUIVO)).thenReturn(new ArquivoLeituras("campo.csv",
                List.of(new LinhaArquivo(2, leituras.get(0)), new LinhaArquivo(7, leituras.get(1)))));
        when(processar.executar(eq(JOAO_LEITE), any(), any(), any())).thenReturn(comRejeicao(1));

        var resultado = new ImportarMedicoes(leitor, processar).executar(JOAO_LEITE, ARQUIVO);

        verify(processar).executar(JOAO_LEITE, leituras, OrigemLeituras.IMPORTACAO, "campo.csv");
        assertEquals(7, resultado.rejeicoes().getFirst().linha());
        assertEquals("linha 7: Valor não numérico: 'x' (VALOR_INVALIDO)", resultado.rejeicoes().getFirst().toString());
    }

    @Test
    void arquivoSemLeituras() {
        when(leitor.ler(ARQUIVO)).thenReturn(new ArquivoLeituras("campo.csv", List.of()));

        assertThrows(ArquivoLeiturasInvalidoException.class,
                () -> new ImportarMedicoes(leitor, processar).executar(JOAO_LEITE, ARQUIVO));
        verifyNoInteractions(processar);
    }
}
