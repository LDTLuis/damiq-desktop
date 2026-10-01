-- Resultado de cada chamada a processar_lote. Medições, rejeições, lacunas e alertas apontam para o
-- processamento, que registra a configuração aplicada (auditoria, RF-12).
CREATE TABLE processamento (
    id              INTEGER PRIMARY KEY,
    barragem_id     TEXT    NOT NULL REFERENCES barragem (id),
    configuracao_id INTEGER NOT NULL REFERENCES configuracao (id),
    executado_em    TEXT    NOT NULL,
    origem          TEXT    NOT NULL CHECK (origem IN ('DIGITACAO', 'IMPORTACAO')),
    arquivo         TEXT,
    status_barragem TEXT    NOT NULL CHECK (status_barragem IN ('OK', 'AVISO', 'ALERTA', 'CRITICO')),
    status_dados    TEXT    NOT NULL CHECK (status_dados IN ('OK', 'AVISO', 'ALERTA', 'CRITICO')),
    nivel_resposta  INTEGER NOT NULL CHECK (nivel_resposta BETWEEN 0 AND 3),
    recebidas       INTEGER NOT NULL,
    gravadas        INTEGER NOT NULL,
    ja_registradas  INTEGER NOT NULL,
    rejeitadas      INTEGER NOT NULL
) STRICT;

CREATE INDEX ix_processamento_barragem ON processamento (barragem_id, executado_em);

-- Medições normalizadas pelo motor (valor na unidade canônica do tipo). Uma leitura por instrumento e
-- momento: reimportar o mesmo arquivo não duplica. O índice único também atende a consulta do histórico.
CREATE TABLE medicao (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento (id),
    barragem_id      TEXT    NOT NULL REFERENCES barragem (id),
    instrumento      TEXT    NOT NULL,
    tipo             TEXT    NOT NULL CHECK (tipo IN ('NIVEL', 'PRESSAO', 'VAZAO', 'DESLOCAMENTO')),
    momento          TEXT    NOT NULL,
    fuso             TEXT    NOT NULL,
    valor            REAL    NOT NULL,
    valor_original   TEXT,
    unidade_original TEXT,
    flags            TEXT    NOT NULL,
    UNIQUE (barragem_id, instrumento, momento)
) STRICT;

-- Leituras recusadas, com os campos como foram informados, para o técnico corrigir.
CREATE TABLE rejeicao (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento (id),
    indice           INTEGER NOT NULL,
    codigo           TEXT    NOT NULL,
    mensagem         TEXT    NOT NULL,
    instrumento      TEXT,
    tipo             TEXT,
    momento          TEXT,
    valor            TEXT,
    unidade          TEXT
) STRICT;

CREATE TABLE lacuna (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento (id),
    barragem_id      TEXT    NOT NULL REFERENCES barragem (id),
    instrumento      TEXT    NOT NULL,
    inicio           TEXT    NOT NULL,
    fim              TEXT    NOT NULL,
    fuso             TEXT    NOT NULL,
    duracao_s        INTEGER NOT NULL
) STRICT;

-- Episódios de alerta. A versão da configuração vem do processamento.
CREATE TABLE alerta (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento (id),
    barragem_id      TEXT    NOT NULL REFERENCES barragem (id),
    instrumento      TEXT    NOT NULL,
    tipo             TEXT    NOT NULL,
    categoria        TEXT    NOT NULL CHECK (categoria IN ('SEGURANCA', 'QUALIDADE')),
    severidade       TEXT    NOT NULL CHECK (severidade IN ('OK', 'AVISO', 'ALERTA', 'CRITICO')),
    inicio           TEXT    NOT NULL,
    fim              TEXT    NOT NULL,
    fuso             TEXT    NOT NULL,
    leituras         INTEGER NOT NULL,
    valor_extremo    REAL    NOT NULL,
    unidade          TEXT    NOT NULL,
    limite           REAL,
    direcao          TEXT,
    leitura_suspeita INTEGER NOT NULL CHECK (leitura_suspeita IN (0, 1)),
    mensagem         TEXT    NOT NULL
) STRICT;

CREATE INDEX ix_alerta_barragem ON alerta (barragem_id, inicio);
