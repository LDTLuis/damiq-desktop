package br.com.damiq.desktop.aplicacao.medicao;

import br.com.damiq.desktop.aplicacao.motor.MedicaoProcessada;
import br.com.damiq.desktop.aplicacao.motor.ResultadoLote;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.alerta.Alerta;
import java.util.List;

/**
 * Resultado de {@link ProcessarMedicoes}.
 *
 * @param lote resposta completa do motor (status, nível de resposta, rejeições, situação dos instrumentos)
 * @param gravadas medições novas gravadas
 * @param jaRegistradas leituras válidas que já estavam gravadas (reimportação) e foram ignoradas
 * @param alertas alertas gravados (sem os episódios formados só por leituras já registradas)
 */
public record ResultadoProcessamento(
        long processamento,
        ResultadoLote lote,
        List<MedicaoProcessada> gravadas,
        List<MedicaoProcessada> jaRegistradas,
        List<Alerta> alertas) {

    public ResultadoProcessamento {
        Validacao.obrigatorio(lote, "resultado do motor");
        gravadas = List.copyOf(Validacao.obrigatorio(gravadas, "medições gravadas"));
        jaRegistradas = List.copyOf(Validacao.obrigatorio(jaRegistradas, "medições já registradas"));
        alertas = List.copyOf(Validacao.obrigatorio(alertas, "alertas"));
    }
}
