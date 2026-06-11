"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Screen, ScreenHeader } from "@/components/screen";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { RecorderPanel } from "@/components/recording/recorder-panel";
import { NotesPanel } from "@/components/recording/notes-panel";
import {
  PhotoStrip,
  type CapturedPhoto,
} from "@/components/recording/photo-strip";
import { useRecorder } from "@/lib/recording/use-recorder";
import { useAuth } from "@/lib/firebase/auth-context";
import { getTeacher } from "@/lib/firebase/teachers";
import {
  createObservation,
  setObservationStatus,
  uploadAudioBlob,
  uploadPhotoBlob,
} from "@/lib/firebase/observations";
import type { Note, Teacher } from "@/types";

type UploadStep = "idle" | "creating" | "uploading-audio" | "uploading-photos" | "finalising" | "done" | "error";

export default function ObservePage({
  params,
}: {
  params: { teacherId: string };
}) {
  const router = useRouter();
  const { user, claims } = useAuth();
  const recorder = useRecorder();
  const [teacher, setTeacher] = useState<Teacher | null>(null);
  const [notes, setNotes] = useState<Note[]>([]);
  const [photos, setPhotos] = useState<CapturedPhoto[]>([]);
  const [uploadStep, setUploadStep] = useState<UploadStep>("idle");
  const [uploadError, setUploadError] = useState<string | null>(null);

  useEffect(() => {
    getTeacher(params.teacherId).then(setTeacher).catch(() => setTeacher(null));
  }, [params.teacherId]);

  useEffect(() => {
    return () => {
      photos.forEach((p) => URL.revokeObjectURL(p.previewUrl));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleStop() {
    if (!user || !claims || !teacher) return;
    const audioBlob = await recorder.stop();
    if (!audioBlob) {
      setUploadError("No audio captured");
      setUploadStep("error");
      return;
    }
    try {
      setUploadError(null);
      setUploadStep("creating");
      // Create the observation doc first so we have an ID for storage paths.
      const observationId = await createObservation({
        schoolId: claims.schoolId,
        teacherId: teacher.id,
        observerId: user.uid,
        durationSec: recorder.elapsedSecs,
        audioPath: "", // patched below
        photoPaths: [],
        notes,
      });

      setUploadStep("uploading-audio");
      const audioPath = await uploadAudioBlob({
        schoolId: claims.schoolId,
        teacherId: teacher.id,
        observationId,
        blob: audioBlob,
      });

      setUploadStep("uploading-photos");
      const photoPaths: string[] = [];
      for (const p of photos) {
        const path = await uploadPhotoBlob({
          schoolId: claims.schoolId,
          teacherId: teacher.id,
          observationId,
          photoId: p.id,
          blob: p.blob,
        });
        photoPaths.push(path);
      }

      // Patch the observation doc with the storage paths.
      setUploadStep("finalising");
      const { getFirebase } = await import("@/lib/firebase/client");
      const { doc, updateDoc } = await import("firebase/firestore");
      const { db } = getFirebase();
      await updateDoc(doc(db, "observations", observationId), {
        audioPath,
        photoPaths,
      });
      // Sprint 3 will flip status to 'transcribing' and kick off the pipeline;
      // for Sprint 2 we leave the observation in 'recorded'.
      await setObservationStatus(observationId, "recorded");

      setUploadStep("done");
      router.push(`/teachers/${teacher.id}?obs=${observationId}`);
    } catch (err) {
      setUploadError(err instanceof Error ? err.message : "Upload failed");
      setUploadStep("error");
    }
  }

  if (!teacher) {
    return (
      <Screen>
        <p className="font-mono text-xs uppercase tracking-widest text-ink-faint">
          Loading teacher…
        </p>
      </Screen>
    );
  }

  const recording = recorder.state === "recording";
  const uploading =
    uploadStep !== "idle" && uploadStep !== "done" && uploadStep !== "error";

  return (
    <Screen>
      <ScreenHeader
        eyebrow={`${teacher.subject} · ${teacher.grade ?? "—"}`}
        title={`Observing ${teacher.name}`}
        description="Press record before the lesson starts. Type short, factual notes as you observe. Photos are captured directly from the camera or library."
        actions={
          <Link href={`/teachers/${teacher.id}`}>
            <Button variant="ghost" disabled={recording || uploading}>
              Cancel
            </Button>
          </Link>
        }
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardContent className="p-6">
            <RecorderPanel
              state={recorder.state}
              errorMessage={recorder.errorMessage}
              elapsedSecs={recorder.elapsedSecs}
              level={recorder.level}
              onStart={recorder.start}
              onStop={handleStop}
            />
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardContent className="flex h-[480px] flex-col p-6">
            <NotesPanel
              notes={notes}
              elapsedSecs={recorder.elapsedSecs}
              disabled={!recording}
              onAdd={(n) => setNotes((curr) => [...curr, n])}
            />
          </CardContent>
        </Card>

        <Card className="lg:col-span-3">
          <CardContent className="p-6">
            <PhotoStrip
              photos={photos}
              elapsedSecs={recorder.elapsedSecs}
              disabled={!recording}
              onAdd={(p) => setPhotos((curr) => [...curr, p])}
              onRemove={(id) => {
                setPhotos((curr) => {
                  const removed = curr.find((p) => p.id === id);
                  if (removed) URL.revokeObjectURL(removed.previewUrl);
                  return curr.filter((p) => p.id !== id);
                });
              }}
            />
          </CardContent>
        </Card>
      </div>

      {(uploading || uploadStep === "error" || uploadStep === "done") && (
        <div className="mt-8 rounded-lg border border-rule bg-card p-4">
          <UploadProgress step={uploadStep} error={uploadError} />
        </div>
      )}
    </Screen>
  );
}

function UploadProgress({
  step,
  error,
}: {
  step: UploadStep;
  error: string | null;
}) {
  const steps: { id: UploadStep; label: string }[] = [
    { id: "creating", label: "Create observation" },
    { id: "uploading-audio", label: "Upload audio" },
    { id: "uploading-photos", label: "Upload photos" },
    { id: "finalising", label: "Finalise" },
  ];
  const currentIdx = steps.findIndex((s) => s.id === step);

  return (
    <div className="flex items-center gap-4">
      {steps.map((s, i) => {
        const done = step === "done" || i < currentIdx;
        const active = i === currentIdx && step !== "done" && step !== "error";
        return (
          <div key={s.id} className="flex items-center gap-2">
            <div
              className={`h-2 w-2 rounded-full ${
                done ? "bg-emerald-500" : active ? "bg-amber animate-pulse" : "bg-rule"
              }`}
            />
            <span
              className={`font-mono text-[10px] uppercase tracking-widest ${
                done || active ? "text-ink" : "text-ink-faint"
              }`}
            >
              {s.label}
            </span>
          </div>
        );
      })}
      {error && (
        <span className="ml-auto font-mono text-xs text-destructive">{error}</span>
      )}
      {step === "done" && (
        <span className="ml-auto font-mono text-xs text-emerald-400">
          Saved · redirecting…
        </span>
      )}
    </div>
  );
}
