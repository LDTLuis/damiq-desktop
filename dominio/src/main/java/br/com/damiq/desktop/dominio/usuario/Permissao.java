package br.com.damiq.desktop.dominio.usuario;

/**
 * O que um usuário pode fazer na barragem dele (RF-01). Quem tem cada uma está em {@link Perfil#permite}; novas
 * permissões entram com os casos de uso que as exigem (cálculos, relatórios, emergência, cópia de segurança).
 */
public enum Permissao {
    /** Consultar leituras, alertas, notificações, cadastro da barragem e quem acionar no PAE. */
    CONSULTAR_DADOS,
    /** Digitar ou importar leituras (UC-03, UC-04). */
    REGISTRAR_LEITURAS,
    /** Reconhecer notificações de alerta (UC-06). */
    RECONHECER_NOTIFICACOES,
    /** Desbloquear usuários da barragem bloqueados por tentativas de acesso malsucedidas. */
    DESBLOQUEAR_USUARIOS,
    /** Apagar os dados de teste (UC-14). */
    APAGAR_DADOS_TESTE
}
