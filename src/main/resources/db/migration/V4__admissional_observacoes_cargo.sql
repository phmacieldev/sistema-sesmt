-- =============================================
-- V4__admissional_observacoes_cargo.sql
-- =============================================

-- ── Observações no agendamento ────────────────
ALTER TABLE agendamento
    ADD COLUMN IF NOT EXISTS observacoes TEXT;

-- ── Admissional sem matrícula ─────────────────
-- Funcionários pré-cadastrados antes de entrar no sistema
-- status: ATIVO, PRE_ADMISSIONAL, DESLIGADO
ALTER TABLE funcionario
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';

-- ── Histórico de cargo/função (mudança de risco) ──
-- Toda alteração de cargo/função fica registrada aqui
CREATE TABLE IF NOT EXISTS historico_cargo (
    id              BIGSERIAL PRIMARY KEY,
    funcionario_id  BIGINT       NOT NULL REFERENCES funcionario(id),
    cargo_anterior  VARCHAR(100),
    cargo_novo      VARCHAR(100),
    setor_anterior  VARCHAR(100),
    setor_novo      VARCHAR(100),
    motivo          VARCHAR(200),  -- ex: 'MUDANCA_DE_RISCO', 'REVERSAO', 'TRANSFERENCIA'
    alterado_por    VARCHAR(50),
    alterado_em     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hist_cargo_func ON historico_cargo(funcionario_id);

-- ── Índice para status do funcionário ─────────
CREATE INDEX IF NOT EXISTS idx_func_status ON funcionario(status);
