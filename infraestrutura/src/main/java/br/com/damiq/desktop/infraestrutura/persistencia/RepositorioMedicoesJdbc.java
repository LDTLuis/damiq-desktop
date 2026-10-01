package br.com.damiq.desktop.infraestrutura.persistencia;

import br.com.damiq.desktop.aplicacao.medicao.ChaveMedicao;
import br.com.damiq.desktop.aplicacao.medicao.RepositorioMedicoes;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import br.com.damiq.desktop.dominio.medicao.TipoMedicao;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/** {@link RepositorioMedicoes} na tabela {@code medicao}. */
public final class RepositorioMedicoesJdbc implements RepositorioMedicoes {

    private final Jdbc jdbc;

    public RepositorioMedicoesJdbc(DataSource fonte) {
        this.jdbc = new Jdbc(Validacao.obrigatorio(fonte, "banco de dados"));
    }

    @Override
    public List<Medicao> ultimas(BarragemId barragem, int porInstrumento) {
        return jdbc.executar("buscar o histórico de medições da barragem " + barragem, conexao -> {
            try (var consulta = conexao.prepareStatement("""
                    SELECT instrumento, tipo, momento, fuso, valor FROM (
                        SELECT instrumento, tipo, momento, fuso, valor,
                               ROW_NUMBER() OVER (PARTITION BY instrumento ORDER BY momento DESC) AS ordem
                        FROM medicao WHERE barragem_id = ?)
                    WHERE ordem <= ?
                    ORDER BY instrumento, momento
                    """)) {
                consulta.setString(1, barragem.valor());
                consulta.setInt(2, porInstrumento);
                try (var linhas = consulta.executeQuery()) {
                    var medicoes = new ArrayList<Medicao>();
                    while (linhas.next()) {
                        medicoes.add(new Medicao(
                                new CodigoInstrumento(linhas.getString("instrumento")),
                                TipoMedicao.valueOf(linhas.getString("tipo")),
                                Instant.parse(linhas.getString("momento"))
                                        .atOffset(ZoneOffset.of(linhas.getString("fuso"))),
                                linhas.getDouble("valor")));
                    }
                    return medicoes;
                }
            }
        });
    }

    @Override
    public Set<ChaveMedicao> registradas(BarragemId barragem, Collection<ChaveMedicao> chaves) {
        if (chaves.isEmpty()) {
            return Set.of();
        }
        var porInstrumento = chaves.stream().collect(Collectors.groupingBy(ChaveMedicao::instrumento));
        return jdbc.executar("consultar medições já gravadas da barragem " + barragem, conexao -> {
            var encontradas = new HashSet<ChaveMedicao>();
            try (var consulta = conexao.prepareStatement("""
                    SELECT momento FROM medicao
                    WHERE barragem_id = ? AND instrumento = ? AND momento BETWEEN ? AND ?
                    """)) {
                for (var grupo : porInstrumento.entrySet()) {
                    var instantes = grupo.getValue().stream().map(ChaveMedicao::instante).toList();
                    consulta.setString(1, barragem.valor());
                    consulta.setString(2, grupo.getKey().valor());
                    consulta.setString(3, BancoDados.data(instantes.stream().min(Comparator.naturalOrder()).orElseThrow()));
                    consulta.setString(4, BancoDados.data(instantes.stream().max(Comparator.naturalOrder()).orElseThrow()));
                    try (var linhas = consulta.executeQuery()) {
                        while (linhas.next()) {
                            encontradas.add(new ChaveMedicao(grupo.getKey(), Instant.parse(linhas.getString("momento"))));
                        }
                    }
                }
            }
            encontradas.retainAll(new HashSet<>(chaves));
            return encontradas;
        });
    }
}
