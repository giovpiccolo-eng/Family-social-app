"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { FRENCH_REVOLUTION_KIT } from "@/lib/sources/french-revolution";
import { TRACKS, type Source, type SuggestionCard, type SuggestResponse, type Track } from "@/lib/types";
import { MicCapture } from "./MicCapture";
import { TranscriptTicker } from "./TranscriptTicker";
import { TrackColumn } from "./TrackColumn";

// How much new transcript to accumulate before we ask Claude for the next batch
// of suggestions. ~150 chars ≈ 25 words ≈ ~10 seconds of talking. Small enough
// to feel live, large enough that we're not pinging the API every word.
const TRIGGER_CHARS = 150;
const TRIGGER_INTERVAL_MS = 9000;

export function Console() {
  const [transcript, setTranscript] = useState("");
  const [inferredTopic, setInferredTopic] = useState("");
  const [pedagogicalMove, setPedagogicalMove] = useState("");
  const [cards, setCards] = useState<SuggestionCard[]>([]);
  const [cardStates, setCardStates] = useState<Record<string, "pending" | "sent" | "dismissed">>({});
  const [status, setStatus] = useState<"idle" | "listening" | "thinking">("idle");
  const [error, setError] = useState<string | null>(null);
  const [manualInput, setManualInput] = useState("");

  // Track what we've already sent to the API so we can submit only the delta.
  const lastSentLenRef = useRef(0);
  const lastSentAtRef = useRef(0);
  const inFlightRef = useRef(false);

  const sourcesById = useMemo<Record<string, Source>>(() => {
    const out: Record<string, Source> = {};
    for (const s of FRENCH_REVOLUTION_KIT.sources) out[s.id] = s;
    return out;
  }, []);

  const requestSuggestions = useCallback(
    async (chunk: string, prior: string) => {
      if (inFlightRef.current) return;
      inFlightRef.current = true;
      setStatus((s) => (s === "listening" ? "thinking" : "thinking"));
      setError(null);
      try {
        const res = await fetch("/api/suggest", {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ transcript: chunk, prior }),
        });
        const data: SuggestResponse | { error: string } = await res.json();
        if (!res.ok || "error" in data) {
          setError("error" in data ? data.error : `HTTP ${res.status}`);
          return;
        }
        setInferredTopic(data.inferred_topic);
        setPedagogicalMove(data.pedagogical_move);
        const now = Date.now();
        const newCards: SuggestionCard[] = data.cards.map((c, i) => ({
          ...c,
          id: `card_${now}_${i}`,
          created_at: now,
        }));
        setCards((prev) => [...newCards, ...prev]);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Request failed.");
      } finally {
        inFlightRef.current = false;
        setStatus((s) => (s === "thinking" ? "listening" : s));
      }
    },
    [],
  );

  // Polling trigger: every TRIGGER_INTERVAL_MS, if there is at least TRIGGER_CHARS
  // of new transcript since the last submission, fire a request.
  useEffect(() => {
    const id = setInterval(() => {
      const fullLen = transcript.length;
      const delta = fullLen - lastSentLenRef.current;
      const elapsed = Date.now() - lastSentAtRef.current;
      if (delta >= TRIGGER_CHARS || (delta >= 40 && elapsed >= TRIGGER_INTERVAL_MS)) {
        const chunk = transcript.slice(lastSentLenRef.current);
        const prior = transcript.slice(0, lastSentLenRef.current);
        lastSentLenRef.current = fullLen;
        lastSentAtRef.current = Date.now();
        requestSuggestions(chunk, prior);
      }
    }, 2000);
    return () => clearInterval(id);
  }, [transcript, requestSuggestions]);

  const appendChunk = useCallback((chunk: string) => {
    setTranscript((prev) => (prev ? `${prev} ${chunk.trim()}` : chunk.trim()));
  }, []);

  const submitManual = () => {
    const v = manualInput.trim();
    if (!v) return;
    appendChunk(v);
    setManualInput("");
  };

  const cardsByTrack = useMemo(() => {
    const out: Record<Track, SuggestionCard[]> = {
      evidence: [],
      visuals: [],
      media: [],
      practice: [],
      inclusion: [],
      "deep-dive": [],
    };
    for (const c of cards) out[c.track].push(c);
    return out;
  }, [cards]);

  const setState = (id: string, s: "pending" | "sent" | "dismissed") =>
    setCardStates((prev) => ({ ...prev, [id]: s }));

  return (
    <div className="flex flex-col h-screen">
      <header className="border-b border-rule bg-white px-4 py-2 flex items-center justify-between">
        <div className="flex items-baseline gap-3">
          <span className="font-serif text-[18px] font-medium">LiveClass</span>
          <span className="text-[11px] uppercase tracking-wide text-muted">
            silent partner · v0
          </span>
        </div>
        <span className="text-[11px] text-muted truncate max-w-[60%]">
          {FRENCH_REVOLUTION_KIT.title}
        </span>
      </header>

      <TranscriptTicker
        transcript={transcript}
        inferredTopic={inferredTopic}
        pedagogicalMove={pedagogicalMove}
        status={status}
      />

      <div className="border-b border-rule bg-paper px-4 py-2 flex flex-wrap items-center gap-3">
        <MicCapture
          onAppend={appendChunk}
          onStatusChange={(listening) => setStatus(listening ? "listening" : "idle")}
        />
        <span className="text-muted text-[11px]">or type:</span>
        <input
          value={manualInput}
          onChange={(e) => setManualInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") submitManual();
          }}
          placeholder='e.g. "So 1789 doesn’t come from nowhere — it comes from a fiscal crisis…"'
          className="flex-1 min-w-[260px] px-2 py-1 text-[12px] border border-rule rounded bg-white"
        />
        <button
          onClick={submitManual}
          className="text-[12px] px-3 py-1.5 bg-ink text-paper rounded hover:bg-accent transition-colors"
        >
          add
        </button>
        <button
          onClick={() => {
            setTranscript("");
            setCards([]);
            setCardStates({});
            setInferredTopic("");
            setPedagogicalMove("");
            lastSentLenRef.current = 0;
            lastSentAtRef.current = 0;
          }}
          className="text-[12px] px-2 py-1.5 text-muted hover:text-ink"
        >
          reset
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 border-b border-rose-200 px-4 py-1.5 text-[12px] text-rose-800">
          {error}
        </div>
      )}

      <main className="flex-1 min-h-0 grid grid-cols-6 gap-3 p-4 bg-paper">
        {TRACKS.map((t) => (
          <TrackColumn
            key={t}
            track={t}
            cards={cardsByTrack[t]}
            sourcesById={sourcesById}
            cardStates={cardStates}
            onSendWall={(id) => setState(id, "sent")}
            onSendStudent={(id) => setState(id, "sent")}
            onDismiss={(id) => setState(id, "dismissed")}
          />
        ))}
      </main>
    </div>
  );
}
