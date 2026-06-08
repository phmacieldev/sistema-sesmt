# PGEO — Sistema de Exames Ocupacionais (Java / Spring Boot)

Migração do projeto Python/Flask para Java/Spring Boot.  
Sistema de agendamento de exames clínicos (ASO) do SESMT.

---

## Stack

| Componente | Python (original) | Java (novo) |
|---|---|---|
| Framework web | Flask | Spring Boot 3 |
| ORM | SQLAlchemy | Spring Data JPA (Hibernate) |
| Migrations | Alembic / Flask-Migrate | Flyway |
| Templates | Jinja2 | Thymeleaf |
| Autenticação | Flask-Login | Spring Security |
| PDF | ReportLab | iText 5 |
| Excel | pandas + openpyxl | Apache POI |
| Banco | SQLite / PostgreSQL | PostgreSQL |

---

## Estrutura do Projeto

```
src/main/java/com/sesmt/pgeo/
├── PgeoApplication.java          ← app.py (ponto de entrada)
├── config/
│   ├── SecurityConfig.java       ← Flask-Login + @login_required
│   └── DataSeeder.java           ← cria usuário padrão na 1ª execução
├── model/
│   ├── Agendamento.java          ← class Agendamento(db.Model)
│   ├── Funcionario.java          ← class Funcionario(db.Model)
│   ├── MedicalLeave.java         ← class MedicalLeave(db.Model)
│   └── Usuario.java              ← class Usuario(UserMixin, db.Model)
├── repository/
│   ├── AgendamentoRepository.java ← Agendamento.query.*
│   ├── FuncionarioRepository.java ← Funcionario.query.*
│   ├── MedicalLeaveRepository.java
│   └── UsuarioRepository.java
├── service/
│   ├── AgendamentoService.java   ← lógica de agenda_routes.py
│   ├── AsoService.java           ← lógica de aso_routes.py
│   ├── FuncionarioImportService.java ← importar_funcionarios_excel_banco()
│   └── PdfService.java           ← utils/pdf_generator.py
└── controller/
    ├── AgendaController.java     ← Blueprint agenda_bp
    ├── AsoController.java        ← Blueprint aso_bp
    ├── AuthController.java       ← Blueprint auth_bp
    └── DashboardController.java  ← Blueprint dashboard_bp

src/main/resources/
├── application.properties        ← config.py + .env
├── db/migration/
│   └── V1__criar_tabelas.sql     ← migrations do Alembic
├── templates/                    ← equivalente a /templates do Flask
│   ├── login.html
│   ├── agenda.html
│   ├── agendar.html
│   ├── editar_agendamento.html
│   ├── dashboard.html
│   ├── dashboard_exames.html
│   ├── dashboard_estatisticas.html
│   ├── indicadores_atestados.html
│   ├── _tabela_agendamentos.html
│   └── components/
│       ├── topo.html
│       └── modal_agendamento.html
└── static/
    ├── css/agenda.css
    └── js/agenda.js
```

---

## Como rodar

### 1. Pré-requisitos
- Java 21+
- Maven 3.9+
- PostgreSQL 14+

### 2. Criar o banco
```sql
CREATE DATABASE pgeo_db;
```

### 3. Configurar variáveis de ambiente
```bash
cp .env.example .env
# Edite o .env com suas credenciais
```

Para o Spring Boot ler o `.env`, você pode:
- Exportar as variáveis: `export $(cat .env | xargs)`
- Ou definir diretamente em `application.properties`

### 4. Rodar
```bash
mvn spring-boot:run
```

Acesse: http://localhost:8080  
Login padrão: **admin / admin123** (troque após o primeiro acesso!)

### 5. Importar funcionários
- No dashboard, clique em **Sincronizar Excel**
- Selecione o arquivo `.xlsx` com as colunas:
  `matrícula | nome | setor | função | email | aso | estabelecimento`

---

## Diferenças principais Flask → Spring Boot

### Rotas
```python
# Flask
@agenda_bp.route("/agenda")
def agenda():
    return render_template("agenda.html")
```
```java
// Spring Boot
@GetMapping("/agenda")
public String agenda(Model model) {
    return "agenda"; // resolve para templates/agenda.html
}
```

### Templates
```html
<!-- Jinja2 -->
{% for a in agendamentos %}
  {{ a.funcionario_nome }}
{% endfor %}

<!-- Thymeleaf -->
<tr th:each="a : ${agendamentos}">
  <td th:text="${a.funcionarioNome}"></td>
</tr>
```

### Queries
```python
# SQLAlchemy
Agendamento.query.filter_by(data_clinico=data).all()
```
```java
// Spring Data JPA
agendamentoRepo.findByDataClinico(data);
// ou com @Query para consultas mais complexas
```

### CSRF
- Flask-WTF: precisa configurar manualmente
- Spring Security + Thymeleaf: **automático** — `th:action` já injeta o token

---

## Próximos passos sugeridos

1. Tela de cadastro/edição de usuários
2. Envio de e-mail de confirmação de agendamento
3. Notificação de ASO próximo do vencimento
4. API REST para consumo mobile (Jackson já está no classpath)
5. Deploy com Docker + docker-compose
