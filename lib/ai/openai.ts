import "server-only";
import OpenAI from "openai";

let client: OpenAI | null = null;

export function openai(): OpenAI {
  if (!client) {
    const apiKey = process.env.OPENAI_API_KEY;
    if (!apiKey) throw new Error("OPENAI_API_KEY not set");
    client = new OpenAI({ apiKey });
  }
  return client;
}

export const WHISPER_MODEL =
  process.env.OPENAI_WHISPER_MODEL ?? "whisper-1";

// Transcribe an audio Blob via Whisper. Throws on API failure; callers retry.
export async function transcribeAudio(blob: Blob, filename = "audio.webm"): Promise<string> {
  const file = new File([blob], filename, { type: blob.type || "audio/webm" });
  const resp = await openai().audio.transcriptions.create({
    file,
    model: WHISPER_MODEL,
    response_format: "text",
  });
  return typeof resp === "string" ? resp : (resp as { text: string }).text;
}
