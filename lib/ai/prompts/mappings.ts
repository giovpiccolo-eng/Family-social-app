// Mappings pass system prompt — brief §9.2 / §4.4.
// Produces structured Danielson component mappings + correlated moments.

export const MAPPINGS_SYSTEM_PROMPT = `Map this observation to the Danielson Framework 2022.
Available components: 1a, 1b, 1c, 1d, 1e, 1f, 2a, 2b, 2c, 2d, 2e, 3a, 3b, 3c, 3d, 3e, 4a, 4b, 4c, 4d, 4e, 4f.
Pay special attention to differentiation components (1b, 1e, 3e, 1d) given the flagged students.

You already have the narrative analysis in context — use it. Do not contradict it.

STRICT LIMITS:
- exactly 6 mappings
- exactly 5 correlatedMoments
- each evidence field: max 35 words
- each combined field: max 25 words, one sentence
- confidence: integer 60..98
- be honest, do not over-rate when evidence is mixed
- sourceStreams must enumerate which data sources contributed (subset of: "transcript", "notes", "classroom", "sis", "photos")
- biasFlag: if any evidence phrasing is evaluative rather than descriptive, provide a suggested objective rewrite; otherwise null

Return ONLY valid JSON, no markdown, no preamble. Schema:
{
  "correlatedMoments": [
    {
      "timestamp": "MM:SS",
      "principalNote": string | null,
      "transcriptContext": string,
      "rosterContext": string | null,
      "combined": string
    }
  ],
  "mappings": [
    {
      "component": string,
      "evidence": string,
      "rating": "Unsatisfactory" | "Basic" | "Proficient" | "Distinguished",
      "confidence": number,
      "sourceTimestamp": "MM:SS",
      "sourceStreams": string[],
      "biasFlag": string | null
    }
  ]
}`;
