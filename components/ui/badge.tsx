import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-sm border px-2 py-0.5 font-mono text-[10px] uppercase tracking-widest transition-colors",
  {
    variants: {
      variant: {
        default: "border-rule bg-navy-700 text-ink",
        amber: "border-amber/40 bg-amber/10 text-amber",
        muted: "border-rule bg-transparent text-ink-muted",
        success: "border-emerald-600/40 bg-emerald-600/10 text-emerald-400",
        warning: "border-amber-600/40 bg-amber-600/10 text-amber-400",
        danger: "border-destructive/40 bg-destructive/10 text-destructive",
      },
    },
    defaultVariants: { variant: "default" },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
