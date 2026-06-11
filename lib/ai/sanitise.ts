import "server-only";
import type { ClassRoster, Student, StudentFlag } from "@/types";

// Flags that must NEVER reach an AI prompt per brief §6.2.
const REDACTED_FLAGS: StudentFlag[] = ["CHILD_PROTECTION"];

export type SanitisedStudent = Pick<Student, "id" | "name"> & {
  flags: StudentFlag[];
};

export interface SanitisedRoster {
  classCode: string;
  yearGroup: string;
  students: SanitisedStudent[];
  redactedCount: number;
}

// sanitiseRosterForAI — single entry point per brief §6.2.
// Strips CHILD_PROTECTION (and any future restricted flags) from every student.
// MUST be called as the first line of any analysis route. CI lint rule (Sprint 5)
// will fail any direct Student[] passing to an AI prompt.
export function sanitiseRosterForAI(roster: ClassRoster): SanitisedRoster {
  let redactedCount = 0;
  const students = roster.students.map((s) => {
    const safe = s.flags.filter((f) => {
      if (REDACTED_FLAGS.includes(f)) {
        redactedCount++;
        return false;
      }
      return true;
    });
    return { id: s.id, name: s.name, flags: safe };
  });
  return {
    classCode: roster.classCode,
    yearGroup: roster.yearGroup,
    students,
    redactedCount,
  };
}
