package br.com.damiq.desktop.dominio.barragem;

/** Papel de um contato no PAE da barragem (PAE §2, §7 e §12). */
public enum PapelContato {
    /** Equipe de monitoramento, operação ou manutenção: detecta a situação e avisa o coordenador do PAE. */
    EQUIPE_TECNICA,
    /** Coordenador do PAE (ou o substituto): classifica o nível de resposta e conduz a notificação. */
    COORDENADOR_PAE,
    /** Empreendedor, responsável legal pela barragem: notificado já no nível verde. */
    EMPREENDEDOR,
    /** Órgão fiscalizador da segurança da barragem (ex.: SEMAD): notificado a partir do nível amarelo. */
    ENTIDADE_FISCALIZADORA,
    /** Defesa Civil estadual ou municipal: notificada no nível vermelho; alerta a população da ZAS. */
    DEFESA_CIVIL,
    /** Consultor externo convocado pelo coordenador do PAE. */
    CONSULTOR_EXTERNO,
    /** Outras entidades (ex.: as que recebem cópia do PAE, §12). */
    OUTRO
}
