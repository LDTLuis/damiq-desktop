-- Notificações de alerta no app (RF-06). Enquanto não reconhecida, há uma notificação aberta por barragem,
-- instrumento e tipo de alerta: novos episódios do mesmo problema somam ocorrências e, se mais graves,
-- elevam a severidade. O reconhecimento (quem, quando, observação) fica registrado para auditoria.
CREATE TABLE notificacao (
    id               INTEGER PRIMARY KEY,
    barragem_id      TEXT    NOT NULL REFERENCES barragem (id),
    instrumento      TEXT    NOT NULL,
    tipo_alerta      TEXT    NOT NULL,
    categoria        TEXT    NOT NULL CHECK (categoria IN ('SEGURANCA', 'QUALIDADE')),
    severidade       TEXT    NOT NULL CHECK (severidade IN ('OK', 'AVISO', 'ALERTA', 'CRITICO')),
    mensagem         TEXT    NOT NULL,
    leitura_suspeita INTEGER NOT NULL CHECK (leitura_suspeita IN (0, 1)),
    nivel_resposta   INTEGER NOT NULL CHECK (nivel_resposta BETWEEN 0 AND 3),
    ocorrencias      INTEGER NOT NULL,
    criada_em        TEXT    NOT NULL,
    atualizada_em    TEXT    NOT NULL,
    reconhecida_em   TEXT,
    reconhecida_por  TEXT,
    observacao       TEXT
) STRICT;

CREATE UNIQUE INDEX ux_notificacao_aberta
    ON notificacao (barragem_id, instrumento, tipo_alerta) WHERE reconhecida_em IS NULL;

-- Alerta ainda sem notificação = pendente. Assim nenhum alerta gravado fica sem notificar, mesmo se o app
-- fechar entre gravar o processamento e gerar as notificações.
ALTER TABLE alerta ADD COLUMN notificacao_id INTEGER REFERENCES notificacao (id);

CREATE INDEX ix_alerta_pendente ON alerta (barragem_id) WHERE notificacao_id IS NULL;
