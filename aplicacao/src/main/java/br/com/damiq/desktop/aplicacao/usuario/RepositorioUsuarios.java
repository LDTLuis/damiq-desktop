package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.Usuario;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import br.com.damiq.desktop.dominio.usuario.VersaoUsuario;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cópia local dos usuários da Central, com a situação do acesso neste aparelho. O usuário "sistema" e os
 * excluídos (exclusão lógica) não aparecem nas consultas.
 */
public interface RepositorioUsuarios {

    /** Usuário ativo com o hash da senha, para conferir o acesso. */
    Optional<Credencial> credencial(Login login);

    Optional<Usuario> buscar(UsuarioId id);

    /** Usuários ativos da barragem, pelo nome. */
    List<Usuario> listar(BarragemId barragem);

    /** Logins de todos os usuários ativos. */
    List<Login> logins();

    /** Versão do cadastro guardada, inclusive de usuário excluído; vazio se nunca sincronizado. */
    Optional<VersaoUsuario> versaoCadastro(Login login);

    /**
     * Grava a cópia do cadastro: cria o usuário ou substitui os dados (reativando-o, se estava excluído). Senha,
     * bloqueio e troca obrigatória passam a ser os da Central, e as tentativas malsucedidas zeram. A marcação de
     * teste vem da barragem do usuário.
     */
    void salvarCadastro(CadastroUsuario cadastro);

    /**
     * Exclusão lógica.
     *
     * @return {@code false} se o usuário não existir ou já estiver excluído
     */
    boolean excluir(Login login);

    /**
     * Soma uma tentativa malsucedida e bloqueia o usuário ao chegar ao limite.
     *
     * @return quantas tentativas malsucedidas seguidas o usuário tem agora
     */
    int registrarFalha(UsuarioId id, int limite);

    /** Acesso bem-sucedido: grava o momento e zera as tentativas malsucedidas. */
    void registrarAcesso(UsuarioId id, Instant momento);

    /** Grava a senha nova escolhida pelo usuário e retira a obrigação de trocá-la. */
    void trocarSenha(UsuarioId id, SenhaHash senhaHash);

    /**
     * Retira o bloqueio e zera as tentativas malsucedidas.
     *
     * @return {@code false} se o usuário não existir
     */
    boolean desbloquear(UsuarioId id);
}
