"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/firebase/auth-context";
import { Topbar } from "@/components/topbar";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { user, loading } = useAuth();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  if (loading || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <span className="font-mono text-xs uppercase tracking-widest text-ink-muted">
          Loading…
        </span>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Topbar />
      {children}
    </div>
  );
}
