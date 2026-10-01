package br.com.damiq.desktop.dominio.usuario;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.time.Instant;
import java.util.Optional;

/**
 * Usuário do Desktop (sem o hash da senha): a cópia do cadastro da Central mais a situação do acesso neste
 * aparelho.
 *
 * @param tentativasFalhas tentativas de acesso malsucedidas seguidas; zera no acesso bem-sucedido
 * @param ultimoAcesso último acesso bem-sucedido neste aparelho; nulo se nunca entrou
 */
public record Usuario(
        UsuarioId id,
        BarragemId barragem,
        Login login,
        Perfil perfil,
        String nome,
        String email,
        String telefone,
        String cargo,
        String registroProfissional,
        boolean bloqueado,
        int tentativasFalhas,
        boolean trocarSenha,
        Instant ultimoAcesso) {

    public Usuario {
        Validacao.obrigatorio(id, "identificador do usuário");
        Validacao.obrigatorio(barragem, "barragem do usuário " + login);
        Validacao.obrigatorio(login, "login");
        Validacao.obrigatorio(perfil, "perfil do usuário " + login);
        nome = Validacao.textoObrigatorio(nome, "nome do usuário " + login);
        email = email(email, login);
        telefone = opcional(telefone);
        cargo = opcional(cargo);
        registroProfissional = registroProfissional(registroProfissional, perfil, login);
        if (tentativasFalhas < 0) {
            throw new IllegalArgumentException("tentativas malsucedidas não pode ser negativo: " + tentativasFalhas);
        }
    }

    public boolean permite(Permissao permissao) {
        return perfil.permite(permissao);
    }

    public Optional<Instant> ultimoAcessoOpcional() {
        return Optional.ofNullable(ultimoAcesso);
    }

    static String email(String email, Login login) {
        var valor = Validacao.textoObrigatorio(email, "e-mail do usuário " + login);
        var arroba = valor.indexOf('@');
        if (arroba < 1 || arroba != valor.lastIndexOf('@') || arroba == valor.length() - 1 || valor.contains(" ")) {
            throw new IllegalArgumentException("e-mail do usuário " + login + " inválido: " + valor);
        }
        return valor;
    }

    static String registroProfissional(String registro, Perfil perfil, Login login) {
        var valor = opcional(registro);
        if (valor == null && perfil != null && perfil.exigeRegistroProfissional()) {
            throw new IllegalArgumentException(
                    "registro profissional (CREA) é obrigatório para o perfil " + perfil + " (usuário " + login + ")");
        }
        return valor;
    }

    static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
