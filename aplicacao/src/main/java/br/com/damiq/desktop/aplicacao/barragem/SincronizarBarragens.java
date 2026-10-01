package br.com.damiq.desktop.aplicacao.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import java.util.ArrayList;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atualiza a cópia local do cadastro de barragens a partir da Central de Configurações (RF-02). O cadastro é
 * feito na Central; no Desktop ele é somente leitura.
 *
 * <ul>
 *   <li>versão nova → grava (barragem nova, ou dados e campos próprios atualizados);
 *   <li>mesma versão → nada muda;
 *   <li>cadastro inválido → é recusado e a cópia anterior, se houver, é mantida;
 *   <li>barragem que saiu da Central → exclusão lógica (os dados continuam gravados).
 * </ul>
 *
 * <p>Se a publicação não puder ser lida, ou vier sem nenhuma barragem válida, nada é excluído.
 */
public final class SincronizarBarragens {

    private static final Logger LOG = LoggerFactory.getLogger(SincronizarBarragens.class);

    private final FonteCadastroBarragens fonte;
    private final RepositorioBarragens barragens;

    public SincronizarBarragens(FonteCadastroBarragens fonte, RepositorioBarragens barragens) {
        this.fonte = Validacao.obrigatorio(fonte, "fonte do cadastro");
        this.barragens = Validacao.obrigatorio(barragens, "repositório de barragens");
    }

    /** @throws FalhaFonteCadastroException se a publicação não puder ser lida; nada é alterado */
    public ResultadoSincronizacao executar() {
        var publicados = fonte.buscar();

        var novas = new ArrayList<BarragemId>();
        var atualizadas = new ArrayList<BarragemId>();
        var inalteradas = new ArrayList<BarragemId>();
        var recusados = new ArrayList<CadastroPublicado>();
        var naCentral = new HashSet<BarragemId>();

        for (var publicado : publicados) {
            if (publicado.barragem() != null) {
                naCentral.add(publicado.barragem());
            }
            if (publicado.valido().isEmpty()) {
                LOG.error("Cadastro da barragem {} recusado; a cópia anterior foi mantida: {}",
                        publicado.identificacao(), publicado.erro());
                recusados.add(publicado);
                continue;
            }
            var cadastro = publicado.valido().get();
            var anterior = barragens.versaoCadastro(cadastro.id());
            var ativa = barragens.buscar(cadastro.id()).isPresent();
            if (anterior.isPresent() && anterior.get().equals(cadastro.versao()) && ativa) {
                inalteradas.add(cadastro.id());
                continue;
            }
            barragens.salvarCadastro(cadastro);
            (anterior.isPresent() ? atualizadas : novas).add(cadastro.id());
            LOG.info("Barragem {} ({}): cadastro {} {}", cadastro.id(), cadastro.nome(), cadastro.versao(),
                    anterior.isPresent() ? "atualizado" : "recebido");
        }

        var excluidas = new ArrayList<BarragemId>();
        if (recusados.size() == publicados.size()) {
            LOG.warn("A Central não publicou nenhum cadastro válido; nenhuma barragem foi excluída");
        } else {
            for (var barragem : barragens.listar()) {
                if (!naCentral.contains(barragem.id()) && barragens.excluir(barragem.id())) {
                    LOG.warn("Barragem {} ({}) saiu da Central: exclusão lógica", barragem.id(), barragem.nome());
                    excluidas.add(barragem.id());
                }
            }
        }
        return new ResultadoSincronizacao(novas, atualizadas, inalteradas, excluidas, recusados);
    }
}
