/**
 * Export an archived brief to a PDF.
 *
 *   npm run pdf 2026-06-01            # -> data/archive/2026-06-01.pdf
 *   npm run pdf 2026-06-01 out.pdf    # custom output path
 */
import { promises as fs } from "fs";
import path from "path";
import { readArchive } from "../lib/archive";
import { renderPdf } from "../lib/pdf";

function longDate(date: string): string {
  const d = new Date(date + "T00:00:00");
  return new Intl.DateTimeFormat("en-GB", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(d);
}

async function main() {
  const date = process.argv[2];
  if (!date || !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    console.error("Usage: npm run pdf <YYYY-MM-DD> [output.pdf]");
    process.exit(1);
  }
  const entry = await readArchive(date);
  if (!entry) {
    console.error(`No archived brief found for ${date}.`);
    process.exit(1);
  }
  const out = process.argv[3] || path.join(process.cwd(), "data", "archive", `${date}.pdf`);
  const buf = await renderPdf(entry.markdown, { title: entry.title, dateLabel: longDate(date) });
  await fs.writeFile(out, buf);
  console.log(`Wrote ${out} (${Math.round(buf.length / 1024)} KB)`);
}

main().catch((e) => {
  console.error("PDF export failed:", e?.message || e);
  process.exit(1);
});
