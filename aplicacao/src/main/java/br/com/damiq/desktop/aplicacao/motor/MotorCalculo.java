package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.configuracao.Configuracao;

/**
 * Porta para o motor de cálculo (contrato JSON 1.0 do {@code modulo-calculo-api}).
 *
 * <p>As chamadas são bloqueantes e levam de 0,4 s a alguns segundos: não as faça na thread da interface.
 * Falhas de execução (motor ausente, tempo esgotado, erro interno, resposta ilegível) viram
 * {@link FalhaMotorException}.
 *
 * <p>As operações {@code listar_calculos} e {@code calcular} entram com as telas de cálculo.
 */
public interface MotorCalculo {

    /** Versão do motor, do contrato e operações disponíveis. */
    InfoMotor info();

    /** Valida uma configuração da Central sem processar dados. Uma configuração recusada volta no resultado. */
    ValidacaoConfiguracao validarConfiguracao(Configuracao configuracao);

    /**
     * Valida, normaliza e avalia um lote de leituras. Leituras com problema voltam em
     * {@link ResultadoLote#rejeicoes()} sem reprovar o lote.
     *
     * @throws RequisicaoRecusadaException se o motor recusar a requisição inteira (ex.: configuração inválida)
     */
    ResultadoLote processarLote(LoteMedicoes lote);
}
