"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/firebase/auth-context";
import { BrandMark } from "./brand-mark";
import { Button } from "./ui/button";
import { cn } from "@/lib/utils";

const NAV = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/teachers", label: "Teachers" },
  { href: "/observations", label: "Observations" },
];

export function Topbar() {
  const pathname = usePathname();
  const { user, claims, signOut } = useAuth();

  return (
    <header className="sticky top-0 z-30 border-b border-rule bg-navy-950/80 backdrop-blur supports-[backdrop-filter]:bg-navy-950/60">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-6">
        <div className="flex items-center gap-8">
          <Link href="/dashboard">
            <BrandMark />
          </Link>
          <nav className="hidden items-center gap-6 md:flex">
            {NAV.map((item) => {
              const active = pathname?.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "font-mono text-xs uppercase tracking-wider transition-colors",
                    active ? "text-amber" : "text-ink-muted hover:text-foreground",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="flex items-center gap-4">
          {claims && (
            <span className="hidden font-mono text-[10px] uppercase tracking-wider text-ink-muted md:inline">
              {claims.role} · {claims.schoolId.slice(0, 6)}
            </span>
          )}
          {user && (
            <Button variant="ghost" size="sm" onClick={() => signOut()}>
              Sign out
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
