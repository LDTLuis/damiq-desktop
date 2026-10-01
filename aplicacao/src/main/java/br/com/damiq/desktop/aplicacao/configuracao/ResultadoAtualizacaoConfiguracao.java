package br.com.damiq.desktop.aplicacao.configuracao;

import br.com.damiq.desktop.aplicacao.motor.MensagemMotor;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.util.List;

/**
 * Resultado de {@link AtualizarConfiguracao}.
 *
 * @param versao versão recebida da fonte
 * @param erros motivos da recusa (só em {@link Situacao#RECUSADA})
 * @param avisos campos que o motor não reconheceu (a configuração é usada mesmo assim)
 */
public record ResultadoAtualizacaoConfiguracao(
        Situacao situacao, VersaoConfiguracao versao, List<MensagemMotor> erros, List<MensagemMotor> avisos) {

    public enum Situacao {
        /** Nova versão validada e em uso. */
        ATIVADA,
        /** A versão recebida já é a vigente; nada mudou. */
        JA_VIGENTE,
        /** Versão inválida ou já usada antes; a vigente foi mantida. */
        RECUSADA
    }

    public ResultadoAtualizacaoConfiguracao {
        Validacao.obrigatorio(situacao, "situação da atualização");
        Validacao.obrigatorio(versao, "versão da configuração");
        erros = List.copyOf(Validacao.obrigatorio(erros, "erros da atualização"));
        avisos = List.copyOf(Validacao.obrigatorio(avisos, "avisos da atualização"));
    }
}
