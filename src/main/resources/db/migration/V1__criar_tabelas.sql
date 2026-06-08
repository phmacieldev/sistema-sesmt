-- =============================================
-- V1__criar_tabelas.sql
-- Migration inicial - equivalente aos models.py
-- =============================================

-- Tabela de usuários (login)
CREATE TABLE IF NOT EXISTS usuario (
    id       SERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL
);

-- Tabela de funcionários
CREATE TABLE IF NOT EXISTS funcionario (
    id              SERIAL PRIMARY KEY,
    matricula       VARCHAR(20)  UNIQUE,
    nome            VARCHAR(120) NOT NULL,
    setor           VARCHAR(100),
    funcao          VARCHAR(100),
    email           VARCHAR(120),
    aso             DATE,
    estabelecimento VARCHAR(10)
);

-- Tabela de agendamentos
CREATE TABLE IF NOT EXISTS agendamento (
    id                     SERIAL PRIMARY KEY,
    funcionario_matricula  VARCHAR(20) REFERENCES funcionario(matricula),
    funcionario_nome       VARCHAR(120),
    funcionario_setor      VARCHAR(100),
    funcionario_funcao     VARCHAR(100),
    tipo_exame             VARCHAR(50),
    data_clinico           DATE,
    hora_clinico           VARCHAR(10),
    data_sangue            DATE,
    aso_enviado            BOOLEAN DEFAULT FALSE,
    aso_recebido           BOOLEAN DEFAULT FALSE,
    data_aso_anterior      DATE
);

-- Tabela de atestados médicos
CREATE TABLE IF NOT EXISTS medical_leaves (
    id                  SERIAL PRIMARY KEY,
    funcionario_id      INTEGER NOT NULL REFERENCES funcionario(id),
    data_afastamento    DATE    NOT NULL,
    dias_afastamento    INTEGER NOT NULL,
    motivo              VARCHAR(100),
    cid                 VARCHAR(10),
    medico_nome         VARCHAR(200),
    medico_crm          VARCHAR(50),
    data_lancamento     TIMESTAMP DEFAULT NOW()
);

-- Índices para buscas frequentes
CREATE INDEX IF NOT EXISTS idx_agendamento_data_clinico ON agendamento(data_clinico);
CREATE INDEX IF NOT EXISTS idx_agendamento_matricula    ON agendamento(funcionario_matricula);
CREATE INDEX IF NOT EXISTS idx_agendamento_nome         ON agendamento(funcionario_nome);
CREATE INDEX IF NOT EXISTS idx_funcionario_matricula    ON funcionario(matricula);
