package br.com.damiq.desktop.infraestrutura.usuario;

import br.com.damiq.desktop.aplicacao.usuario.FalhaFonteUsuariosException;
import br.com.damiq.desktop.aplicacao.usuario.FonteCadastroUsuarios;
import br.com.damiq.desktop.aplicacao.usuario.UsuarioPublicado;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.Perfil;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.VersaoUsuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Usuários publicados pela Central em arquivo local, enquanto a API não existe: um JSON no formato de
 * {@code docs/central/cadastro-usuarios.md}, com todos os usuários.
 */
public final class FonteCadastroUsuariosArquivo implements FonteCadastroUsuarios {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path arquivo;

    public FonteCadastroUsuariosArquivo(Path arquivo) {
        this.arquivo = Validacao.obrigatorio(arquivo, "arquivo do cadastro de usuários");
    }

    @Override
    public List<UsuarioPublicado> buscar() {
        JsonNode raiz;
        try {
            raiz = JSON.readTree(Files.readString(arquivo, StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            throw new FalhaFonteUsuariosException("Arquivo do cadastro de usuários não encontrado: " + arquivo, e);
        } catch (IOException e) {
            throw new FalhaFonteUsuariosException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        } catch (JacksonException e) {
            throw new FalhaFonteUsuariosException(arquivo + " não é um JSON válido: " + e.getOriginalMessage(), e);
        }
        var usuarios = raiz.path("usuarios");
        if (!usuarios.isArray()) {
            throw new FalhaFonteUsuariosException(arquivo + ": falta a lista 'usuarios'");
        }

        var publicados = new ArrayList<UsuarioPublicado>();
        for (int i = 0; i < usuarios.size(); i++) {
            publicados.add(usuario(usuarios.get(i), i + 1));
        }
        return publicados;
    }

    private static UsuarioPublicado usuario(JsonNode item, int posicao) {
        Login login;
        try {
            login = new Login(texto(item, "login"));
        } catch (RuntimeException e) {
            return UsuarioPublicado.recusado("item " + posicao, null, "login ausente ou inválido");
        }
        try {
            var hash = texto(item, "senha_hash");
            if (!CodificadorSenhaBcrypt.formatoValido(hash)) {
                throw new IllegalArgumentException("senha_hash ausente ou fora do formato bcrypt");
            }
            var barragem = texto(item, "barragem");
            return UsuarioPublicado.valido(new CadastroUsuario(
                    login,
                    new VersaoUsuario(versao(item)),
                    barragem != null ? new BarragemId(barragem) : null,
                    perfil(texto(item, "perfil")),
                    texto(item, "nome"),
                    texto(item, "email"),
                    texto(item, "telefone"),
                    texto(item, "cargo"),
                    texto(item, "registro_profissional"),
                    new SenhaHash(hash),
                    item.path("bloqueado").asBoolean(false),
                    item.path("trocar_senha").asBoolean(false)));
        } catch (RuntimeException e) {
            return UsuarioPublicado.recusado(login.valor(), login, e.getMessage());
        }
    }

    private static String texto(JsonNode no, String campo) {
        var valor = no.path(campo);
        return valor.isString() ? valor.asString() : null;
    }

    private static String versao(JsonNode item) {
        var versao = item.path("versao");
        if (!versao.isString() && !versao.isIntegralNumber()) {
            throw new IllegalArgumentException("versao ausente (texto ou inteiro)");
        }
        return versao.asString();
    }

    private static Perfil perfil(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return Perfil.valueOf(nome.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "perfil '" + nome + "' inválido (ADMINISTRADOR, ENGENHEIRO ou TECNICO_CAMPO)", e);
        }
    }
}
