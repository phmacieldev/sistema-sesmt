# PGEO — Sistema de Gestão de Saúde Ocupacional (SESMT)

Sistema web para controle de agendamentos de exames clínicos (ASO), atestados médicos e gestão de funcionários do SESMT.

---

## Funcionalidades

### Agendamentos
- Dashboard com filtros por mês, funcionário, estabelecimento e período personalizado
- Agenda semanal visual com todos os horários e navegação com setas
- Criação, edição e exclusão de agendamentos
- Controle de status ASO (enviado / recebido) com atualização em tempo real via WebSocket
- Export para Excel com todos os filtros ativos
- Limite de 10 exames de sangue por dia (validação automática)
- **Guia PDF por agendamento** — guia completa, guia de sangue e guia clínico Proteus (botões individuais por linha)
- Badges coloridos por tipo de exame (Periódico, Admissional, Demissional, Retorno, Mudança de Risco)
- Pré-preenchimento automático do formulário ao agendar a partir de ASO vencido

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
- Lista de ASOs vencidos com nome, setor, função, estabelecimento e data — botão Agendar pré-preenche o formulário
- Próximos agendamentos (14 dias) e atestados recentes

### Estatísticas de Exames
- Dashboard completo com 4 gráficos (exames por mês, por tipo, por setor, por estabelecimento)
- KPIs para todos os tipos de exame
- Filtro por ano com aplicação automática (sem botão confirmar)

### Funcionários
- Cards de KPI no topo: Total, Ativos, Pré-admissional, Desligados

### Admin
- Gerenciamento de usuários (criar, editar, redefinir senha, ativar/desativar)
- Auditoria completa de ações (criação, edição, exclusão, login)
- Controle de papéis (ADMIN / OPERADOR)

### Geral
- Tema claro / escuro com persistência por localStorage — ícone sincronizado corretamente no reload
- Navbar responsiva com dropdown estável (hover gap corrigido)
- Notificações em tempo real via WebSocket — banner "X atualizações pendentes" sem interromper o usuário
- Optimistic locking em todas as entidades principais (conflito de edição simultânea detectado e informado)
- CSP (Content Security Policy) com nonce por requisição
- Rate limiting no login (proteção contra força bruta)
- Sessão expira após 4 horas de inatividade (prod)
- Rodapé com copyright em todas as páginas
- Agenda com navegação Anterior/Próximo em português e botões FullCalendar corrigidos
- **Direitos autorais:** cabeçalho de copyright em todos os arquivos fonte (`.java`, `.css`, `.js`, `.html`) e arquivo `LICENSE` com licença de uso institucional

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

### Opção 1 — Railway (ambiente de produção/testes atual)

1. Criar o projeto no Railway a partir deste repositório (build automático via `Dockerfile`).
2. Adicionar o plugin **PostgreSQL** do Railway ao projeto.
3. No serviço da aplicação, configurar as variáveis de ambiente (referenciando as do plugin do Postgres quando possível):
   ```
   DB_HOST=${{Postgres.PGHOST}}
   DB_PORT=${{Postgres.PGPORT}}
   DB_NAME=${{Postgres.PGDATABASE}}
   DB_USER=${{Postgres.PGUSER}}
   DB_PASSWORD=${{Postgres.PGPASSWORD}}
   SECRET_KEY=<gerar com: openssl rand -hex 32>
   ADMIN_PASSWORD=<senha forte>
   OPERADOR_PASSWORD=<senha forte>
   VISUALIZADOR_PASSWORD=<senha forte>
   ```
4. O deploy automático (após o CI passar no `main`) é feito pelo workflow `.github/workflows/deploy.yml` via Railway CLI — requer o secret `RAILWAY_TOKEN` no GitHub (ver comentário no início do arquivo).
5. Domínio HTTPS é gerado automaticamente pelo Railway (Settings → Networking → Generate Domain).

> No plano free/trial do Railway o app não hiberna por inatividade (diferente do Render), mas o uso é medido — para manter algo rodando continuamente em teste, o plano Hobby (uso avulso, ~US$5/mês) evita ficar sem créditos no meio do teste.

---

### Opção 2 — Docker (VPS / servidor on-premise)

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

### Opção 3 — Desenvolvimento local (IDE)

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

Criadas automaticamente no primeiro boot (só quando a tabela de usuários está vazia):

| Usuário | Senha (padrão dev, sem env var) | Papel |
|---|---|---|
| `admin` | `admin@123` | ADMIN |
| `operador` | `oper@123` | OPERADOR |
| `visualizador` | `view@123` | VISUALIZADOR |

Em produção, defina `ADMIN_PASSWORD`, `OPERADOR_PASSWORD` e `VISUALIZADOR_PASSWORD` (ver `.env.example`) para não subir com as senhas padrão.

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
