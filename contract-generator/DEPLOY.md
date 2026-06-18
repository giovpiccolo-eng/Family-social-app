# Deploy — Google Cloud Run + Firebase

Step-by-step setup for going live. Everything below is one-time setup;
after that, future deploys are a single `gcloud run deploy` command.

## Prerequisites

- A Google Cloud / Firebase project (you can reuse the existing
  `familynest-1e2aa` project from this repo, or create a new one).
- `gcloud` CLI installed and logged in (`gcloud auth login`).
- Your Workspace domain (e.g. `ingeniumeducationgroup.com`).

## 1. Pick / set the project

```bash
export PROJECT_ID=ingenium-contracts        # or familynest-1e2aa
export REGION=europe-west1
export SERVICE=contract-generator
gcloud config set project "$PROJECT_ID"
```

Enable the APIs we need:

```bash
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    cloudbuild.googleapis.com \
    firestore.googleapis.com \
    storage.googleapis.com \
    secretmanager.googleapis.com
```

## 2. Create Firestore database

(Skip if Firestore is already enabled in the project.)

```bash
gcloud firestore databases create --location="$REGION"
```

The app uses collection `ingenium_contracts` — namespaced to avoid
collisions with any other Firestore data.

## 3. Create a Cloud Storage bucket for generated files

```bash
export BUCKET="${PROJECT_ID}-contracts"
gcloud storage buckets create "gs://$BUCKET" \
    --location="$REGION" \
    --uniform-bucket-level-access
```

## 4. Create the OAuth 2.0 client

1. Open <https://console.cloud.google.com/apis/credentials>
2. Configure OAuth consent screen (Internal — restricted to your Workspace).
3. Create credentials → OAuth client ID → Web application.
4. Authorized redirect URI:
   `https://<service-url>/auth/callback`
   (You'll get the URL after the first deploy — set a placeholder for now
    and update after step 6.)
5. Copy the **Client ID** and **Client secret**.

## 5. Stash secrets in Secret Manager

```bash
echo -n "<paste client id>"     | gcloud secrets create google-client-id     --data-file=-
echo -n "<paste client secret>" | gcloud secrets create google-client-secret --data-file=-
# 32 random bytes, hex-encoded, used to sign Flask session cookies:
openssl rand -hex 32 | gcloud secrets create flask-secret-key --data-file=-
```

## 6. Build & deploy

```bash
cd contract-generator

gcloud run deploy "$SERVICE" \
    --source . \
    --region "$REGION" \
    --allow-unauthenticated \
    --memory 1Gi --cpu 1 \
    --concurrency 4 --max-instances 4 \
    --set-env-vars="MODE=prod" \
    --set-env-vars="FIREBASE_PROJECT_ID=$PROJECT_ID" \
    --set-env-vars="GCS_BUCKET=$BUCKET" \
    --set-env-vars="ALLOWED_GOOGLE_DOMAIN=ingeniumeducationgroup.com" \
    --update-secrets="GOOGLE_CLIENT_ID=google-client-id:latest" \
    --update-secrets="GOOGLE_CLIENT_SECRET=google-client-secret:latest" \
    --update-secrets="FLASK_SECRET_KEY=flask-secret-key:latest"
```

Cloud Run prints the service URL. Take it, go back to the OAuth client
in step 4, and set the redirect URI to:

```
https://<service-url>/auth/callback
```

Optionally also set:

```bash
gcloud run services update "$SERVICE" --region "$REGION" \
    --update-env-vars="OAUTH_REDIRECT_URI=https://<service-url>/auth/callback"
```

## 7. Grant the service IAM permissions

Cloud Run's default service account needs Firestore + Storage access:

```bash
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA" \
    --role="roles/datastore.user"

gcloud storage buckets add-iam-policy-binding "gs://$BUCKET" \
    --member="serviceAccount:$SA" \
    --role="roles/storage.objectAdmin"

# Needed for v4 signed URLs (so HR can download from the browser):
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA" \
    --role="roles/iam.serviceAccountTokenCreator"
```

## 8. (Optional) Custom domain

```bash
gcloud beta run domain-mappings create \
    --service="$SERVICE" \
    --region="$REGION" \
    --domain=contracts.ingeniumeducationgroup.com
```

Cloud Run will give you the DNS records to set.

---

## Running locally (no auth, no cloud)

```bash
cd contract-generator
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python app.py
# → http://localhost:8080
```

In dev mode the app auto-logs in as `dev@localhost`, persists contracts
to `output/_local_contracts.json` and serves files from `output/`. No
Google credentials needed.

## Re-deploying after code changes

```bash
gcloud run deploy "$SERVICE" --source . --region "$REGION"
```

That's it — same env vars and secrets carry over.

## Environment variables reference

| Variable | Required? | Notes |
|---|---|---|
| `MODE`                  | optional | `dev` or `prod`. Auto-detected if unset. |
| `FLASK_SECRET_KEY`      | prod     | Random 32-byte hex. Sign-in cookie signing. |
| `GOOGLE_CLIENT_ID`      | prod     | OAuth 2.0 client. |
| `GOOGLE_CLIENT_SECRET`  | prod     | OAuth 2.0 client. |
| `OAUTH_REDIRECT_URI`    | optional | Override if behind a proxy / custom domain. |
| `ALLOWED_GOOGLE_DOMAIN` | strongly recommended | e.g. `ingeniumeducationgroup.com` — restricts sign-in. |
| `ALLOWED_EMAILS`        | optional | Comma-separated email list always allowed (admins). |
| `FIREBASE_PROJECT_ID`   | prod     | GCP project ID. |
| `FIRESTORE_COLLECTION`  | optional | Default `ingenium_contracts`. |
| `GCS_BUCKET`            | prod     | Bucket name (no `gs://` prefix). |
