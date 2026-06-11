import { NextRequest, NextResponse } from "next/server";
import { FieldValue } from "firebase-admin/firestore";
import { adminDb, adminStorage } from "@/lib/firebase/admin";
import { AuthError, verifyRequest } from "@/lib/firebase/server-auth";
import { transcribeAudio } from "@/lib/ai/openai";
import { analyseObservation } from "@/lib/ai/analysis";
import { sanitiseRosterForAI } from "@/lib/ai/sanitise";
import { componentDomain, ratingToNumber } from "@/lib/danielson";
import type { ClassRoster, Note, ObservationAnalysis, Teacher } from "@/types";

export const runtime = "nodejs";
export const maxDuration = 300;

// POST /api/observations/:id/process
// One-shot pipeline: transcribe → analyse → denormalise evidence.
// Client typically calls this once after Stop; the processing screen listens
// to Firestore for status updates.
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

    // 1. Transcribe.
    await obsRef.update({ status: "transcribing" });
    const [audioBuf] = await adminStorage().bucket().file(obs.audioPath).download();
    const audioBytes = new Uint8Array(audioBuf);
    const transcript = await transcribeAudio(
      new Blob([audioBytes], { type: "audio/webm" }),
    );
    await obsRef.update({
      transcript,
      transcribedAt: FieldValue.serverTimestamp(),
      status: "analysing",
    });

    // 2. Load teacher + roster + classroom; sanitise; run two-pass analysis.
    const teacher = await loadTeacher(obs.teacherId as string);
    const roster = teacher?.sisClassId
      ? await loadRoster(auth.schoolId, teacher.sisClassId)
      : null;
    const sanitised = roster ? sanitiseRosterForAI(roster) : undefined;

    const { analysis, warnings } = await analyseObservation({
      transcript,
      notes: (obs.notes ?? []) as Note[],
      roster: sanitised,
    });

    await obsRef.update({
      analysis,
      analysisWarnings: warnings,
      analysedAt: FieldValue.serverTimestamp(),
      status: "complete",
    });

    // 3. Denormalise mappings to evidence collection.
    await writeEvidence({
      observationId: params.id,
      schoolId: auth.schoolId,
      teacherId: obs.teacherId as string,
      analysis,
    });

    return NextResponse.json({ ok: true, warnings });
  } catch (err) {
    if (err instanceof AuthError) {
      return NextResponse.json({ error: err.message }, { status: err.status });
    }
    const message = err instanceof Error ? err.message : "Unknown error";
    await adminDb()
      .collection("observations")
      .doc(params.id)
      .update({ status: "error", error: `process: ${message}` })
      .catch(() => {});
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

async function loadTeacher(teacherId: string): Promise<Teacher | null> {
  const snap = await adminDb().collection("teachers").doc(teacherId).get();
  if (!snap.exists) return null;
  return { id: snap.id, ...(snap.data() as Omit<Teacher, "id">) };
}

async function loadRoster(
  schoolId: string,
  sisClassId: string,
): Promise<ClassRoster | null> {
  const snap = await adminDb()
    .collection("classes")
    .where("schoolId", "==", schoolId)
    .where("classCode", "==", sisClassId)
    .limit(1)
    .get();
  if (snap.empty) return null;
  const doc = snap.docs[0];
  return { id: doc.id, ...(doc.data() as Omit<ClassRoster, "id">) };
}

async function writeEvidence({
  observationId,
  schoolId,
  teacherId,
  analysis,
}: {
  observationId: string;
  schoolId: string;
  teacherId: string;
  analysis: ObservationAnalysis;
}) {
  if (!analysis.mappings.length) return;
  const batch = adminDb().batch();
  const col = adminDb().collection("evidence");
  for (const m of analysis.mappings) {
    let domain: 1 | 2 | 3 | 4;
    try {
      domain = componentDomain(m.component);
    } catch {
      continue;
    }
    const ref = col.doc();
    batch.set(ref, {
      schoolId,
      teacherId,
      observationId,
      component: m.component,
      domain,
      rating: m.rating,
      ratingNum: ratingToNumber(m.rating),
      evidence: m.evidence,
      confidence: m.confidence,
      sourceTimestamp: m.sourceTimestamp,
      sourceStreams: m.sourceStreams,
      biasFlag: m.biasFlag,
      createdAt: FieldValue.serverTimestamp(),
    });
  }
  await batch.commit();
}
