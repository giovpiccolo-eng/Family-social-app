"use client";

import {
  collection,
  query,
  where,
  orderBy,
  addDoc,
  updateDoc,
  doc,
  getDoc,
  getDocs,
  serverTimestamp,
  Timestamp,
} from "firebase/firestore";
import { getFirebase } from "./client";
import type { Teacher } from "@/types";

const COL = "teachers";

export async function listTeachers(schoolId: string): Promise<Teacher[]> {
  const { db } = getFirebase();
  const q = query(
    collection(db, COL),
    where("schoolId", "==", schoolId),
    orderBy("name", "asc"),
  );
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }) as Teacher);
}

export async function getTeacher(teacherId: string): Promise<Teacher | null> {
  const { db } = getFirebase();
  const snap = await getDoc(doc(db, COL, teacherId));
  if (!snap.exists()) return null;
  return { id: snap.id, ...snap.data() } as Teacher;
}

export interface CreateTeacherInput {
  schoolId: string;
  createdBy: string;
  name: string;
  subject: string;
  grade?: string;
  campus?: string;
  classroomCourseId?: string;
  sisClassId?: string;
}

export async function createTeacher(input: CreateTeacherInput): Promise<string> {
  const { db } = getFirebase();
  const ref = await addDoc(collection(db, COL), {
    ...input,
    archived: false,
    createdAt: serverTimestamp() as unknown as Timestamp,
  });
  return ref.id;
}

export async function updateTeacher(
  teacherId: string,
  patch: Partial<Omit<Teacher, "id" | "schoolId" | "createdAt" | "createdBy">>,
): Promise<void> {
  const { db } = getFirebase();
  await updateDoc(doc(db, COL, teacherId), patch);
}

export async function archiveTeacher(teacherId: string): Promise<void> {
  return updateTeacher(teacherId, { archived: true });
}
