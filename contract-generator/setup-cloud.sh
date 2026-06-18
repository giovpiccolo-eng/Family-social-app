#!/usr/bin/env bash
# One-shot Cloud Run deployment. Designed to run in Google Cloud Shell.
#
# Usage:   ./setup-cloud.sh           (interactive — prompts for everything)
#          PROJECT_ID=... DOMAIN=... ./setup-cloud.sh    (env-driven)
#
# What this does, in order:
#   1. Verifies the project + billing are set up.
#   2. Enables required APIs.
#   3. Creates Firestore database (if needed).
#   4. Creates a Cloud Storage bucket.
#   5. Generates a Flask session-signing secret.
#   6. Pauses for you to create the OAuth client in the Console
#      (one-time, ~2 min — instructions printed inline).
#   7. Stores OAuth creds + Flask secret in Secret Manager.
#   8. Builds the container and deploys to Cloud Run.
#   9. Grants Cloud Run's service account the IAM it needs (Firestore +
#      Storage + signed-URL token creation).
#  10. Updates the OAuth client's redirect URI with the live URL.
#  11. Prints the service URL.
#
# Tested in Cloud Shell (Debian, gcloud preinstalled, curl + openssl present).

set -euo pipefail

# ---- Colors ---------------------------------------------------------------
B=$'\e[1m'; R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; C=$'\e[36m'; X=$'\e[0m'
say()  { printf "${C}==>${X} ${B}%s${X}\n" "$*"; }
ok()   { printf "${G}✓${X} %s\n" "$*"; }
warn() { printf "${Y}!${X} %s\n" "$*"; }
err()  { printf "${R}✗${X} %s\n" "$*" >&2; }
pause(){ printf "\n${Y}%s${X}\n" "$*"; read -rp "Premi INVIO quando hai finito... " _; }

# ---- Prereqs --------------------------------------------------------------
command -v gcloud >/dev/null || { err "gcloud non trovato. Apri Cloud Shell (icona terminale in alto a destra del Console)."; exit 1; }

# ---- Inputs ---------------------------------------------------------------
PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null || true)}"
if [[ -z "${PROJECT_ID}" || "${PROJECT_ID}" == "(unset)" ]]; then
    read -rp "Project ID Google Cloud (es. ingenium-contracts): " PROJECT_ID
fi
gcloud config set project "$PROJECT_ID" >/dev/null

REGION="${REGION:-europe-west1}"
SERVICE="${SERVICE:-contract-generator}"
BUCKET="${BUCKET:-${PROJECT_ID}-contracts}"

DOMAIN="${DOMAIN:-}"
if [[ -z "${DOMAIN}" ]]; then
    read -rp "Dominio Workspace per limitare l'accesso (es. ingeniumeducationgroup.com, vuoto = nessun limite): " DOMAIN
fi

say "Configurazione:"
echo "  Project:  $PROJECT_ID"
echo "  Region:   $REGION"
echo "  Service:  $SERVICE"
echo "  Bucket:   $BUCKET"
echo "  Domain:   ${DOMAIN:-(nessun limite)}"
echo

# ---- 1. Billing -----------------------------------------------------------
say "Verifico che la fatturazione sia abilitata..."
if ! gcloud beta billing projects describe "$PROJECT_ID" --format='value(billingEnabled)' 2>/dev/null | grep -q True; then
    err "La fatturazione NON è abilitata su questo progetto."
    cat <<'NEEDS_BILLING'

Devi collegare un account di fatturazione al progetto:
  1. Vai su https://console.cloud.google.com/billing/linkedaccount
  2. Seleziona il progetto corretto in alto.
  3. Clicca "LINK A BILLING ACCOUNT" e seleziona un account.

Non costerà nulla: la free tier di Cloud Run + Firestore + Storage
copre 50–100 contratti al mese a zero euro. Devi però collegare una
carta perché Google possa attivare i servizi.

Rilancia questo script quando hai fatto.
NEEDS_BILLING
    exit 1
fi
ok "Fatturazione attiva."

# ---- 2. Enable APIs -------------------------------------------------------
say "Abilito le API necessarie (può richiedere 1–2 min)..."
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    cloudbuild.googleapis.com \
    firestore.googleapis.com \
    storage.googleapis.com \
    secretmanager.googleapis.com \
    iamcredentials.googleapis.com \
    --quiet
ok "API abilitate."

# ---- 3. Firestore ---------------------------------------------------------
say "Verifico Firestore..."
if gcloud firestore databases describe --database='(default)' >/dev/null 2>&1; then
    ok "Firestore già attivo."
else
    say "Creo il database Firestore in $REGION..."
    gcloud firestore databases create --location="$REGION" --quiet
    ok "Firestore creato."
fi

# ---- 4. GCS bucket --------------------------------------------------------
say "Verifico bucket gs://$BUCKET..."
if gcloud storage buckets describe "gs://$BUCKET" >/dev/null 2>&1; then
    ok "Bucket già esistente."
else
    gcloud storage buckets create "gs://$BUCKET" \
        --location="$REGION" --uniform-bucket-level-access --quiet
    ok "Bucket creato."
fi

# ---- 5. Flask secret ------------------------------------------------------
FLASK_KEY=$(openssl rand -hex 32)

# ---- 6. OAuth client (manual) ---------------------------------------------
say "Creazione OAuth client (passaggio manuale, ~2 min)"
cat <<INSTRUCTIONS

Adesso devi creare le credenziali OAuth nel Console. È l'unica parte
che NON posso automatizzare.

  1. Apri:  https://console.cloud.google.com/apis/credentials?project=$PROJECT_ID

  2. Se ti chiede di configurare la "OAuth consent screen":
     - User Type: ${DOMAIN:+"Internal"}${DOMAIN:-"External"}
     - App name:   "Contract Generator"
     - User support email: la tua email
     - Sviluppatore email: la tua email
     - Salva.

  3. Torna a "Credentials" e clicca "+ CREATE CREDENTIALS" → "OAuth client ID":
     - Application type: "Web application"
     - Name: "Contract Generator"
     - Authorized redirect URIs: lascia VUOTO per ora (lo aggiorniamo dopo).
     - CREATE.

  4. Copia il "Client ID" e "Client secret" dalla finestra che appare.

INSTRUCTIONS
pause "Quando hai i due valori, premi INVIO per continuare."

read -rp "Incolla il Client ID: " CLIENT_ID
read -rsp "Incolla il Client secret (non verrà mostrato): " CLIENT_SECRET; echo

# ---- 7. Secret Manager ----------------------------------------------------
say "Salvo i segreti in Secret Manager..."
upsert_secret() {
    local name=$1; local value=$2
    if gcloud secrets describe "$name" >/dev/null 2>&1; then
        printf "%s" "$value" | gcloud secrets versions add "$name" --data-file=- --quiet >/dev/null
    else
        printf "%s" "$value" | gcloud secrets create "$name" --data-file=- --quiet >/dev/null
    fi
}
upsert_secret google-client-id     "$CLIENT_ID"
upsert_secret google-client-secret "$CLIENT_SECRET"
upsert_secret flask-secret-key     "$FLASK_KEY"
ok "Segreti salvati."

# ---- 8. Deploy ------------------------------------------------------------
say "Build + deploy su Cloud Run (5–8 minuti per la prima volta)..."
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

ENV_VARS="MODE=prod,FIREBASE_PROJECT_ID=$PROJECT_ID,GCS_BUCKET=$BUCKET"
if [[ -n "$DOMAIN" ]]; then
    ENV_VARS="$ENV_VARS,ALLOWED_GOOGLE_DOMAIN=$DOMAIN"
fi

gcloud run deploy "$SERVICE" \
    --source . \
    --region "$REGION" \
    --allow-unauthenticated \
    --memory 1Gi --cpu 1 \
    --concurrency 4 --max-instances 4 --timeout 120 \
    --set-env-vars="$ENV_VARS" \
    --update-secrets="GOOGLE_CLIENT_ID=google-client-id:latest,GOOGLE_CLIENT_SECRET=google-client-secret:latest,FLASK_SECRET_KEY=flask-secret-key:latest" \
    --quiet

SERVICE_URL=$(gcloud run services describe "$SERVICE" --region "$REGION" --format='value(status.url)')
ok "Deploy completato: $SERVICE_URL"

# ---- 9. IAM grants --------------------------------------------------------
say "Concedo i permessi al service account..."
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA" --role="roles/datastore.user" --quiet >/dev/null

gcloud storage buckets add-iam-policy-binding "gs://$BUCKET" \
    --member="serviceAccount:$SA" --role="roles/storage.objectAdmin" --quiet >/dev/null

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA" --role="roles/iam.serviceAccountTokenCreator" --quiet >/dev/null

# Re-deploy so the new IAM is picked up by a fresh revision
gcloud run services update "$SERVICE" --region "$REGION" \
    --update-env-vars="OAUTH_REDIRECT_URI=${SERVICE_URL}/auth/callback" \
    --quiet >/dev/null

ok "Permessi configurati."

# ---- 10. Finalize OAuth redirect URI -------------------------------------
say "ULTIMA AZIONE MANUALE — aggiungi il redirect URI all'OAuth client"
cat <<FINAL

  1. Apri:  https://console.cloud.google.com/apis/credentials?project=$PROJECT_ID
  2. Clicca sull'OAuth client "Contract Generator".
  3. In "Authorized redirect URIs" clicca "+ ADD URI" e incolla:

        ${SERVICE_URL}/auth/callback

  4. SAVE.

FINAL

# ---- 11. Done -------------------------------------------------------------
cat <<DONE

${G}${B}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${X}
${G}${B} ✓ Contract Generator è in produzione!${X}
${G}${B}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${X}

  URL:        ${SERVICE_URL}
  Login:      ${SERVICE_URL}/login
  Health:     ${SERVICE_URL}/healthz

  Per ridistribuire dopo modifiche al codice, basta:
      gcloud run deploy $SERVICE --source . --region $REGION

DONE
