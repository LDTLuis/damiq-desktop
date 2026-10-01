-- Cópia do cadastro de barragens feito na Central de Configurações (RF-02).
-- Os campos obrigatórios aceitam nulo no banco porque barragens anteriores ao cadastro (só identificação) já
-- existem; versao_cadastro nula = cadastro ainda não sincronizado. A obrigatoriedade é garantida ao receber
-- o cadastro da Central.
ALTER TABLE barragem ADD COLUMN versao_cadastro TEXT;
ALTER TABLE barragem ADD COLUMN empreendedor TEXT;
ALTER TABLE barragem ADD COLUMN finalidade TEXT;
ALTER TABLE barragem ADD COLUMN municipios TEXT; -- lista em JSON, ex.: ["Goiânia"]
ALTER TABLE barragem ADD COLUMN uf TEXT CHECK (uf IS NULL OR length(uf) = 2);
ALTER TABLE barragem ADD COLUMN latitude REAL CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90);
ALTER TABLE barragem ADD COLUMN longitude REAL CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);
ALTER TABLE barragem ADD COLUMN curso_dagua TEXT;
ALTER TABLE barragem ADD COLUMN tipo_macico TEXT;
ALTER TABLE barragem ADD COLUMN altura_macico_m REAL CHECK (altura_macico_m IS NULL OR altura_macico_m > 0);
ALTER TABLE barragem ADD COLUMN capacidade_total_m3 REAL CHECK (capacidade_total_m3 IS NULL OR capacidade_total_m3 > 0);

-- Grupos de campos de cada barragem (ex.: Maciço, Vertedouro), na ordem de exibição.
CREATE TABLE barragem_grupo (
    id             INTEGER PRIMARY KEY,
    barragem_id    TEXT    NOT NULL REFERENCES barragem (id),
    chave          TEXT    NOT NULL,
    nome           TEXT    NOT NULL,
    ordem          INTEGER NOT NULL,
    criado_em      TEXT    NOT NULL,
    criado_por     INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em  TEXT    NOT NULL,
    atualizado_por INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em    TEXT,
    teste          INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

CREATE UNIQUE INDEX ux_barragem_grupo_chave ON barragem_grupo (barragem_id, chave) WHERE excluido_em IS NULL;

-- Campos próprios de cada barragem (características e indicadores além dos obrigatórios), com o valor em
-- texto no formato canônico do tipo. padrao = 1: campo do catálogo da Central (mesma chave, mesmo
-- significado em toda barragem).
CREATE TABLE barragem_campo (
    id             INTEGER PRIMARY KEY,
    barragem_id    TEXT    NOT NULL REFERENCES barragem (id),
    grupo_id       INTEGER REFERENCES barragem_grupo (id),
    chave          TEXT    NOT NULL,
    rotulo         TEXT    NOT NULL,
    tipo           TEXT    NOT NULL CHECK (tipo IN ('NUMERO', 'TEXTO', 'DATA', 'BOOLEANO')),
    valor          TEXT    NOT NULL,
    unidade        TEXT,
    padrao         INTEGER NOT NULL CHECK (padrao IN (0, 1)),
    ordem          INTEGER NOT NULL,
    criado_em      TEXT    NOT NULL,
    criado_por     INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em  TEXT    NOT NULL,
    atualizado_por INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em    TEXT,
    teste          INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

CREATE UNIQUE INDEX ux_barragem_campo_chave ON barragem_campo (barragem_id, chave) WHERE excluido_em IS NULL;

CREATE TRIGGER tg_barragem_grupo_exclusao_fisica BEFORE DELETE ON barragem_grupo WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'barragem_grupo: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_barragem_campo_exclusao_fisica BEFORE DELETE ON barragem_campo WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'barragem_campo: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;
