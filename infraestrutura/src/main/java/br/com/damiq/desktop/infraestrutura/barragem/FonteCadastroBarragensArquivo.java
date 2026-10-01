package br.com.damiq.desktop.infraestrutura.barragem;

import br.com.damiq.desktop.aplicacao.barragem.CadastroPublicado;
import br.com.damiq.desktop.aplicacao.barragem.FalhaFonteCadastroException;
import br.com.damiq.desktop.aplicacao.barragem.FonteCadastroBarragens;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.barragem.CadastroBarragem;
import br.com.damiq.desktop.dominio.barragem.CampoBarragem;
import br.com.damiq.desktop.dominio.barragem.ContatoBarragem;
import br.com.damiq.desktop.dominio.barragem.Coordenadas;
import br.com.damiq.desktop.dominio.barragem.GrupoCampos;
import br.com.damiq.desktop.dominio.barragem.MeioContato;
import br.com.damiq.desktop.dominio.barragem.PapelContato;
import br.com.damiq.desktop.dominio.barragem.TipoCampo;
import br.com.damiq.desktop.dominio.barragem.UnidadeFederativa;
import br.com.damiq.desktop.dominio.barragem.VersaoCadastro;
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
 * Cadastro de barragens em arquivo local, enquanto a API da Central não existe: um JSON no formato de
 * {@code docs/central/cadastro-barragens.md}, com todas as barragens publicadas.
 */
public final class FonteCadastroBarragensArquivo implements FonteCadastroBarragens {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path arquivo;

    public FonteCadastroBarragensArquivo(Path arquivo) {
        this.arquivo = Validacao.obrigatorio(arquivo, "arquivo do cadastro de barragens");
    }

    @Override
    public List<CadastroPublicado> buscar() {
        JsonNode raiz;
        try {
            raiz = JSON.readTree(Files.readString(arquivo, StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            throw new FalhaFonteCadastroException("Arquivo do cadastro de barragens não encontrado: " + arquivo, e);
        } catch (IOException e) {
            throw new FalhaFonteCadastroException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        } catch (JacksonException e) {
            throw new FalhaFonteCadastroException(arquivo + " não é um JSON válido: " + e.getOriginalMessage(), e);
        }
        var barragens = raiz.path("barragens");
        if (!barragens.isArray()) {
            throw new FalhaFonteCadastroException(arquivo + ": falta a lista 'barragens'");
        }

        var publicados = new ArrayList<CadastroPublicado>();
        for (int i = 0; i < barragens.size(); i++) {
            publicados.add(cadastro(barragens.get(i), i + 1));
        }
        return publicados;
    }

    private static CadastroPublicado cadastro(JsonNode item, int posicao) {
        BarragemId id;
        try {
            id = new BarragemId(texto(item, "id"));
        } catch (RuntimeException e) {
            return CadastroPublicado.recusado("item " + posicao, null, "id da barragem ausente ou inválido");
        }
        try {
            var cadastro = new CadastroBarragem(
                    id,
                    new VersaoCadastro(versao(item)),
                    texto(item, "nome"),
                    item.path("teste").asBoolean(false),
                    texto(item, "empreendedor"),
                    texto(item, "finalidade"),
                    municipios(item),
                    uf(texto(item, "uf")),
                    new Coordenadas(numero(item, "latitude"), numero(item, "longitude")),
                    texto(item, "curso_dagua"),
                    texto(item, "tipo_macico"),
                    numero(item, "altura_macico_m"),
                    numero(item, "capacidade_total_m3"),
                    grupos(item.path("grupos")),
                    campos(item.path("campos")),
                    contatos(item.path("contatos")));
            return CadastroPublicado.valido(cadastro);
        } catch (RuntimeException e) {
            return CadastroPublicado.recusado(id.valor(), id, e.getMessage());
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

    private static double numero(JsonNode no, String campo) {
        var valor = no.path(campo);
        if (!valor.isNumber()) {
            throw new IllegalArgumentException(campo + " ausente ou não numérico");
        }
        return valor.asDouble();
    }

    private static UnidadeFederativa uf(String sigla) {
        try {
            return UnidadeFederativa.valueOf(Validacao.textoObrigatorio(sigla, "uf").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("UF inválida: " + sigla, e);
        }
    }

    private static List<String> municipios(JsonNode item) {
        var lista = item.path("municipios");
        if (!lista.isArray()) {
            throw new IllegalArgumentException("municipios deve ser uma lista");
        }
        var municipios = new ArrayList<String>();
        lista.forEach(m -> municipios.add(m.isString() ? m.asString() : null));
        return municipios;
    }

    private static TipoCampo tipoCampo(String tipo, String chave) {
        if (tipo == null) {
            return null;
        }
        try {
            return TipoCampo.valueOf(tipo.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "campo " + chave + ": tipo '" + tipo + "' inválido (NUMERO, TEXTO, DATA ou BOOLEANO)", e);
        }
    }

    private static List<CampoBarragem> campos(JsonNode lista) {
        if (lista.isMissingNode() || lista.isNull()) {
            return List.of();
        }
        if (!lista.isArray()) {
            throw new IllegalArgumentException("campos deve ser uma lista");
        }
        var campos = new ArrayList<CampoBarragem>();
        for (var campo : lista) {
            var tipo = texto(campo, "tipo");
            var valor = campo.path("valor");
            campos.add(new CampoBarragem(
                    texto(campo, "chave"),
                    texto(campo, "rotulo"),
                    texto(campo, "grupo"),
                    tipoCampo(tipo, texto(campo, "chave")),
                    valor.isValueNode() && !valor.isNull() ? valor.asString() : null,
                    texto(campo, "unidade"),
                    campo.path("padrao").asBoolean(false)));
        }
        return campos;
    }

    private static List<GrupoCampos> grupos(JsonNode lista) {
        if (lista.isMissingNode() || lista.isNull()) {
            return List.of();
        }
        if (!lista.isArray()) {
            throw new IllegalArgumentException("grupos deve ser uma lista");
        }
        var grupos = new ArrayList<GrupoCampos>();
        lista.forEach(grupo -> grupos.add(new GrupoCampos(texto(grupo, "chave"), texto(grupo, "nome"))));
        return grupos;
    }

    private static List<ContatoBarragem> contatos(JsonNode lista) {
        if (lista.isMissingNode() || lista.isNull()) {
            return List.of();
        }
        if (!lista.isArray()) {
            throw new IllegalArgumentException("contatos deve ser uma lista");
        }
        var contatos = new ArrayList<ContatoBarragem>();
        for (var contato : lista) {
            var chave = texto(contato, "chave");
            var nivel = contato.path("nivel_acionamento");
            if (!nivel.isMissingNode() && !nivel.isNull() && !nivel.isIntegralNumber()) {
                throw new IllegalArgumentException("contato " + chave + ": nivel_acionamento deve ser 1, 2 ou 3");
            }
            contatos.add(new ContatoBarragem(
                    chave,
                    constante(PapelContato.class, texto(contato, "papel"), "contato " + chave + ": papel"),
                    texto(contato, "entidade"),
                    texto(contato, "responsavel"),
                    texto(contato, "cargo"),
                    meios(contato.path("meios"), chave),
                    nivel.isIntegralNumber() ? nivel.asInt() : null,
                    texto(contato, "substitui"),
                    contato.path("recebe_copia_pae").asBoolean(false)));
        }
        return contatos;
    }

    private static List<MeioContato> meios(JsonNode lista, String chave) {
        if (lista.isMissingNode() || lista.isNull()) {
            return List.of();
        }
        if (!lista.isArray()) {
            throw new IllegalArgumentException("contato " + chave + ": meios deve ser uma lista");
        }
        var meios = new ArrayList<MeioContato>();
        for (var meio : lista) {
            try {
                meios.add(new MeioContato(constante(MeioContato.Tipo.class, texto(meio, "tipo"), "tipo"),
                        texto(meio, "valor")));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("contato " + chave + ": " + e.getMessage(), e);
            }
        }
        return meios;
    }

    /** Constante do enum pelo nome, sem diferenciar maiúsculas; {@code null} se ausente. */
    private static <E extends Enum<E>> E constante(Class<E> tipo, String nome, String campo) {
        if (nome == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, nome.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(campo + " '" + nome + "' inválido", e);
        }
    }
}
