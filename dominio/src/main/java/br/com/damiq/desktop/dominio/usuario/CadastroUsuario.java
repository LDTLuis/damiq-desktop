package br.com.damiq.desktop.dominio.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;

/**
 * Cadastro de um usuário publicado pela Central de Configurações, onde o administrador cria e edita os usuários
 * (RF-01). O Desktop guarda uma cópia para conferir a senha sem rede. Cada usuário pertence a uma única barragem,
 * que é o contexto de tudo o que ele vê e altera. CPF não é coletado (LGPD, princípio da necessidade).
 *
 * @param registroProfissional número do CREA; obrigatório para o perfil {@link Perfil#ENGENHEIRO}
 * @param bloqueado acesso suspenso pelo administrador
 * @param trocarSenha o usuário deve trocar a senha no próximo acesso (ex.: senha provisória)
 */
public record CadastroUsuario(
        Login login,
        VersaoUsuario versao,
        BarragemId barragem,
        Perfil perfil,
        String nome,
        String email,
        String telefone,
        String cargo,
        String registroProfissional,
        SenhaHash senhaHash,
        boolean bloqueado,
        boolean trocarSenha) {

    public CadastroUsuario {
        Validacao.obrigatorio(login, "login");
        Validacao.obrigatorio(versao, "versão do cadastro do usuário");
        Validacao.obrigatorio(barragem, "barragem do usuário " + login);
        Validacao.obrigatorio(perfil, "perfil do usuário " + login);
        nome = Validacao.textoObrigatorio(nome, "nome do usuário " + login);
        email = Usuario.email(email, login);
        telefone = Usuario.opcional(telefone);
        cargo = Usuario.opcional(cargo);
        registroProfissional = Usuario.registroProfissional(registroProfissional, perfil, login);
        Validacao.obrigatorio(senhaHash, "hash da senha do usuário " + login);
    }
}
