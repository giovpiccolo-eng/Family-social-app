/**
 * Entry point for the daily job (and manual runs).
 *
 *   npm run brief         # respects the send-time guard
 *   npm run brief:force   # FORCE=1, generate + send right now
 *
 * Flags:
 *   --force        bypass the send-time guard
 *   --no-email     generate + archive only, don't send
 */
import { runMorningBrief } from "../lib/pipeline";

async function main() {
  const args = process.argv.slice(2);
  const force = args.includes("--force") || process.env.FORCE === "1";
  const skipEmail = args.includes("--no-email");

  console.log(`[morning-brief] starting (force=${force}, skipEmail=${skipEmail})`);

  const outcome = await runMorningBrief({ force, skipEmail });

  if (!outcome.ran) {
    console.log(`[morning-brief] not run: ${outcome.reason}`);
    return;
  }

  console.log(`[morning-brief] generated "${outcome.title}" for ${outcome.date}`);
  console.log(`[morning-brief] archived to ${outcome.archivePath}`);
  console.log(`[morning-brief] emailed: ${outcome.emailed}`);
}

main().catch((err) => {
  console.error("[morning-brief] FAILED:", err?.message || err);
  process.exit(1);
});
