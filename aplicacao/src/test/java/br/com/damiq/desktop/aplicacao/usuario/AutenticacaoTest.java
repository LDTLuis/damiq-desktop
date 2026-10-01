package br.com.damiq.desktop.aplicacao.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.usuario.EntradaRecusadaException.Motivo;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.Perfil;
import br.com.damiq.desktop.dominio.usuario.Permissao;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.Usuario;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import br.com.damiq.desktop.dominio.usuario.VersaoUsuario;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Entrar, sessão, controle de acesso, troca de senha e desbloqueio (RF-01). */
class AutenticacaoTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final BarragemId OUTRA = new BarragemId("serra-mesa");
    private static final Instant AGORA = Instant.parse("2026-10-01T13:45:00Z");
    private static final String SENHA = "Provisoria2026";

    private final UsuariosEmMemoria usuarios = new UsuariosEmMemoria();
    private final RepositorioBarragens barragens = mock(RepositorioBarragens.class);
    private final Sessao sessao = new Sessao();
    private final ControleAcesso controle = new ControleAcesso(sessao);
    private final Entrar entrar = new Entrar(usuarios, barragens, UsuariosEmMemoria.CODIFICADOR, sessao,
            Clock.fixed(AGORA, ZoneOffset.UTC), 3);
    private final TrocarSenha trocarSenha = new TrocarSenha(usuarios, UsuariosEmMemoria.CODIFICADOR, sessao);
    private final DesbloquearUsuario desbloquear = new DesbloquearUsuario(usuarios, controle);

    static CadastroUsuario cadastro(String login, BarragemId barragem, Perfil perfil, boolean trocarSenha) {
        return new CadastroUsuario(new Login(login), new VersaoUsuario("1"), barragem, perfil, login, login + "@x.com",
                null, null, perfil == Perfil.ENGENHEIRO ? "CREA-GO 1/D" : null, new SenhaHash("hash:" + SENHA), false,
                trocarSenha);
    }

    @BeforeEach
    void barragensAtivas() {
        when(barragens.buscar(any())).thenAnswer(chamada -> {
            BarragemId id = chamada.getArgument(0);
            return Optional.of(new Barragem(id, id.valor()));
        });
        usuarios.salvarCadastro(cadastro("eng", JOAO_LEITE, Perfil.ENGENHEIRO, false));
        usuarios.salvarCadastro(cadastro("tecnico", JOAO_LEITE, Perfil.TECNICO_CAMPO, true));
        usuarios.salvarCadastro(cadastro("admin", JOAO_LEITE, Perfil.ADMINISTRADOR, false));
        usuarios.salvarCadastro(cadastro("admin.outra", OUTRA, Perfil.ADMINISTRADOR, false));
        usuarios.salvarCadastro(cadastro("eng.outro", JOAO_LEITE, Perfil.ENGENHEIRO, false));
    }

    private Usuario usuario(String login) {
        return usuarios.credencial(new Login(login)).orElseThrow().usuario();
    }

    @Test
    void entraNaBarragemDoUsuarioEAssinaAsAlteracoes() {
        assertEquals(UsuarioId.SISTEMA, sessao.id());

        var conectado = entrar.executar(" ENG ", SENHA.toCharArray());

        assertEquals(new Login("eng"), conectado.login());
        assertEquals(JOAO_LEITE, sessao.barragem());
        assertEquals(conectado.id(), sessao.id());
        assertEquals(AGORA, conectado.ultimoAcesso());
        assertEquals(conectado, controle.exigir(Permissao.RECONHECER_NOTIFICACOES, JOAO_LEITE));

        sessao.encerrar();
        assertEquals(UsuarioId.SISTEMA, sessao.id());
        assertThrows(AcessoNegadoException.class, sessao::barragem);
    }

    @Test
    void loginDesconhecidoESenhaErradaTemAMesmaRecusa() {
        var desconhecido = assertThrows(EntradaRecusadaException.class, () -> entrar.executar("ninguem", SENHA.toCharArray()));
        var senhaErrada = assertThrows(EntradaRecusadaException.class, () -> entrar.executar("eng", "errada123".toCharArray()));
        var loginInvalido = assertThrows(EntradaRecusadaException.class, () -> entrar.executar(" ", SENHA.toCharArray()));

        assertEquals(Motivo.CREDENCIAIS_INVALIDAS, desconhecido.motivo());
        assertEquals(desconhecido.getMessage(), senhaErrada.getMessage());
        assertEquals(desconhecido.getMessage(), loginInvalido.getMessage());
        assertTrue(sessao.usuario().isEmpty());
    }

    @Test
    void bloqueiaNoLimiteDeTentativasEOAdministradorDesbloqueia() {
        assertThrows(EntradaRecusadaException.class, () -> entrar.executar("eng", "errada1".toCharArray()));
        assertThrows(EntradaRecusadaException.class, () -> entrar.executar("eng", "errada2".toCharArray()));
        var terceira = assertThrows(EntradaRecusadaException.class, () -> entrar.executar("eng", "errada3".toCharArray()));
        assertEquals(Motivo.BLOQUEADO, terceira.motivo());

        // bloqueado: nem a senha certa entra, e não conta tentativa
        var certa = assertThrows(EntradaRecusadaException.class, () -> entrar.executar("eng", SENHA.toCharArray()));
        assertEquals(Motivo.BLOQUEADO, certa.motivo());
        assertEquals(3, usuario("eng").tentativasFalhas());

        // engenheiro não desbloqueia; administrador de outra barragem também não
        entrar.executar("eng.outro", SENHA.toCharArray());
        assertThrows(AcessoNegadoException.class, () -> desbloquear.executar(usuario("eng").id()));
        entrar.executar("admin.outra", SENHA.toCharArray());
        assertThrows(AcessoNegadoException.class, () -> desbloquear.executar(usuario("eng").id()));
        entrar.executar("admin", SENHA.toCharArray());
        desbloquear.executar(usuario("eng").id());

        assertFalse(usuario("eng").bloqueado());
        assertEquals(new Login("eng"), entrar.executar("eng", SENHA.toCharArray()).login());
    }

    @Test
    void acessoBemSucedidoZeraAsTentativas() {
        assertThrows(EntradaRecusadaException.class, () -> entrar.executar("eng", "errada1".toCharArray()));
        assertEquals(0, entrar.executar("eng", SENHA.toCharArray()).tentativasFalhas());
    }

    @Test
    void recusaQuandoABarragemSaiuDaCentral() {
        when(barragens.buscar(JOAO_LEITE)).thenReturn(Optional.empty());

        var recusa = assertThrows(EntradaRecusadaException.class, () -> entrar.executar("eng", SENHA.toCharArray()));
        assertEquals(Motivo.BARRAGEM_INDISPONIVEL, recusa.motivo());
        assertTrue(sessao.usuario().isEmpty());
    }

    @Test
    void trocaDeSenhaPendenteSoPermiteTrocarASenha() {
        var tecnico = entrar.executar("tecnico", SENHA.toCharArray());
        assertTrue(tecnico.trocarSenha());
        assertThrows(AcessoNegadoException.class, () -> controle.exigir(Permissao.REGISTRAR_LEITURAS));

        assertThrows(IllegalArgumentException.class, () -> trocarSenha.executar("errada".toCharArray(), "Barragem2027".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> trocarSenha.executar(SENHA.toCharArray(), "curta1".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> trocarSenha.executar(SENHA.toCharArray(), SENHA.toCharArray()));
        var atualizado = trocarSenha.executar(SENHA.toCharArray(), "Barragem2027".toCharArray());

        assertFalse(atualizado.trocarSenha());
        assertEquals(atualizado, controle.exigir(Permissao.REGISTRAR_LEITURAS, JOAO_LEITE));
        sessao.encerrar();
        assertThrows(EntradaRecusadaException.class, () -> entrar.executar("tecnico", SENHA.toCharArray()));
        assertEquals(tecnico.id(), entrar.executar("tecnico", "Barragem2027".toCharArray()).id());
    }

    @Test
    void controleDeAcessoPorPerfilEBarragem() {
        assertThrows(AcessoNegadoException.class, () -> controle.exigir(Permissao.CONSULTAR_DADOS));

        entrar.executar("eng", SENHA.toCharArray());
        assertThrows(AcessoNegadoException.class, () -> controle.exigir(Permissao.APAGAR_DADOS_TESTE));
        assertThrows(AcessoNegadoException.class, () -> controle.exigir(Permissao.CONSULTAR_DADOS, OUTRA));
    }

    @Test
    void novaEntradaEncerraASessaoAnterior() {
        entrar.executar("eng", SENHA.toCharArray());
        assertThrows(EntradaRecusadaException.class, () -> entrar.executar("admin", "errada".toCharArray()));
        assertTrue(sessao.usuario().isEmpty());
    }
}
