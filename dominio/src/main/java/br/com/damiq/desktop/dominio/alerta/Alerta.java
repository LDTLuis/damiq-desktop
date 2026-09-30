package br.com.damiq.desktop.dominio.alerta;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import br.com.damiq.desktop.dominio.instrumento.CodigoInstrumento;
import java.time.OffsetDateTime;

/**
 * Episódio de alerta: leituras consecutivas do mesmo instrumento que dispararam a mesma regra, com a maior
 * severidade atingida.
 *
 * @param limite limite ultrapassado, na {@code unidade} do alerta; {@code null} quando a regra não tem limite
 * @param direcao {@code "acima"} ou {@code "abaixo"} do limite; {@code null} quando não se aplica
 * @param leituraSuspeita a leitura também está fora da faixa plausível: verificar o instrumento antes de
 *     acionar o PAE
 * @param versaoConfiguracao versão da configuração usada pelo motor (auditoria, RF-12)
 */
public record Alerta(
        TipoAlerta tipo,
        CategoriaAlerta categoria,
        CodigoInstrumento instrumento,
        Severidade severidade,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        int leituras,
        double valorExtremo,
        String unidade,
        Double limite,
        String direcao,
        boolean leituraSuspeita,
        String mensagem,
        VersaoConfiguracao versaoConfiguracao) {

    public Alerta {
        Validacao.obrigatorio(tipo, "tipo do alerta");
        Validacao.obrigatorio(categoria, "categoria do alerta");
        Validacao.obrigatorio(instrumento, "instrumento do alerta");
        Validacao.obrigatorio(severidade, "severidade do alerta");
        Validacao.obrigatorio(inicio, "início do alerta");
        Validacao.obrigatorio(fim, "fim do alerta");
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("fim do alerta (" + fim + ") antes do início (" + inicio + ")");
        }
        if (leituras < 1) {
            throw new IllegalArgumentException("alerta deve ter ao menos 1 leitura: " + leituras);
        }
        Validacao.finito(valorExtremo, "valor extremo do alerta");
        unidade = Validacao.textoObrigatorio(unidade, "unidade do alerta");
        if (limite != null) {
            Validacao.finito(limite, "limite do alerta");
        }
        mensagem = Validacao.textoObrigatorio(mensagem, "mensagem do alerta");
        Validacao.obrigatorio(versaoConfiguracao, "versão da configuração do alerta");
    }

    /** Alerta que compõe o status da barragem (e não só o status dos dados). */
    public boolean afetaSegurancaDaBarragem() {
        return categoria == CategoriaAlerta.SEGURANCA;
    }
}
