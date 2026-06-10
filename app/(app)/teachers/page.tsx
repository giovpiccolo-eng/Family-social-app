import { Screen, ScreenHeader } from "@/components/screen";
import { Card, CardContent } from "@/components/ui/card";

export default function TeachersPage() {
  return (
    <Screen>
      <ScreenHeader
        eyebrow="Sprint 2"
        title="Teachers"
        description="Roster view, add/edit/archive, and the recording entry point land in Sprint 2."
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
