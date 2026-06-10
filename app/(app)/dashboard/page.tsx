"use client";

import Link from "next/link";
import { Screen, ScreenHeader } from "@/components/screen";
import { useAuth } from "@/lib/firebase/auth-context";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { DANIELSON_DOMAINS } from "@/lib/danielson";

export default function DashboardPage() {
  const { user, claims } = useAuth();

  return (
    <Screen>
      <ScreenHeader
        eyebrow="Overview"
        title={`Welcome, ${user?.displayName ?? user?.email ?? "Observer"}.`}
        description="Select a teacher to begin a new observation, or review evidence accumulated across your school."
        actions={
          <Link href="/teachers">
            <Button>New observation</Button>
          </Link>
        }
      />

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        {Object.entries(DANIELSON_DOMAINS).map(([id, name]) => (
          <Card key={id}>
            <CardHeader>
              <span className="font-mono text-xs uppercase tracking-widest text-amber">
                Domain {id}
              </span>
              <CardTitle className="text-xl">{name}</CardTitle>
              <CardDescription>
                Rolling averages will populate after the first observations
                land.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="font-mono text-3xl font-semibold text-ink-muted">
                —
              </div>
              <p className="mt-1 font-mono text-[10px] uppercase tracking-widest text-ink-faint">
                No evidence yet
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="mt-12 space-y-4">
        <h2 className="font-serif text-2xl font-semibold tracking-tight">
          Sprint 1 — Foundation
        </h2>
        <p className="max-w-2xl text-sm text-ink-muted">
          Auth, multi-tenant data model, security rules, and visual system are
          wired. Teacher CRUD and live recording arrive in Sprint 2.
        </p>
        {claims && (
          <p className="font-mono text-[11px] uppercase tracking-widest text-ink-faint">
            Signed in · {claims.role} · school {claims.schoolId}
          </p>
        )}
      </div>
    </Screen>
  );
}
