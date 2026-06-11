"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Screen, ScreenHeader } from "@/components/screen";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { getTeacher } from "@/lib/firebase/teachers";
import { listObservationsForTeacher } from "@/lib/firebase/observations";
import { useAuth } from "@/lib/firebase/auth-context";
import type { Observation, Teacher } from "@/types";

export default function TeacherDetailPage({
  params,
}: {
  params: { teacherId: string };
}) {
  const { claims } = useAuth();
  const [teacher, setTeacher] = useState<Teacher | null>(null);
  const [observations, setObservations] = useState<Observation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!claims) return;
    let cancelled = false;
    (async () => {
      try {
        const t = await getTeacher(params.teacherId);
        if (!cancelled) setTeacher(t);
        if (t) {
          const obs = await listObservationsForTeacher(
            claims.schoolId,
            params.teacherId,
          );
          if (!cancelled) setObservations(obs);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Load failed");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [claims, params.teacherId]);

  if (loading) {
    return (
      <Screen>
        <p className="font-mono text-xs uppercase tracking-widest text-ink-faint">
          Loading…
        </p>
      </Screen>
    );
  }

  if (!teacher) {
    return (
      <Screen>
        <ScreenHeader
          title="Teacher not found"
          description="They may have been archived or you may not have access in this school."
        />
        <Link href="/teachers">
          <Button variant="outline">Back to roster</Button>
        </Link>
      </Screen>
    );
  }

  return (
    <Screen>
      <ScreenHeader
        eyebrow={teacher.subject}
        title={teacher.name}
        description={[teacher.grade, teacher.campus].filter(Boolean).join(" · ")}
        actions={
          <Link href={`/teachers/${teacher.id}/observe`}>
            <Button>Start observation</Button>
          </Link>
        }
      />

      {error && (
        <p className="mb-6 font-mono text-xs text-destructive">{error}</p>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-xl">Recent observations</CardTitle>
            <CardDescription>
              Audio and analysis history for this teacher.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {observations.length === 0 ? (
              <p className="font-mono text-xs uppercase tracking-widest text-ink-faint">
                No observations yet.
              </p>
            ) : (
              <ul className="divide-y divide-rule">
                {observations.map((o) => (
                  <li
                    key={o.id}
                    className="flex items-center justify-between py-3"
                  >
                    <div>
                      <p className="font-mono text-xs uppercase tracking-widest text-ink-muted">
                        {o.observedAt?.toDate?.().toLocaleString() ?? "—"}
                      </p>
                      <p className="text-sm">
                        Duration · {Math.round((o.durationSec ?? 0) / 60)} min
                      </p>
                    </div>
                    <Badge
                      variant={
                        o.status === "complete"
                          ? "success"
                          : o.status === "error"
                          ? "danger"
                          : "muted"
                      }
                    >
                      {o.status}
                    </Badge>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-xl">Integrations</CardTitle>
            <CardDescription>
              Linked Google Classroom and SIS identifiers.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 font-mono text-xs uppercase tracking-widest text-ink-muted">
            <Row label="Classroom course" value={teacher.classroomCourseId} />
            <Row label="SIS class" value={teacher.sisClassId} />
            <Row label="Created" value={teacher.createdAt?.toDate?.().toLocaleDateString()} />
          </CardContent>
        </Card>
      </div>
    </Screen>
  );
}

function Row({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="flex items-center justify-between">
      <span>{label}</span>
      <span className="text-ink">{value || "—"}</span>
    </div>
  );
}
