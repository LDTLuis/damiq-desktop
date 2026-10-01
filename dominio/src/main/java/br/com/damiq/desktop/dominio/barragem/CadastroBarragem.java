package br.com.damiq.desktop.dominio.barragem;

import br.com.damiq.desktop.dominio.Validacao;
import java.util.HashSet;
import java.util.List;

/**
 * Cadastro da barragem, feito na Central de Configurações; o Desktop guarda uma cópia (RF-02).
 *
 * <p>Os campos fixos são os obrigatórios. As demais características e indicadores, que variam de barragem para
 * barragem (ficha técnica do PAE: crista, vertedouro, volumes, hidrologia…), vão em {@link #campos()}.
 *
 * @param teste barragem de teste (UAT): só vale no primeiro cadastro, não muda depois
 * @param municipios municípios onde a barragem está (ao menos um)
 * @param alturaMacicoM altura do maciço, do ponto mais baixo da fundação à crista, em metros (critério da
 *     PNSB, Lei 12.334/2010)
 * @param capacidadeTotalM3 capacidade total do reservatório, em m³
 * @param grupos grupos de campos da barragem, na ordem de exibição; chaves únicas
 * @param campos campos próprios da barragem, na ordem de exibição; chaves únicas, cada um num grupo desta
 *     barragem (ou sem grupo)
 */
public record CadastroBarragem(
        BarragemId id,
        VersaoCadastro versao,
        String nome,
        boolean teste,
        String empreendedor,
        String finalidade,
        List<String> municipios,
        UnidadeFederativa uf,
        Coordenadas coordenadas,
        String cursoDagua,
        String tipoMacico,
        double alturaMacicoM,
        double capacidadeTotalM3,
        List<GrupoCampos> grupos,
        List<CampoBarragem> campos) {

    public CadastroBarragem {
        Validacao.obrigatorio(id, "identificador da barragem");
        Validacao.obrigatorio(versao, "versão do cadastro");
        nome = Validacao.textoObrigatorio(nome, "nome da barragem");
        empreendedor = Validacao.textoObrigatorio(empreendedor, "empreendedor");
        finalidade = Validacao.textoObrigatorio(finalidade, "finalidade");
        municipios = Validacao.obrigatorio(municipios, "municípios").stream()
                .map(m -> Validacao.textoObrigatorio(m, "município"))
                .toList();
        if (municipios.isEmpty()) {
            throw new IllegalArgumentException("informe ao menos um município");
        }
        Validacao.obrigatorio(uf, "UF");
        Validacao.obrigatorio(coordenadas, "coordenadas");
        cursoDagua = Validacao.textoObrigatorio(cursoDagua, "curso d'água");
        tipoMacico = Validacao.textoObrigatorio(tipoMacico, "tipo do maciço");
        if (!(Validacao.finito(alturaMacicoM, "altura do maciço") > 0)) {
            throw new IllegalArgumentException("altura do maciço deve ser positiva: " + alturaMacicoM);
        }
        if (!(Validacao.finito(capacidadeTotalM3, "capacidade total") > 0)) {
            throw new IllegalArgumentException("capacidade total deve ser positiva: " + capacidadeTotalM3);
        }
        grupos = List.copyOf(Validacao.obrigatorio(grupos, "grupos"));
        var chavesGrupos = new HashSet<String>();
        for (var grupo : grupos) {
            if (!chavesGrupos.add(grupo.chave())) {
                throw new IllegalArgumentException("grupo repetido: " + grupo.chave());
            }
        }
        campos = List.copyOf(Validacao.obrigatorio(campos, "campos"));
        var chaves = new HashSet<String>();
        for (var campo : campos) {
            if (!chaves.add(campo.chave())) {
                throw new IllegalArgumentException("campo repetido: " + campo.chave());
            }
            if (campo.grupo() != null && !chavesGrupos.contains(campo.grupo())) {
                throw new IllegalArgumentException(
                        "campo " + campo.chave() + ": grupo '" + campo.grupo() + "' não existe nesta barragem");
            }
        }
    }

    /** Identificação da barragem. */
    public Barragem barragem() {
        return new Barragem(id, nome, teste);
    }
}
