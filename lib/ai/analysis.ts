import "server-only";
import {
  complete,
  type ContentBlock,
  type ImageMediaType,
} from "./anthropic";
import { tolerantJsonParse } from "./tolerant-json";
import { NARRATIVE_SYSTEM_PROMPT } from "./prompts/narrative";
import { MAPPINGS_SYSTEM_PROMPT } from "./prompts/mappings";
import { REPORT_SYSTEM_PROMPT } from "./prompts/report";
import { BIAS_FILTER_SYSTEM_PROMPT } from "./prompts/bias-filter";
import type { SanitisedRoster } from "./sanitise";
import type {
  CorrelatedMoment,
  Mapping,
  Note,
  ObservationAnalysis,
} from "@/types";

export interface ClassroomContext {
  postedPlan?: string;
  resources?: string[];
  homework?: string;
}

export interface AnalysisInput {
  transcript: string;
  notes: Note[];
  classroom?: ClassroomContext;
  roster?: SanitisedRoster; // already sanitised — CHILD_PROTECTION stripped
  photos?: { timestamp: string; base64: string; mediaType: ImageMediaType }[];
}

type NarrativeResult = Omit<
  ObservationAnalysis,
  "correlatedMoments" | "mappings"
>;

type MappingsResult = {
  correlatedMoments: CorrelatedMoment[];
  mappings: Mapping[];
};

function formatNotes(notes: Note[]): string {
  if (notes.length === 0) return "(none)";
  return notes
    .slice()
    .sort((a, b) => a.elapsedSecs - b.elapsedSecs)
    .map((n) => `[${n.timestamp}] ${n.text}`)
    .join("\n");
}

function formatClassroom(c?: ClassroomContext): string {
  if (!c) return "(not connected)";
  const lines: string[] = [];
  if (c.postedPlan) lines.push(`Posted plan:\n${c.postedPlan}`);
  if (c.resources?.length) lines.push(`Resources:\n- ${c.resources.join("\n- ")}`);
  if (c.homework) lines.push(`Homework:\n${c.homework}`);
  return lines.join("\n\n") || "(empty)";
}

function formatRoster(r?: SanitisedRoster): string {
  if (!r) return "(not available)";
  const flaggedSummary = r.students
    .filter((s) => s.flags.length > 0)
    .map((s) => `- ${s.name}: ${s.flags.join(", ")}`);
  if (flaggedSummary.length === 0) return `${r.classCode} (${r.yearGroup}) — no flagged students`;
  return `${r.classCode} (${r.yearGroup})\n${flaggedSummary.join("\n")}`;
}

function userMessageForNarrative(input: AnalysisInput): ContentBlock[] {
  const blocks: ContentBlock[] = [];
  blocks.push({
    type: "text",
    text: [
      "LESSON TRANSCRIPT:",
      input.transcript || "(empty)",
      "",
      "PRINCIPAL NOTES:",
      formatNotes(input.notes),
      "",
      "GOOGLE CLASSROOM CONTEXT:",
      formatClassroom(input.classroom),
      "",
      "SCHOOL SIS:",
      formatRoster(input.roster),
    ].join("\n"),
  });
  // Per brief §4.6 — photos as base64 image blocks with timestamp text.
  for (const p of input.photos ?? []) {
    blocks.push({ type: "text", text: `Photo captured at ${p.timestamp}:` });
    blocks.push({
      type: "image",
      source: { type: "base64", media_type: p.mediaType, data: p.base64 },
    });
  }
  return blocks;
}

function userMessageForMappings(
  input: AnalysisInput,
  narrative: NarrativeResult,
): string {
  return [
    "NARRATIVE ANALYSIS (already produced):",
    JSON.stringify(narrative, null, 2),
    "",
    "LESSON TRANSCRIPT:",
    input.transcript || "(empty)",
    "",
    "PRINCIPAL NOTES:",
    formatNotes(input.notes),
    "",
    "GOOGLE CLASSROOM CONTEXT:",
    formatClassroom(input.classroom),
    "",
    "SCHOOL SIS:",
    formatRoster(input.roster),
  ].join("\n");
}

// Two-pass analysis per brief §4.1.
// Pass 1: narrative (3000 tokens). Pass 2: mappings (4000 tokens).
// If mappings fail, narrative is still usable.
export async function analyseObservation(
  input: AnalysisInput,
): Promise<{
  analysis: ObservationAnalysis;
  warnings: string[];
}> {
  const warnings: string[] = [];

  const narrativeRaw = await complete({
    system: NARRATIVE_SYSTEM_PROMPT,
    user: userMessageForNarrative(input),
    maxTokens: 3000,
  });
  const narrativeParsed = tolerantJsonParse<NarrativeResult>(narrativeRaw);
  if (narrativeParsed.recovered) {
    warnings.push(`narrative:${narrativeParsed.warning ?? "recovered"}`);
  }
  const narrative = narrativeParsed.value;

  let correlatedMoments: CorrelatedMoment[] = [];
  let mappings: Mapping[] = [];
  try {
    const mappingsRaw = await complete({
      system: MAPPINGS_SYSTEM_PROMPT,
      user: userMessageForMappings(input, narrative),
      maxTokens: 4000,
    });
    const mappingsParsed = tolerantJsonParse<MappingsResult>(mappingsRaw);
    if (mappingsParsed.recovered) {
      warnings.push(`mappings:${mappingsParsed.warning ?? "recovered"}`);
    }
    correlatedMoments = mappingsParsed.value.correlatedMoments ?? [];
    mappings = mappingsParsed.value.mappings ?? [];
  } catch (err) {
    warnings.push(
      `mappings:failed:${err instanceof Error ? err.message : "unknown"}`,
    );
  }

  return {
    analysis: { ...narrative, correlatedMoments, mappings },
    warnings,
  };
}

// Report composer — brief §9.3 / §4.5.
export async function composeReport(
  analysis: ObservationAnalysis,
  teacherName: string,
  subject: string,
): Promise<string> {
  const user = [
    `Teacher: ${teacherName}`,
    `Subject: ${subject}`,
    "",
    "Analysis (JSON):",
    JSON.stringify(analysis, null, 2),
  ].join("\n");
  return complete({
    system: REPORT_SYSTEM_PROMPT,
    user,
    maxTokens: 3000,
  });
}

// Bias filter — brief §4.5 / §9.4.
export interface BiasFilterResult {
  issues: { original: string; suggestion: string; reason: string }[];
  cleanText: string;
}

export async function runBiasFilter(reportText: string): Promise<BiasFilterResult> {
  const raw = await complete({
    system: BIAS_FILTER_SYSTEM_PROMPT,
    user: reportText,
    maxTokens: 1500,
  });
  const parsed = tolerantJsonParse<BiasFilterResult>(raw);
  return parsed.value;
}
