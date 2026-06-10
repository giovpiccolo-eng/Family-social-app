import { Screen, ScreenHeader } from "@/components/screen";
import { Card, CardContent } from "@/components/ui/card";

export default function ObservationsPage() {
  return (
    <Screen>
      <ScreenHeader
        eyebrow="Sprint 3"
        title="Observations"
        description="Recording capture, Whisper transcription, and the two-pass Claude analysis pipeline land in Sprint 3."
      />
      <Card>
        <CardContent className="py-12 text-center">
          <p className="font-mono text-xs uppercase tracking-widest text-ink-faint">
            Not yet implemented
          </p>
        </CardContent>
      </Card>
    </Screen>
  );
}
