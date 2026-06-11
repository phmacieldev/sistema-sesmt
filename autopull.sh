#!/bin/bash
# Monitora o branch remoto e faz pull automático quando detecta novos commits.
# Uso: bash autopull.sh

BRANCH="claude/exam-scheduling-review-l0lfeu"
INTERVAL=10

echo "🔄 Auto-pull ativo — verificando a cada ${INTERVAL}s no branch ${BRANCH}"
echo "   Ctrl+C para parar."
echo ""

while true; do
  git fetch origin "$BRANCH" --quiet 2>/dev/null

  LOCAL=$(git rev-parse HEAD)
  REMOTE=$(git rev-parse "origin/$BRANCH")

  if [ "$LOCAL" != "$REMOTE" ]; then
    echo "$(date '+%H:%M:%S') ↓ Novo commit detectado — puxando..."
    git pull origin "$BRANCH" --quiet
    echo "$(date '+%H:%M:%S') ✅ Atualizado: $(git log -1 --oneline)"
    echo ""
  fi

  sleep "$INTERVAL"
done
