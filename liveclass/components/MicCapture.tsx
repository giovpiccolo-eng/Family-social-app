"use client";

import { useEffect, useRef, useState } from "react";

// Minimal Web Speech API typings — the standard DOM lib does not include these.
type SpeechResult = { isFinal: boolean; 0: { transcript: string } };
type SpeechEvent = { resultIndex: number; results: ArrayLike<SpeechResult> };
type SpeechRecognitionLike = {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start: () => void;
  stop: () => void;
  onresult: ((e: SpeechEvent) => void) | null;
  onerror: ((e: { error: string }) => void) | null;
  onend: (() => void) | null;
};

type Props = {
  onAppend: (chunk: string) => void;
  onStatusChange: (listening: boolean) => void;
};

export function MicCapture({ onAppend, onStatusChange }: Props) {
  const [supported, setSupported] = useState(false);
  const [listening, setListening] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const recRef = useRef<SpeechRecognitionLike | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const Ctor =
      (window as unknown as { SpeechRecognition?: new () => SpeechRecognitionLike }).SpeechRecognition ??
      (window as unknown as { webkitSpeechRecognition?: new () => SpeechRecognitionLike }).webkitSpeechRecognition;
    if (!Ctor) return;
    setSupported(true);
    const rec = new Ctor();
    rec.continuous = true;
    rec.interimResults = false;
    rec.lang = "en-US";
    rec.onresult = (e) => {
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i];
        if (r.isFinal) onAppend(r[0].transcript);
      }
    };
    rec.onerror = (e) => {
      setErr(e.error);
      setListening(false);
      onStatusChange(false);
    };
    rec.onend = () => {
      // Auto-restart if the user wanted to keep listening — the browser stops
      // recognition after a pause, but we present it as a continuous stream.
      if (recRef.current && listening) {
        try {
          recRef.current.start();
        } catch {
          /* already started */
        }
      }
    };
    recRef.current = rec;
    return () => {
      try {
        rec.stop();
      } catch {
        /* ignore */
      }
    };
    // We intentionally don't depend on `listening` here — the closure inside
    // onend reads the latest ref, and re-running the effect would recreate the
    // recognizer mid-session.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const toggle = () => {
    const rec = recRef.current;
    if (!rec) return;
    if (listening) {
      rec.stop();
      setListening(false);
      onStatusChange(false);
    } else {
      try {
        rec.start();
        setListening(true);
        onStatusChange(true);
        setErr(null);
      } catch (e) {
        setErr(e instanceof Error ? e.message : "Failed to start microphone.");
      }
    }
  };

  if (!supported) {
    return (
      <div className="text-[11px] text-muted">
        Mic capture not supported in this browser (Web Speech API). Use the text box below to feed
        transcript chunks.
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={toggle}
        className={`text-[12px] px-3 py-1.5 rounded border ${
          listening
            ? "bg-rose-600 text-white border-rose-700"
            : "bg-white border-ink hover:bg-ink hover:text-paper"
        } transition-colors`}
      >
        {listening ? "■ Stop mic" : "● Start mic"}
      </button>
      {err && <span className="text-[11px] text-rose-700">mic: {err}</span>}
    </div>
  );
}
