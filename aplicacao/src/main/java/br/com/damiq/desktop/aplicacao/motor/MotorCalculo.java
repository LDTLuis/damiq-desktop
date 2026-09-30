package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.configuracao.Configuracao;

/**
 * Porta para o motor de cálculo (contrato JSON 1.0 do {@code modulo-calculo-api}).
 *
 * <p>As chamadas são bloqueantes e levam de 0,4 s a alguns segundos: não as faça na thread da interface.
 * Falhas de execução (motor ausente, tempo esgotado, erro interno, resposta ilegível) viram
 * {@link FalhaMotorException}; entradas recusadas pelo motor voltam no resultado da operação.
 *
 * <p>As operações {@code processar_lote}, {@code listar_calculos} e {@code calcular} entram com os casos de
 * uso que as usam.
 */
public interface MotorCalculo {

    /** Versão do motor, do contrato e operações disponíveis. */
    InfoMotor info();

    /** Valida uma configuração da Central sem processar dados. */
    ValidacaoConfiguracao validarConfiguracao(Configuracao configuracao);
}
