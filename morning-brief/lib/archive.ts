import { promises as fs } from "fs";
import path from "path";
import type { BriefResult } from "./types";

const ARCHIVE_DIR = path.join(process.cwd(), "data", "archive");

export interface ArchiveEntry {
  date: string;
  title: string;
}

/** Persist a generated brief to the archive as a Markdown file with front matter. */
export async function saveToArchive(result: BriefResult): Promise<string> {
  await fs.mkdir(ARCHIVE_DIR, { recursive: true });
  const filePath = path.join(ARCHIVE_DIR, `${result.date}.md`);
  const frontMatter = `---\ntitle: ${JSON.stringify(result.title)}\ndate: ${result.date}\n---\n\n`;
  await fs.writeFile(filePath, frontMatter + result.markdown + "\n", "utf8");
  return filePath;
}

/** List archived briefs, newest first. */
export async function listArchive(): Promise<ArchiveEntry[]> {
  let files: string[];
  try {
    files = await fs.readdir(ARCHIVE_DIR);
  } catch {
    return [];
  }
  const entries: ArchiveEntry[] = [];
  for (const f of files) {
    if (!f.endsWith(".md")) continue;
    const date = f.replace(/\.md$/, "");
    const raw = await fs.readFile(path.join(ARCHIVE_DIR, f), "utf8");
    const titleMatch = raw.match(/^title:\s*(.+)$/m) || raw.match(/^#\s+(.+)$/m);
    let title = date;
    if (titleMatch) {
      try {
        title = JSON.parse(titleMatch[1]);
      } catch {
        title = titleMatch[1].trim();
      }
    }
    entries.push({ date, title });
  }
  return entries.sort((a, b) => (a.date < b.date ? 1 : -1));
}

/** Read one archived brief's Markdown body (front matter stripped). */
export async function readArchive(date: string): Promise<{ title: string; markdown: string } | null> {
  // Guard against path traversal — only YYYY-MM-DD is valid.
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return null;
  try {
    const raw = await fs.readFile(path.join(ARCHIVE_DIR, `${date}.md`), "utf8");
    const titleMatch = raw.match(/^title:\s*(.+)$/m);
    let title = date;
    if (titleMatch) {
      try {
        title = JSON.parse(titleMatch[1]);
      } catch {
        title = titleMatch[1].trim();
      }
    }
    const body = raw.replace(/^---\n[\s\S]*?\n---\n/, "").trim();
    return { title, markdown: body };
  } catch {
    return null;
  }
}
