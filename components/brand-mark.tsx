import { cn } from "@/lib/utils";

interface BrandMarkProps {
  className?: string;
  showWordmark?: boolean;
}

export function BrandMark({ className, showWordmark = true }: BrandMarkProps) {
  return (
    <div className={cn("flex items-center gap-3", className)}>
      <div className="flex h-8 w-8 items-center justify-center rounded-sm bg-amber font-mono text-base font-bold text-navy-950">
        O
      </div>
      {showWordmark && (
        <span className="font-mono text-sm tracking-widest text-foreground">
          OBSERVER<span className="text-amber">.AI</span>
        </span>
      )}
    </div>
  );
}
