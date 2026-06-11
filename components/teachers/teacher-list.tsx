"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { listTeachers } from "@/lib/firebase/teachers";
import { useAuth } from "@/lib/firebase/auth-context";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { Teacher } from "@/types";

interface TeacherListProps {
  refreshKey?: number;
}

export function TeacherList({ refreshKey = 0 }: TeacherListProps) {
  const { claims } = useAuth();
  const [teachers, setTeachers] = useState<Teacher[] | null>(null);
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!claims) return;
    let cancelled = false;
    setError(null);
    listTeachers(claims.schoolId)
      .then((rows) => {
        if (!cancelled) setTeachers(rows);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load teachers");
          setTeachers([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [claims, refreshKey]);

  const visible = useMemo(() => {
    if (!teachers) return null;
    const needle = search.trim().toLowerCase();
    const active = teachers.filter((t) => !t.archived);
    if (!needle) return active;
    return active.filter(
      (t) =>
        t.name.toLowerCase().includes(needle) ||
        t.subject.toLowerCase().includes(needle) ||
        t.grade?.toLowerCase().includes(needle),
    );
  }, [teachers, search]);

  if (!claims) return null;

  return (
    <div className="space-y-4">
      <Input
        type="search"
        placeholder="Search by name, subject, grade"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="max-w-md"
      />

      {error && (
        <p className="font-mono text-xs text-destructive">{error}</p>
      )}

      {visible === null ? (
        <p className="font-mono text-xs uppercase tracking-widest text-ink-faint">
          Loading…
        </p>
      ) : visible.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="font-mono text-xs uppercase tracking-widest text-ink-faint">
              {search ? "No teachers match that search." : "No teachers yet — add your first."}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="overflow-hidden rounded-lg border border-rule">
          <table className="w-full">
            <thead className="bg-navy-900">
              <tr className="text-left">
                <Th>Name</Th>
                <Th>Subject</Th>
                <Th>Grade</Th>
                <Th>Campus</Th>
                <Th className="text-right pr-6">Action</Th>
              </tr>
            </thead>
            <tbody>
              {visible.map((t) => (
                <tr
                  key={t.id}
                  className="border-t border-rule hover:bg-navy-900/60"
                >
                  <Td>
                    <Link
                      href={`/teachers/${t.id}`}
                      className="font-medium text-foreground hover:text-amber"
                    >
                      {t.name}
                    </Link>
                  </Td>
                  <Td>{t.subject}</Td>
                  <Td>{t.grade ?? <span className="text-ink-faint">—</span>}</Td>
                  <Td>
                    {t.campus ? (
                      <Badge variant="muted">{t.campus}</Badge>
                    ) : (
                      <span className="text-ink-faint">—</span>
                    )}
                  </Td>
                  <Td className="text-right pr-6">
                    <Link
                      href={`/teachers/${t.id}/observe`}
                      className="font-mono text-xs uppercase tracking-widest text-amber hover:text-amber-400"
                    >
                      Observe →
                    </Link>
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Th({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <th
      className={`px-6 py-3 font-mono text-[10px] uppercase tracking-widest text-ink-muted ${className}`}
    >
      {children}
    </th>
  );
}

function Td({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <td className={`px-6 py-4 text-sm ${className}`}>{children}</td>;
}
