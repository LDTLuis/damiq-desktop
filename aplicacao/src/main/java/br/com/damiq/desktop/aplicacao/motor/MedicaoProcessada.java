package br.com.damiq.desktop.aplicacao.motor;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import java.util.List;

/**
 * Leitura aceita pelo motor: a medição normalizada, o valor como foi informado e as marcações do motor.
 *
 * @param flags marcações da leitura (ex.: {@code FORA_FAIXA_PLAUSIVEL}); a leitura é mantida mesmo assim
 */
public record MedicaoProcessada(
        Medicao medicao, String valorOriginal, String unidadeOriginal, List<String> flags) {

    public MedicaoProcessada {
        Validacao.obrigatorio(medicao, "medição");
        flags = List.copyOf(Validacao.obrigatorio(flags, "marcações da medição"));
    }
}
