import { promises as fs } from "fs";
import path from "path";
import type { BriefConfig } from "./types";

const CONFIG_PATH = path.join(process.cwd(), "data", "config.json");

const DEFAULT_CONFIG: BriefConfig = {
  interests: [
    "AI",
    "education",
    "schools",
    "neuroscience",
    "astrophysics",
    "geopolitics",
    "leadership",
    "finance",
    "history",
    "literature",
  ],
  numStories: 10,
  wordCount: 3000,
  timezone: "Europe/Rome",
  sendHour: 7,
  recipient: process.env.BRIEF_TO || process.env.GMAIL_USER || "",
  readerName: "",
};

export async function loadConfig(): Promise<BriefConfig> {
  try {
    const raw = await fs.readFile(CONFIG_PATH, "utf8");
    const parsed = JSON.parse(raw) as Partial<BriefConfig>;
    return { ...DEFAULT_CONFIG, ...parsed };
  } catch {
    return { ...DEFAULT_CONFIG };
  }
}

export async function saveConfig(config: BriefConfig): Promise<void> {
  await fs.mkdir(path.dirname(CONFIG_PATH), { recursive: true });
  await fs.writeFile(CONFIG_PATH, JSON.stringify(config, null, 2) + "\n", "utf8");
}

/** Sanitise/normalise an incoming config (e.g. from the settings form). */
export function normalizeConfig(input: Partial<BriefConfig>): BriefConfig {
  const merged = { ...DEFAULT_CONFIG, ...input };
  return {
    interests: (merged.interests || [])
      .map((s) => String(s).trim())
      .filter(Boolean),
    numStories: clamp(Number(merged.numStories) || 10, 1, 25),
    wordCount: clamp(Number(merged.wordCount) || 3000, 500, 8000),
    timezone: String(merged.timezone || "Europe/Rome"),
    sendHour: clamp(Number(merged.sendHour) || 7, 0, 23),
    recipient: String(merged.recipient || "").trim(),
    readerName: String(merged.readerName || "").trim(),
  };
}

function clamp(n: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, n));
}

export { DEFAULT_CONFIG };
