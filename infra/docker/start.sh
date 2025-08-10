#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

if [ -f "$SCRIPT_DIR/.env" ]; then
  echo "Using env file: $SCRIPT_DIR/.env"
else
  if [ -f "$SCRIPT_DIR/.env.example" ]; then
    echo "No .env found; copying from .env.example"
    cp "$SCRIPT_DIR/.env.example" "$SCRIPT_DIR/.env"
  elif [ -f "$SCRIPT_DIR/env.example" ]; then
    echo "No .env found; copying from env.example"
    cp "$SCRIPT_DIR/env.example" "$SCRIPT_DIR/.env"
  else
    echo "No .env or example found; proceeding with defaults"
  fi
fi

docker compose -f "$COMPOSE_FILE" up --build -d
docker compose -f "$COMPOSE_FILE" ps


