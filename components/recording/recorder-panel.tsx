"use client";

import { Mic, Square } from "lucide-react";
import { Button } from "@/components/ui/button";
import { formatTimestamp } from "@/lib/utils";
import type { RecorderState } from "@/lib/recording/use-recorder";

interface RecorderPanelProps {
  state: RecorderState;
  errorMessage: string | null;
  elapsedSecs: number;
  level: number;
  onStart: () => void;
  onStop: () => void;
}

export function RecorderPanel({
  state,
  errorMessage,
  elapsedSecs,
  level,
  onStart,
  onStop,
}: RecorderPanelProps) {
  const recording = state === "recording";
  const requesting = state === "requesting";

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex items-center justify-between">
        <span className="font-mono text-xs uppercase tracking-widest text-amber">
          Audio
        </span>
        <span
          className={`font-mono text-[10px] uppercase tracking-widest ${
            recording ? "text-amber" : "text-ink-faint"
          }`}
        >
          {recording ? "● Live" : state}
        </span>
      </div>

      <div className="grid place-items-center rounded-lg border border-rule bg-navy-900 px-6 py-10">
        <div className="font-mono text-5xl font-semibold tabular-nums text-foreground">
          {formatTimestamp(elapsedSecs)}
        </div>
        <p className="mt-2 font-mono text-[10px] uppercase tracking-widest text-ink-muted">
          Lesson recording
        </p>

        <div className="mt-6 flex h-2 w-full max-w-xs items-center gap-0.5">
          {Array.from({ length: 24 }).map((_, i) => {
            const threshold = (i + 1) / 24;
            const lit = level >= threshold * 0.7;
            return (
              <div
                key={i}
                className={`h-full flex-1 rounded-sm transition-colors ${
                  lit
                    ? threshold > 0.85
                      ? "bg-destructive"
                      : threshold > 0.6
                      ? "bg-amber"
                      : "bg-emerald-500"
                    : "bg-rule"
                }`}
              />
            );
          })}
        </div>
      </div>

      {errorMessage && (
        <p className="font-mono text-xs text-destructive">{errorMessage}</p>
      )}

      <div className="mt-auto flex gap-3">
        {!recording ? (
          <Button
            type="button"
            size="lg"
            className="w-full"
            onClick={onStart}
            disabled={requesting}
          >
            <Mic className="h-5 w-5" />
            {requesting ? "Requesting mic…" : "Start recording"}
          </Button>
        ) : (
          <Button
            type="button"
            size="lg"
            variant="destructive"
            className="w-full"
            onClick={onStop}
          >
            <Square className="h-5 w-5" />
            Stop and process
          </Button>
        )}
      </div>
    </div>
  );
}
