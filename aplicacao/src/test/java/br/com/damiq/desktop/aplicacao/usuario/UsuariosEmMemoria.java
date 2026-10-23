package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.usuario.CadastroUsuario;
import br.com.damiq.desktop.dominio.usuario.Login;
import br.com.damiq.desktop.dominio.usuario.SenhaHash;
import br.com.damiq.desktop.dominio.usuario.Usuario;
import br.com.damiq.desktop.dominio.usuario.UsuarioId;
import br.com.damiq.desktop.dominio.usuario.VersaoUsuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** {@link RepositorioUsuarios} em memória, com as mesmas regras do repositório JDBC. */
final class UsuariosEmMemoria implements RepositorioUsuarios {

    /** Senha "hash" = "hash:" + senha, para os testes não dependerem do bcrypt. */
    static final CodificadorSenha CODIFICADOR = new CodificadorSenha() {
        @Override
        public SenhaHash codificar(char[] senha) {
            return new SenhaHash("hash:" + new String(senha));
        }

        @Override
        public boolean confere(char[] senha, SenhaHash hash) {
            return hash.valor().equals("hash:" + new String(senha));
        }
    };

    private static final class Registro {
        final long id;
        CadastroUsuario cadastro;
        SenhaHash senha;
        boolean bloqueado;
        boolean trocarSenha;
        int tentativas;
        Instant ultimoAcesso;
        boolean excluido;

        Registro(long id) {
            this.id = id;
        }

        Usuario usuario() {
            var c = cadastro;
            return new Usuario(new UsuarioId(id), c.barragem(), c.login(), c.perfil(), c.nome(), c.email(), c.telefone(),
                    c.cargo(), c.registroProfissional(), bloqueado, tentativas, trocarSenha, ultimoAcesso);
        }
    }

    private final Map<Login, Registro> registros = new LinkedHashMap<>();
    private long proximoId = 2;
    final List<Login> gravacoes = new ArrayList<>();

    private Optional<Registro> ativo(Login login) {
        return Optional.ofNullable(registros.get(login)).filter(r -> !r.excluido);
    }

    private Optional<Registro> ativo(UsuarioId id) {
        return registros.values().stream().filter(r -> r.id == id.valor() && !r.excluido).findFirst();
    }

    @Override
    public Optional<Credencial> credencial(Login login) {
        return ativo(login).map(r -> new Credencial(r.usuario(), r.senha));
    }

    @Override
    public Optional<Usuario> buscar(UsuarioId id) {
        return ativo(id).map(Registro::usuario);
    }

    @Override
    public List<Usuario> listar(BarragemId barragem) {
        return registros.values().stream().filter(r -> !r.excluido && r.cadastro.barragem().equals(barragem))
                .map(Registro::usuario).sorted(Comparator.comparing(Usuario::nome)).toList();
    }

    @Override
    public List<Login> logins() {
        return registros.values().stream().filter(r -> !r.excluido).map(r -> r.cadastro.login()).toList();
    }

    @Override
    public Optional<VersaoUsuario> versaoCadastro(Login login) {
        return Optional.ofNullable(registros.get(login)).map(r -> r.cadastro.versao());
    }

    @Override
    public void salvarCadastro(CadastroUsuario cadastro) {
        var registro = registros.computeIfAbsent(cadastro.login(), l -> new Registro(proximoId++));
        registro.cadastro = cadastro;
        registro.senha = cadastro.senhaHash();
        registro.bloqueado = cadastro.bloqueado();
        registro.trocarSenha = cadastro.trocarSenha();
        registro.tentativas = 0;
        registro.excluido = false;
        gravacoes.add(cadastro.login());
    }

    @Override
    public boolean excluir(Login login) {
        var registro = ativo(login);
        registro.ifPresent(r -> r.excluido = true);
        return registro.isPresent();
    }

    @Override
    public int registrarFalha(UsuarioId id, int limite) {
        var registro = ativo(id).orElseThrow();
        registro.tentativas++;
        registro.bloqueado |= registro.tentativas >= limite;
        return registro.tentativas;
    }

    @Override
    public void registrarAcesso(UsuarioId id, Instant momento) {
        var registro = ativo(id).orElseThrow();
        registro.ultimoAcesso = momento;
        registro.tentativas = 0;
    }

    @Override
    public void trocarSenha(UsuarioId id, SenhaHash senhaHash) {
        var registro = ativo(id).orElseThrow();
        registro.senha = senhaHash;
        registro.trocarSenha = false;
    }

    @Override
    public boolean desbloquear(UsuarioId id) {
        var registro = ativo(id);
        registro.ifPresent(r -> {
            r.bloqueado = false;
            r.tentativas = 0;
        });
        return registro.isPresent();
    }
}
