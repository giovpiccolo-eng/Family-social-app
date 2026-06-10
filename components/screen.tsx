import { cn } from "@/lib/utils";

interface ScreenProps {
  children: React.ReactNode;
  className?: string;
}

export function Screen({ children, className }: ScreenProps) {
  return (
    <main className={cn("mx-auto w-full max-w-7xl px-6 py-10", className)}>
      {children}
    </main>
  );
}

interface ScreenHeaderProps {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: React.ReactNode;
}

export function ScreenHeader({
  eyebrow,
  title,
  description,
  actions,
}: ScreenHeaderProps) {
  return (
    <div className="mb-10 flex items-start justify-between gap-6">
      <div className="space-y-2">
        {eyebrow && (
          <span className="font-mono text-xs uppercase tracking-widest text-amber">
            {eyebrow}
          </span>
        )}
        <h1 className="font-serif text-4xl font-semibold leading-tight tracking-tight text-foreground">
          {title}
        </h1>
        {description && (
          <p className="max-w-2xl text-sm text-ink-muted">{description}</p>
        )}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-3">{actions}</div>}
    </div>
  );
}
