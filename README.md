# PGEO — Sistema de Gestão de Saúde Ocupacional (SESMT)

Sistema web para controle de agendamentos de exames clínicos (ASO), atestados médicos e gestão de funcionários do SESMT.

---

## Funcionalidades

### Agendamentos
- Dashboard com filtros por mês, funcionário, estabelecimento e período personalizado
- Agenda semanal visual com todos os horários
- Criação, edição e exclusão de agendamentos
- Controle de status ASO (enviado / recebido) com atualização em tempo real via WebSocket
- Export para Excel com todos os filtros ativos
- Limite de 10 exames de sangue por dia (validação automática)
- Guia PDF por agendamento
- Badges coloridos por tipo de exame (Periódico, Admissional, Demissional, Retorno, Mudança de Risco)

### Atestados Médicos
- Lançamento semanal com navegação por semanas (terça a segunda)
- Busca global por nome em todos os períodos (live search sem botão)
- Totais por setor e tipo de afastamento
- Indicadores de absenteísmo dos últimos 60 dias com alerta de risco INSS (≥ 15 dias)
- Export PDF do relatório semanal

### Funcionários
- Lista paginada com busca ao vivo
- Perfil completo com histórico de cargos, agendamentos e atestados
- Edição inline de dados cadastrais (Admin)
- Alteração de cargo/setor com registro de histórico para auditoria
- Efetivar pré-admissional com matrícula

### Dashboard Sangue
- Controle de agendamentos de exame de sangue por mês
- Limite diário configurável

### Dashboard ASO
- Lista de exames por status (em dia / a vencer / vencidos) com filtros
- Export Excel

### Início
- KPIs: exames do mês, ASOs vencidos, atestados dos últimos 60 dias
- Atalhos rápidos para todas as seções
- Lista de ASOs vencidos com nome, setor, função, estabelecimento e data
- Próximos agendamentos (14 dias) e atestados recentes

### Admin
- Gerenciamento de usuários (criar, editar, redefinir senha, ativar/desativar)
- Auditoria completa de ações (criação, edição, exclusão, login)
- Controle de papéis (ADMIN / OPERADOR)

### Geral
- Tema claro / escuro com persistência por cookie
- Navbar responsiva com compactação progressiva e min-width para zoom
- Notificações em tempo real via WebSocket — banner "X atualizações pendentes" sem interromper o usuário
- Optimistic locking em todas as entidades principais (conflito de edição simultânea detectado e informado)
- CSP (Content Security Policy) com nonce por requisição
- Rate limiting no login (proteção contra força bruta)
- Sessão expira após 4 horas de inatividade (prod)

---

## Stack

| Componente | Tecnologia |
|---|---|
| Framework | Spring Boot 3.3 |
| ORM | Spring Data JPA / Hibernate 6 |
| Migrations | Flyway |
| Templates | Thymeleaf 3.1 |
| Autenticação | Spring Security 6 |
| PDF | iText 5 |
| Excel | Apache POI |
| Banco | PostgreSQL 16 |
| WebSocket | STOMP over SockJS |
| Deploy | Docker + nginx (proxy reverso SSL) |
| Java | 21 |

---

## Como rodar

### Opção 1 — Docker (recomendado para produção)

**Pré-requisitos:** Docker + Docker Compose

```bash
# 1. Clone o repositório
git clone <repo-url>
cd sistema-sesmt

# 2. Copie e configure o .env
cp .env.example .env
# Edite o .env com suas credenciais e SECRET_KEY

# 3. Gere os certificados TLS (auto-assinados para uso interno)
cd nginx && ./gen-self-signed.sh && cd ..

# 4. Suba toda a stack
docker compose up -d --build

# 5. Acesse
# https://localhost  (via nginx)
```

**Logs:**
```bash
docker compose logs -f app   # logs da aplicação
docker compose logs -f db    # logs do banco
```

**Parar / reiniciar:**
```bash
docker compose down
docker compose restart app
```

---

### Opção 2 — Desenvolvimento local (IDE)

**Pré-requisitos:** Java 21, Maven 3.9+, PostgreSQL 16 local ou via Docker

```bash
# 1. Suba só o banco (se não tiver PostgreSQL local)
docker compose up -d db

# 2. Rode com perfil local
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Acesse: http://localhost:8080

---

## Credenciais padrão

| Usuário | Senha | Papel |
|---|---|---|
| `admin` | `admin123` | ADMIN |

> **Troque a senha no primeiro acesso em:** Navbar → 🔑 → Alterar senha

---

## Variáveis de ambiente (.env)

```env
# Banco
DB_NAME=pgeo_db
DB_USER=pgeo_user
DB_PASSWORD=senha-segura

# Segurança
SECRET_KEY=chave-aleatoria-longa-e-segura

# E-mail (notificações de ASO — opcional)
SMTP_HOST=smtp.hospital.local
SMTP_PORT=587
SMTP_USER=sesmt@hospital.local
SMTP_PASSWORD=senha
SMTP_AUTH=true
SMTP_TLS=true
NOTIFICACAO_EMAIL_HABILITADO=true
NOTIFICACAO_EMAIL_DESTINATARIO=sesmt@hospital.local
NOTIFICACAO_EMAIL_REMETENTE=pgeo@noreply.local
NOTIFICACAO_EMAIL_DIAS_AVISO=30
```

---

## Migrations

O Flyway gerencia o schema automaticamente ao iniciar a aplicação.  
Migrations ficam em `src/main/resources/db/migration/` e seguem o padrão `V{n}__{descricao}.sql`.

Para adicionar uma migration manualmente:
```bash
# Conecte no banco e execute o SQL, ou crie o arquivo V{n}__ e reinicie a app
```

---

## Estrutura de pastas (simplificada)

```
src/main/java/com/sesmt/pgeo/
├── config/          → SecurityConfig, DataInitializer
├── controller/      → Agendamentos, Atestados, Funcionarios, Admin, Home, ...
├── model/           → Agendamento, Funcionario, MedicalLeave, Usuario, HistoricoCargo
├── repository/      → Spring Data JPA + Specifications
├── service/         → Regras de negócio, PDF, Excel, E-mail, WebSocket
├── websocket/       → Notificações em tempo real (STOMP/SockJS)
├── audit/           → AuditLog + AuditService
└── security/        → CSP nonce filter, rate limit filter

src/main/resources/
├── db/migration/    → V1 a V7 (Flyway)
├── templates/       → Thymeleaf (dashboard, agenda, atestados, funcionario, admin, ...)
└── static/
    ├── css/global.css
    └── js/           → theme.js, websocket.js, agenda.js
```

---

## Perfis Spring

| Perfil | Uso | ddl-auto |
|---|---|---|
| `prod` (padrão) | Docker / servidor | `validate` |
| `local` | IDE com banco Docker local | `validate` |
| `dev` | Desenvolvimento com reset do banco | `create-drop` |
