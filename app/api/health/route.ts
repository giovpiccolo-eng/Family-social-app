import { NextResponse } from "next/server";

export function GET() {
  return NextResponse.json({
    ok: true,
    service: "observer-ai",
    sprint: 1,
    timestamp: new Date().toISOString(),
  });
}
