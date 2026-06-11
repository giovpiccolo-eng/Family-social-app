import { NextRequest, NextResponse } from "next/server";
import { FieldValue } from "firebase-admin/firestore";
import { adminDb, adminStorage } from "@/lib/firebase/admin";
import { AuthError, verifyRequest } from "@/lib/firebase/server-auth";
import { transcribeAudio } from "@/lib/ai/openai";

export const runtime = "nodejs";
export const maxDuration = 300; // 5 minutes for long lessons

// POST /api/observations/:id/transcribe
// Pulls the audio from Storage, sends to Whisper, writes the transcript back.
export async function POST(
  req: NextRequest,
  { params }: { params: { id: string } },
) {
  try {
    const auth = await verifyRequest(req);
    const obsRef = adminDb().collection("observations").doc(params.id);
    const snap = await obsRef.get();
    if (!snap.exists) {
      return NextResponse.json({ error: "Observation not found" }, { status: 404 });
    }
    const obs = snap.data()!;
    if (obs.schoolId !== auth.schoolId) {
      return NextResponse.json({ error: "Cross-tenant access denied" }, { status: 403 });
    }
    if (!obs.audioPath) {
      return NextResponse.json({ error: "audioPath missing" }, { status: 400 });
    }

    await obsRef.update({ status: "transcribing" });

    const [buffer] = await adminStorage().bucket().file(obs.audioPath).download();
    const bytes = new Uint8Array(buffer);
    const blob = new Blob([bytes], { type: "audio/webm" });

    const transcript = await transcribeAudio(blob);

    await obsRef.update({
      transcript,
      status: "transcribing", // remains until analyse flips to 'analysing'
      transcribedAt: FieldValue.serverTimestamp(),
    });

    return NextResponse.json({ ok: true, length: transcript.length });
  } catch (err) {
    if (err instanceof AuthError) {
      return NextResponse.json({ error: err.message }, { status: err.status });
    }
    const message = err instanceof Error ? err.message : "Unknown error";
    await adminDb()
      .collection("observations")
      .doc(params.id)
      .update({ status: "error", error: `transcribe: ${message}` })
      .catch(() => {});
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
