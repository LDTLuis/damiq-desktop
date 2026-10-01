package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.regex.Pattern;

/**
 * Campo próprio de uma barragem, além dos obrigatórios: características e indicadores que variam de barragem
 * para barragem (ex.: cota da crista, largura do vertedouro, área de drenagem).
 *
 * @param chave identificador estável do campo na barragem (minúsculas, números e {@code _}), ex.:
 *     {@code cota_crista}
 * @param rotulo nome exibido, ex.: "Cota da crista"
 * @param grupo agrupamento na tela, ex.: "Maciço"; {@code null} se não houver
 * @param valor no formato canônico do {@link TipoCampo}
 * @param unidade ex.: {@code m}, {@code hm³}; {@code null} se não houver
 */
public record CampoBarragem(String chave, String rotulo, String grupo, TipoCampo tipo, String valor, String unidade) {

    private static final Pattern CHAVE = Pattern.compile("[a-z][a-z0-9_]{0,63}");

    public CampoBarragem {
        chave = Validacao.textoObrigatorio(chave, "chave do campo");
        if (!CHAVE.matcher(chave).matches()) {
            throw new IllegalArgumentException(
                    "chave do campo deve ter só minúsculas, números e _ (até 64, começando por letra): " + chave);
        }
        rotulo = Validacao.textoObrigatorio(rotulo, "rótulo do campo " + chave);
        grupo = grupo == null || grupo.isBlank() ? null : grupo.strip();
        Validacao.obrigatorio(tipo, "tipo do campo " + chave);
        try {
            valor = tipo.canonico(Validacao.textoObrigatorio(valor, "valor do campo " + chave));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("campo " + chave + ": " + e.getMessage(), e);
        }
        unidade = unidade == null || unidade.isBlank() ? null : unidade.strip();
    }

    /** Valor numérico de um campo {@link TipoCampo#NUMERO}. */
    public double numero() {
        if (tipo != TipoCampo.NUMERO) {
            throw new IllegalStateException("o campo " + chave + " não é numérico");
        }
        return Double.parseDouble(valor);
    }
}
