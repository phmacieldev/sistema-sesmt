-- Optimistic locking: coluna version para controle de edições simultâneas
ALTER TABLE agendamento    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE funcionario    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE medical_leaves ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
