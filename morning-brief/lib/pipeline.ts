import { loadConfig } from "./config";
import { generateBrief, localDate } from "./generate";
import { saveToArchive } from "./archive";
import { sendEmail } from "./email";
import { markdownToHtml, renderEmailHtml } from "./markdown";
import type { BriefConfig } from "./types";

export interface RunOptions {
  /** Bypass the send-time guard (manual / test runs). */
  force?: boolean;
  /** Generate + archive but don't send the email. */
  skipEmail?: boolean;
}

export interface RunOutcome {
  ran: boolean;
  reason?: string;
  date?: string;
  title?: string;
  archivePath?: string;
  emailed?: boolean;
}

/** Current local hour (0-23) in the given timezone. */
function localHour(timezone: string, when: Date = new Date()): number {
  const h = new Intl.DateTimeFormat("en-GB", {
    timeZone: timezone,
    hour: "2-digit",
    hour12: false,
  }).format(when);
  return parseInt(h, 10);
}

function longDateLabel(timezone: string): string {
  return new Intl.DateTimeFormat("en-GB", {
    timeZone: timezone,
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date());
}

/**
 * Run the full morning pipeline: time-guard -> generate -> archive -> email.
 * Returns an outcome describing what happened (so callers can log/report).
 */
export async function runMorningBrief(opts: RunOptions = {}): Promise<RunOutcome> {
  const config: BriefConfig = await loadConfig();

  // Time guard: the scheduler may fire at a couple of UTC times to cover DST,
  // so we only proceed when it's actually the configured local send hour.
  if (!opts.force) {
    const hour = localHour(config.timezone);
    if (hour !== config.sendHour) {
      return {
        ran: false,
        reason: `Local hour in ${config.timezone} is ${hour}, send hour is ${config.sendHour}. Skipping.`,
      };
    }
  }

  const result = await generateBrief(config);
  const archivePath = await saveToArchive(result);

  if (opts.skipEmail) {
    return { ran: true, date: result.date, title: result.title, archivePath, emailed: false };
  }

  const recipient = config.recipient || process.env.BRIEF_TO || process.env.GMAIL_USER || "";
  if (!recipient) {
    throw new Error("No recipient configured (set recipient in config or BRIEF_TO/GMAIL_USER).");
  }

  const bodyHtml = markdownToHtml(result.markdown);
  const dateLabel = longDateLabel(config.timezone);
  const greeting = config.readerName ? `Good morning, ${config.readerName}.` : "Good morning.";
  const html = renderEmailHtml({ title: result.title, bodyHtml, dateLabel, greeting });

  await sendEmail({
    to: recipient,
    subject: result.title,
    html,
    text: result.markdown,
  });

  return {
    ran: true,
    date: result.date,
    title: result.title,
    archivePath,
    emailed: true,
  };
}

export { localDate };
