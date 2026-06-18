# Contract Generator — Ingenium Education Group

Web app that automates employment-contract creation for Ingenium
Education Group schools (Acorn International School and St Francis
International School), based on **CCNL ANINSEI 2024–2027** (CCNL Scuole
Private Laiche, signed 15/06/2024).

HR fills in a guided wizard; the app calculates every contractual value
(tabellare, AFAC, prolungamento art. 35, doposcuola, school camp, RAL),
generates a print-ready DOCX + PDF, and saves a searchable history.

## Two run modes — same code

| Mode | Auth | Storage | Use case |
|------|------|---------|----------|
| **Dev** | Auto-logged in as `dev@localhost` | Filesystem + JSON file | Local laptop, no setup |
| **Prod** | Google OAuth (Workspace domain-restricted) | Firestore + Cloud Storage | Cloud Run deployment |

Mode auto-detects from env vars (presence of `GOOGLE_CLIENT_ID` +
`FIREBASE_PROJECT_ID` enables prod) or set `MODE=dev` / `MODE=prod`
explicitly.

## Local quick-start

```bash
cd contract-generator
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
./run_local.sh                 # http://localhost:8080
```

LibreOffice is optional locally — without it you get DOCX only.

```bash
# Debian / Ubuntu
sudo apt-get install libreoffice
```

## Deploy to production

See [DEPLOY.md](./DEPLOY.md) for the full Google Cloud Run + Firebase
walkthrough. tl;dr:

```bash
gcloud run deploy contract-generator --source . --region europe-west1 \
    --set-env-vars="MODE=prod,FIREBASE_PROJECT_ID=...,GCS_BUCKET=...,ALLOWED_GOOGLE_DOMAIN=ingeniumeducationgroup.com" \
    --update-secrets="GOOGLE_CLIENT_ID=...,GOOGLE_CLIENT_SECRET=...,FLASK_SECRET_KEY=..."
```

The Dockerfile bundles LibreOffice so PDF conversion works out of the box.

## Project layout

```
contract-generator/
    app.py                 # Flask app, routes
    auth.py                # Google OAuth (Authlib) + login_required
    config.py              # Env-driven config + mode detection
    generator.py           # Contract calc engine + DOCX builder + PDF conv
    ccnl_data.py           # CCNL tables, text blocks, gender inflections
    helpers.py             # Italian currency / dates / num-to-words
    storage_backend.py     # LocalBackend (dev) / CloudBackend (Firestore + GCS)
    templates/
        _appbar.html       # Shared header with user chip + nav
        index.html         # Wizard (8 steps + summary)
        contracts.html     # History list (filter / search)
        contract_detail.html  # Per-contract page + download buttons
        result.html        # Legacy post-generation download page
        login.html         # Google sign-in screen
    static/
    assets/
        logo_acorn.png     # Drop here when available
        logo_sfis.png      # ditto
    output/                # Generated files + local-DB JSON in dev mode
    test_generator.py      # 29 calc-engine unit tests
    test_docgen.py         # 17 DOCX-structure tests
    test_integration.py    # 25 end-to-end Flask+content tests
    Dockerfile             # Includes LibreOffice for PDF conversion
    DEPLOY.md              # Cloud Run setup walkthrough
    requirements.txt
    run_local.sh
```

## Tests

```bash
python test_generator.py     # calc engine
python test_docgen.py        # DOCX structure
python test_integration.py   # end-to-end Flask
```

**Current status: 71/71 passing** (2 PDF tests cleanly skip if
LibreOffice can't actually convert in the local environment).

## Routes

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET    | `/`                 | login | Wizard |
| POST   | `/generate`         | login | JSON payload → DOCX (+PDF) + saves record |
| GET    | `/contracts`        | login | History list |
| GET    | `/contracts/<id>`   | login | Saved contract detail + download buttons |
| GET    | `/download/<file>`  | login | Local-file download (dev only) |
| GET    | `/result`           | login | Post-generation page (legacy) |
| GET    | `/login`            | —     | Start OAuth |
| GET    | `/auth/callback`    | —     | OAuth return |
| GET    | `/logout`           | —     | Clear session |
| GET    | `/healthz`          | —     | Config snapshot |

## Environment variables

See [DEPLOY.md](./DEPLOY.md#environment-variables-reference) for the
full reference.

## CCNL coverage

| Section | File |
|---------|------|
| §4.1 Livelli, aree, mansioni     | `ccnl_data.AREA`, `ORE_BASE`, `PROFILO_TIPICO` |
| §4.2 Tabellare 2025/2026/2027    | `ccnl_data.TABELLARE` |
| §4.3 Periodo di prova            | `ccnl_data.PERIODO_PROVA_MESI` |
| §4.4 Preavviso                   | `ccnl_data.preavviso_mesi()` |
| §4.5 Prolungamento orario        | `ccnl_data.PROLUNGAMENTO` |
| §5.x Calcoli                     | `generator.py` |
| §6.x Text blocks condizionali    | `ccnl_data.TEXT_BLOCKS` |
| §7.x Document structure          | `generator.build_contract()` |
| §11 Static text blocks           | `ccnl_data.STATIC_BLOCKS` |

All monetary values use `decimal.Decimal` end-to-end with `ROUND_HALF_UP`
to two places (payroll convention).
