-- =============================================
-- V2__fase1_melhorias.sql
-- Fase 1: Roles, auditoria, campos novos
-- =============================================

-- ── Tabela usuario: roles e controle ─────────────────────────
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS nome_completo  VARCHAR(120);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS role           VARCHAR(20)  NOT NULL DEFAULT 'OPERADOR';
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS ativo          BOOLEAN      NOT NULL DEFAULT TRUE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS ultimo_login   TIMESTAMP;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS criado_em      TIMESTAMP    DEFAULT NOW();

-- ── Tabela funcionario: novos campos ─────────────────────────
ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS exige_sangue  BOOLEAN  NOT NULL DEFAULT TRUE;
ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS ativo         BOOLEAN  NOT NULL DEFAULT TRUE;
ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS criado_em     TIMESTAMP DEFAULT NOW();
ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS atualizado_em TIMESTAMP DEFAULT NOW();

-- ── Tabela agendamento: FK real + Enum + auditoria ───────────

-- FK para funcionario (nova coluna — convive com a matrícula string existente)
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS funcionario_id BIGINT REFERENCES funcionario(id);

-- Popula funcionario_id a partir da matrícula (migração dos dados existentes)
UPDATE agendamento a
SET funcionario_id = f.id
FROM funcionario f
WHERE a.funcionario_matricula = f.matricula
  AND a.funcionario_id IS NULL;

-- Tipo de exame como string (enum Java → string no banco)
-- A coluna tipo_exame já existe; precisamos padronizar os valores para o Enum
UPDATE agendamento SET tipo_exame = 'PERIODICO'           WHERE LOWER(tipo_exame) LIKE '%peri%';
UPDATE agendamento SET tipo_exame = 'ADMISSIONAL'         WHERE LOWER(tipo_exame) LIKE '%admiss%';
UPDATE agendamento SET tipo_exame = 'DEMISSIONAL'         WHERE LOWER(tipo_exame) LIKE '%demiss%';
UPDATE agendamento SET tipo_exame = 'RETORNO_AO_TRABALHO' WHERE LOWER(tipo_exame) LIKE '%retorno%';
UPDATE agendamento SET tipo_exame = 'MUDANCA_DE_RISCO'    WHERE LOWER(tipo_exame) LIKE '%mudan%';

-- Campos de auditoria
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS criado_em      TIMESTAMP DEFAULT NOW();
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS atualizado_em  TIMESTAMP DEFAULT NOW();
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS criado_por     VARCHAR(50);
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS atualizado_por VARCHAR(50);

-- ── Tabela de auditoria ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    usuario     VARCHAR(50)  NOT NULL,
    entidade    VARCHAR(50)  NOT NULL,
    entidade_id BIGINT,
    acao        VARCHAR(30)  NOT NULL,
    descricao   VARCHAR(500),
    dados_antes TEXT,
    dados_depois TEXT,
    ip_origem   VARCHAR(50),
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_entidade  ON audit_log(entidade, entidade_id);
CREATE INDEX IF NOT EXISTS idx_audit_usuario   ON audit_log(usuario);
CREATE INDEX IF NOT EXISTS idx_audit_criado_em ON audit_log(criado_em);

-- ── Índices adicionais ────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_ag_funcionario_id ON agendamento(funcionario_id);
CREATE INDEX IF NOT EXISTS idx_func_ativo        ON funcionario(ativo);
CREATE INDEX IF NOT EXISTS idx_usuario_role      ON usuario(role);
