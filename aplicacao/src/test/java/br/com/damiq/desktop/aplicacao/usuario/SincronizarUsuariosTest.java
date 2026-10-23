package br.com.damiq.desktop.aplicacao.usuario;

import static br.com.damiq.desktop.aplicacao.usuario.AutenticacaoTest.cadastro;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.Perfil;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.VersaoUsuario;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SincronizarUsuariosTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final BarragemId AUSENTE = new BarragemId("ausente");
    private static final Login ANA = new Login("ana");
    private static final Login BETO = new Login("beto");

    private final UsuariosEmMemoria usuarios = new UsuariosEmMemoria();
    private final RepositorioBarragens barragens = mock(RepositorioBarragens.class);
    private final List<UsuarioPublicado> publicados = new ArrayList<>();
    private final SincronizarUsuarios sincronizar = new SincronizarUsuarios(() -> List.copyOf(publicados), usuarios, barragens);

    @BeforeEach
    void barragens() {
        when(barragens.buscar(any())).thenReturn(Optional.empty());
        when(barragens.buscar(JOAO_LEITE)).thenReturn(Optional.of(new Barragem(JOAO_LEITE, "João Leite")));
    }

    private static CadastroUsuario versao(CadastroUsuario c, String versao, String hash) {
        return new CadastroUsuario(c.login(), new VersaoUsuario(versao), c.barragem(), c.perfil(), c.nome(), c.email(),
                c.telefone(), c.cargo(), c.registroProfissional(), new SenhaHash(hash), c.bloqueado(), c.trocarSenha());
    }

    @Test
    void cadastraAtualizaEExcluiQuemSaiuDaCentral() {
        var ana = cadastro("ana", JOAO_LEITE, Perfil.ENGENHEIRO, false);
        publicados.addAll(List.of(UsuarioPublicado.valido(ana),
                UsuarioPublicado.valido(cadastro("beto", JOAO_LEITE, Perfil.TECNICO_CAMPO, true))));
        assertEquals(List.of(ANA, BETO), sincronizar.executar().novos());

        // a senha trocada no aparelho continua enquanto a versão não muda
        var anaId = usuarios.credencial(ANA).orElseThrow().usuario().id();
        usuarios.trocarSenha(anaId, new SenhaHash("hash:local"));
        var mesma = sincronizar.executar();
        assertEquals(List.of(ANA, BETO), mesma.inalterados());
        assertEquals("hash:local", usuarios.credencial(ANA).orElseThrow().senhaHash().valor());

        // versão nova (ex.: senha redefinida pelo administrador) substitui; beto saiu da Central
        publicados.clear();
        publicados.add(UsuarioPublicado.valido(versao(ana, "2", "hash:central")));
        var resultado = sincronizar.executar();
        assertEquals(List.of(ANA), resultado.atualizados());
        assertEquals(List.of(BETO), resultado.excluidos());
        assertEquals("hash:central", usuarios.credencial(ANA).orElseThrow().senhaHash().valor());
        assertTrue(usuarios.credencial(BETO).isEmpty());

        // beto volta: reativado
        publicados.add(UsuarioPublicado.valido(cadastro("beto", JOAO_LEITE, Perfil.TECNICO_CAMPO, true)));
        assertEquals(List.of(BETO), sincronizar.executar().atualizados());
        assertTrue(usuarios.credencial(BETO).isPresent());
    }

    @Test
    void recusaUsuarioDeBarragemQueNaoEstaNoDesktopEMantemACopia() {
        var ana = cadastro("ana", JOAO_LEITE, Perfil.ENGENHEIRO, false);
        publicados.add(UsuarioPublicado.valido(ana));
        sincronizar.executar();

        publicados.clear();
        publicados.add(UsuarioPublicado.valido(versao(cadastro("ana", AUSENTE, Perfil.ENGENHEIRO, false), "2", "hash:x")));
        publicados.add(UsuarioPublicado.valido(cadastro("beto", JOAO_LEITE, Perfil.TECNICO_CAMPO, false)));
        var resultado = sincronizar.executar();

        assertEquals(1, resultado.recusados().size());
        assertTrue(resultado.recusados().getFirst().erro().contains("ausente"));
        assertEquals(List.of(), resultado.excluidos());
        assertEquals(JOAO_LEITE, usuarios.credencial(ANA).orElseThrow().usuario().barragem());
    }

    @Test
    void semNenhumCadastroValidoNadaEExcluido() {
        publicados.add(UsuarioPublicado.valido(cadastro("ana", JOAO_LEITE, Perfil.ENGENHEIRO, false)));
        sincronizar.executar();

        publicados.clear();
        publicados.add(UsuarioPublicado.recusado("item 1", null, "login ausente"));
        var resultado = sincronizar.executar();

        assertEquals(List.of(), resultado.excluidos());
        assertTrue(usuarios.credencial(ANA).isPresent());
    }

    @Test
    void falhaNaFonteNaoAlteraNada() {
        var falha = new SincronizarUsuarios(() -> {
            throw new FalhaFonteUsuariosException("arquivo ausente");
        }, usuarios, barragens);
        assertThrows(FalhaFonteUsuariosException.class, falha::executar);
        assertEquals(List.of(), usuarios.gravacoes);
    }
}
