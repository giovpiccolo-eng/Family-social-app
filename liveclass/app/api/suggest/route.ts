import Anthropic from "@anthropic-ai/sdk";
import { zodOutputFormat } from "@anthropic-ai/sdk/helpers/zod";
import { z } from "zod";
import { FRENCH_REVOLUTION_KIT } from "@/lib/sources/french-revolution";
import { TRACKS } from "@/lib/types";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const client = new Anthropic();

const CardSchema = z.object({
  track: z.enum(TRACKS),
  title: z.string().min(3).max(80),
  summary: z.string().min(10).max(400),
  source_id: z.string().nullable(),
  why_now: z.string().min(8).max(220),
});

const ResponseSchema = z.object({
  inferred_topic: z.string().min(2).max(120),
  pedagogical_move: z.enum([
    "lecturing",
    "questioning",
    "demonstrating",
    "transitioning",
    "reviewing",
    "other",
  ]),
  cards: z.array(CardSchema).max(6),
});

const SYSTEM = `You are LiveClass, an AI co-pilot listening to a teacher's live lesson and suggesting vetted material for the teacher to push to students.

Hard rules — these are the product's spine, not preferences:
1. NOTHING reaches a screen unless the teacher chooses it. You propose; the teacher disposes. Never speak directly to students.
2. You may ONLY cite sources from the provided lesson kit. If no kit source fits, set source_id to null and synthesise a card description without inventing citations.
3. Suggest cards that are PEDAGOGICALLY USEFUL RIGHT NOW. Not every transcript chunk needs a card on every track. Returning fewer than six cards — sometimes zero — is correct. Quality over coverage.
4. Each card title should fit on a phone screen (~60 chars). Each summary is 1–2 sentences of what the teacher sees on the card itself. Each why_now is one sentence explaining the pedagogical fit with what the teacher just said.
5. The six tracks have distinct purposes:
   - evidence: primary sources, archives, datasets, quotations.
   - visuals: maps, diagrams, paintings, photographs, charts.
   - media: video / audio clips.
   - practice: a short exercise or check-for-understanding prompt the teacher could push to one student or the class.
   - inclusion: an adapted version of material on the wall — plain-language rewrite, audio, high-contrast — for a student with a learning difference. The badge a student would see reads "Adapted for you", never naming the difference.
   - deep-dive: an optional extension for a student ready to go further.

Also infer the pedagogical_move underway: lecturing, questioning, demonstrating, transitioning, reviewing, or other.`;

function buildKitDescription() {
  const kit = FRENCH_REVOLUTION_KIT;
  const sources = kit.sources
    .map((s) => `- [${s.id}] (${s.kind}, ${s.year ?? "n.d."}) "${s.title}" — ${s.citation}\n  ${s.blurb}`)
    .join("\n");
  return `LESSON KIT — only sources below may be cited (use source_id verbatim).

Title: ${kit.title}
Objectives:
${kit.objectives.map((o) => `- ${o}`).join("\n")}
Outline:
${kit.outline.map((o) => `- ${o}`).join("\n")}

Sources:
${sources}`;
}

export async function POST(req: Request) {
  if (!process.env.ANTHROPIC_API_KEY) {
    return Response.json(
      { error: "ANTHROPIC_API_KEY is not set on the server." },
      { status: 500 },
    );
  }

  let body: { transcript?: string; prior?: string };
  try {
    body = await req.json();
  } catch {
    return Response.json({ error: "Invalid JSON body." }, { status: 400 });
  }

  const transcript = (body.transcript ?? "").trim();
  if (transcript.length < 12) {
    return Response.json({ inferred_topic: "", pedagogical_move: "other", cards: [] });
  }

  const prior = (body.prior ?? "").slice(-1200);

  const userMessage = `${buildKitDescription()}

EARLIER IN THE LESSON (for context, last ~1200 chars):
${prior || "(none yet — this is the opening of the lesson)"}

TEACHER JUST SAID:
"${transcript}"

Generate up to six suggestion cards across the tracks that would be useful to push RIGHT NOW. Return zero cards on a track if nothing in the kit fits. Prefer high-precision picks over breadth.`;

  try {
    const response = await client.messages.parse({
      model: "claude-opus-4-8",
      max_tokens: 16000,
      thinking: { type: "adaptive" },
      output_config: {
        effort: "medium",
        format: zodOutputFormat(ResponseSchema),
      },
      system: [
        { type: "text", text: SYSTEM, cache_control: { type: "ephemeral" } },
      ],
      messages: [{ role: "user", content: userMessage }],
    });

    if (!response.parsed_output) {
      return Response.json(
        { error: "Model returned no parseable output." },
        { status: 502 },
      );
    }

    return Response.json(response.parsed_output);
  } catch (err) {
    if (err instanceof Anthropic.APIError) {
      return Response.json(
        { error: `Anthropic API error (${err.status}): ${err.message}` },
        { status: 502 },
      );
    }
    const message = err instanceof Error ? err.message : "Unknown error";
    return Response.json({ error: message }, { status: 500 });
  }
}
