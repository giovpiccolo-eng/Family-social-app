// Danielson Framework for Teaching, 2022 edition.
// All 22 components must be available per brief §4.3.
import type { Rating } from "@/types";

export interface DanielsonComponent {
  id: string; // "1a", "3b", etc.
  domain: 1 | 2 | 3 | 4;
  title: string;
}

export const DANIELSON_DOMAINS = {
  1: "Planning and Preparation",
  2: "Learning Environments",
  3: "Learning Experiences",
  4: "Principled Teaching",
} as const;

export const DANIELSON_COMPONENTS: DanielsonComponent[] = [
  { id: "1a", domain: 1, title: "Applying knowledge of content and pedagogy" },
  { id: "1b", domain: 1, title: "Knowing and valuing students" },
  { id: "1c", domain: 1, title: "Setting instructional outcomes" },
  { id: "1d", domain: 1, title: "Using resources effectively" },
  { id: "1e", domain: 1, title: "Planning coherent instruction" },
  { id: "1f", domain: 1, title: "Designing and analysing assessment" },

  { id: "2a", domain: 2, title: "Cultivating respectful and affirming environments" },
  { id: "2b", domain: 2, title: "Fostering a culture for learning" },
  { id: "2c", domain: 2, title: "Maintaining purposeful environments" },
  { id: "2d", domain: 2, title: "Supporting positive student behaviour" },
  { id: "2e", domain: 2, title: "Organising spaces for learning" },

  { id: "3a", domain: 3, title: "Communicating about purpose and content" },
  { id: "3b", domain: 3, title: "Using questioning and discussion techniques" },
  { id: "3c", domain: 3, title: "Engaging students in learning" },
  { id: "3d", domain: 3, title: "Using assessment for learning" },
  { id: "3e", domain: 3, title: "Responding flexibly to student needs" },

  { id: "4a", domain: 4, title: "Engaging in reflective practice" },
  { id: "4b", domain: 4, title: "Documenting student progress" },
  { id: "4c", domain: 4, title: "Engaging families and community" },
  { id: "4d", domain: 4, title: "Contributing to school community and culture" },
  { id: "4e", domain: 4, title: "Growing and developing professionally" },
  { id: "4f", domain: 4, title: "Acting in service of students" },
];

// Components most load-bearing for differentiation per §4.3.
export const DIFFERENTIATION_COMPONENTS = ["1b", "1e", "3e", "1d"] as const;

export const RATINGS: Rating[] = [
  "Unsatisfactory",
  "Basic",
  "Proficient",
  "Distinguished",
];

export function ratingToNumber(r: Rating): 1 | 2 | 3 | 4 {
  return (RATINGS.indexOf(r) + 1) as 1 | 2 | 3 | 4;
}

export function componentDomain(componentId: string): 1 | 2 | 3 | 4 {
  const found = DANIELSON_COMPONENTS.find((c) => c.id === componentId);
  if (!found) throw new Error(`Unknown Danielson component: ${componentId}`);
  return found.domain;
}
