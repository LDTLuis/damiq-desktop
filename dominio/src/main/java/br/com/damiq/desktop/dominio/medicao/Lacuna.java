package br.com.damiq.desktop.dominio.medicao;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Intervalo sem leituras de um instrumento maior que o esperado (frequência × tolerância da configuração).
 *
 * @param inicio última leitura antes da lacuna
 * @param fim primeira leitura depois da lacuna
 */
public record Lacuna(CodigoInstrumento instrumento, OffsetDateTime inicio, OffsetDateTime fim, Duration duracao) {

    public Lacuna {
        Validacao.obrigatorio(instrumento, "instrumento da lacuna");
        Validacao.obrigatorio(inicio, "início da lacuna");
        Validacao.obrigatorio(fim, "fim da lacuna");
        Validacao.obrigatorio(duracao, "duração da lacuna");
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("fim da lacuna (" + fim + ") deve ser depois do início (" + inicio + ")");
        }
        if (duracao.isNegative() || duracao.isZero()) {
            throw new IllegalArgumentException("duração da lacuna deve ser positiva: " + duracao);
        }
    }
}
