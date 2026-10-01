-- Colunas de controle em todas as tabelas:
--   criado_em / criado_por        cadastro do registro
--   atualizado_em / atualizado_por última atualização
--   excluido_em                   exclusão lógica (nulo = ativo); quem excluiu fica em atualizado_por
--   teste                         1 = dado de teste, pode ser apagado fisicamente; herdado da barragem
-- Um gatilho por tabela impede apagar fisicamente registros com teste = 0.
--
-- O SQLite não altera restrições de tabelas existentes, e a migração roda numa transação (sem desligar as
-- chaves estrangeiras). Por isso cada tabela é recriada com o sufixo _novo, os dados são copiados, as antigas
-- são apagadas (filhas primeiro) e as novas renomeadas; o SQLite atualiza as referências ao renomear.
-- As unicidades viram índices parciais, que valem só para registros não excluídos.

-- Usuários. Até a autenticação (RF-01), as alterações são do usuário 1, "sistema".
CREATE TABLE usuario (
    id             INTEGER PRIMARY KEY,
    nome           TEXT    NOT NULL,
    criado_em      TEXT    NOT NULL,
    criado_por     INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em  TEXT    NOT NULL,
    atualizado_por INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em    TEXT,
    teste          INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO usuario (id, nome, criado_em, criado_por, atualizado_em, atualizado_por)
VALUES (1, 'sistema', strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 1, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 1);

-- barragem ----------------------------------------------------------------------------------------------
CREATE TABLE barragem_novo (
    id             TEXT    PRIMARY KEY,
    nome           TEXT    NOT NULL,
    criado_em      TEXT    NOT NULL,
    criado_por     INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em  TEXT    NOT NULL,
    atualizado_por INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em    TEXT,
    teste          INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO barragem_novo (id, nome, criado_em, criado_por, atualizado_em, atualizado_por)
SELECT id, nome, criada_em, 1, criada_em, 1 FROM barragem;

-- configuracao (recebida_em vira criado_em) -------------------------------------------------------------
CREATE TABLE configuracao_novo (
    id             INTEGER PRIMARY KEY,
    barragem_id    TEXT    NOT NULL REFERENCES barragem_novo (id),
    versao         TEXT    NOT NULL,
    conteudo_json  TEXT    NOT NULL,
    origem         TEXT    NOT NULL CHECK (origem IN ('ARQUIVO', 'CENTRAL')),
    vigente        INTEGER NOT NULL CHECK (vigente IN (0, 1)),
    criado_em      TEXT    NOT NULL,
    criado_por     INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em  TEXT    NOT NULL,
    atualizado_por INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em    TEXT,
    teste          INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO configuracao_novo (id, barragem_id, versao, conteudo_json, origem, vigente,
                               criado_em, criado_por, atualizado_em, atualizado_por)
SELECT id, barragem_id, versao, conteudo_json, origem, vigente, recebida_em, 1, recebida_em, 1 FROM configuracao;

-- processamento (executado_em vira criado_em) -----------------------------------------------------------
CREATE TABLE processamento_novo (
    id              INTEGER PRIMARY KEY,
    barragem_id     TEXT    NOT NULL REFERENCES barragem_novo (id),
    configuracao_id INTEGER NOT NULL REFERENCES configuracao_novo (id),
    origem          TEXT    NOT NULL CHECK (origem IN ('DIGITACAO', 'IMPORTACAO')),
    arquivo         TEXT,
    status_barragem TEXT    NOT NULL CHECK (status_barragem IN ('OK', 'AVISO', 'ALERTA', 'CRITICO')),
    status_dados    TEXT    NOT NULL CHECK (status_dados IN ('OK', 'AVISO', 'ALERTA', 'CRITICO')),
    nivel_resposta  INTEGER NOT NULL CHECK (nivel_resposta BETWEEN 0 AND 3),
    recebidas       INTEGER NOT NULL,
    gravadas        INTEGER NOT NULL,
    ja_registradas  INTEGER NOT NULL,
    rejeitadas      INTEGER NOT NULL,
    criado_em       TEXT    NOT NULL,
    criado_por      INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em   TEXT    NOT NULL,
    atualizado_por  INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em     TEXT,
    teste           INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO processamento_novo (id, barragem_id, configuracao_id, origem, arquivo, status_barragem, status_dados,
                                nivel_resposta, recebidas, gravadas, ja_registradas, rejeitadas,
                                criado_em, criado_por, atualizado_em, atualizado_por)
SELECT id, barragem_id, configuracao_id, origem, arquivo, status_barragem, status_dados,
       nivel_resposta, recebidas, gravadas, ja_registradas, rejeitadas, executado_em, 1, executado_em, 1
FROM processamento;

-- medicao -----------------------------------------------------------------------------------------------
CREATE TABLE medicao_novo (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento_novo (id),
    barragem_id      TEXT    NOT NULL REFERENCES barragem_novo (id),
    instrumento      TEXT    NOT NULL,
    tipo             TEXT    NOT NULL CHECK (tipo IN ('NIVEL', 'PRESSAO', 'VAZAO', 'DESLOCAMENTO')),
    momento          TEXT    NOT NULL,
    fuso             TEXT    NOT NULL,
    valor            REAL    NOT NULL,
    valor_original   TEXT,
    unidade_original TEXT,
    flags            TEXT    NOT NULL,
    criado_em        TEXT    NOT NULL,
    criado_por       INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em    TEXT    NOT NULL,
    atualizado_por   INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em      TEXT,
    teste            INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO medicao_novo (id, processamento_id, barragem_id, instrumento, tipo, momento, fuso, valor,
                          valor_original, unidade_original, flags, criado_em, criado_por, atualizado_em, atualizado_por)
SELECT m.id, m.processamento_id, m.barragem_id, m.instrumento, m.tipo, m.momento, m.fuso, m.valor,
       m.valor_original, m.unidade_original, m.flags, p.executado_em, 1, p.executado_em, 1
FROM medicao m JOIN processamento p ON p.id = m.processamento_id;

-- rejeicao ----------------------------------------------------------------------------------------------
CREATE TABLE rejeicao_novo (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento_novo (id),
    indice           INTEGER NOT NULL,
    codigo           TEXT    NOT NULL,
    mensagem         TEXT    NOT NULL,
    instrumento      TEXT,
    tipo             TEXT,
    momento          TEXT,
    valor            TEXT,
    unidade          TEXT,
    criado_em        TEXT    NOT NULL,
    criado_por       INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em    TEXT    NOT NULL,
    atualizado_por   INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em      TEXT,
    teste            INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO rejeicao_novo (id, processamento_id, indice, codigo, mensagem, instrumento, tipo, momento, valor, unidade,
                           criado_em, criado_por, atualizado_em, atualizado_por)
SELECT r.id, r.processamento_id, r.indice, r.codigo, r.mensagem, r.instrumento, r.tipo, r.momento, r.valor, r.unidade,
       p.executado_em, 1, p.executado_em, 1
FROM rejeicao r JOIN processamento p ON p.id = r.processamento_id;

-- lacuna ------------------------------------------------------------------------------------------------
CREATE TABLE lacuna_novo (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento_novo (id),
    barragem_id      TEXT    NOT NULL REFERENCES barragem_novo (id),
    instrumento      TEXT    NOT NULL,
    inicio           TEXT    NOT NULL,
    fim              TEXT    NOT NULL,
    fuso             TEXT    NOT NULL,
    duracao_s        INTEGER NOT NULL,
    criado_em        TEXT    NOT NULL,
    criado_por       INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em    TEXT    NOT NULL,
    atualizado_por   INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em      TEXT,
    teste            INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO lacuna_novo (id, processamento_id, barragem_id, instrumento, inicio, fim, fuso, duracao_s,
                         criado_em, criado_por, atualizado_em, atualizado_por)
SELECT l.id, l.processamento_id, l.barragem_id, l.instrumento, l.inicio, l.fim, l.fuso, l.duracao_s,
       p.executado_em, 1, p.executado_em, 1
FROM lacuna l JOIN processamento p ON p.id = l.processamento_id;

-- notificacao (criada_em/atualizada_em viram criado_em/atualizado_em) ------------------------------------
CREATE TABLE notificacao_novo (
    id               INTEGER PRIMARY KEY,
    barragem_id      TEXT    NOT NULL REFERENCES barragem_novo (id),
    instrumento      TEXT    NOT NULL,
    tipo_alerta      TEXT    NOT NULL,
    categoria        TEXT    NOT NULL CHECK (categoria IN ('SEGURANCA', 'QUALIDADE')),
    severidade       TEXT    NOT NULL CHECK (severidade IN ('OK', 'AVISO', 'ALERTA', 'CRITICO')),
    mensagem         TEXT    NOT NULL,
    leitura_suspeita INTEGER NOT NULL CHECK (leitura_suspeita IN (0, 1)),
    nivel_resposta   INTEGER NOT NULL CHECK (nivel_resposta BETWEEN 0 AND 3),
    ocorrencias      INTEGER NOT NULL,
    reconhecida_em   TEXT,
    reconhecida_por  TEXT,
    observacao       TEXT,
    criado_em        TEXT    NOT NULL,
    criado_por       INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em    TEXT    NOT NULL,
    atualizado_por   INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em      TEXT,
    teste            INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO notificacao_novo (id, barragem_id, instrumento, tipo_alerta, categoria, severidade, mensagem,
                              leitura_suspeita, nivel_resposta, ocorrencias, reconhecida_em, reconhecida_por,
                              observacao, criado_em, criado_por, atualizado_em, atualizado_por)
SELECT id, barragem_id, instrumento, tipo_alerta, categoria, severidade, mensagem, leitura_suspeita, nivel_resposta,
       ocorrencias, reconhecida_em, reconhecida_por, observacao, criada_em, 1, atualizada_em, 1
FROM notificacao;

-- alerta ------------------------------------------------------------------------------------------------
CREATE TABLE alerta_novo (
    id               INTEGER PRIMARY KEY,
    processamento_id INTEGER NOT NULL REFERENCES processamento_novo (id),
    barragem_id      TEXT    NOT NULL REFERENCES barragem_novo (id),
    notificacao_id   INTEGER REFERENCES notificacao_novo (id),
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
    mensagem         TEXT    NOT NULL,
    criado_em        TEXT    NOT NULL,
    criado_por       INTEGER NOT NULL REFERENCES usuario (id),
    atualizado_em    TEXT    NOT NULL,
    atualizado_por   INTEGER NOT NULL REFERENCES usuario (id),
    excluido_em      TEXT,
    teste            INTEGER NOT NULL DEFAULT 0 CHECK (teste IN (0, 1))
) STRICT;

INSERT INTO alerta_novo (id, processamento_id, barragem_id, notificacao_id, instrumento, tipo, categoria, severidade,
                         inicio, fim, fuso, leituras, valor_extremo, unidade, limite, direcao, leitura_suspeita,
                         mensagem, criado_em, criado_por, atualizado_em, atualizado_por)
SELECT a.id, a.processamento_id, a.barragem_id, a.notificacao_id, a.instrumento, a.tipo, a.categoria, a.severidade,
       a.inicio, a.fim, a.fuso, a.leituras, a.valor_extremo, a.unidade, a.limite, a.direcao, a.leitura_suspeita,
       a.mensagem, p.executado_em, 1, p.executado_em, 1
FROM alerta a JOIN processamento p ON p.id = a.processamento_id;

-- troca das tabelas: apaga as antigas (filhas primeiro) e renomeia as novas -------------------------------
DROP TABLE alerta;
DROP TABLE rejeicao;
DROP TABLE lacuna;
DROP TABLE medicao;
DROP TABLE notificacao;
DROP TABLE processamento;
DROP TABLE configuracao;
DROP TABLE barragem;

ALTER TABLE barragem_novo RENAME TO barragem;
ALTER TABLE configuracao_novo RENAME TO configuracao;
ALTER TABLE processamento_novo RENAME TO processamento;
ALTER TABLE medicao_novo RENAME TO medicao;
ALTER TABLE rejeicao_novo RENAME TO rejeicao;
ALTER TABLE lacuna_novo RENAME TO lacuna;
ALTER TABLE notificacao_novo RENAME TO notificacao;
ALTER TABLE alerta_novo RENAME TO alerta;

-- índices (as unicidades valem só para registros não excluídos) ------------------------------------------
CREATE UNIQUE INDEX ux_configuracao_versao ON configuracao (barragem_id, versao) WHERE excluido_em IS NULL;
CREATE UNIQUE INDEX ux_configuracao_vigente ON configuracao (barragem_id) WHERE vigente = 1 AND excluido_em IS NULL;
CREATE INDEX ix_processamento_barragem ON processamento (barragem_id, criado_em);
CREATE UNIQUE INDEX ux_medicao_momento ON medicao (barragem_id, instrumento, momento) WHERE excluido_em IS NULL;
CREATE INDEX ix_alerta_barragem ON alerta (barragem_id, inicio);
CREATE INDEX ix_alerta_pendente ON alerta (barragem_id) WHERE notificacao_id IS NULL AND excluido_em IS NULL;
CREATE UNIQUE INDEX ux_notificacao_aberta ON notificacao (barragem_id, instrumento, tipo_alerta)
    WHERE reconhecida_em IS NULL AND excluido_em IS NULL;

-- exclusão física só de dados de teste ---------------------------------------------------------------------
CREATE TRIGGER tg_usuario_exclusao_fisica BEFORE DELETE ON usuario WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'usuario: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_barragem_exclusao_fisica BEFORE DELETE ON barragem WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'barragem: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_configuracao_exclusao_fisica BEFORE DELETE ON configuracao WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'configuracao: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_processamento_exclusao_fisica BEFORE DELETE ON processamento WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'processamento: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_medicao_exclusao_fisica BEFORE DELETE ON medicao WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'medicao: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_rejeicao_exclusao_fisica BEFORE DELETE ON rejeicao WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'rejeicao: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_lacuna_exclusao_fisica BEFORE DELETE ON lacuna WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'lacuna: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_notificacao_exclusao_fisica BEFORE DELETE ON notificacao WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'notificacao: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;

CREATE TRIGGER tg_alerta_exclusao_fisica BEFORE DELETE ON alerta WHEN OLD.teste = 0
BEGIN SELECT RAISE(ABORT, 'alerta: registro real não pode ser apagado; use a exclusão lógica (excluido_em)'); END;
