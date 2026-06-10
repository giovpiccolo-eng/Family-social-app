"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createUserWithEmailAndPassword } from "firebase/auth";
import { getFirebase } from "@/lib/firebase/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function InvitePage({
  params,
}: {
  params: { token: string };
}) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const { auth } = getFirebase();
      const cred = await createUserWithEmailAndPassword(
        auth,
        email,
        password,
      );
      // Hit the server route to claim the invite — it sets custom claims
      // (schoolId, role) and writes the user doc, then we force token refresh.
      const idToken = await cred.user.getIdToken();
      const res = await fetch(`/api/invites/${params.token}/accept`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify({ displayName }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error ?? "Invite acceptance failed");
      }
      await cred.user.getIdToken(true); // refresh to pick up new claims
      router.replace("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to accept invite");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <span className="font-mono text-xs uppercase tracking-widest text-amber">
          Invite · {params.token.slice(0, 8)}
        </span>
        <CardTitle>Create your account.</CardTitle>
        <CardDescription>
          Use the email address the invitation was sent to.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="displayName">Full name</Label>
            <Input
              id="displayName"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              required
            />
          </div>
          {error && (
            <p className="font-mono text-xs text-destructive">{error}</p>
          )}
          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting ? "Creating account…" : "Accept invite"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
