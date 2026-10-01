-- Convenções: datas em texto ISO-8601 UTC com milissegundos ('2026-09-29T14:00:00.000Z'), que ordena
-- cronologicamente; booleanos em INTEGER 0/1; valores fixos em TEXT com CHECK; JSON em TEXT.

-- Identificação mínima; o cadastro completo (RF-02) entra numa migração própria.
CREATE TABLE barragem (
    id        TEXT PRIMARY KEY,
    nome      TEXT NOT NULL,
    criada_em TEXT NOT NULL
) STRICT;

-- Configurações recebidas da Central (ou de arquivo local), já validadas pelo motor.
-- As anteriores ficam com vigente = 0 e formam o histórico (auditoria, RF-12).
CREATE TABLE configuracao (
    id            INTEGER PRIMARY KEY,
    barragem_id   TEXT    NOT NULL REFERENCES barragem (id),
    versao        TEXT    NOT NULL,
    conteudo_json TEXT    NOT NULL,
    origem        TEXT    NOT NULL CHECK (origem IN ('ARQUIVO', 'CENTRAL')),
    recebida_em   TEXT    NOT NULL,
    vigente       INTEGER NOT NULL CHECK (vigente IN (0, 1)),
    UNIQUE (barragem_id, versao)
) STRICT;

-- No máximo uma configuração vigente por barragem.
CREATE UNIQUE INDEX ux_configuracao_vigente ON configuracao (barragem_id) WHERE vigente = 1;
