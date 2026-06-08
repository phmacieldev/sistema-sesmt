-- =============================================
-- V3__dashboard_sangue.sql
-- Índice para o dashboard de sangue e
-- ajuste na coluna tipo_exame para o Enum
-- =============================================

-- Índice na data de sangue (nova query do dashboard)
CREATE INDEX IF NOT EXISTS idx_ag_data_sangue ON agendamento(data_sangue)
    WHERE data_sangue IS NOT NULL;

-- Garante que tipo_exame aceita os novos valores do Enum
-- (os dados foram migrados na V2, este só adiciona o índice)
CREATE INDEX IF NOT EXISTS idx_ag_tipo_exame ON agendamento(tipo_exame);

-- Índice para o filtro por mês/ano de sangue (dashboard_sangue)
-- PostgreSQL pode usar o índice de data_sangue com EXTRACT — só documentando
COMMENT ON COLUMN agendamento.data_sangue IS 'Data do exame laboratorial. NULL = cargo não exige sangue. Máximo 5 por dia.';
COMMENT ON COLUMN agendamento.tipo_exame  IS 'Enum: PERIODICO, ADMISSIONAL, DEMISSIONAL, RETORNO_AO_TRABALHO, MUDANCA_DE_RISCO';
COMMENT ON COLUMN funcionario.exige_sangue IS 'Se false, o campo data_sangue pode ser omitido no agendamento';
