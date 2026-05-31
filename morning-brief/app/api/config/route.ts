import { NextResponse } from "next/server";
import { loadConfig, saveConfig, normalizeConfig } from "@/lib/config";

export const dynamic = "force-dynamic";

export async function GET() {
  const config = await loadConfig();
  return NextResponse.json(config);
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const config = normalizeConfig(body);
    await saveConfig(config);
    return NextResponse.json(config);
  } catch (e) {
    return NextResponse.json({ error: (e as Error).message }, { status: 400 });
  }
}
