// Data model — mirrors brief §3. Keep shapes exact; the AI pipeline depends on them.
import type { Timestamp } from "firebase/firestore";

export type Role = "admin" | "principal" | "evaluator";
export type Plan = "pilot" | "standard" | "enterprise";

// 3.1 schools
export interface School {
  id: string;
  name: string;
  region?: string;
  plan: Plan;
  ownerUid: string;
  createdAt: Timestamp;
  sisConfig?: SisConfig;
  classroomConfig?: ClassroomConfig;
}

// 3.2 users
export interface User {
  uid: string;
  email: string;
  displayName: string;
  role: Role;
  schoolId: string;
  createdAt: Timestamp;
  consentAcceptedAt?: Timestamp;
}

// 3.3 teachers
export interface Teacher {
  id: string;
  schoolId: string;
  name: string;
  subject: string;
  grade?: string;
  campus?: string;
  classroomCourseId?: string;
  sisClassId?: string;
  createdBy: string;
  createdAt: Timestamp;
  archived?: boolean;
}

// 3.4.1 Note shape
export interface Note {
  id: string;
  text: string;
  timestamp: string; // "MM:SS"
  elapsedSecs: number;
}

export type ObservationStatus =
  | "recorded"
  | "transcribing"
  | "analysing"
  | "complete"
  | "error";

// 3.4 observations
export interface Observation {
  id: string;
  schoolId: string;
  teacherId: string;
  observerId: string;
  observedAt: Timestamp;
  durationSec: number;
  audioPath: string;
  photoPaths?: string[];
  transcript?: string;
  notes: Note[];
  analysis?: ObservationAnalysis;
  report?: string;
  biasFilterApplied?: boolean;
  status: ObservationStatus;
  error?: string;
  createdAt: Timestamp;
}

// 3.5 observation.analysis (embedded)
export type DifferentiationVerdict =
  | "Strong differentiation"
  | "Adequate"
  | "Inconsistent"
  | "Insufficient"
  | "Absent";

export type Rating =
  | "Unsatisfactory"
  | "Basic"
  | "Proficient"
  | "Distinguished";

export type SourceStream = "transcript" | "notes" | "classroom" | "sis" | "photos";

export interface CorrelatedMoment {
  timestamp: string; // "MM:SS"
  principalNote: string | null;
  transcriptContext: string;
  rosterContext: string | null;
  combined: string;
}

export interface Mapping {
  component: string; // "3b", "2d", etc.
  evidence: string;
  rating: Rating;
  confidence: number; // 60..98
  sourceTimestamp: string;
  sourceStreams: SourceStream[];
  biasFlag: string | null;
}

export interface ObservationAnalysis {
  lessonContext: string;
  alignmentWithPlan: string;
  observationalSummary: string;
  differentiationAssessment: {
    flaggedStudentsObserved: string[];
    scaffoldsExpected: string;
    scaffoldsObserved: string;
    verdict: DifferentiationVerdict;
    verdictReason: string;
  };
  googleClassroomFindings: {
    postedResourceUse: string;
    homeworkAlignment: string;
    concerns: string[];
  };
  correlatedMoments: CorrelatedMoment[];
  mappings: Mapping[];
  keyBehaviours: string[];
  strengthArea: string;
  growthArea: string;
}

// 3.6 evidence (denormalised)
export interface Evidence {
  id: string;
  schoolId: string;
  teacherId: string;
  observationId: string;
  component: string;
  domain: 1 | 2 | 3 | 4;
  rating: Rating;
  ratingNum: 1 | 2 | 3 | 4;
  evidence: string;
  confidence: number;
  sourceTimestamp: string;
  sourceStreams: SourceStream[];
  biasFlag: string | null;
  createdAt: Timestamp;
}

// 3.7 SIS adapter config
export interface SisConfig {
  type: "csv" | "manual" | "managebac" | "powerschool" | "stub";
  uploadedRosterPath?: string;
  apiEndpoint?: string;
  apiToken?: string; // encrypted at rest
  lastSyncedAt?: Timestamp;
}

export interface ClassroomConfig {
  domain?: string;
  refreshToken?: string; // encrypted at rest
  connectedAt?: Timestamp;
}

// 3.8 classes
export type StudentFlag =
  | "EAL"
  | "GIFTED"
  | "IEP_DYSLEXIA"
  | "IEP_ADHD"
  | "IEP_AUTISM"
  | "IEP_DYSCALCULIA"
  | "BEHAVIOUR_PLAN"
  | "CHILD_PROTECTION" // restricted — never reaches AI; see §6.2
  | "MEDICAL_PLAN";

export interface Student {
  id: string;
  name: string;
  flags: StudentFlag[];
}

export interface ClassRoster {
  id: string;
  schoolId: string;
  teacherId?: string;
  classCode: string;
  yearGroup: string;
  students: Student[];
  syncedAt: Timestamp;
}

// Invite — first-time user flow.
export interface Invite {
  token: string;
  schoolId: string;
  email: string;
  role: Role;
  invitedBy: string;
  createdAt: Timestamp;
  acceptedAt?: Timestamp;
  expiresAt: Timestamp;
}

// Custom claims mirrored from the user doc.
export interface AuthClaims {
  schoolId: string;
  role: Role;
}
