"use client";

import { useState } from "react";
import { Screen, ScreenHeader } from "@/components/screen";
import { TeacherList } from "@/components/teachers/teacher-list";
import { AddTeacherDialog } from "@/components/teachers/add-teacher-dialog";

export default function TeachersPage() {
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <Screen>
      <ScreenHeader
        eyebrow="Roster"
        title="Teachers"
        description="Select a teacher to begin a new observation, or add a teacher to your roster."
        actions={<AddTeacherDialog onCreated={() => setRefreshKey((k) => k + 1)} />}
      />
      <TeacherList refreshKey={refreshKey} />
    </Screen>
  );
}
