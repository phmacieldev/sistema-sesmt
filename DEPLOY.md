# PGEO — Guia de Deploy para TI

Sistema de Agendamento de Exames Ocupacionais — SESMT  
Stack: Java 21 / Spring Boot / PostgreSQL / Docker

---

## Pré-requisitos no servidor

```bash
# Docker Engine (20.10+)
curl -fsSL https://get.docker.com | sh

# Docker Compose v2 (já vem com Docker Desktop)
docker compose version  # deve retornar v2.x.x

# Verificar
docker --version && docker compose version
```

---

## Estrutura de arquivos recebida

```
pgeo-java/
├── src/                        → código-fonte Java
├── Dockerfile                  → receita da imagem Docker
├── docker-compose.yml          → orquestra app + banco
├── deploy.sh                   → script de deploy (use este)
├── .env                        → senhas (EDITAR antes de subir)
├── .env.example                → template de referência
└── pom.xml                     → dependências Maven
```

---

## Primeiro deploy (passo a passo)

### 1. Configurar senhas

```bash
# Editar o arquivo .env com as senhas do ambiente
nano .env
```

Conteúdo do `.env`:
```env
DB_NAME=pgeo_db
DB_USER=pgeo_user
DB_PASSWORD=senha_forte_aqui

# Gerar chave secreta aleatória:
# openssl rand -hex 32
SECRET_KEY=cole_aqui_a_chave_gerada
```

### 2. Subir o sistema

```bash
# Torna o script executável (só na primeira vez)
chmod +x deploy.sh

# Sobe tudo — banco + aplicação
./deploy.sh
```

O script vai:
1. Construir a imagem Java (baixa dependências + compila)
2. Subir o PostgreSQL
3. Aguardar o banco estar pronto
4. Subir a aplicação
5. Aplicar as migrations do banco automaticamente
6. Confirmar quando estiver acessível

**Primeira execução leva ~3–5 minutos** (download das imagens base).  
Atualizações subsequentes levam ~1–2 minutos.

### 3. Acessar

```
http://IP_DO_SERVIDOR:8080
```

Credenciais padrão — **trocar imediatamente após o primeiro acesso:**

| Usuário | Senha padrão | Perfil |
|---|---|---|
| admin | admin@123 | Acesso total |
| operador | oper@123 | Criar/editar agendamentos |
| visualizador | view@123 | Somente leitura |

> Senha mínima: **8 caracteres**. Use `/minha-senha` para trocar.
> Sessão expira automaticamente após **4 horas** de inatividade.

---

## Comandos do dia a dia

```bash
# Ver status dos containers
./deploy.sh --status

# Ver logs da aplicação em tempo real
./deploy.sh --logs

# Atualizar para nova versão (recebe ZIP novo do SESMT)
./deploy.sh            # recompila e sobe

# Reiniciar só a aplicação (banco fica de pé)
./deploy.sh --restart

# Fazer backup do banco
./deploy.sh --backup
# Gera: pgeo_backup_YYYYMMDD_HHMMSS.sql

# Parar tudo
./deploy.sh --stop
```

---

## Portas utilizadas

| Porta | Serviço | Observação |
|---|---|---|
| 8080 | Aplicação | Acesso dos usuários |
| 5432 | PostgreSQL | Pode fechar no firewall se não precisar de acesso externo ao banco |

---

## Exposição na rede local

Para acessar pelo nome em vez de IP, configurar no DNS interno ou no arquivo `hosts` das máquinas:

```
192.168.x.x    pgeo.sesmt.local
```

Para HTTPS com certificado SSL (recomendado se exposto além da rede local), colocar um **Nginx** como proxy reverso na frente:

```nginx
# /etc/nginx/sites-available/pgeo
server {
    listen 80;
    server_name pgeo.sesmt.local;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name pgeo.sesmt.local;

    ssl_certificate     /etc/ssl/pgeo.crt;
    ssl_certificate_key /etc/ssl/pgeo.key;

    location / {
        proxy_pass         http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection "upgrade";  # necessário para WebSocket
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
    }
}
```

---

## Backup automático (cron)

```bash
# Backup diário às 2h da manhã
crontab -e

# Adicionar a linha:
0 2 * * * cd /caminho/para/pgeo && ./deploy.sh --backup >> /var/log/pgeo-backup.log 2>&1
```

---

## Dados persistentes

Os dados do banco ficam num volume Docker (`pgeo_db_data`).  
Mesmo que o container seja recriado, os dados são preservados.

Para ver onde o Docker armazena:
```bash
docker volume inspect pgeo-java_pgeo_db_data
```

---

## Restaurar backup

```bash
# Copiar o arquivo de backup para dentro do container do banco
docker compose exec -T db psql \
    -U pgeo_user \
    -d pgeo_db \
    < pgeo_backup_YYYYMMDD_HHMMSS.sql
```

---

## Logs

Os logs da aplicação ficam em dois lugares:
- **Container**: `docker compose logs -f app`
- **Arquivo**: volume `pgeo_logs` → dentro do container em `/app/logs/pgeo.log`

Para acessar os arquivos de log diretamente:
```bash
docker compose exec app cat /app/logs/pgeo.log
```

---

## Atualização de versão

Quando o SESMT enviar um novo ZIP:

```bash
# 1. Substituir os arquivos src/ pelo conteúdo novo
# 2. Manter o .env (não sobrescrever)
# 3. Rodar:
./deploy.sh
```

O Flyway aplica automaticamente quaisquer novas migrations de banco.  
Os dados existentes são preservados.

---

## Solução de problemas

**Aplicação não sobe:**
```bash
docker compose logs app
```

**Banco não conecta:**
```bash
docker compose logs db
docker compose exec db pg_isready -U pgeo_user
```

**Porta 8080 ocupada:**
```bash
# Mudar a porta no docker-compose.yml:
ports:
  - "8090:8080"  # acessa na 8090, app continua na 8080 internamente
```

**Reiniciar tudo do zero (APAGA OS DADOS):**
```bash
docker compose down -v  # -v remove os volumes
./deploy.sh
```
---

## Segurança

### O que está configurado

- **BCrypt strength 12** — senhas armazenadas com hash forte (imperceptível ao usuário)
- **Session timeout: 4 horas** — sessão expira automaticamente por inatividade
- **Cookie HttpOnly** — JavaScript não acessa o cookie de sessão
- **Headers HTTP** — X-Content-Type-Options, CSP, Referrer-Policy configurados
- **Log de login** — tentativas bem-sucedidas e falhas registradas com IP no log
- **Banco não exposto** — PostgreSQL acessível apenas internamente pelo container da app

### Para a TI: acessar o banco em manutenção

O banco não está exposto na rede. Para acessar:
```bash
# Conectar ao banco via container (substitui o DBeaver)
docker exec -it pgeo-db psql -U pgeo_user -d pgeo_db

# Ou habilitar temporariamente (só em manutenção):
# Adicionar no docker-compose.yml > db > ports:
#   - "127.0.0.1:5432:5432"   ← só localhost, não a rede
# Depois: docker compose restart db
```

### Se precisar de HTTPS (recomendado se sair da rede local)

Configurar Nginx com certificado SSL conforme documentado mais acima,
e alterar no `application-prod.properties`:
```properties
server.servlet.session.cookie.secure=true
```

