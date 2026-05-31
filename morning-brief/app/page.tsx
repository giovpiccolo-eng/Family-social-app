"use client";

import { useEffect, useState } from "react";
import type { BriefConfig } from "@/lib/types";

const TIMEZONES = [
  "Europe/Rome",
  "Europe/London",
  "Europe/Paris",
  "America/New_York",
  "America/Los_Angeles",
  "Asia/Tokyo",
  "UTC",
];

export default function SettingsPage() {
  const [config, setConfig] = useState<BriefConfig | null>(null);
  const [interestsText, setInterestsText] = useState("");
  const [status, setStatus] = useState<{ kind: "ok" | "err"; msg: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const [running, setRunning] = useState(false);

  useEffect(() => {
    fetch("/api/config")
      .then((r) => r.json())
      .then((c: BriefConfig) => {
        setConfig(c);
        setInterestsText((c.interests || []).join("\n"));
      })
      .catch(() => setStatus({ kind: "err", msg: "Could not load config." }));
  }, []);

  if (!config) {
    return (
      <>
        <p className="kicker">Morning Brief</p>
        <h1>Settings</h1>
        <p>Loading…</p>
      </>
    );
  }

  function update<K extends keyof BriefConfig>(key: K, value: BriefConfig[K]) {
    setConfig((prev) => (prev ? { ...prev, [key]: value } : prev));
  }

  async function save() {
    if (!config) return;
    setSaving(true);
    setStatus(null);
    const payload = {
      ...config,
      interests: interestsText
        .split(/[\n,]/)
        .map((s) => s.trim())
        .filter(Boolean),
    };
    try {
      const res = await fetch("/api/config", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error(await res.text());
      const saved = (await res.json()) as BriefConfig;
      setConfig(saved);
      setInterestsText((saved.interests || []).join("\n"));
      setStatus({ kind: "ok", msg: "Saved." });
    } catch (e) {
      setStatus({ kind: "err", msg: `Save failed: ${(e as Error).message}` });
    } finally {
      setSaving(false);
    }
  }

  async function sendTest() {
    setRunning(true);
    setStatus({ kind: "ok", msg: "Generating and sending — this can take a minute…" });
    try {
      const res = await fetch("/api/run", { method: "POST" });
      const data = await res.json();
      if (!res.ok) throw new Error(data?.error || "Run failed");
      setStatus({
        kind: "ok",
        msg: data.emailed
          ? `Sent: "${data.title}" → ${config?.recipient}`
          : `Generated "${data.title}" (not emailed).`,
      });
    } catch (e) {
      setStatus({ kind: "err", msg: `Run failed: ${(e as Error).message}` });
    } finally {
      setRunning(false);
    }
  }

  return (
    <>
      <p className="kicker">Morning Brief</p>
      <h1>Settings</h1>
      <p className="hint">
        Your interests shape the daily ~{config.wordCount}-word learning essay, emailed at{" "}
        {String(config.sendHour).padStart(2, "0")}:00 {config.timezone.replace("_", " ")}.
      </p>

      <div className="card">
        <label htmlFor="interests">
          Interests
          <span className="hint"> — one per line (or comma-separated)</span>
        </label>
        <textarea
          id="interests"
          value={interestsText}
          onChange={(e) => setInterestsText(e.target.value)}
          placeholder="AI&#10;neuroscience&#10;geopolitics"
        />

        <div className="row">
          <div>
            <label htmlFor="num"># of stories</label>
            <input
              id="num"
              type="number"
              min={1}
              max={25}
              value={config.numStories}
              onChange={(e) => update("numStories", Number(e.target.value))}
            />
          </div>
          <div>
            <label htmlFor="words">Essay length (words)</label>
            <input
              id="words"
              type="number"
              min={500}
              max={8000}
              step={100}
              value={config.wordCount}
              onChange={(e) => update("wordCount", Number(e.target.value))}
            />
          </div>
        </div>

        <div className="row">
          <div>
            <label htmlFor="tz">Timezone</label>
            <select
              id="tz"
              value={config.timezone}
              onChange={(e) => update("timezone", e.target.value)}
            >
              {TIMEZONES.includes(config.timezone) ? null : (
                <option value={config.timezone}>{config.timezone}</option>
              )}
              {TIMEZONES.map((tz) => (
                <option key={tz} value={tz}>
                  {tz}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="hour">Send hour (local)</label>
            <input
              id="hour"
              type="number"
              min={0}
              max={23}
              value={config.sendHour}
              onChange={(e) => update("sendHour", Number(e.target.value))}
            />
          </div>
        </div>

        <label htmlFor="recipient">Deliver to</label>
        <input
          id="recipient"
          type="email"
          value={config.recipient}
          onChange={(e) => update("recipient", e.target.value)}
        />

        <label htmlFor="name">
          Your name <span className="hint">— for the greeting (optional)</span>
        </label>
        <input
          id="name"
          type="text"
          value={config.readerName || ""}
          onChange={(e) => update("readerName", e.target.value)}
        />

        <div className="actions">
          <button onClick={save} disabled={saving}>
            {saving ? "Saving…" : "Save settings"}
          </button>
          <button className="secondary" onClick={sendTest} disabled={running}>
            {running ? "Working…" : "Send a brief now"}
          </button>
        </div>

        {status && <p className={`status ${status.kind}`}>{status.msg}</p>}
      </div>
    </>
  );
}
