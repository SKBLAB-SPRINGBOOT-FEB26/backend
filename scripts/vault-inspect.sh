#!/usr/bin/env bash
# Inspect the JWT EC keys stored in Vault.
# The Spring Boot app self-seeds the keys on first boot, so this script is for
# verification / demo purposes (e.g. show the panel during defense).

set -euo pipefail

VAULT_URI="${VAULT_URI:-http://127.0.0.1:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-root}"
VAULT_KV_PATH="${VAULT_KV_PATH:-secret/backend/jwt}"

mount="${VAULT_KV_PATH%%/*}"
inner="${VAULT_KV_PATH#*/}"

echo "[vault-inspect] addr=${VAULT_URI} path=${VAULT_KV_PATH}"

resp=$(curl -fsS -H "X-Vault-Token: ${VAULT_TOKEN}" \
    "${VAULT_URI}/v1/${mount}/data/${inner}" 2>/dev/null || true)

if [[ -z "${resp}" ]]; then
    echo "[vault-inspect] No data at '${VAULT_KV_PATH}' yet — start the Spring Boot app to seed it."
    exit 1
fi

if command -v jq >/dev/null 2>&1; then
    echo
    echo "Stored keys at '${VAULT_KV_PATH}':"
    echo "----------------------------"
    echo "${resp}" | jq -r '.data.data | to_entries[] | "\n## \(.key)\n\(.value)"'
else
    echo "${resp}"
fi
