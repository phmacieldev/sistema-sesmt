#!/usr/bin/env bash
# Backup diário do banco PGEO via pg_dump.
# Guarda os últimos KEEP_DAYS dias de backups e remove os mais antigos.
#
# Uso direto:
#   PGHOST=localhost PGPORT=5432 PGUSER=pgeo PGPASSWORD=senha PGDATABASE=pgeo ./backup.sh
#
# Uso via cron (exemplo — todo dia às 02h):
#   0 2 * * * /opt/pgeo/scripts/backup.sh >> /var/log/pgeo_backup.log 2>&1
#
# Uso via Docker (adicionar ao docker-compose.yml como serviço ou volume):
#   Basta montar este script no container do DB e configurar o cron lá.

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/pgeo}"
KEEP_DAYS="${KEEP_DAYS:-7}"
DB_HOST="${PGHOST:-localhost}"
DB_PORT="${PGPORT:-5432}"
DB_USER="${PGUSER:-pgeo}"
DB_NAME="${PGDATABASE:-pgeo}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILE="${BACKUP_DIR}/pgeo_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iniciando backup: $FILE"

PGPASSWORD="${PGPASSWORD:-}" \
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" \
  --no-owner --no-acl --clean --if-exists \
  | gzip > "$FILE"

SIZE=$(du -sh "$FILE" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup concluído: $FILE ($SIZE)"

# Remove backups mais antigos que KEEP_DAYS dias
find "$BACKUP_DIR" -name "pgeo_*.sql.gz" -mtime +"$KEEP_DAYS" -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backups antigos removidos (> ${KEEP_DAYS} dias)"
