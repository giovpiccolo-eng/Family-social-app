"""
Runtime configuration — all driven by env vars so the same code runs:

  - Locally:  no auth, filesystem storage, no Firestore (auto-fallback).
  - Cloud Run: Google OAuth + Workspace domain restriction + Firestore
               for contract metadata + Cloud Storage for DOCX/PDFs.

Set MODE explicitly to "dev" or "prod", or leave it unset and let the
presence of GOOGLE_CLIENT_ID + FIREBASE_PROJECT_ID decide.
"""

from __future__ import annotations

import os
from pathlib import Path


def _env(key: str, default: str | None = None) -> str | None:
    val = os.environ.get(key)
    return val if val else default


# -- Mode auto-detect -------------------------------------------------------
_explicit_mode = _env("MODE", "").lower()
_has_oauth = bool(_env("GOOGLE_CLIENT_ID") and _env("GOOGLE_CLIENT_SECRET"))
_has_fs = bool(_env("FIREBASE_PROJECT_ID"))

if _explicit_mode in ("dev", "prod"):
    MODE = _explicit_mode
else:
    MODE = "prod" if (_has_oauth and _has_fs) else "dev"

IS_PROD = MODE == "prod"
IS_DEV = MODE == "dev"


# -- Flask ------------------------------------------------------------------
SECRET_KEY = _env("FLASK_SECRET_KEY",
                  "dev-secret-change-me-in-production")
SESSION_COOKIE_SECURE = IS_PROD
SESSION_COOKIE_SAMESITE = "Lax"


# -- Google OAuth ----------------------------------------------------------
GOOGLE_CLIENT_ID = _env("GOOGLE_CLIENT_ID")
GOOGLE_CLIENT_SECRET = _env("GOOGLE_CLIENT_SECRET")

# Restrict sign-in to a single Google Workspace domain (e.g.
# "ingeniumeducationgroup.com"). If empty, any Google account may sign in.
ALLOWED_GOOGLE_DOMAIN = _env("ALLOWED_GOOGLE_DOMAIN", "")

# Comma-separated list of explicit user emails always allowed (overrides
# the domain check — useful for admin emails outside the Workspace).
ALLOWED_EMAILS = {
    e.strip().lower()
    for e in (_env("ALLOWED_EMAILS", "") or "").split(",")
    if e.strip()
}

OAUTH_REDIRECT_URI = _env("OAUTH_REDIRECT_URI", "")


# -- Firestore --------------------------------------------------------------
FIREBASE_PROJECT_ID = _env("FIREBASE_PROJECT_ID")

# Firestore collection name — namespaced so it can coexist with other
# apps in the same Firebase project.
FIRESTORE_COLLECTION = _env("FIRESTORE_COLLECTION", "ingenium_contracts")


# -- Cloud Storage ---------------------------------------------------------
GCS_BUCKET = _env("GCS_BUCKET")


# -- Local paths ------------------------------------------------------------
BASE_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = BASE_DIR / "output"
LOCAL_DB_PATH = BASE_DIR / "output" / "_local_contracts.json"


def summary() -> dict:
    """Return a human-readable snapshot for /healthz."""
    return {
        "mode": MODE,
        "google_oauth": bool(GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET),
        "allowed_domain": ALLOWED_GOOGLE_DOMAIN or "(any)",
        "firestore": bool(FIREBASE_PROJECT_ID),
        "gcs_bucket": GCS_BUCKET or "(local filesystem)",
    }
