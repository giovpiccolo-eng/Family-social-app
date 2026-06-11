"use client";

import { useRef } from "react";
import { Camera, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { resizeImage } from "@/lib/recording/resize-image";
import { formatTimestamp } from "@/lib/utils";

export interface CapturedPhoto {
  id: string;
  elapsedSecs: number;
  timestamp: string;
  previewUrl: string;
  blob: Blob;
}

interface PhotoStripProps {
  photos: CapturedPhoto[];
  elapsedSecs: number;
  disabled: boolean;
  onAdd: (p: CapturedPhoto) => void;
  onRemove: (id: string) => void;
}

export function PhotoStrip({
  photos,
  elapsedSecs,
  disabled,
  onAdd,
  onRemove,
}: PhotoStripProps) {
  const fileRef = useRef<HTMLInputElement>(null);

  async function handleFiles(files: FileList | null) {
    if (!files) return;
    for (const file of Array.from(files)) {
      try {
        const blob = await resizeImage(file);
        const id = crypto.randomUUID();
        onAdd({
          id,
          elapsedSecs,
          timestamp: formatTimestamp(elapsedSecs),
          previewUrl: URL.createObjectURL(blob),
          blob,
        });
      } catch {
        // Skip files that fail to decode; surface in UI later if needed.
      }
    }
    if (fileRef.current) fileRef.current.value = "";
  }

  return (
    <div className="flex items-center gap-3 overflow-x-auto">
      <input
        ref={fileRef}
        type="file"
        accept="image/*"
        capture="environment"
        multiple
        className="hidden"
        onChange={(e) => handleFiles(e.target.files)}
      />
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={disabled}
        onClick={() => fileRef.current?.click()}
        className="shrink-0"
      >
        <Camera className="h-4 w-4" />
        Capture photo
      </Button>

      {photos.length === 0 ? (
        <span className="font-mono text-[10px] uppercase tracking-widest text-ink-faint">
          Photos held in state until you stop the recording.
        </span>
      ) : (
        photos.map((p) => (
          <div
            key={p.id}
            className="relative shrink-0 overflow-hidden rounded border border-rule"
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={p.previewUrl}
              alt={`Photo at ${p.timestamp}`}
              className="h-16 w-24 object-cover"
            />
            <span className="absolute bottom-0 left-0 bg-navy-950/70 px-1 font-mono text-[9px] uppercase tracking-widest text-amber">
              {p.timestamp}
            </span>
            <button
              type="button"
              onClick={() => onRemove(p.id)}
              className="absolute right-0 top-0 rounded-bl bg-navy-950/70 p-0.5 text-ink-muted hover:text-destructive"
            >
              <X className="h-3 w-3" />
            </button>
          </div>
        ))
      )}
    </div>
  );
}
