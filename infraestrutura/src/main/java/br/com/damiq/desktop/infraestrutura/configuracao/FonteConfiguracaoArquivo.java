package br.com.damiq.desktop.infraestrutura.configuracao;

import br.com.damiq.desktop.aplicacao.configuracao.FalhaFonteConfiguracaoException;
import br.com.damiq.desktop.aplicacao.configuracao.FonteConfiguracao;
import br.com.damiq.desktop.dominio.Validacao;
import br.com.damiq.desktop.dominio.barragem.BarragemId;
import br.com.damiq.desktop.dominio.configuracao.Configuracao;
import br.com.damiq.desktop.dominio.configuracao.OrigemConfiguracao;
import br.com.damiq.desktop.dominio.configuracao.VersaoConfiguracao;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuração em arquivo local, enquanto a Central não existe: {@code <diretório>/<id da barragem>.json}, no
 * formato da seção {@code configuracao} do contrato do motor (com {@code versao} obrigatória).
 */
public final class FonteConfiguracaoArquivo implements FonteConfiguracao {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path diretorio;

    public FonteConfiguracaoArquivo(Path diretorio) {
        this.diretorio = Validacao.obrigatorio(diretorio, "diretório das configurações");
    }

    @Override
    public Configuracao buscar(BarragemId barragem) {
        var arquivo = diretorio.resolve(barragem.valor() + ".json");
        String conteudo;
        try {
            conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new FalhaFonteConfiguracaoException("Arquivo de configuração não encontrado: " + arquivo, e);
        } catch (IOException e) {
            throw new FalhaFonteConfiguracaoException(
                    "Não foi possível ler a configuração " + arquivo + ": " + e.getMessage(), e);
        }

        try {
            var versao = JSON.readTree(conteudo).path("versao");
            if (!versao.isString() && !versao.isIntegralNumber()) {
                throw new FalhaFonteConfiguracaoException(
                        "A configuração " + arquivo + " não tem o campo 'versao' (texto ou inteiro)");
            }
            return new Configuracao(new VersaoConfiguracao(versao.asString()), conteudo);
        } catch (JacksonException e) {
            throw new FalhaFonteConfiguracaoException(
                    "A configuração " + arquivo + " não é um JSON válido: " + e.getOriginalMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new FalhaFonteConfiguracaoException("Configuração " + arquivo + " inválida: " + e.getMessage(), e);
        }
    }

    @Override
    public OrigemConfiguracao origem() {
        return OrigemConfiguracao.ARQUIVO;
    }
}
