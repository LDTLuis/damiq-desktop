package br.com.damiq.desktop.dominio.usuario;

import java.util.EnumSet;
import java.util.Set;

/**
 * Perfil de acesso do usuário (RF-01), definido pelo administrador na Central. Cada perfil tem um conjunto fixo
 * de permissões, conforme os atores dos casos de uso.
 */
public enum Perfil {
    /** Gestão dos usuários e da instalação: desbloqueio, dados de teste, cópia de segurança. */
    ADMINISTRADOR(EnumSet.of(Permissao.CONSULTAR_DADOS, Permissao.DESBLOQUEAR_USUARIOS, Permissao.APAGAR_DADOS_TESTE)),
    /** Responsável técnico: acompanha e reconhece alertas; assina laudos (exige registro profissional). */
    ENGENHEIRO(EnumSet.of(
            Permissao.CONSULTAR_DADOS, Permissao.REGISTRAR_LEITURAS, Permissao.RECONHECER_NOTIFICACOES)),
    /** Faz as leituras em campo e as registra no sistema. */
    TECNICO_CAMPO(EnumSet.of(Permissao.CONSULTAR_DADOS, Permissao.REGISTRAR_LEITURAS));

    private final Set<Permissao> permissoes;

    Perfil(Set<Permissao> permissoes) {
        this.permissoes = permissoes;
    }

    public boolean permite(Permissao permissao) {
        return permissoes.contains(permissao);
    }

    /** Perfis que assinam documentos técnicos e por isso precisam de registro profissional (CREA). */
    public boolean exigeRegistroProfissional() {
        return this == ENGENHEIRO;
    }
}
