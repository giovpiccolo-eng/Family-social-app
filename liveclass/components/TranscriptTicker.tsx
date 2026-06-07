"use client";

import { useEffect, useRef } from "react";

type Props = {
  transcript: string;
  inferredTopic: string;
  pedagogicalMove: string;
  status: "idle" | "listening" | "thinking";
};

export function TranscriptTicker({ transcript, inferredTopic, pedagogicalMove, status }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (ref.current) ref.current.scrollLeft = ref.current.scrollWidth;
  }, [transcript]);

  const statusLabel =
    status === "thinking" ? "thinking…" : status === "listening" ? "listening" : "idle";
  const dot =
    status === "thinking" ? "bg-amber-500 animate-pulse" : status === "listening" ? "bg-emerald-500" : "bg-muted";

  return (
    <div className="border-b border-rule bg-white">
      <div className="px-4 py-2 flex items-center gap-4 text-[12px]">
        <span className="flex items-center gap-2 shrink-0">
          <span className={`w-1.5 h-1.5 rounded-full ${dot}`} aria-hidden />
          <span className="text-muted uppercase tracking-wide text-[10px]">{statusLabel}</span>
        </span>
        <span className="shrink-0">
          <span className="text-muted text-[10px] uppercase tracking-wide mr-1">topic</span>
          <span className="font-medium">{inferredTopic || "—"}</span>
        </span>
        <span className="shrink-0">
          <span className="text-muted text-[10px] uppercase tracking-wide mr-1">move</span>
          <span className="font-medium">{pedagogicalMove || "—"}</span>
        </span>
        <div
          ref={ref}
          className="flex-1 min-w-0 overflow-x-hidden whitespace-nowrap text-muted font-mono text-[11px]"
          aria-label="Live transcript"
        >
          {transcript.slice(-400) || "…waiting for transcript…"}
        </div>
      </div>
    </div>
  );
}
