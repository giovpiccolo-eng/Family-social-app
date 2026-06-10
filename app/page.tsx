"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/firebase/auth-context";
import { BrandMark } from "@/components/brand-mark";
import { Button } from "@/components/ui/button";

export default function Home() {
  const router = useRouter();
  const { user, loading } = useAuth();

  useEffect(() => {
    if (!loading && user) router.replace("/dashboard");
  }, [loading, user, router]);

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-rule">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-6">
          <BrandMark />
          <Link href="/login">
            <Button variant="outline" size="sm">
              Sign in
            </Button>
          </Link>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-4xl flex-1 flex-col justify-center px-6 py-16">
        <span className="mb-6 font-mono text-xs uppercase tracking-widest text-amber">
          v1.0 · Closed pilot
        </span>
        <h1 className="font-serif text-5xl font-semibold leading-[1.05] tracking-tight text-balance">
          Evidence-anchored teacher evaluation,
          <span className="text-amber"> compressed to 30 minutes.</span>
        </h1>
        <p className="mt-6 max-w-2xl text-lg text-ink-muted">
          Observer.AI integrates classroom audio, principal notes, posted lesson
          plans, and SIS roster data into a complete Danielson Framework
          evaluation. Every rating is traceable to source.
        </p>
        <div className="mt-10 flex flex-wrap gap-3">
          <Link href="/login">
            <Button size="lg">Sign in</Button>
          </Link>
          <Button size="lg" variant="outline" disabled>
            Request access
          </Button>
        </div>
      </main>

      <footer className="border-t border-rule">
        <div className="mx-auto flex h-12 max-w-7xl items-center justify-between px-6 font-mono text-[10px] uppercase tracking-widest text-ink-faint">
          <span>Observer.AI · Confidential</span>
          <span>Danielson Framework for Teaching · 2022</span>
        </div>
      </footer>
    </div>
  );
}
