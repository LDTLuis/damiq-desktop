package br.com.damiq.desktop.aplicacao.medicao;

import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.medicao.Medicao;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Consultas às medições já gravadas. */
public interface RepositorioMedicoes {

    /** As últimas {@code porInstrumento} medições de cada instrumento da barragem, da mais antiga à mais nova. */
    List<Medicao> ultimas(BarragemId barragem, int porInstrumento);

    /** Quais das chaves já têm medição gravada na barragem. */
    Set<ChaveMedicao> registradas(BarragemId barragem, Collection<ChaveMedicao> chaves);
}
