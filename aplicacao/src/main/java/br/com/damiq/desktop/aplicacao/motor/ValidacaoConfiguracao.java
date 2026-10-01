package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.List;

/**
 * Resultado de {@code validar_configuracao}.
 *
 * @param erros motivos da recusa; vazio se a configuração é válida
 * @param avisos campos não reconhecidos pelo motor; não impedem o uso da configuração, mas devem ir para o log
 */
public record ValidacaoConfiguracao(List<MensagemMotor> erros, List<MensagemMotor> avisos) {

    public ValidacaoConfiguracao {
        erros = List.copyOf(Validacao.obrigatorio(erros, "erros da validação"));
        avisos = List.copyOf(Validacao.obrigatorio(avisos, "avisos da validação"));
    }

    public boolean valida() {
        return erros.isEmpty();
    }
}
