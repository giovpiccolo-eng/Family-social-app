"use client";

import {
  collection,
  query,
  where,
  orderBy,
  limit,
  addDoc,
  updateDoc,
  doc,
  getDoc,
  getDocs,
  serverTimestamp,
  Timestamp,
} from "firebase/firestore";
import { ref as storageRef, uploadBytes } from "firebase/storage";
import { getFirebase } from "./client";
import type { Note, Observation, ObservationStatus } from "@/types";

const COL = "observations";

export interface CreateObservationInput {
  schoolId: string;
  teacherId: string;
  observerId: string;
  durationSec: number;
  audioPath: string;
  photoPaths: string[];
  notes: Note[];
}

export async function createObservation(
  input: CreateObservationInput,
): Promise<string> {
  const { db } = getFirebase();
  const ref = await addDoc(collection(db, COL), {
    ...input,
    observedAt: serverTimestamp() as unknown as Timestamp,
    status: "recorded" satisfies ObservationStatus,
    createdAt: serverTimestamp() as unknown as Timestamp,
  });
  return ref.id;
}

export async function setObservationStatus(
  observationId: string,
  status: ObservationStatus,
  error?: string,
) {
  const { db } = getFirebase();
  await updateDoc(doc(db, COL, observationId), {
    status,
    ...(error ? { error } : {}),
  });
}

export async function getObservation(
  observationId: string,
): Promise<Observation | null> {
  const { db } = getFirebase();
  const snap = await getDoc(doc(db, COL, observationId));
  if (!snap.exists()) return null;
  return { id: snap.id, ...snap.data() } as Observation;
}

export async function listObservationsForTeacher(
  schoolId: string,
  teacherId: string,
  max = 20,
): Promise<Observation[]> {
  const { db } = getFirebase();
  const q = query(
    collection(db, COL),
    where("schoolId", "==", schoolId),
    where("teacherId", "==", teacherId),
    orderBy("observedAt", "desc"),
    limit(max),
  );
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }) as Observation);
}

// Storage uploads. Per brief §2.2 / §2.3 — clients write directly to Storage;
// security rules enforce schoolId and contentType.
export interface AudioUploadInput {
  schoolId: string;
  teacherId: string;
  observationId: string;
  blob: Blob;
}

export async function uploadAudioBlob({
  schoolId,
  teacherId,
  observationId,
  blob,
}: AudioUploadInput): Promise<string> {
  const { storage } = getFirebase();
  const path = `audio/${schoolId}/${teacherId}/${observationId}.webm`;
  await uploadBytes(storageRef(storage, path), blob, {
    contentType: blob.type || "audio/webm",
  });
  return path;
}

export interface PhotoUploadInput {
  schoolId: string;
  teacherId: string;
  observationId: string;
  photoId: string;
  blob: Blob;
}

export async function uploadPhotoBlob({
  schoolId,
  teacherId,
  observationId,
  photoId,
  blob,
}: PhotoUploadInput): Promise<string> {
  const { storage } = getFirebase();
  const path = `photos/${schoolId}/${teacherId}/${observationId}/${photoId}.jpg`;
  await uploadBytes(storageRef(storage, path), blob, {
    contentType: "image/jpeg",
  });
  return path;
}
