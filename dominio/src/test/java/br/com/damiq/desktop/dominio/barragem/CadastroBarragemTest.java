package br.com.damiq.desktop.dominio.barragem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CadastroBarragemTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final GrupoCampos MACICO = new GrupoCampos("macico", "Maciço");

    private static CadastroBarragem cadastro(
            List<String> municipios, double altura, List<GrupoCampos> grupos, List<CampoBarragem> campos) {
        return new CadastroBarragem(JOAO_LEITE, new VersaoCadastro("3"), "João Leite", false, "SANEAGO",
                "Abastecimento público", municipios, UnidadeFederativa.GO, new Coordenadas(-16.57, -49.21),
                "Ribeirão João Leite", "CCR", altura, 129_000_000, grupos, campos, List.of());
    }

    private static final MeioContato CELULAR = new MeioContato(MeioContato.Tipo.CELULAR, "(62) 99999-0001");

    private static CadastroBarragem comContatos(ContatoBarragem... contatos) {
        return new CadastroBarragem(JOAO_LEITE, new VersaoCadastro("3"), "João Leite", false, "SANEAGO",
                "Abastecimento público", List.of("Goiânia"), UnidadeFederativa.GO, new Coordenadas(-16.57, -49.21),
                "Ribeirão João Leite", "CCR", 50, 129_000_000, List.of(), List.of(), List.of(contatos));
    }

    private static ContatoBarragem contato(String chave, Integer nivel, String substitui, MeioContato... meios) {
        return new ContatoBarragem(chave, PapelContato.COORDENADOR_PAE, "Empresa", "Fulano", null, List.of(meios),
                nivel, substitui, false);
    }

    private static CampoBarragem campo(String chave, String grupo, TipoCampo tipo, String valor) {
        return new CampoBarragem(chave, "Rótulo", grupo, tipo, valor, null, true);
    }

    @Test
    void cadastroValido() {
        var cadastro = cadastro(List.of(" Goiânia "), 50, List.of(MACICO),
                List.of(campo("cota_crista", "macico", TipoCampo.NUMERO, "752.50"),
                        campo("sem_grupo", null, TipoCampo.TEXTO, "x")));

        assertEquals(List.of("Goiânia"), cadastro.municipios());
        assertEquals(new Barragem(JOAO_LEITE, "João Leite", false), cadastro.barragem());
        assertEquals("752.5", cadastro.campos().getFirst().valor());
        assertEquals(752.5, cadastro.campos().getFirst().numero());
    }

    @Test
    void campoEmGrupoQueNaoExisteNaBarragem() {
        var falha = assertThrows(IllegalArgumentException.class, () -> cadastro(List.of("Goiânia"), 50, List.of(MACICO),
                List.of(campo("largura_vertedouro", "vertedouro", TipoCampo.NUMERO, "50"))));

        assertTrue(falha.getMessage().contains("vertedouro"), falha.getMessage());
    }

    @Test
    void gruposComChaveRepetida() {
        assertThrows(IllegalArgumentException.class, () -> cadastro(List.of("Goiânia"), 50,
                List.of(MACICO, new GrupoCampos("macico", "Outro nome")), List.of()));
    }

    @Test
    void exigeAoMenosUmMunicipio() {
        assertThrows(IllegalArgumentException.class, () -> cadastro(List.of(), 50, List.of(), List.of()));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN})
    void alturaDeveSerPositiva(double altura) {
        assertThrows(IllegalArgumentException.class, () -> cadastro(List.of("Goiânia"), altura, List.of(), List.of()));
    }

    @Test
    void camposComChaveRepetida() {
        var campo = campo("cota_crista", null, TipoCampo.NUMERO, "752.5");

        var falha = assertThrows(IllegalArgumentException.class,
                () -> cadastro(List.of("Goiânia"), 50, List.of(), List.of(campo, campo)));

        assertTrue(falha.getMessage().contains("cota_crista"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {91, -91})
    void latitudeForaDoIntervalo(double latitude) {
        assertThrows(IllegalArgumentException.class, () -> new Coordenadas(latitude, -49));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "NUMERO|752.50|752.5",
        "NUMERO|1E+2|100",
        "NUMERO|-3|-3",
        "DATA|2009-12-18|2009-12-18",
        "BOOLEANO|true|true",
        "TEXTO|  Portaria 946/2009  |Portaria 946/2009"
    })
    void valorNoFormatoCanonico(TipoCampo tipo, String valor, String canonico) {
        assertEquals(canonico, campo("campo", null, tipo, valor).valor());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "NUMERO|752,5", "NUMERO|0x1p3", "NUMERO|1.5d", "NUMERO|NaN",
        "DATA|18/12/2009", "BOOLEANO|sim"
    })
    void valorIncompativelComOTipo(TipoCampo tipo, String valor) {
        var falha = assertThrows(IllegalArgumentException.class, () -> campo("campo", null, tipo, valor));

        assertTrue(falha.getMessage().startsWith("campo campo:"), falha.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Cota", "cota crista", "1cota", "cota-crista"})
    void chaveInvalida(String chave) {
        assertThrows(IllegalArgumentException.class, () -> campo(chave, null, TipoCampo.TEXTO, "x"));
        assertThrows(IllegalArgumentException.class, () -> new GrupoCampos(chave, "Grupo"));
    }

    @Test
    void numeroSoParaCampoNumerico() {
        assertThrows(IllegalStateException.class, () -> campo("outorga", null, TipoCampo.TEXTO, "x").numero());
    }

    @Test
    void contatoComSubstituto() {
        var cadastro = comContatos(contato("coordenador_pae", 1, null, CELULAR),
                contato("coordenador_substituto", null, "coordenador_pae", CELULAR));

        var titular = cadastro.contatos().getFirst();
        assertTrue(titular.acionadoNo(1));
        assertTrue(titular.acionadoNo(3));
        assertFalse(titular.acionadoNo(0));
    }

    @Test
    void contatosComChaveRepetida() {
        var falha = assertThrows(IllegalArgumentException.class, () -> comContatos(
                contato("coordenador_pae", 1, null, CELULAR), contato("coordenador_pae", 2, null, CELULAR)));

        assertTrue(falha.getMessage().contains("coordenador_pae"), falha.getMessage());
    }

    @Test
    void substitutoDeContatoQueNaoExiste() {
        var falha = assertThrows(IllegalArgumentException.class,
                () -> comContatos(contato("coordenador_substituto", null, "coordenador_pae", CELULAR)));

        assertTrue(falha.getMessage().contains("não existe"), falha.getMessage());
    }

    @Test
    void substitutoDeSubstituto() {
        assertThrows(IllegalArgumentException.class, () -> comContatos(contato("titular", 1, null, CELULAR),
                contato("substituto", null, "titular", CELULAR), contato("outro", null, "substituto", CELULAR)));
    }

    @Test
    void substitutoHerdaONivelDoTitular() {
        assertThrows(IllegalArgumentException.class, () -> comContatos(contato("titular", 1, null, CELULAR),
                contato("substituto", 2, "titular", CELULAR)));
        assertEquals(2, comContatos(contato("titular", 1, null, CELULAR), contato("substituto", 1, "titular", CELULAR))
                .contatos().size());
    }

    @Test
    void contatoAcionadoPrecisaDeMeioDeContato() {
        assertThrows(IllegalArgumentException.class, () -> contato("coordenador_pae", 1, null));
        assertThrows(IllegalArgumentException.class, () -> comContatos(contato("titular", 1, null, CELULAR),
                contato("substituto", null, "titular")));
        // fora do fluxo de notificação (ex.: só recebe cópia do PAE), o meio de contato é opcional
        assertEquals(1, comContatos(contato("prefeitura", null, null)).contatos().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, -1})
    void nivelDeAcionamentoEntre1E3(int nivel) {
        assertThrows(IllegalArgumentException.class, () -> contato("coordenador_pae", nivel, null, CELULAR));
    }

    @Test
    void contatoNaoSubstituiASiMesmo() {
        assertThrows(IllegalArgumentException.class, () -> contato("coordenador_pae", 1, "coordenador_pae", CELULAR));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"EMAIL|sem-arroba", "EMAIL|a@b@c", "CELULAR|não tem", "TELEFONE|()"})
    void meioDeContatoInvalido(MeioContato.Tipo tipo, String valor) {
        assertThrows(IllegalArgumentException.class, () -> new MeioContato(tipo, valor));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "EMAIL|coordenacao@exemplo.com.br", "CELULAR|+55 (62) 99999-0001", "TELEFONE|0800 000 0000", "RADIO|Canal 3"
    })
    void meioDeContatoValido(MeioContato.Tipo tipo, String valor) {
        assertEquals(valor, new MeioContato(tipo, " " + valor + " ").valor());
    }
}
