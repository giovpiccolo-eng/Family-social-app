# Observer.AI

Web-based AI platform for automated teacher evaluation using the Danielson
Framework for Teaching (2022 edition).

> Compresses the teacher observation cycle for international schools from 3–5
> hours to under 30 minutes by integrating lesson audio, principal notes,
> Google Classroom, and SIS data into an evidence-anchored Danielson
> evaluation.

## Status

**Sprint 3 — AI pipeline.** Whisper transcription, two-pass Claude analysis
(narrative + mappings), tolerant JSON parser, evidence denormalisation, and
the live processing screen are in place. Sprint 4 (summary / evidence bank /
report drafting / bias filter UI) is next.

## Stack

- **Next.js 14** (App Router) · TypeScript · Tailwind · shadcn-style UI
  primitives
- **Firebase**: Auth · Firestore · Storage. Multi-tenant via `schoolId`
  custom claims.
- **Anthropic** `claude-sonnet-4-5` for analysis (server-side only). Two-pass
  architecture per the build brief.
- **OpenAI Whisper** for server-side transcription.
- **Vercel** for hosting; env vars carry all secrets.

## Repo layout

```
app/
  (auth)/        login + invite acceptance
  (app)/         authenticated shell (dashboard, teachers, observations)
  api/           server routes (invite accept, health, future AI pipeline)
components/      Topbar, Screen, BrandMark, shadcn UI primitives
lib/
  firebase/      client + admin SDK glue, AuthProvider
  danielson.ts   Framework constants (22 components, 4 domains, ratings)
  utils.ts       cn helper, timestamp formatter
types/           Data model — mirrors brief §3 exactly
firestore.rules  Multi-tenant rules (per-school read/write isolation)
storage.rules    Audio, photos, SIS roster paths
```

## Running locally

```bash
cp .env.example .env.local   # fill in Firebase + provider credentials
npm install
npm run dev                  # http://localhost:3000
```

Use the Firebase emulators while iterating:

```bash
npx firebase emulators:start
```

## Data model

All types live in [`types/index.ts`](types/index.ts) and match brief §3.
Critical shapes — `ObservationAnalysis`, `Mapping`, `CorrelatedMoment` — are
the contract between the AI pipeline (Sprint 3) and the UI (Sprint 4).

## Security & privacy

- **Multi-tenancy.** Every document carries `schoolId`. Firestore rules deny
  cross-tenant reads/writes. Custom claims (`schoolId`, `role`) are set by
  the server on invite acceptance.
- **CHILD_PROTECTION flag.** Per brief §6.2, this student flag must never
  appear in AI prompts. The `sanitiseRosterForAI` function (Sprint 5) will
  enforce this on every payload built for Claude.
- **API keys.** Anthropic and OpenAI credentials are server-only. Client
  uploads audio and photos directly to Firebase Storage via signed URLs.

## Build plan

| Sprint | Focus | Status |
|--------|-------|--------|
| 1 | Foundation: Next.js, Firebase, auth, visual tokens, layout | ✅ |
| 2 | Teachers CRUD + live recording (MediaRecorder, notes, photos) | ✅ |
| 3 | AI pipeline: Whisper + two-pass Claude analysis | ✅ this commit |
| 4 | Summary, evidence bank, report drafting, bias filter | ⏳ |
| 5 | Google Classroom OAuth + SIS adapter (CSV + stub) | ⏳ |
| 6 | Dashboard, settings, audit log, Sentry, consent flow | ⏳ |
| 7 | Pilot prep (optional) | ⏳ |

The build order is non-negotiable per the brief — each sprint unlocks the
next sprint's dependencies.

## Open questions for product owner

Pulled from brief §10.1 — answers needed before Sprints 4–6 close:

- Per-school custom Danielson rubric weightings?
- Should teachers see their own reports automatically, or only after
  principal sign-off?
- How are disputes / appeals handled in the data model (add a `disputed`
  status)?
- Multi-language lessons — Whisper supports many languages; reports in
  English only?
- Pricing tier feature gating — which features are pilot vs standard vs
  enterprise?
