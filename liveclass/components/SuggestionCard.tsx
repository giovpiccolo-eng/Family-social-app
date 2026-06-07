"use client";

import type { Source, SuggestionCard as SuggestionCardT, Track } from "@/lib/types";

const TRACK_TINT: Record<Track, string> = {
  evidence: "border-l-amber-700",
  visuals: "border-l-sky-700",
  media: "border-l-violet-700",
  practice: "border-l-emerald-700",
  inclusion: "border-l-rose-700",
  "deep-dive": "border-l-slate-700",
};

type Props = {
  card: SuggestionCardT;
  source: Source | undefined;
  state: "pending" | "sent" | "dismissed";
  onSendWall: () => void;
  onSendStudent: () => void;
  onDismiss: () => void;
};

export function SuggestionCardView({ card, source, state, onSendWall, onSendStudent, onDismiss }: Props) {
  return (
    <article
      className={`bg-white border border-rule rounded-md p-3 border-l-4 ${TRACK_TINT[card.track]} ${
        state === "dismissed" ? "opacity-40" : ""
      }`}
    >
      <header className="flex items-start justify-between gap-2">
        <h3 className="font-medium text-[14px] leading-snug">{card.title}</h3>
        {state === "sent" && (
          <span className="text-[10px] uppercase tracking-wide text-emerald-700 shrink-0 mt-0.5">✓ sent</span>
        )}
      </header>

      <p className="text-[13px] mt-1.5 text-ink/85 leading-snug">{card.summary}</p>

      {source && (
        <p className="text-[11px] mt-2 text-muted italic leading-snug">
          {source.citation}
        </p>
      )}
      {!source && card.source_id === null && (
        <p className="text-[11px] mt-2 text-muted italic">No source — synthesized prompt.</p>
      )}

      <p className="text-[11px] mt-2 text-muted">
        <span className="uppercase tracking-wide">Why now ·</span> {card.why_now}
      </p>

      {state === "pending" && (
        <div className="flex gap-1.5 mt-3">
          <button
            onClick={onSendWall}
            className="text-[11px] px-2 py-1 bg-ink text-paper rounded hover:bg-accent transition-colors"
          >
            → Wall
          </button>
          <button
            onClick={onSendStudent}
            className="text-[11px] px-2 py-1 border border-ink rounded hover:bg-ink hover:text-paper transition-colors"
          >
            → Student
          </button>
          <button
            onClick={onDismiss}
            className="text-[11px] px-2 py-1 text-muted hover:text-ink ml-auto"
          >
            dismiss
          </button>
        </div>
      )}
    </article>
  );
}
