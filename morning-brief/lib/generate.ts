import Anthropic from "@anthropic-ai/sdk";
import type { BriefConfig, BriefResult } from "./types";

const MODEL = process.env.BRIEF_MODEL || "claude-sonnet-4-6";

/**
 * Today's date in the reader's timezone, as YYYY-MM-DD.
 */
export function localDate(timezone: string, when: Date = new Date()): string {
  // en-CA gives ISO-style YYYY-MM-DD formatting.
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: timezone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(when);
}

/** Long, human-friendly date string in the reader's timezone. */
function longDate(timezone: string, when: Date = new Date()): string {
  return new Intl.DateTimeFormat("en-GB", {
    timeZone: timezone,
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(when);
}

const SYSTEM_PROMPT = `You are "Morning Brief", an erudite editor and teacher who writes a single daily long-form essay that helps a curious, intelligent reader both keep up with the news AND learn something lasting from it.

Your style:
- Write like a brilliant magazine essayist (think The Economist meets Aeon): clear, confident, intellectually generous, never breathless or clickbaity.
- You do not merely summarise headlines. For each story you weave in genuine context: the history behind it, the underlying science or economics, key concepts, people and their ideas, and connections across fields. The reader should finish each section knowing something they did not know before.
- Surface non-obvious connections between stories and across the reader's interests.
- Be accurate and grounded. Only state facts you can support from your research. Attribute claims and include the source links you actually used.
- Be calm and balanced on contentious topics; present multiple perspectives.

Hard rules:
- Use the web_search tool to find genuinely recent stories (prefer the last 24-48 hours). Do not invent events, quotes, or statistics.
- Every factual claim about current events must trace to a source you actually found.
- Output GitHub-Flavored Markdown only. No preamble like "Here is your brief" — start directly with the title.`;

function buildUserPrompt(config: BriefConfig, dateStr: string): string {
  const interests = config.interests.join(", ");
  return `Create today's Morning Brief for ${longDate(config.timezone)} (local date ${dateStr}).

Reader's interests (use these to choose and frame stories): ${interests}.

Steps:
1. Use web_search to find the top ${config.numStories} most significant and interesting news stories from roughly the last 24-48 hours that connect to the reader's interests. Favour substance over sensation. Spread coverage across the interest areas rather than clustering on one.
2. Write a single cohesive essay of approximately ${config.wordCount} words (this is important — aim for the full length, do not stop short).

Structure the essay as:
- A title line as a Markdown H1 (#). Make it evocative and specific to today.
- A short italic standfirst/epigraph (1-2 sentences) framing the day.
- A 3-5 sentence opening that sets the scene and previews the threads.
- One section per story (Markdown H2, ##). For each: explain what happened, then go deeper — the context, the concepts, the history or science, why it matters, and an interesting fact or connection the reader probably doesn't know. Link inline to the source(s) you used.
- A closing "## The thread" section (~150 words) that ties the day's stories together into one reflection or lesson.
- A final "## Sources" section: a Markdown bullet list of every source you cited, as [Title](url).

Write the whole thing now. Target ~${config.wordCount} words.`;
}

export async function generateBrief(config: BriefConfig): Promise<BriefResult> {
  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey) {
    throw new Error("ANTHROPIC_API_KEY is not set. Add it to your environment / .env file.");
  }

  const client = new Anthropic({ apiKey });
  const dateStr = localDate(config.timezone);

  const response = await client.messages.create({
    model: MODEL,
    // ~3000 words of essay plus headroom for the markdown/sources.
    max_tokens: 8000,
    system: [
      {
        type: "text",
        text: SYSTEM_PROMPT,
        // Cache the (stable) instructions to cut cost on repeated daily runs.
        cache_control: { type: "ephemeral" },
      },
    ],
    messages: [
      {
        role: "user",
        content: buildUserPrompt(config, dateStr),
      },
    ],
    tools: [
      {
        type: "web_search_20250305",
        name: "web_search",
        max_uses: Math.max(8, config.numStories + 4),
      } as Anthropic.Messages.ToolUnion,
    ],
  });

  const { markdown, sources } = extractContent(response);

  if (!markdown.trim()) {
    throw new Error("Claude returned no essay text. Check the API response / model id.");
  }

  return {
    date: dateStr,
    title: extractTitle(markdown) || `Morning Brief — ${dateStr}`,
    markdown,
    sources,
  };
}

/** Pull the essay text and any cited source URLs out of the API response. */
function extractContent(response: Anthropic.Messages.Message): {
  markdown: string;
  sources: { title: string; url: string }[];
} {
  const textParts: string[] = [];
  const sourceMap = new Map<string, string>();

  for (const block of response.content) {
    if (block.type === "text") {
      textParts.push(block.text);
      // Citations attached to text blocks (from web search) carry source URLs.
      const citations = (block as { citations?: unknown[] }).citations;
      if (Array.isArray(citations)) {
        for (const c of citations as Array<Record<string, unknown>>) {
          const url = typeof c.url === "string" ? c.url : undefined;
          const title = typeof c.title === "string" ? c.title : url;
          if (url && !sourceMap.has(url)) sourceMap.set(url, title || url);
        }
      }
    } else if (block.type === "web_search_tool_result") {
      const content = (block as { content?: unknown }).content;
      if (Array.isArray(content)) {
        for (const r of content as Array<Record<string, unknown>>) {
          const url = typeof r.url === "string" ? r.url : undefined;
          const title = typeof r.title === "string" ? r.title : url;
          if (url && !sourceMap.has(url)) sourceMap.set(url, title || url);
        }
      }
    }
  }

  return {
    markdown: textParts.join("").trim(),
    sources: Array.from(sourceMap.entries()).map(([url, title]) => ({ url, title })),
  };
}

function extractTitle(markdown: string): string | null {
  const match = markdown.match(/^#\s+(.+)$/m);
  return match ? match[1].trim() : null;
}
