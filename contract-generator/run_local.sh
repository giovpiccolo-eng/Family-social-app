#!/usr/bin/env bash
# Local dev runner: no auth, filesystem storage, no cloud calls.
set -euo pipefail
cd "$(dirname "$0")"
export MODE=dev
export PYTHONUNBUFFERED=1
exec python app.py
