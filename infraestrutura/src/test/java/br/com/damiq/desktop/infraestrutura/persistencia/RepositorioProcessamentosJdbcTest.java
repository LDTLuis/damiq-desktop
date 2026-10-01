package br.com.damiq.desktop.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.damiq.desktop.aplicacao.medicao.ChaveMedicao;
import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import br.com.damiq.desktop.aplicacao.medicao.OrigemLeituras;
import br.com.damiq.desktop.aplicacao.medicao.RegistroProcessamento;
import br.com.damiq.desktop.aplicacao.medicao.RegistroProcessamento.RejeicaoLeitura;
import br.com.damiq.desktop.aplicacao.motor.MedicaoProcessada;
import br.com.damiq.desktop.aplicacao.motor.Rejeicao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import br.com.damiq.desktop.dominio.alerta.CategoriaAlerta;
import br.com.damiq.desktop.dominio.alerta.Severidade;
import br.com.damiq.desktop.dominio.alerta.TipoAlerta;
import br.com.damiq.desktop.dominio.barragem.Barragem;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Lacuna;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Gravação de processamentos e consultas de medições contra um SQLite temporário. */
class RepositorioProcessamentosJdbcTest {

    private static final BarragemId JOAO_LEITE = new BarragemId("joao-leite");
    private static final VersaoConfiguracao V21 = new VersaoConfiguracao("21");
    private static final CodigoInstrumento PZ_01 = new CodigoInstrumento("PZ-01");
    private static final CodigoInstrumento NV_01 = new CodigoInstrumento("NV-01");
    private static final OffsetDateTime T08 = OffsetDateTime.parse("2026-09-29T08:00:00-03:00");

    @TempDir
    Path diretorio;

    private DataSource banco;
    private RepositorioProcessamentosJdbc processamentos;
    private RepositorioMedicoesJdbc medicoes;

    @BeforeEach
    void criarBanco() {
        banco = BancoDados.abrir(diretorio.resolve("damiq.db"));
        new RepositorioBarragensJdbc(banco, Clock.systemUTC()).salvar(new Barragem(JOAO_LEITE, "João Leite"));
        new RepositorioConfiguracoesJdbc(banco).ativar(JOAO_LEITE,
                new Configuracao(V21, "{\"versao\": 21}"), OrigemConfiguracao.ARQUIVO, Instant.now());
        processamentos = new RepositorioProcessamentosJdbc(banco);
        medicoes = new RepositorioMedicoesJdbc(banco);
    }

    private static MedicaoProcessada medicao(CodigoInstrumento instrumento, OffsetDateTime momento, double valor) {
        var tipo = instrumento.equals(PZ_01) ? TipoMedicao.PRESSAO : TipoMedicao.NIVEL;
        return new MedicaoProcessada(new Medicao(instrumento, tipo, momento, valor), "1,4", "bar",
                List.of("FORA_FAIXA_PLAUSIVEL"));
    }

    private static RegistroProcessamento registro(VersaoConfiguracao versao, List<MedicaoProcessada> lista) {
        var alerta = new Alerta(TipoAlerta.LIMITE, CategoriaAlerta.SEGURANCA, PZ_01, Severidade.CRITICO, T08,
                T08.plusHours(1), 2, 600, "kPa", null, null, true, "PZ-01 acima do crítico", versao);
        return new RegistroProcessamento(
                JOAO_LEITE, versao, Instant.parse("2026-09-30T02:00:00Z"), OrigemLeituras.IMPORTACAO, "campo.csv",
                Severidade.CRITICO, Severidade.AVISO, 3, lista.size() + 1, 0, lista,
                List.of(new RejeicaoLeitura(new Rejeicao(2, "VALOR_INVALIDO", "Valor não numérico: 'x'"),
                        new LeituraInformada("PZ-01", "pressao", "2026-09-29T14:00", "x", null))),
                List.of(new Lacuna(PZ_01, T08, T08.plusHours(4), Duration.ofHours(4))),
                List.of(alerta));
    }

    private List<String> linhas(String sql) throws SQLException {
        try (var conexao = banco.getConnection();
                var consulta = conexao.createStatement();
                var resultado = consulta.executeQuery(sql)) {
            var linhas = new ArrayList<String>();
            while (resultado.next()) {
                var colunas = new ArrayList<String>();
                for (int i = 1; i <= resultado.getMetaData().getColumnCount(); i++) {
                    colunas.add(resultado.getString(i));
                }
                linhas.add(String.join("|", colunas));
            }
            return linhas;
        }
    }

    @Test
    void gravaOProcessamentoCompleto() throws SQLException {
        var id = processamentos.registrar(registro(V21, List.of(medicao(PZ_01, T08, 140))));

        assertEquals(
                List.of(id + "|21|IMPORTACAO|campo.csv|CRITICO|AVISO|3|2|1|0|1"),
                linhas("""
                        SELECT p.id, c.versao, p.origem, p.arquivo, p.status_barragem, p.status_dados,
                               p.nivel_resposta, p.recebidas, p.gravadas, p.ja_registradas, p.rejeitadas
                        FROM processamento p JOIN configuracao c ON c.id = p.configuracao_id"""));
        assertEquals(
                List.of("PZ-01|PRESSAO|2026-09-29T11:00:00.000Z|-03:00|140.0|1,4|bar|FORA_FAIXA_PLAUSIVEL"),
                linhas("SELECT instrumento, tipo, momento, fuso, valor, valor_original, unidade_original, flags FROM medicao"));
        assertEquals(List.of("2|VALOR_INVALIDO|x|null"), linhas("SELECT indice, codigo, valor, unidade FROM rejeicao"));
        assertEquals(List.of("PZ-01|14400"), linhas("SELECT instrumento, duracao_s FROM lacuna"));
        assertEquals(List.of("LIMITE|CRITICO|null|1"), linhas("SELECT tipo, severidade, limite, leitura_suspeita FROM alerta"));
    }

    @Test
    void ultimasPorInstrumentoPreservandoOFuso() {
        var lista = new ArrayList<MedicaoProcessada>();
        for (int hora = 0; hora < 5; hora++) {
            lista.add(medicao(PZ_01, T08.plusHours(hora), 140 + hora));
        }
        lista.add(medicao(NV_01, T08, 12.5));
        processamentos.registrar(registro(V21, lista));

        var ultimas = medicoes.ultimas(JOAO_LEITE, 3);

        assertEquals(4, ultimas.size());
        assertEquals(NV_01, ultimas.getFirst().instrumento());
        var pz01 = ultimas.subList(1, 4);
        assertEquals(List.of(142.0, 143.0, 144.0), pz01.stream().map(Medicao::valor).toList());
        assertEquals(T08.plusHours(2), pz01.getFirst().momento());
        assertEquals("-03:00", pz01.getFirst().momento().getOffset().getId());
    }

    @Test
    void consultaQuaisJaEstaoGravadas() {
        processamentos.registrar(registro(V21, List.of(medicao(PZ_01, T08, 140))));
        var gravada = new ChaveMedicao(PZ_01, T08.toInstant());
        // mesmo instante escrito em outro fuso
        var mesmaEmUtc = new ChaveMedicao(PZ_01, OffsetDateTime.parse("2026-09-29T11:00:00Z").toInstant());
        var nova = new ChaveMedicao(PZ_01, T08.plusHours(1).toInstant());

        assertEquals(Set.of(gravada), medicoes.registradas(JOAO_LEITE, List.of(mesmaEmUtc, nova)));
        assertTrue(medicoes.registradas(new BarragemId("outra"), List.of(gravada)).isEmpty());
    }

    @Test
    void medicaoRepetidaDesfazOProcessamentoInteiro() throws SQLException {
        processamentos.registrar(registro(V21, List.of(medicao(PZ_01, T08, 140))));

        assertThrows(FalhaBancoDadosException.class,
                () -> processamentos.registrar(registro(V21, List.of(medicao(PZ_01, T08, 141)))));

        assertEquals(List.of("1"), linhas("SELECT count(*) FROM processamento"));
        assertEquals(List.of("1"), linhas("SELECT count(*) FROM alerta"));
    }

    @Test
    void configuracaoDesconhecidaNaoGrava() throws SQLException {
        assertThrows(FalhaBancoDadosException.class,
                () -> processamentos.registrar(registro(new VersaoConfiguracao("99"), List.of())));

        assertEquals(List.of("0"), linhas("SELECT count(*) FROM processamento"));
    }
}
