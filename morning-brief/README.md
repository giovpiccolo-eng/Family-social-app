# Morning Brief

A personal news-learning app. Every morning it scans the news with Claude's web
search, picks the top stories across **your** interests, and writes a single
~3000-word essay that doesn't just summarise the headlines but teaches you the
context, history, science, and connections behind them — then emails it to you.

- **Web app (Next.js):** set your interests, schedule, and recipient; browse an
  archive of past briefs.
- **Daily pipeline:** Claude API (`web_search` tool) → Markdown essay → archived
  → emailed via Gmail SMTP.
- **Scheduler:** GitHub Actions cron at 07:00 Europe/Rome (DST-safe).

## How it works

```
config.json ──► generateBrief() ──► saveToArchive() ──► sendEmail()
 (interests)     Claude + web_search    data/archive/*.md   Gmail SMTP
```

The generation step (`lib/generate.ts`) sends one request to Claude with the
server-side `web_search` tool enabled. Claude searches for recent stories tied
to your interests and writes the full essay with inline source links in a single
integrated pass. Output is saved as Markdown and rendered to HTML for the email.

## Setup

1. **Install** (Node 20+):

   ```bash
   cd morning-brief
   npm install
   ```

2. **Configure secrets** — copy `.env.example` to `.env` and fill in:

   - `ANTHROPIC_API_KEY` — from <https://console.anthropic.com/>
   - `GMAIL_USER` — your Gmail address
   - `GMAIL_APP_PASSWORD` — a Gmail **App Password** (needs 2-Step Verification):
     <https://myaccount.google.com/apppasswords>
   - `BRIEF_TO` — where to deliver (defaults to `GMAIL_USER`)

3. **Run the web app** to tweak interests/schedule:

   ```bash
   npm run dev    # http://localhost:3000
   ```

4. **Generate + send a brief right now** (bypasses the time guard):

   ```bash
   npm run brief:force
   ```

   Or generate without emailing:

   ```bash
   npm run brief -- --force --no-email
   ```

## Daily automation (GitHub Actions)

The workflow lives at `.github/workflows/morning-brief.yml` (repo root). It runs
at 05:00 and 06:00 UTC; the pipeline's send-time guard ensures it only actually
sends at 07:00 Europe/Rome regardless of DST.

Add these in **Settings → Secrets and variables → Actions**:

| Type     | Name                  | Value                          |
| -------- | --------------------- | ------------------------------ |
| Secret   | `ANTHROPIC_API_KEY`   | your Claude API key            |
| Secret   | `GMAIL_USER`          | your Gmail address             |
| Secret   | `GMAIL_APP_PASSWORD`  | Gmail app password             |
| Secret   | `BRIEF_TO`            | recipient (optional)           |
| Variable | `BRIEF_MODEL`         | `claude-sonnet-4-6` (optional) |

You can also trigger it manually from the **Actions** tab (“Run workflow”).

## Configuration (`data/config.json`)

| Field        | Meaning                                   |
| ------------ | ----------------------------------------- |
| `interests`  | topics that shape story selection         |
| `numStories` | how many stories to cover (default 10)    |
| `wordCount`  | target essay length (default 3000)        |
| `timezone`   | IANA tz for the send-time guard           |
| `sendHour`   | local hour to send (0–23)                 |
| `recipient`  | delivery email address                    |
| `readerName` | optional, used in the greeting            |

Edit it directly or via the Settings page.

## Notes

- The archive (`data/archive/`) is written locally and is git-ignored. In the
  GitHub Actions run it's ephemeral — the **email is the deliverable**. Run the
  pipeline locally (or deploy the web app with a persistent volume) if you want a
  browsable archive.
- Model defaults to `claude-sonnet-4-6`; set `BRIEF_MODEL=claude-opus-4-8` for
  maximum quality.
