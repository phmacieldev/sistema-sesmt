#!/usr/bin/env bash
# gen-self-signed.sh — gera certificado TLS autoassinado para desenvolvimento/homologação.
#
# Para produção real, substitua os arquivos gerados por um certificado
# emitido por uma CA confiável (ex: Let's Encrypt, ICP-Brasil, CA corporativa).
#
# Uso:
#   chmod +x gen-self-signed.sh
#   ./gen-self-signed.sh
#
# Os arquivos são criados em ./certs/ — montados no container nginx.

set -euo pipefail

CERTS_DIR="$(cd "$(dirname "$0")" && pwd)/certs"
mkdir -p "$CERTS_DIR"

echo "Gerando certificado autoassinado em $CERTS_DIR ..."

openssl req -x509 -nodes -days 3650 \
  -newkey rsa:2048 \
  -keyout "$CERTS_DIR/server.key" \
  -out    "$CERTS_DIR/server.crt" \
  -subj   "/C=BR/ST=SP/L=SaoPaulo/O=SESMT/CN=pgeo.local" \
  -addext "subjectAltName=DNS:pgeo.local,DNS:localhost,IP:127.0.0.1"

chmod 600 "$CERTS_DIR/server.key"
chmod 644 "$CERTS_DIR/server.crt"

echo ""
echo "Certificado gerado:"
echo "  Cert: $CERTS_DIR/server.crt"
echo "  Key:  $CERTS_DIR/server.key"
echo ""
echo "ATENÇÃO: Este é um certificado autoassinado."
echo "O navegador exibirá um aviso de segurança — clique em 'Avançado' > 'Continuar'."
echo "Para produção, substitua por um certificado de CA confiável."
