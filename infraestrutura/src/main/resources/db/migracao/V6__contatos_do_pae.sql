-- Contatos do PAE de cada barragem (RF-02), vindos do cadastro da Central, com o nível de resposta a partir do
-- qual cada um é acionado (RF-06). A ordem é a de acionamento dentro de cada nível.
-- nivel_acionamento nulo: fora do fluxo de notificação (ex.: só recebe cópia do PAE). substitui: chave do
-- contato titular (o substituto herda o nível dele). meios: lista em JSON na ordem de preferência, ex.:
-- [{"tipo":"CELULAR","valor":"(62) 99999-0000"}].
CREATE TABLE barragem_contato (
    id                INTEGER PRIMARY KEY,
    barragem_id       TEXT    NOT NULL REFERENCES barragem (id),
    chave             TEXT    NOT NULL,
    papel             TEXT    NOT NULL CHECK (papel IN ('EQUIPE_TECNICA', 'COORDENADOR_PAE', 'EMPREENDEDOR',
                                  'ENTIDADE_FISCALIZADORA', 'DEFESA_CIVIL', 'CONSULTOR_EXTERNO', 'OUTRO')),
    entidade          TEXT    NOT NULL,
    responsavel       TEXT,
    cargo             TEXT,
    meios             TEXT    NOT NULL,
    nivel_acionamento INTEGER CHECK (nivel_acionamento IS NULL OR nivel_acionamento BETWEEN 1 AND 3),
    substitui         TEXT,
    recebe_copia_pae  INTEGER NOT NULL CHECK (recebe_copia_pae IN (0, 1)),
    ordem             INTEGER NOT NULL,
    criado_em         TEXT    NOT NULL,
    criado_por        INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em     TEXT    NOT NULL,
    atualizado_por    INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em       TEXT,
    teste             INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

CREATE UNIQUE INDEX ux_barragem_contato_chave ON barragem_contato (barragem_id, chave) WHERE excluido_em IS NULL;

CREATE TRIGGER tg_barragem_contato_exclusao_fisica BEFORE DELETE ON barragem_contato WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'barragem_contato: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;
