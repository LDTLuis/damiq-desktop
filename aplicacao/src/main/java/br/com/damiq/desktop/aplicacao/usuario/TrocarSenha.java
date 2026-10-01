package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.PoliticaSenha;
import br.com.damiq.desktop.dominio.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O usuário conectado troca a própria senha, obrigatoriamente quando o cadastro pede ({@code trocar_senha}). A
 * senha nova segue a {@link PoliticaSenha} e vale neste aparelho até a Central publicar uma nova versão do
 * cadastro do usuário; o envio à Central entra com a sincronização.
 */
public final class TrocarSenha {

    private static final Logger LOG = LoggerFactory.getLogger(TrocarSenha.class);

    private final RepositorioUsuarios usuarios;
    private final CodificadorSenha codificador;
    private final Sessao sessao;

    public TrocarSenha(RepositorioUsuarios usuarios, CodificadorSenha codificador, Sessao sessao) {
        this.usuarios = Validacao.obrigatorio(usuarios, "repositório de usuários");
        this.codificador = Validacao.obrigatorio(codificador, "codificador de senhas");
        this.sessao = Validacao.obrigatorio(sessao, "sessão");
    }

    /**
     * @return o usuário conectado, já sem troca pendente
     * @throws AcessoNegadoException se ninguém estiver conectado
     * @throws IllegalArgumentException se a senha atual não conferir ou a nova não atender à política
     */
    public Usuario executar(char[] senhaAtual, char[] senhaNova) {
        var conectado = sessao.exigirUsuario();
        var credencial = usuarios.credencial(conectado.login())
                .orElseThrow(() -> new AcessoNegadoException("O usuário " + conectado.login() + " não está mais ativo"));
        if (senhaAtual == null || !codificador.confere(senhaAtual, credencial.senhaHash())) {
            throw new IllegalArgumentException("A senha atual não confere");
        }
        PoliticaSenha.verificar(senhaNova, conectado.login());
        if (codificador.confere(senhaNova, credencial.senhaHash())) {
            throw new IllegalArgumentException("A senha nova deve ser diferente da atual");
        }

        usuarios.trocarSenha(conectado.id(), codificador.codificar(senhaNova));
        var atualizado = usuarios.buscar(conectado.id()).orElseThrow();
        sessao.iniciar(atualizado);
        LOG.info("Usuário {} trocou a senha", conectado.login());
        return atualizado;
    }
}
