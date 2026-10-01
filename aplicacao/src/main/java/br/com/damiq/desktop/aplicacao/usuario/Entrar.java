package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.aplicacao.usuario.EntradaRecusadaException.Motivo;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.Usuario;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrar no sistema (UC-01, RF-01): confere login e senha na cópia local dos usuários, sem precisar de rede, e
 * abre a sessão na barragem do usuário.
 *
 * <ul>
 *   <li>login desconhecido ou senha errada → a mesma recusa, para não revelar quais logins existem;
 *   <li>cada senha errada soma uma tentativa; ao chegar ao limite, o usuário é bloqueado até o administrador
 *       desbloqueá-lo (neste aparelho ou republicando o cadastro na Central);
 *   <li>usuário bloqueado é recusado mesmo com a senha certa, sem contar tentativa;
 *   <li>com troca de senha pendente, a sessão abre, mas só permite trocar a senha ({@link ControleAcesso}).
 * </ul>
 */
public final class Entrar {

    /** Tentativas malsucedidas seguidas que bloqueiam o usuário. */
    public static final int LIMITE_TENTATIVAS = 5;

    private static final Logger LOG = LoggerFactory.getLogger(Entrar.class);
    private static final String CREDENCIAIS_INVALIDAS = "Login ou senha incorretos";

    private final RepositorioUsuarios usuarios;
    private final RepositorioBarragens barragens;
    private final CodificadorSenha codificador;
    private final Sessao sessao;
    private final Clock relogio;
    private final int limiteTentativas;

    public Entrar(
            RepositorioUsuarios usuarios,
            RepositorioBarragens barragens,
            CodificadorSenha codificador,
            Sessao sessao,
            Clock relogio,
            int limiteTentativas) {
        this.usuarios = Validacao.obrigatorio(usuarios, "repositório de usuários");
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
        this.codificador = Validacao.obrigatorio(codificador, "codificador de senhas");
        this.sessao = Validacao.obrigatorio(sessao, "sessão");
        this.relogio = Validacao.obrigatorio(relogio, "relógio");
        if (limiteTentativas < 1) {
            throw new IllegalArgumentException("limite de tentativas deve ser positivo: " + limiteTentativas);
        }
        this.limiteTentativas = limiteTentativas;
    }

    public Entrar(
            RepositorioUsuarios usuarios,
            RepositorioBarragens barragens,
            CodificadorSenha codificador,
            Sessao sessao,
            Clock relogio) {
        this(usuarios, barragens, codificador, sessao, relogio, LIMITE_TENTATIVAS);
    }

    /**
     * @param senha digitada; quem chamou deve apagá-la depois ({@code Arrays.fill(senha, '\0')})
     * @return o usuário conectado; se {@link Usuario#trocarSenha()}, a tela deve pedir a senha nova
     * @throws EntradaRecusadaException se o acesso for recusado; a sessão anterior, se houver, é encerrada
     */
    public Usuario executar(String login, char[] senha) {
        sessao.encerrar();
        Login chave;
        try {
            chave = new Login(login);
        } catch (RuntimeException e) {
            throw new EntradaRecusadaException(Motivo.CREDENCIAIS_INVALIDAS, CREDENCIAIS_INVALIDAS);
        }
        var credencial = usuarios.credencial(chave).orElse(null);
        if (credencial == null) {
            LOG.warn("Acesso recusado: login desconhecido {}", chave);
            throw new EntradaRecusadaException(Motivo.CREDENCIAIS_INVALIDAS, CREDENCIAIS_INVALIDAS);
        }
        var usuario = credencial.usuario();
        if (usuario.bloqueado()) {
            LOG.warn("Acesso recusado: usuário {} bloqueado", chave);
            throw bloqueado();
        }
        if (senha == null || !codificador.confere(senha, credencial.senhaHash())) {
            var tentativas = usuarios.registrarFalha(usuario.id(), limiteTentativas);
            if (tentativas >= limiteTentativas) {
                LOG.warn("Usuário {} bloqueado após {} tentativas malsucedidas", chave, tentativas);
                throw bloqueado();
            }
            LOG.warn("Acesso recusado: senha incorreta para {} ({} de {} tentativas)", chave, tentativas,
                    limiteTentativas);
            throw new EntradaRecusadaException(Motivo.CREDENCIAIS_INVALIDAS, CREDENCIAIS_INVALIDAS);
        }
        if (barragens.buscar(usuario.barragem()).isEmpty()) {
            LOG.warn("Acesso recusado: a barragem {} do usuário {} não está mais cadastrada", usuario.barragem(),
                    chave);
            throw new EntradaRecusadaException(Motivo.BARRAGEM_INDISPONIVEL,
                    "A barragem deste usuário não está mais cadastrada; procure o administrador");
        }

        usuarios.registrarAcesso(usuario.id(), relogio.instant());
        var conectado = usuarios.buscar(usuario.id()).orElseThrow();
        sessao.iniciar(conectado);
        LOG.info("Usuário {} ({}) entrou na barragem {}{}", chave, conectado.perfil(), conectado.barragem(),
                conectado.trocarSenha() ? "; troca de senha pendente" : "");
        return conectado;
    }

    private static EntradaRecusadaException bloqueado() {
        return new EntradaRecusadaException(Motivo.BLOQUEADO, "Usuário bloqueado; procure o administrador");
    }
}
