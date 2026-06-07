"use client";

import { TRACK_DESCRIPTIONS, TRACK_LABELS, type Source, type SuggestionCard, type Track } from "@/lib/types";
import { SuggestionCardView } from "./SuggestionCard";

type Props = {
  track: Track;
  cards: SuggestionCard[];
  sourcesById: Record<string, Source>;
  cardStates: Record<string, "pending" | "sent" | "dismissed">;
  onSendWall: (id: string) => void;
  onSendStudent: (id: string) => void;
  onDismiss: (id: string) => void;
};

export function TrackColumn({ track, cards, sourcesById, cardStates, onSendWall, onSendStudent, onDismiss }: Props) {
  return (
    <section className="flex flex-col min-w-0 h-full">
      <header className="pb-2 border-b border-rule mb-3">
        <h2 className="font-serif text-[15px] font-medium">{TRACK_LABELS[track]}</h2>
        <p className="text-[11px] text-muted mt-0.5 leading-snug">{TRACK_DESCRIPTIONS[track]}</p>
      </header>
      <div className="flex flex-col gap-2 overflow-y-auto pr-1 flex-1 min-h-0">
        {cards.length === 0 && (
          <p className="text-[11px] text-muted italic mt-2">— quiet —</p>
        )}
        {cards.map((c) => (
          <SuggestionCardView
            key={c.id}
            card={c}
            source={c.source_id ? sourcesById[c.source_id] : undefined}
            state={cardStates[c.id] ?? "pending"}
            onSendWall={() => onSendWall(c.id)}
            onSendStudent={() => onSendStudent(c.id)}
            onDismiss={() => onDismiss(c.id)}
          />
        ))}
      </div>
    </section>
  );
}
