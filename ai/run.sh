#!/usr/bin/env bash

set -e

HOST=${HOST:-0.0.0.0}
PORT=${PORT:-8888}
APP=${APP:-src.main:app}
WORKERS=${WORKERS:-1}
RELOAD=${RELOAD:-true}

if [ "$RELOAD" = "true" ]; then
  exec uvicorn "$APP" --host "$HOST" --port "$PORT" --reload
else
  exec uvicorn "$APP" --host "$HOST" --port "$PORT" --workers "$WORKERS"
fi