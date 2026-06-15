-- Rastreamento de envio das guias (sangue e clínico) e exames solicitados no sangue
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS guia_sangue_enviada  BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS guia_clinico_enviada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE agendamento ADD COLUMN IF NOT EXISTS exames_sangue        TEXT;
