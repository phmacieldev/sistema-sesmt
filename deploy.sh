#!/bin/bash
# =============================================
# deploy.sh — Script de deploy do PGEO
#
# Uso:
#   ./deploy.sh           → primeira instalação ou atualização
#   ./deploy.sh --logs    → mostra logs em tempo real após subir
#   ./deploy.sh --stop    → para tudo
#   ./deploy.sh --restart → reinicia só a aplicação (mantém o banco)
#   ./deploy.sh --status  → mostra status dos containers
# =============================================

set -e  # para imediatamente se qualquer comando falhar

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log()  { echo -e "${GREEN}[PGEO]${NC} $1"; }
warn() { echo -e "${YELLOW}[AVISO]${NC} $1"; }
err()  { echo -e "${RED}[ERRO]${NC} $1"; exit 1; }
info() { echo -e "${BLUE}[INFO]${NC} $1"; }

# ── Verificações iniciais ─────────────────────
check_deps() {
    command -v docker >/dev/null 2>&1 || err "Docker não encontrado. Instale: https://docs.docker.com/get-docker/"
    docker compose version >/dev/null 2>&1 || err "Docker Compose não encontrado."

    if [ ! -f ".env" ]; then
        warn "Arquivo .env não encontrado. Usando valores padrão."
        warn "Crie o .env com suas senhas antes de ir para produção!"
    fi
}

# ── Comandos ──────────────────────────────────
case "${1}" in

    --stop)
        log "Parando todos os containers..."
        docker compose down
        log "Parado."
        exit 0
        ;;

    --restart)
        log "Reiniciando a aplicação (banco mantido)..."
        docker compose restart app
        log "Reiniciado. Aguardando ficar saudável..."
        docker compose ps app
        exit 0
        ;;

    --status)
        docker compose ps
        exit 0
        ;;

    --logs)
        docker compose logs -f app
        exit 0
        ;;

    --db-logs)
        docker compose logs -f db
        exit 0
        ;;

    --backup)
        BACKUP_FILE="pgeo_backup_$(date +%Y%m%d_%H%M%S).sql"
        log "Gerando backup do banco → ${BACKUP_FILE}"
        source .env 2>/dev/null || true
        docker compose exec db pg_dump \
            -U "${DB_USER:-pgeo_user}" \
            "${DB_NAME:-pgeo_db}" > "${BACKUP_FILE}"
        log "Backup salvo em: ${BACKUP_FILE}"
        exit 0
        ;;

esac

# ── Deploy / Atualização ──────────────────────
check_deps

echo ""
echo -e "${BLUE}╔══════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        PGEO — Deploy / Update        ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════╝${NC}"
echo ""

# Para a aplicação (não o banco) se estiver rodando
if docker compose ps app 2>/dev/null | grep -q "running"; then
    log "Parando a aplicação para atualizar..."
    docker compose stop app
fi

# Reconstrói a imagem com o código mais recente
log "Construindo imagem Docker (pode levar alguns minutos na primeira vez)..."
docker compose build app

# Sobe tudo
log "Subindo os serviços..."
docker compose up -d

# Aguarda o banco ficar pronto
log "Aguardando o banco de dados..."
RETRIES=30
until docker compose exec db pg_isready -U "${DB_USER:-pgeo_user}" -q 2>/dev/null; do
    RETRIES=$((RETRIES - 1))
    if [ $RETRIES -le 0 ]; then
        err "Banco não ficou pronto. Verifique: docker compose logs db"
    fi
    sleep 2
done
log "Banco pronto ✓"

# Aguarda a aplicação ficar saudável
log "Aguardando a aplicação inicializar (pode levar até 90 segundos)..."
RETRIES=30
until curl -sf http://localhost:8080/login >/dev/null 2>&1; do
    RETRIES=$((RETRIES - 1))
    if [ $RETRIES -le 0 ]; then
        warn "Aplicação demorou para responder. Veja os logs:"
        docker compose logs --tail=30 app
        err "Deploy falhou. Verifique os logs acima."
    fi
    sleep 3
done

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           ✅ PGEO rodando com sucesso!        ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  Acesse: http://localhost:8080               ║${NC}"
echo -e "${GREEN}║  Login:  admin / admin123                    ║${NC}"
echo -e "${GREEN}║  TROQUE A SENHA após o primeiro acesso!      ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  Comandos úteis:                             ║${NC}"
echo -e "${GREEN}║    ./deploy.sh --logs     → ver logs         ║${NC}"
echo -e "${GREEN}║    ./deploy.sh --status   → status           ║${NC}"
echo -e "${GREEN}║    ./deploy.sh --backup   → backup do banco  ║${NC}"
echo -e "${GREEN}║    ./deploy.sh --stop     → parar tudo       ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""

if [ "${1}" == "--logs" ]; then
    docker compose logs -f app
fi
