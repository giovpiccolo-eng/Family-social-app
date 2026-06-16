# Contract Generator — Ingenium Education Group

Web application that automates the creation of employment contracts for
Ingenium Education Group schools (Acorn International School and St Francis
International School), based on **CCNL ANINSEI 2024–2027** (Contratto
Collettivo Nazionale di Lavoro per le Scuole Private Laiche, signed
15/06/2024).

The administrator fills in a step-by-step questionnaire; the app calculates
all contractual values (tabellare, AFAC, prolungamento orario, doposcuola,
school camp, RAL) and generates a print-ready DOCX + PDF.

## Project structure

```
contract-generator/
    app.py              # Flask app, routes
    generator.py        # Contract logic & DOCX generation
    ccnl_data.py        # All CCNL tables, rates, text blocks
    templates/
        index.html      # Questionnaire UI (wizard)
        result.html     # Download page
    static/
        style.css
        app.js          # Form wizard logic
    assets/
        logo_acorn.png  # Acorn International School logo
        logo_sfis.png   # St Francis International School logo
    output/             # Generated files (transient)
    requirements.txt
    README.md
```

## Stack

| Layer            | Technology                                |
|------------------|-------------------------------------------|
| Backend          | Python 3.11+ · Flask                      |
| Frontend         | HTML5 / CSS3 / Vanilla JS (no framework)  |
| DOCX generation  | python-docx 1.x                           |
| PDF conversion   | LibreOffice headless (`soffice --convert-to pdf`) |
| Logo insertion   | python-docx `InlineImage`                 |
| Data storage     | None — stateless per session              |

## Setup

```bash
cd contract-generator
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

LibreOffice (headless) must be installed system-wide for PDF conversion:

```bash
# Debian/Ubuntu
sudo apt-get install libreoffice
```

## Run (development)

```bash
flask --app app run --debug
# → http://127.0.0.1:5000/
```

## Monetary values

All monetary computations use `decimal.Decimal` end-to-end. Tabellare values
in `ccnl_data.TABELLARE` are stored as `Decimal('1234.56')` strings to avoid
any binary floating-point rounding error. Currency display in the frontend
uses Italian locale (`1.234,56 €`).

## CCNL data layer (`ccnl_data.py`)

Hard-coded from CCNL ANINSEI 2024–2027, Parte Seconda:

- `LIVELLI`, `AREA`, `PROFILO_TIPICO`, `ORE_BASE` — §4.1
- `TABELLARE[livello][year]` — §4.2 (years 2025 / 2026 / 2027)
- `PERIODO_PROVA_MESI[(tipo, livello)]` — §4.3
- `preavviso_mesi(tipo, livello, anzianità)` — §4.4
- `PROLUNGAMENTO` — §4.5
- `DOPOSCUOLA_RATE_HOUR`, `SCHOOL_CAMP_RATE_WEEK`, `FRINGE_BENEFIT`
- `SEDI` — addresses and logo files per school
- `TEXT_BLOCKS` — §6.1–§6.6 conditional templates
- `STATIC_BLOCKS` — §11 verbatim blocks
- `INFLESSIONI` — gender inflection helper

## Branches handled by the wizard

1. Identity (nome, sesso, data nascita, CF)
2. Contract type (pieno/part-time, indeterminato/determinato, sede)
3. Role & livello (auto-fills area + ore base + prova)
4. Dates (inizio, fine if determinato)
5. Orario (ore settimanali, prolungamento art. 35 if V livello)
6. Retribuzione (tabellare auto, AFAC, indennità funzione, fringe)
7. Voci aggiuntive (doposcuola, school camp)
8. Clausole speciali (patto durata minima, rinnovo + motivazione)

See `TechnicalBrief` v1.0 (Ingenium Education Group, June 2026) for full
specification.
