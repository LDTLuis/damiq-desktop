-- Autenticação e perfis (RF-01). Os usuários são cadastrados na Central pelo administrador; o Desktop guarda
-- uma cópia para conferir a senha sem rede. Cada usuário pertence a uma única barragem (barragem_id), vazia só
-- no usuário 1, "sistema", que não entra no sistema. CPF não é coletado (LGPD, princípio da necessidade).
--
-- Todas as tabelas referenciam usuario, por isso ela não é recriada: as colunas entram com ALTER TABLE e as
-- regras que envolvem mais de uma coluna ficam em gatilhos.
--   versao_cadastro        revisão do cadastro na Central; a cópia só muda quando ela muda
--   senha_hash             bcrypt; a senha nunca é guardada
--   registro_profissional  CREA; obrigatório para ENGENHEIRO
--   bloqueado              suspenso pelo administrador ou por tentativas malsucedidas
--   tentativas_falhas      tentativas malsucedidas seguidas neste aparelho
--   trocar_senha           obriga a troca da senha no próximo acesso
--   ultimo_acesso          último acesso bem-sucedido neste aparelho (UTC)
ALTER TABLE usuario ADD COLUMN barragem_id TEXT REFERENCES barragem (id);
ALTER TABLE usuario ADD COLUMN login TEXT;
ALTER TABLE usuario ADD COLUMN versao_cadastro TEXT;
ALTER TABLE usuario ADD COLUMN senha_hash TEXT;
ALTER TABLE usuario ADD COLUMN perfil TEXT CHECK (perfil IS NULL OR perfil IN ('ADMINISTRADOR', 'ENGENHEIRO', 'TECNICO_CAMPO'));
ALTER TABLE usuario ADD COLUMN email TEXT;
ALTER TABLE usuario ADD COLUMN telefone TEXT;
ALTER TABLE usuario ADD COLUMN cargo TEXT;
ALTER TABLE usuario ADD COLUMN registro_profissional TEXT;
ALTER TABLE usuario ADD COLUMN bloqueado INTEGER NOT NULL DEFAULT 0 CHECK (bloqueado IN (0, 1));
ALTER TABLE usuario ADD COLUMN tentativas_falhas INTEGER NOT NULL DEFAULT 0 CHECK (tentativas_falhas >= 0);
ALTER TABLE usuario ADD COLUMN trocar_senha INTEGER NOT NULL DEFAULT 0 CHECK (trocar_senha IN (0, 1));
ALTER TABLE usuario ADD COLUMN ultimo_acesso TEXT;

CREATE UNIQUE INDEX ux_usuario_login ON usuario (login) WHERE excluido_em IS NULL;
CREATE INDEX ix_usuario_barragem ON usuario (barragem_id);

-- O "sistema" não tem barragem nem acesso; os demais têm barragem, login, senha, perfil e e-mail, e o
-- engenheiro, registro profissional.
CREATE TRIGGER tg_usuario_cadastro_inclusao BEFORE INSERT ON usuario
WHEN (NEW.id IS 1 AND NEW.barragem_id IS NOT NULL)
  OR (NEW.id IS NOT 1 AND (NEW.barragem_id IS NULL OR NEW.login IS NULL OR NEW.senha_hash IS NULL
      OR NEW.perfil IS NULL OR NEW.email IS NULL
      OR (NEW.perfil = 'ENGENHEIRO' AND NEW.registro_profissional IS NULL)))
BEGIN SELECT RAISE(ABORT, 'usuario: barragem, login, senha, perfil, e-mail (e CREA do engenheiro) são obrigatórios; o sistema não tem barragem'); END;

CREATE TRIGGER tg_usuario_cadastro_alteracao BEFORE UPDATE ON usuario
WHEN (NEW.id = 1 AND NEW.barragem_id IS NOT NULL)
  OR (NEW.id <> 1 AND (NEW.barragem_id IS NULL OR NEW.login IS NULL OR NEW.senha_hash IS NULL
      OR NEW.perfil IS NULL OR NEW.email IS NULL
      OR (NEW.perfil = 'ENGENHEIRO' AND NEW.registro_profissional IS NULL)))
BEGIN SELECT RAISE(ABORT, 'usuario: barragem, login, senha, perfil, e-mail (e CREA do engenheiro) são obrigatórios; o sistema não tem barragem'); END;
