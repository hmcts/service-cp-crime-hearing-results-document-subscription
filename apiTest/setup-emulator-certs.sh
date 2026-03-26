#!/usr/bin/env bash
# Generates self-signed certificates required by azure-keyvault-emulator.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERTS_DIR="${SCRIPT_DIR}/certs"

mkdir -p "${CERTS_DIR}"

echo "Generating emulator certificates in ${CERTS_DIR} ..."

openssl req -x509 -newkey rsa:4096 -nodes \
  -keyout "${CERTS_DIR}/emulator.key" \
  -out    "${CERTS_DIR}/emulator.crt" \
  -days   3650 \
  -subj   "/CN=keyvault-emulator" \
  -addext "subjectAltName=DNS:localhost,DNS:keyvault-emulator,DNS:emulator,IP:127.0.0.1"

openssl pkcs12 -export \
  -out    "${CERTS_DIR}/emulator.pfx" \
  -inkey  "${CERTS_DIR}/emulator.key" \
  -in     "${CERTS_DIR}/emulator.crt" \
  -passout pass:emulator

echo "Done. Files written to ${CERTS_DIR}:"
ls -1 "${CERTS_DIR}"
