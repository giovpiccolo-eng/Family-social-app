"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { onSnapshot, doc } from "firebase/firestore";
import { Screen, ScreenHeader } from "@/components/screen";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { getFirebase } from "@/lib/firebase/client";
import { useAuth } from "@/lib/firebase/auth-context";
import type { Observation, ObservationStatus } from "@/types";

const PIPELINE_STEPS: { id: ObservationStatus; label: string; description: string }[] = [
  {
    id: "recorded",
    label: "Ingest",
    description: "Audio uploaded, observation persisted.",
  },
  {
    id: "transcribing",
    label: "Transcribe",
    description: "Whisper turning lesson audio into text.",
  },
  {
    id: "analysing",
    label: "Correlate + Differentiate",
    description: "Claude integrating transcript, notes, Classroom, SIS.",
  },
  {
    id: "complete",
    label: "Map",
    description: "Danielson mappings persisted to evidence bank.",
  },
];

export default function ObservationProcessingPage({
  params,
}: {
  params: { observationId: string };
}) {
  const { user, claims } = useAuth();
  const [obs, setObs] = useState<Observation | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!claims || !user) return;
    const { db } = getFirebase();
    const unsub = onSnapshot(
      doc(db, "observations", params.observationId),
      (snap) => {
        if (!snap.exists()) {
          setError("Observation not found");
          return;
        }
        setObs({ id: snap.id, ...snap.data() } as Observation);
      },
      (err) => setError(err.message),
    );
    return () => unsub();
  }, [claims, user, params.observationId]);

  if (error) {
    return (
      <Screen>
        <ScreenHeader title="Observation" description={error} />
        <Link href="/teachers">
          <Button variant="outline">Back to teachers</Button>
        </Link>
      </Screen>
    );
  }

  if (!obs) {
    return (
      <Screen>
        <p className="font-mono text-xs uppercase tracking-widest text-ink-faint">
          Loading…
        </p>
      </Screen>
    );
  }

  const statusIdx = PIPELINE_STEPS.findIndex((s) => s.id === obs.status);
  const isError = obs.status === "error";
  const isComplete = obs.status === "complete";

  return (
    <Screen>
      <ScreenHeader
        eyebrow={`Observation · ${obs.id.slice(0, 8)}`}
        title={
          isComplete
            ? "Analysis complete"
            : isError
            ? "Processing error"
            : "Processing observation…"
        }
        description={
          isComplete
            ? "Evidence has been written to the bank. Open the summary to view differentiation verdict, mappings, and source moments."
            : isError
            ? "The pipeline failed. See the error details below."
            : "The pipeline runs in four steps. Estimated 30 seconds for a 30-minute lesson."
        }
        actions={
          isComplete ? (
            <Link href={`/observations/${obs.id}/summary`}>
              <Button>View summary</Button>
            </Link>
          ) : null
        }
      />

      <Card>
        <CardContent className="space-y-4 p-6">
          {PIPELINE_STEPS.map((step, i) => {
            const done = isComplete || i < statusIdx;
            const active = !isError && i === statusIdx && !isComplete;
            return (
              <div
                key={step.id}
                className="flex items-start gap-4 border-b border-rule pb-4 last:border-0 last:pb-0"
              >
                <div
                  className={`mt-1 h-3 w-3 shrink-0 rounded-full ${
                    isError && i === statusIdx
                      ? "bg-destructive"
                      : done
                      ? "bg-emerald-500"
                      : active
                      ? "bg-amber animate-pulse"
                      : "bg-rule"
                  }`}
                />
                <div className="flex-1">
                  <p className="font-mono text-xs uppercase tracking-widest text-foreground">
                    {step.label}
                  </p>
                  <p className="text-sm text-ink-muted">{step.description}</p>
                </div>
                <Badge
                  variant={
                    done
                      ? "success"
                      : active
                      ? "amber"
                      : isError && i === statusIdx
                      ? "danger"
                      : "muted"
                  }
                >
                  {done ? "Done" : active ? "Working" : isError && i === statusIdx ? "Error" : "Queued"}
                </Badge>
              </div>
            );
          })}
        </CardContent>
      </Card>

      {isError && obs.error && (
        <div className="mt-6 rounded-lg border border-destructive/40 bg-destructive/10 p-4">
          <p className="font-mono text-xs uppercase tracking-widest text-destructive">
            Error
          </p>
          <p className="mt-1 text-sm text-ink">{obs.error}</p>
        </div>
      )}

      {isComplete && obs.analysis && (
        <div className="mt-10 grid gap-4">
          <h2 className="font-serif text-2xl font-semibold tracking-tight">
            Quick look
          </h2>
          <Card>
            <CardContent className="space-y-3 p-6">
              <p className="font-mono text-xs uppercase tracking-widest text-amber">
                Differentiation verdict
              </p>
              <p className="font-serif text-3xl">
                {obs.analysis.differentiationAssessment.verdict}
              </p>
              <p className="text-sm text-ink-muted">
                {obs.analysis.differentiationAssessment.verdictReason}
              </p>
            </CardContent>
          </Card>
        </div>
      )}
    </Screen>
  );
}
