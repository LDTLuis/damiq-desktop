package br.com.damiq.desktop.infraestrutura.importacao;

import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeituras;
import br.com.damiq.desktop.aplicacao.importacao.ArquivoLeiturasInvalidoException;
import br.com.damiq.desktop.aplicacao.importacao.LinhaArquivo;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * CSV (RFC 4180, com aspas). Aceita os formatos que o Excel em português gera:
 *
 * <ul>
 *   <li>separador {@code ;}, {@code ,} ou tabulação, detectado pela linha de títulos;
 *   <li>UTF-8 (com ou sem BOM) ou Windows-1252;
 *   <li>fim de linha CRLF ou LF.
 * </ul>
 */
final class LeitorCsv {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private LeitorCsv() {}

    static ArquivoLeituras ler(String nome, byte[] conteudo) {
        var registros = registros(texto(conteudo));
        var titulos = registros.stream().filter(r -> !r.vazio()).findFirst()
                .orElseThrow(() -> new ArquivoLeiturasInvalidoException("O arquivo " + nome + " está vazio"));
        var cabecalho = Cabecalho.de(titulos.campos(), nome);

        var linhas = new ArrayList<LinhaArquivo>();
        for (var registro : registros.subList(registros.indexOf(titulos) + 1, registros.size())) {
            var campos = registro.campos();
            IntFunction<String> celula = i -> i < campos.size() ? campos.get(i) : "";
            if (!cabecalho.vazia(celula)) {
                linhas.add(new LinhaArquivo(registro.linha(), cabecalho.leitura(celula)));
            }
        }
        return new ArquivoLeituras(nome, linhas);
    }

    /** UTF-8 se o conteúdo for UTF-8 válido (sem o BOM); senão Windows-1252, padrão do Excel no Windows. */
    static String texto(byte[] conteudo) {
        var inicio = conteudo.length >= 3
                        && (conteudo[0] & 0xFF) == 0xEF && (conteudo[1] & 0xFF) == 0xBB && (conteudo[2] & 0xFF) == 0xBF
                ? 3
                : 0;
        var bytes = ByteBuffer.wrap(conteudo, inicio, conteudo.length - inicio);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(bytes)
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(conteudo, WINDOWS_1252);
        }
    }

    record Registro(int linha, List<String> campos) {

        boolean vazio() {
            return campos.stream().allMatch(String::isBlank);
        }
    }

    /** Separa os registros, respeitando aspas (que podem conter o separador e quebras de linha). */
    static List<Registro> registros(String texto) {
        var separador = separador(texto);
        var registros = new ArrayList<Registro>();
        var campos = new ArrayList<String>();
        var campo = new StringBuilder();
        var entreAspas = false;
        var linha = 1;
        var linhaDoRegistro = 1;

        for (int i = 0; i < texto.length(); i++) {
            var c = texto.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    if (c == '\n') {
                        linha++;
                    }
                    campo.append(c);
                }
            } else if (c == '"' && campo.isEmpty()) {
                entreAspas = true;
            } else if (c == separador) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                campos.add(campo.toString());
                registros.add(new Registro(linhaDoRegistro, List.copyOf(campos)));
                campos.clear();
                campo.setLength(0);
                linha++;
                linhaDoRegistro = linha;
            } else {
                campo.append(c);
            }
        }
        if (!campo.isEmpty() || !campos.isEmpty()) {
            campos.add(campo.toString());
            registros.add(new Registro(linhaDoRegistro, List.copyOf(campos)));
        }
        return registros;
    }

    /** O separador mais frequente fora de aspas na primeira linha não vazia. */
    static char separador(String texto) {
        var primeira = texto.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
        char escolhido = ';';
        long maior = 0;
        for (var candidato : new char[] {';', ',', '\t'}) {
            var entreAspas = false;
            long ocorrencias = 0;
            for (var c : primeira.toCharArray()) {
                if (c == '"') {
                    entreAspas = !entreAspas;
                } else if (c == candidato && !entreAspas) {
                    ocorrencias++;
                }
            }
            if (ocorrencias > maior) {
                maior = ocorrencias;
                escolhido = candidato;
            }
        }
        return escolhido;
    }
}
