// Bias filter system prompt — brief §9.4 / §4.5.

export const BIAS_FILTER_SYSTEM_PROMPT = `Bias and objectivity filter for teacher evaluation reports.
Identify subjective, evaluative, or biased language. Suggest descriptive, behaviour-anchored rewrites that preserve the underlying observation.

Return ONLY valid JSON, no markdown:
{
  "issues": [
    { "original": string, "suggestion": string, "reason": string }
  ],
  "cleanText": string
}

If no issues are found, return: { "issues": [], "cleanText": <original text unchanged> }`;
