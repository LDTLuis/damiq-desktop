package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;

/**
 * Erro ou aviso devolvido pelo motor ({@code erros[]} e {@code avisos[]} do contrato).
 *
 * @param campo caminho do campo com problema (ex.: {@code configuracao.sensores.PZ-01.faixa}); {@code null}
 *     quando o problema não é de um campo específico
 */
public record MensagemMotor(String codigo, String mensagem, String campo) {

    public MensagemMotor {
        codigo = Validacao.textoObrigatorio(codigo, "código da mensagem do motor");
        mensagem = Validacao.textoObrigatorio(mensagem, "texto da mensagem do motor");
    }

    @Override
    public String toString() {
        return campo == null ? codigo + ": " + mensagem : codigo + " em " + campo + ": " + mensagem;
    }
}
