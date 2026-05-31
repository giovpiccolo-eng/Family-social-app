export interface BriefConfig {
  /** Topics the reader wants covered each morning. */
  interests: string[];
  /** How many top stories to surface (default 10). */
  numStories: number;
  /** Approximate target length of the essay in words (default 3000). */
  wordCount: number;
  /** IANA timezone used for the send-time guard, e.g. "Europe/Rome". */
  timezone: string;
  /** Local hour (0-23) at which the brief should be sent. */
  sendHour: number;
  /** Email address the brief is delivered to. */
  recipient: string;
  /** Optional name used to personalise the greeting. */
  readerName?: string;
}

export interface BriefResult {
  /** ISO date (YYYY-MM-DD) the brief was generated for, in the reader's timezone. */
  date: string;
  /** Human-friendly title for the day's brief. */
  title: string;
  /** The full essay in Markdown. */
  markdown: string;
  /** Source URLs Claude cited while researching. */
  sources: { title: string; url: string }[];
}
