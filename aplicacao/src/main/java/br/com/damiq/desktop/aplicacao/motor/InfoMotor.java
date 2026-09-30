package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.List;

/** Resposta da operação {@code info}. */
public record InfoMotor(String versaoMotor, String versaoContrato, List<String> operacoes) {

    public InfoMotor {
        versaoMotor = Validacao.textoObrigatorio(versaoMotor, "versão do motor");
        versaoContrato = Validacao.textoObrigatorio(versaoContrato, "versão do contrato");
        operacoes = List.copyOf(Validacao.obrigatorio(operacoes, "operações do motor"));
    }
}
