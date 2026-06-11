// Tolerant JSON parser per brief §4.2.
// Strategy:
//  1. Try JSON.parse on the raw input.
//  2. Strip markdown fences (```json ... ```) if present and retry.
//  3. Find the largest balanced JSON object substring, retry.
//  4. If still failing, walk braces left-to-right, close any open structures,
//     and parse. Log the recovery so prompts can be tightened.

export interface ParseResult<T> {
  value: T;
  recovered: boolean;
  warning?: string;
}

const FENCE_RE = /```(?:json)?\s*([\s\S]*?)```/i;

export function tolerantJsonParse<T = unknown>(input: string): ParseResult<T> {
  const raw = input.trim();

  // 1. Direct.
  try {
    return { value: JSON.parse(raw) as T, recovered: false };
  } catch {
    // continue
  }

  // 2. Strip markdown fences.
  const fenced = raw.match(FENCE_RE);
  if (fenced) {
    try {
      return { value: JSON.parse(fenced[1].trim()) as T, recovered: true, warning: "stripped-fence" };
    } catch {
      // continue
    }
  }

  // 3. Largest balanced object substring.
  const balanced = findLargestBalancedObject(raw);
  if (balanced) {
    try {
      return { value: JSON.parse(balanced) as T, recovered: true, warning: "extracted-object" };
    } catch {
      // continue
    }
  }

  // 4. Brace-balancing recovery.
  const fixed = closeUnbalanced(raw);
  if (fixed) {
    try {
      return { value: JSON.parse(fixed) as T, recovered: true, warning: "closed-unbalanced" };
    } catch {
      // continue
    }
  }

  throw new Error(`Tolerant JSON parse failed: ${truncate(raw, 200)}`);
}

function findLargestBalancedObject(s: string): string | null {
  const start = s.indexOf("{");
  if (start === -1) return null;
  let depth = 0;
  let inString = false;
  let escape = false;
  let lastClose = -1;
  for (let i = start; i < s.length; i++) {
    const ch = s[i];
    if (inString) {
      if (escape) {
        escape = false;
      } else if (ch === "\\") {
        escape = true;
      } else if (ch === '"') {
        inString = false;
      }
      continue;
    }
    if (ch === '"') {
      inString = true;
      continue;
    }
    if (ch === "{") depth++;
    else if (ch === "}") {
      depth--;
      if (depth === 0) lastClose = i;
    }
  }
  if (lastClose === -1) return null;
  return s.slice(start, lastClose + 1);
}

function closeUnbalanced(s: string): string | null {
  const start = s.indexOf("{");
  if (start === -1) return null;
  const stack: string[] = [];
  let inString = false;
  let escape = false;
  let end = s.length;
  for (let i = start; i < s.length; i++) {
    const ch = s[i];
    if (inString) {
      if (escape) {
        escape = false;
      } else if (ch === "\\") {
        escape = true;
      } else if (ch === '"') {
        inString = false;
      }
      continue;
    }
    if (ch === '"') inString = true;
    else if (ch === "{") stack.push("}");
    else if (ch === "[") stack.push("]");
    else if (ch === "}" || ch === "]") stack.pop();
    end = i + 1;
  }

  // Trim any trailing partial token (comma, colon, partial key/value) so we can
  // close cleanly. Walk backwards past whitespace, commas, partial strings.
  let trimEnd = end;
  while (trimEnd > start) {
    const ch = s[trimEnd - 1];
    if (ch === "," || /\s/.test(ch)) {
      trimEnd--;
      continue;
    }
    break;
  }

  let candidate = s.slice(start, trimEnd);
  if (inString) candidate += '"';
  // Close any unclosed structures in reverse order.
  for (let i = stack.length - 1; i >= 0; i--) {
    candidate += stack[i];
  }
  return candidate;
}

function truncate(s: string, n: number): string {
  return s.length <= n ? s : `${s.slice(0, n)}…`;
}
