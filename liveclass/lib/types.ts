export const TRACKS = [
  "evidence",
  "visuals",
  "media",
  "practice",
  "inclusion",
  "deep-dive",
] as const;

export type Track = (typeof TRACKS)[number];

export const TRACK_LABELS: Record<Track, string> = {
  evidence: "Evidence",
  visuals: "Visuals",
  media: "Media",
  practice: "Practice",
  inclusion: "Inclusion",
  "deep-dive": "Deep dives",
};

export const TRACK_DESCRIPTIONS: Record<Track, string> = {
  evidence: "Primary sources, archives, datasets, quotations.",
  visuals: "Maps, diagrams, paintings, photographs, charts.",
  media: "Video and audio clips that ground the moment.",
  practice: "Short exercises and check-for-understanding prompts.",
  inclusion: "Adapted versions for students with learning differences.",
  "deep-dive": "Optional extensions for students ready to go further.",
};

export type SourceKind = "primary" | "image" | "video" | "audio" | "dataset" | "article";

export type Source = {
  id: string;
  kind: SourceKind;
  title: string;
  citation: string;
  year?: string;
  blurb: string;
};

export type LessonKit = {
  title: string;
  objectives: string[];
  outline: string[];
  sources: Source[];
};

export type SuggestionCard = {
  id: string;
  track: Track;
  title: string;
  summary: string;
  source_id: string | null;
  why_now: string;
  created_at: number;
};

export type SuggestResponse = {
  inferred_topic: string;
  pedagogical_move:
    | "lecturing"
    | "questioning"
    | "demonstrating"
    | "transitioning"
    | "reviewing"
    | "other";
  cards: Omit<SuggestionCard, "id" | "created_at">[];
};
