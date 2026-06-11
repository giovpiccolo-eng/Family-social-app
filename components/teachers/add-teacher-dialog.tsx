"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createTeacher } from "@/lib/firebase/teachers";
import { useAuth } from "@/lib/firebase/auth-context";

interface AddTeacherDialogProps {
  onCreated?: (id: string) => void;
}

export function AddTeacherDialog({ onCreated }: AddTeacherDialogProps) {
  const { user, claims } = useAuth();
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [subject, setSubject] = useState("");
  const [grade, setGrade] = useState("");
  const [campus, setCampus] = useState("");
  const [classroomCourseId, setClassroomCourseId] = useState("");
  const [sisClassId, setSisClassId] = useState("");

  function reset() {
    setName("");
    setSubject("");
    setGrade("");
    setCampus("");
    setClassroomCourseId("");
    setSisClassId("");
    setError(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !claims) {
      setError("Not signed in");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const id = await createTeacher({
        schoolId: claims.schoolId,
        createdBy: user.uid,
        name: name.trim(),
        subject: subject.trim(),
        grade: grade.trim() || undefined,
        campus: campus.trim() || undefined,
        classroomCourseId: classroomCourseId.trim() || undefined,
        sisClassId: sisClassId.trim() || undefined,
      });
      reset();
      setOpen(false);
      onCreated?.(id);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add teacher");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>Add teacher</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <span className="font-mono text-xs uppercase tracking-widest text-amber">
            New teacher
          </span>
          <DialogTitle>Add a teacher to your roster.</DialogTitle>
          <DialogDescription>
            You can link Google Classroom and SIS identifiers later from the
            teacher detail screen.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2 space-y-2">
              <Label htmlFor="name">Full name</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                placeholder="Aanya Patel"
              />
            </div>
            <div className="col-span-2 space-y-2">
              <Label htmlFor="subject">Subject</Label>
              <Input
                id="subject"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                required
                placeholder="IB Physics HL"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="grade">Grade</Label>
              <Input
                id="grade"
                value={grade}
                onChange={(e) => setGrade(e.target.value)}
                placeholder="Year 11–12"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="campus">Campus</Label>
              <Input
                id="campus"
                value={campus}
                onChange={(e) => setCampus(e.target.value)}
                placeholder="Main"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="classroomCourseId">Classroom course ID</Label>
              <Input
                id="classroomCourseId"
                value={classroomCourseId}
                onChange={(e) => setClassroomCourseId(e.target.value)}
                placeholder="optional"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="sisClassId">SIS class ID</Label>
              <Input
                id="sisClassId"
                value={sisClassId}
                onChange={(e) => setSisClassId(e.target.value)}
                placeholder="optional"
              />
            </div>
          </div>
          {error && (
            <p className="font-mono text-xs text-destructive">{error}</p>
          )}
          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              onClick={() => setOpen(false)}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Adding…" : "Add teacher"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
