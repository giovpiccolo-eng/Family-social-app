"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { formatTimestamp } from "@/lib/utils";
import type { Note } from "@/types";

interface NotesPanelProps {
  notes: Note[];
  elapsedSecs: number;
  disabled: boolean;
  onAdd: (note: Note) => void;
}

export function NotesPanel({ notes, elapsedSecs, disabled, onAdd }: NotesPanelProps) {
  const [draft, setDraft] = useState("");

  function commit() {
    const text = draft.trim();
    if (!text) return;
    onAdd({
      id: crypto.randomUUID(),
      text,
      timestamp: formatTimestamp(elapsedSecs),
      elapsedSecs,
    });
    setDraft("");
  }

  return (
    <div className="flex h-full flex-col gap-3">
      <div className="flex items-center justify-between">
        <span className="font-mono text-xs uppercase tracking-widest text-amber">
          Notes
        </span>
        <span className="font-mono text-[10px] uppercase tracking-widest text-ink-faint">
          {notes.length} entries
        </span>
      </div>

      <div className="flex-1 space-y-2 overflow-y-auto rounded-md border border-rule bg-navy-900 p-3">
        {notes.length === 0 ? (
          <p className="font-mono text-[10px] uppercase tracking-widest text-ink-faint">
            Notes you type appear here, stamped with the recording timestamp.
          </p>
        ) : (
          notes
            .slice()
            .sort((a, b) => a.elapsedSecs - b.elapsedSecs)
            .map((n) => (
              <div
                key={n.id}
                className="flex gap-3 rounded border border-rule bg-navy-800/60 p-2"
              >
                <span className="font-mono text-[10px] uppercase tracking-widest text-amber">
                  {n.timestamp}
                </span>
                <p className="flex-1 text-sm text-ink">{n.text}</p>
              </div>
            ))
        )}
      </div>

      <div className="space-y-2">
        <Textarea
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={
            disabled
              ? "Start recording to add timestamped notes."
              : "Short observation. Press ⌘/Ctrl+Enter to stamp."
          }
          disabled={disabled}
          onKeyDown={(e) => {
            if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
              e.preventDefault();
              commit();
            }
          }}
        />
        <div className="flex items-center justify-between">
          <span className="font-mono text-[10px] uppercase tracking-widest text-ink-faint">
            Stamp at {formatTimestamp(elapsedSecs)}
          </span>
          <Button
            type="button"
            size="sm"
            onClick={commit}
            disabled={disabled || draft.trim().length === 0}
          >
            Add note
          </Button>
        </div>
      </div>
    </div>
  );
}
