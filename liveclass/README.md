# LiveClass — silent partner (v0)

An AI co-pilot that listens to a teacher's live lesson and surfaces vetted
material in real time, on a private console. The teacher chooses what to push.
Nothing reaches a student.

This is the **v0 silent partner**: one teacher, one topic (French Revolution),
one whitelisted source kit, console only. No student devices yet. Enough to
prove the listen → infer → suggest → curate loop.

## Stack

- Next.js 15 (app router) + TypeScript + Tailwind
- Anthropic SDK with `claude-opus-4-8` (adaptive thinking, structured outputs)
- Web Speech API for in-browser transcription (free; falls back to a text box
  in browsers that don't support it — Firefox, most mobile)

## Run it

```bash
cd liveclass
npm install
cp .env.example .env.local       # then paste your ANTHROPIC_API_KEY
npm run dev
```

Open <http://localhost:3000>. Start the mic (Chrome/Edge/Safari) **or** paste
transcript chunks into the text box. After about 150 chars of new transcript,
the console pings the model and cards stream in across six tracks: Evidence,
Visuals, Media, Practice, Inclusion, Deep dives.

## What's wired

- `app/api/suggest/route.ts` — server route that takes a transcript chunk +
  prior context, sends them to Claude with the whitelisted source kit, and
  returns up to six suggestion cards. Adaptive thinking on; structured output
  schema enforced via Zod.
- `lib/sources/french-revolution.ts` — the seeded source kit. Claude is
  instructed it may only cite `source_id`s from this list.
- `components/Console.tsx` — orchestrates the loop: mic / text input →
  transcript ticker → batched submission → six track columns.
- `components/MicCapture.tsx` — Web Speech API wrapper.
- The wall and student-device destinations are stubs (the buttons mark a card
  "✓ sent" but go nowhere). That's v0 scope per the package doc.

## What's stubbed for now

- **Wall push / student device push.** The two destination buttons mark cards
  as sent but the wall view and student view don't exist yet.
- **Per-student adaptation.** No student profiles yet; the Inclusion track
  produces generic "Adapted for you" cards rather than profile-specific ones.
- **Lesson kit upload.** The French Revolution kit is hardcoded. A real
  teacher would build their own.
- **RAG.** The kit is small (~12 sources), so we pass all metadata in the
  prompt and let Claude pick. With a real-scale library, swap in embeddings +
  retrieval.
- **Transcription quality.** Web Speech API is fine for a demo but you'll
  want Whisper or AssemblyAI for production.
