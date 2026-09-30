package br.com.damiq.desktop.infraestrutura.motor;

/** Executa uma chamada ao motor: recebe a requisição JSON e devolve a saída do processo. */
@FunctionalInterface
interface ExecutorMotor {

    /**
     * @throws br.com.damiq.desktop.aplicacao.motor.FalhaMotorException se o processo não puder ser iniciado ou
     *     passar do tempo limite
     */
    ExecucaoMotor executar(String requisicaoJson);

    /**
     * @param resposta JSON escrito pelo motor em {@code --saida}; vazio se ele não escreveu nada
     * @param stderr saída de erro (traceback em caso de erro interno)
     */
    record ExecucaoMotor(int codigoSaida, String resposta, String stderr) {}
}
