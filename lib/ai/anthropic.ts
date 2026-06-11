import "server-only";
import Anthropic from "@anthropic-ai/sdk";

let client: Anthropic | null = null;

export function anthropic(): Anthropic {
  if (!client) {
    const apiKey = process.env.ANTHROPIC_API_KEY;
    if (!apiKey) throw new Error("ANTHROPIC_API_KEY not set");
    client = new Anthropic({ apiKey });
  }
  return client;
}

export const ANTHROPIC_MODEL =
  process.env.ANTHROPIC_MODEL ?? "claude-sonnet-4-5";

export type ImageMediaType =
  | "image/jpeg"
  | "image/png"
  | "image/gif"
  | "image/webp";

export type ContentBlock =
  | { type: "text"; text: string }
  | {
      type: "image";
      source: { type: "base64"; media_type: ImageMediaType; data: string };
    };

// Wrapper that returns the concatenated text content from a message response.
export async function complete({
  system,
  user,
  maxTokens,
}: {
  system: string;
  user: string | ContentBlock[];
  maxTokens: number;
}): Promise<string> {
  const messages = [
    {
      role: "user" as const,
      content:
        typeof user === "string"
          ? [{ type: "text" as const, text: user }]
          : user,
    },
  ];

  const resp = await anthropic().messages.create({
    model: ANTHROPIC_MODEL,
    max_tokens: maxTokens,
    system,
    messages,
  });

  return resp.content
    .map((block) => (block.type === "text" ? block.text : ""))
    .join("");
}
