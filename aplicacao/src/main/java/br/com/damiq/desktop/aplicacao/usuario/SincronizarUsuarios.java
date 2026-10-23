package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.aplicacao.barragem.RepositorioBarragens;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.usuario.Login;
import java.util.ArrayList;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atualiza a cópia local dos usuários a partir da Central de Configurações (RF-01), onde o administrador os
 * cadastra. Roda depois de {@code SincronizarBarragens}, porque cada usuário pertence a uma barragem.
 *
 * <ul>
 *   <li>versão nova → grava (senha, bloqueio e troca obrigatória passam a ser os da Central);
 *   <li>mesma versão → nada muda: a senha trocada e o bloqueio por tentativas neste aparelho continuam;
 *   <li>cadastro inválido ou de barragem que não está no Desktop → recusado; a cópia anterior é mantida;
 *   <li>usuário que saiu da Central → exclusão lógica (não entra mais).
 * </ul>
 *
 * <p>Se a publicação não puder ser lida, ou vier sem nenhum usuário válido, nada é excluído.
 */
public final class SincronizarUsuarios {

    private static final Logger LOG = LoggerFactory.getLogger(SincronizarUsuarios.class);

    private final FonteCadastroUsuarios fonte;
    private final RepositorioUsuarios usuarios;
    private final RepositorioBarragens barragens;

    public SincronizarUsuarios(
            FonteCadastroUsuarios fonte, RepositorioUsuarios usuarios, RepositorioBarragens barragens) {
        this.fonte = Validacao.obrigatorio(fonte, "fonte do cadastro de usuários");
        this.usuarios = Validacao.obrigatorio(usuarios, "repositório de usuários");
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
    }

    /** @throws FalhaFonteUsuariosException se a publicação não puder ser lida; nada é alterado */
    public ResultadoSincronizacaoUsuarios executar() {
        var publicados = fonte.buscar();

        var novos = new ArrayList<Login>();
        var atualizados = new ArrayList<Login>();
        var inalterados = new ArrayList<Login>();
        var recusados = new ArrayList<UsuarioPublicado>();
        var naCentral = new HashSet<Login>();

        for (var publicado : publicados) {
            if (publicado.login() != null) {
                naCentral.add(publicado.login());
            }
            if (publicado.valido().isPresent()
                    && barragens.buscar(publicado.valido().get().barragem()).isEmpty()) {
                publicado = UsuarioPublicado.recusado(publicado.identificacao(), publicado.login(),
                        "a barragem " + publicado.valido().get().barragem() + " não está cadastrada no Desktop");
            }
            if (publicado.valido().isEmpty()) {
                LOG.error("Cadastro do usuário {} recusado; a cópia anterior foi mantida: {}",
                        publicado.identificacao(), publicado.erro());
                recusados.add(publicado);
                continue;
            }
            var cadastro = publicado.valido().get();
            var anterior = usuarios.versaoCadastro(cadastro.login());
            var ativo = usuarios.credencial(cadastro.login()).isPresent();
            if (anterior.isPresent() && anterior.get().equals(cadastro.versao()) && ativo) {
                inalterados.add(cadastro.login());
                continue;
            }
            usuarios.salvarCadastro(cadastro);
            (anterior.isPresent() ? atualizados : novos).add(cadastro.login());
            LOG.info("Usuário {} ({}, barragem {}): cadastro {} {}", cadastro.login(), cadastro.perfil(),
                    cadastro.barragem(), cadastro.versao(), anterior.isPresent() ? "atualizado" : "recebido");
        }

        var excluidos = new ArrayList<Login>();
        if (recusados.size() == publicados.size()) {
            LOG.warn("A Central não publicou nenhum usuário válido; nenhum usuário foi excluído");
        } else {
            for (var login : usuarios.logins()) {
                if (!naCentral.contains(login) && usuarios.excluir(login)) {
                    LOG.warn("Usuário {} saiu da Central: exclusão lógica", login);
                    excluidos.add(login);
                }
            }
        }
        return new ResultadoSincronizacaoUsuarios(novos, atualizados, inalterados, excluidos, recusados);
    }
}
