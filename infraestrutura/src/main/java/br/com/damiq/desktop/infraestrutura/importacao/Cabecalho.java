package br.com.damiq.desktop.infraestrutura.importacao;

import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeiturasInvalidoException;
import br.com.damiq.desktop.aplicacao.medicao.LeituraInformada;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Posição de cada {@link Papel} no arquivo, a partir da linha de títulos. Colunas não reconhecidas são
 * ignoradas.
 */
final class Cabecalho {

    private final Map<Papel, Integer> posicoes;

    private Cabecalho(Map<Papel, Integer> posicoes) {
        this.posicoes = posicoes;
    }

    /**
     * @throws ArquivoLeiturasInvalidoException se faltar uma coluna obrigatória ou houver coluna repetida
     */
    static Cabecalho de(List<String> titulos, String arquivo) {
        var posicoes = new EnumMap<Papel, Integer>(Papel.class);
        for (int i = 0; i < titulos.size(); i++) {
            var indice = i;
            Papel.doTitulo(titulos.get(i)).ifPresent(papel -> {
                if (posicoes.putIfAbsent(papel, indice) != null) {
                    throw new ArquivoLeiturasInvalidoException(
                            arquivo + ": a coluna '" + titulos.get(indice) + "' aparece mais de uma vez");
                }
            });
        }

        var faltando = new ArrayList<String>();
        for (var papel : List.of(Papel.INSTRUMENTO, Papel.TIPO, Papel.VALOR, Papel.UNIDADE)) {
            if (!posicoes.containsKey(papel)) {
                faltando.add(papel.name().toLowerCase(Locale.ROOT));
            }
        }
        if (!posicoes.containsKey(Papel.DATA_HORA) && !posicoes.containsKey(Papel.DATA)) {
            faltando.add("data_hora (ou data e hora)");
        }
        if (!faltando.isEmpty()) {
            throw new ArquivoLeiturasInvalidoException(arquivo + ": faltam as colunas " + String.join(", ", faltando)
                    + ". Colunas esperadas: instrumento, tipo, data_hora (ou data e hora), valor, unidade");
        }
        return new Cabecalho(posicoes);
    }

    boolean tem(Papel papel) {
        return posicoes.containsKey(papel);
    }

    /** Papel da coluna, ou {@code null} se ela for ignorada. */
    Papel papelDaColuna(int coluna) {
        for (var entrada : posicoes.entrySet()) {
            if (entrada.getValue() == coluna) {
                return entrada.getKey();
            }
        }
        return null;
    }

    /**
     * Monta a leitura de uma linha. Campos vazios viram {@code null}, e o motor recusa a linha com
     * {@code CAMPO_AUSENTE}.
     *
     * @param celula texto da célula na coluna informada ({@code ""} se ela não existir na linha)
     */
    LeituraInformada leitura(IntFunction<String> celula) {
        var dataHora = tem(Papel.DATA_HORA)
                ? Campos.dataHora(valor(Papel.DATA_HORA, celula))
                : Campos.dataHora(valor(Papel.DATA, celula), tem(Papel.HORA) ? valor(Papel.HORA, celula) : null);
        return new LeituraInformada(
                valor(Papel.INSTRUMENTO, celula),
                valor(Papel.TIPO, celula),
                dataHora,
                valor(Papel.VALOR, celula),
                valor(Papel.UNIDADE, celula));
    }

    /** Se a linha não tem nenhum valor nas colunas reconhecidas. */
    boolean vazia(IntFunction<String> celula) {
        return posicoes.keySet().stream().allMatch(papel -> valor(papel, celula) == null);
    }

    private String valor(Papel papel, IntFunction<String> celula) {
        var texto = celula.apply(posicoes.get(papel));
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
