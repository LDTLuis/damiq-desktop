package br.com.damiq.desktop.aplicacao.usuario;

import br.com.damiq.desktop.dominio.Validacao;

/** O acesso foi recusado. A mensagem pode ir para a tela: não diz se o login existe. */
public class EntradaRecusadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        /** Login desconhecido ou senha errada (não se diz qual). */
        CREDENCIAIS_INVALIDAS,
        /** Bloqueado pelo administrador ou por tentativas malsucedidas. */
        BLOQUEADO,
        /** A barragem do usuário saiu do cadastro da Central. */
        BARRAGEM_INDISPONIVEL
    }

    private final Motivo motivo;

    public EntradaRecusadaException(Motivo motivo, String mensagem) {
        super(mensagem);
        this.motivo = Validacao.obrigatorio(motivo, "motivo");
    }

    public Motivo motivo() {
        return motivo;
    }
}
