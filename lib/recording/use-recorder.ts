"use client";

import { useCallback, useEffect, useRef, useState } from "react";

export type RecorderState = "idle" | "requesting" | "recording" | "paused" | "stopped" | "error";

interface RecorderResult {
  state: RecorderState;
  errorMessage: string | null;
  elapsedSecs: number;
  level: number; // 0..1 mic level for the meter
  blob: Blob | null;
  start: () => Promise<void>;
  stop: () => Promise<Blob | null>;
  reset: () => void;
}

// MediaRecorder + Web Audio mic level. 1-second chunks per brief §2.2.
export function useRecorder(): RecorderResult {
  const [state, setState] = useState<RecorderState>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [elapsedSecs, setElapsedSecs] = useState(0);
  const [level, setLevel] = useState(0);
  const [blob, setBlob] = useState<Blob | null>(null);

  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const rafRef = useRef<number | null>(null);
  const tickRef = useRef<number | null>(null);
  const startedAtRef = useRef<number>(0);
  const stopResolverRef = useRef<((b: Blob | null) => void) | null>(null);

  const cleanup = useCallback(() => {
    if (rafRef.current) cancelAnimationFrame(rafRef.current);
    if (tickRef.current) window.clearInterval(tickRef.current);
    rafRef.current = null;
    tickRef.current = null;
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
    audioCtxRef.current?.close().catch(() => {});
    audioCtxRef.current = null;
    analyserRef.current = null;
    recorderRef.current = null;
  }, []);

  useEffect(() => {
    return () => cleanup();
  }, [cleanup]);

  const pickMimeType = useCallback((): string => {
    const candidates = [
      "audio/webm;codecs=opus",
      "audio/webm",
      "audio/ogg;codecs=opus",
      "audio/mp4",
    ];
    for (const t of candidates) {
      if (typeof MediaRecorder !== "undefined" && MediaRecorder.isTypeSupported(t)) {
        return t;
      }
    }
    return "";
  }, []);

  const start = useCallback(async () => {
    setErrorMessage(null);
    setBlob(null);
    chunksRef.current = [];
    setState("requesting");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true },
      });
      streamRef.current = stream;

      // Web Audio for live mic-level meter.
      const AudioCtx =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext })
          .webkitAudioContext;
      const ctx = new AudioCtx();
      audioCtxRef.current = ctx;
      const source = ctx.createMediaStreamSource(stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 512;
      source.connect(analyser);
      analyserRef.current = analyser;

      const buf = new Uint8Array(analyser.frequencyBinCount);
      const loop = () => {
        if (!analyserRef.current) return;
        analyserRef.current.getByteTimeDomainData(buf);
        let sumSq = 0;
        for (let i = 0; i < buf.length; i++) {
          const v = (buf[i] - 128) / 128;
          sumSq += v * v;
        }
        const rms = Math.sqrt(sumSq / buf.length);
        setLevel(Math.min(1, rms * 2.2));
        rafRef.current = requestAnimationFrame(loop);
      };
      rafRef.current = requestAnimationFrame(loop);

      const mimeType = pickMimeType();
      const rec = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      rec.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) chunksRef.current.push(e.data);
      };
      rec.onstop = () => {
        const type = chunksRef.current[0]?.type || mimeType || "audio/webm";
        const finalBlob = new Blob(chunksRef.current, { type });
        setBlob(finalBlob);
        stopResolverRef.current?.(finalBlob);
        stopResolverRef.current = null;
        cleanup();
        setState("stopped");
      };
      rec.onerror = () => {
        setErrorMessage("Recording error");
        setState("error");
        cleanup();
      };

      recorderRef.current = rec;
      rec.start(1000); // 1-second chunks
      startedAtRef.current = Date.now();
      setElapsedSecs(0);
      tickRef.current = window.setInterval(() => {
        setElapsedSecs(Math.floor((Date.now() - startedAtRef.current) / 1000));
      }, 250);
      setState("recording");
    } catch (err) {
      const msg =
        err instanceof Error
          ? err.message
          : "Could not access microphone";
      setErrorMessage(msg);
      setState("error");
      cleanup();
    }
  }, [cleanup, pickMimeType]);

  const stop = useCallback((): Promise<Blob | null> => {
    return new Promise((resolve) => {
      const rec = recorderRef.current;
      if (!rec || rec.state === "inactive") {
        resolve(blob);
        return;
      }
      stopResolverRef.current = resolve;
      rec.stop();
    });
  }, [blob]);

  const reset = useCallback(() => {
    cleanup();
    chunksRef.current = [];
    setBlob(null);
    setElapsedSecs(0);
    setLevel(0);
    setErrorMessage(null);
    setState("idle");
  }, [cleanup]);

  return { state, errorMessage, elapsedSecs, level, blob, start, stop, reset };
}
