-- =============================================
-- V5__fix_serial_to_bigint.sql
-- Converte colunas id de SERIAL (INTEGER) para BIGINT
-- para alinhar com o mapeamento Long das entidades JPA.
-- =============================================

-- Dropa FKs que referenciam funcionario.id antes de alterar o tipo
ALTER TABLE agendamento    DROP CONSTRAINT IF EXISTS agendamento_funcionario_id_fkey;
ALTER TABLE medical_leaves DROP CONSTRAINT IF EXISTS medical_leaves_funcionario_id_fkey;
ALTER TABLE historico_cargo DROP CONSTRAINT IF EXISTS historico_cargo_funcionario_id_fkey;

-- Converte PKs de INTEGER para BIGINT
ALTER TABLE agendamento    ALTER COLUMN id TYPE BIGINT USING id::bigint;
ALTER TABLE funcionario    ALTER COLUMN id TYPE BIGINT USING id::bigint;
ALTER TABLE usuario        ALTER COLUMN id TYPE BIGINT USING id::bigint;
ALTER TABLE medical_leaves ALTER COLUMN id TYPE BIGINT USING id::bigint;

-- Converte FK de INTEGER para BIGINT
ALTER TABLE medical_leaves ALTER COLUMN funcionario_id TYPE BIGINT USING funcionario_id::bigint;

-- Recria as FKs
ALTER TABLE agendamento    ADD CONSTRAINT agendamento_funcionario_id_fkey
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id);
ALTER TABLE medical_leaves ADD CONSTRAINT medical_leaves_funcionario_id_fkey
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id);
ALTER TABLE historico_cargo ADD CONSTRAINT historico_cargo_funcionario_id_fkey
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id);
