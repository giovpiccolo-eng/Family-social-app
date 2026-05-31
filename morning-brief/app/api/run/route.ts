import { NextResponse } from "next/server";
import { runMorningBrief } from "@/lib/pipeline";

export const dynamic = "force-dynamic";
// Generation + web search can take a while; give it room.
export const maxDuration = 300;

/**
 * Manually trigger a brief (used by the "Send a brief now" button).
 * Forces past the send-time guard. Optional ?noEmail=1 to skip sending.
 */
export async function POST(request: Request) {
  const url = new URL(request.url);
  const skipEmail = url.searchParams.get("noEmail") === "1";
  try {
    const outcome = await runMorningBrief({ force: true, skipEmail });
    return NextResponse.json(outcome);
  } catch (e) {
    return NextResponse.json({ error: (e as Error).message }, { status: 500 });
  }
}
