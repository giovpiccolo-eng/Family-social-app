import { NextRequest, NextResponse } from "next/server";
import { FieldValue } from "firebase-admin/firestore";
import { adminDb, adminStorage } from "@/lib/firebase/admin";
import { AuthError, verifyRequest } from "@/lib/firebase/server-auth";
import { analyseObservation, type ClassroomContext } from "@/lib/ai/analysis";
import { sanitiseRosterForAI } from "@/lib/ai/sanitise";
import { componentDomain, ratingToNumber } from "@/lib/danielson";
import type {
  ClassRoster,
  Evidence,
  Note,
  ObservationAnalysis,
  Teacher,
} from "@/types";

export const runtime = "nodejs";
export const maxDuration = 300;

// POST /api/observations/:id/analyse
// Reads transcript + notes + roster + classroom context, runs the two-pass
// Claude analysis, writes the analysis sub-document, and denormalises mappings
// to the evidence collection (brief §2.2 step 9).
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
    if (!obs.transcript) {
      return NextResponse.json({ error: "Transcript missing" }, { status: 400 });
    }

    await obsRef.update({ status: "analysing" });

    const teacher = await loadTeacher(obs.teacherId as string);
    const roster = teacher?.sisClassId
      ? await loadRoster(auth.schoolId, teacher.sisClassId)
      : null;
    const classroomContext = teacher?.classroomCourseId
      ? await loadClassroom(auth.schoolId, teacher.classroomCourseId)
      : undefined;

    const photos = await loadPhotos(
      (obs.photoPaths ?? []) as string[],
      (obs.notes ?? []) as Note[],
    );

    // CRITICAL — every roster payload bound for AI must pass through this
    // function. CHILD_PROTECTION is stripped here (brief §6.2).
    const sanitised = roster ? sanitiseRosterForAI(roster) : undefined;

    const { analysis, warnings } = await analyseObservation({
      transcript: obs.transcript as string,
      notes: (obs.notes ?? []) as Note[],
      classroom: classroomContext,
      roster: sanitised,
      photos,
    });

    await obsRef.update({
      analysis,
      status: "complete",
      analysisWarnings: warnings,
      analysedAt: FieldValue.serverTimestamp(),
    });

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
      .update({ status: "error", error: `analyse: ${message}` })
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

async function loadClassroom(
  _schoolId: string,
  _courseId: string,
): Promise<ClassroomContext | undefined> {
  // Stub for Sprint 3 — real Google Classroom OAuth lands in Sprint 5.
  return undefined;
}

type AcceptedImageType = "image/jpeg" | "image/png" | "image/gif" | "image/webp";

function normaliseMediaType(s: string | undefined): AcceptedImageType {
  switch (s) {
    case "image/png":
      return "image/png";
    case "image/gif":
      return "image/gif";
    case "image/webp":
      return "image/webp";
    default:
      return "image/jpeg";
  }
}

async function loadPhotos(
  paths: string[],
  notes: Note[],
): Promise<
  | { timestamp: string; base64: string; mediaType: AcceptedImageType }[]
  | undefined
> {
  if (!paths.length) return undefined;
  const bucket = adminStorage().bucket();
  const out: {
    timestamp: string;
    base64: string;
    mediaType: AcceptedImageType;
  }[] = [];
  for (const path of paths) {
    try {
      const file = bucket.file(path);
      const [buffer] = await file.download();
      const [meta] = await file.getMetadata();
      const mediaType = normaliseMediaType(meta.contentType as string | undefined);
      // Best-effort timestamp — UI tracks per-photo timestamps in state but
      // does not yet persist them; the nearest note timestamp is a reasonable
      // approximation. Persisting per-photo timestamps is a small follow-up.
      const timestamp = nearestTimestamp(notes);
      out.push({
        timestamp,
        base64: buffer.toString("base64"),
        mediaType,
      });
    } catch {
      // Skip unreadable photos rather than failing the whole analysis.
    }
  }
  return out;
}

function nearestTimestamp(notes: Note[]): string {
  if (notes.length === 0) return "00:00";
  return notes[notes.length - 1].timestamp;
}

interface WriteEvidenceInput {
  observationId: string;
  schoolId: string;
  teacherId: string;
  analysis: ObservationAnalysis;
}

async function writeEvidence({
  observationId,
  schoolId,
  teacherId,
  analysis,
}: WriteEvidenceInput) {
  if (!analysis.mappings.length) return;
  const batch = adminDb().batch();
  const col = adminDb().collection("evidence");
  for (const m of analysis.mappings) {
    let domain: 1 | 2 | 3 | 4;
    try {
      domain = componentDomain(m.component);
    } catch {
      continue; // unknown component, skip rather than corrupt the dashboard
    }
    const ref = col.doc();
    const ev: Omit<Evidence, "id" | "createdAt"> & {
      createdAt: FirebaseFirestore.FieldValue;
    } = {
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
    };
    batch.set(ref, ev);
  }
  await batch.commit();
}
