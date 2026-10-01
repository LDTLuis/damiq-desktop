package br.com.damiq.desktop.aplicacao.barragem;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.damiq.desktop.dominio.barragem.BarragemId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcluirBarragemTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");

    @Mock
    private RepositorioBarragens barragens;

    @Test
    void excluiLogicamente() {
        when(barragens.excluir(JOAO_LEITE)).thenReturn(true);

        new ExcluirBarragem(barragens).executar(JOAO_LEITE);

        verify(barragens).excluir(JOAO_LEITE);
    }

    @Test
    void barragemInexistenteOuJaExcluida() {
        when(barragens.excluir(JOAO_LEITE)).thenReturn(false);

        assertThrows(BarragemNaoCadastradaException.class, () -> new ExcluirBarragem(barragens).executar(JOAO_LEITE));
    }
}
