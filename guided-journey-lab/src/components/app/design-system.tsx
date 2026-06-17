import type { ReactNode } from "react";
import { AlertCircle, Loader2 } from "lucide-react";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

interface PageHeaderProps {
  eyebrow?: string;
  title: string;
  detail?: string;
  action?: ReactNode;
  className?: string;
}

export function PageHeader({ eyebrow, title, detail, action, className }: PageHeaderProps) {
  return (
    <header
      className={cn(
        "flex flex-col gap-5 rounded-2xl border border-border/70 bg-surface-elevated px-5 py-6 shadow-soft sm:px-6 lg:flex-row lg:items-end lg:justify-between",
        className,
      )}
    >
      <div className="max-w-3xl">
        {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
        <h1 className="mt-4 text-display text-2xl leading-tight text-foreground sm:text-3xl">
          {title}
        </h1>
        {detail ? (
          <p className="mt-2 max-w-[64ch] text-sm leading-relaxed text-muted-foreground">
            {detail}
          </p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </header>
  );
}

interface SectionHeaderProps {
  number?: string;
  title: string;
  detail?: string;
  action?: ReactNode;
  className?: string;
}

export function SectionHeader({ number, title, detail, action, className }: SectionHeaderProps) {
  return (
    <div
      className={cn("flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between", className)}
    >
      <div className="min-w-0">
        <div className="flex items-center gap-3">
          {number ? (
            <span className="text-[10px] font-semibold uppercase tracking-[0.18em] text-primary">
              {number}
            </span>
          ) : null}
          <span className="h-px flex-1 bg-border/70 sm:w-16 sm:flex-none" />
        </div>
        <h2 className="mt-3 text-display text-xl leading-tight text-foreground">{title}</h2>
        {detail ? (
          <p className="mt-1.5 max-w-[60ch] text-sm leading-relaxed text-muted-foreground">
            {detail}
          </p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}

export function GlassPanel({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <section
      className={cn(
        "rounded-2xl border border-border/70 bg-surface-elevated/82 p-5 shadow-soft backdrop-blur-md sm:p-6",
        className,
      )}
    >
      {children}
    </section>
  );
}

export function MetricCard({
  label,
  value,
  detail,
  icon,
  className,
}: {
  label: string;
  value: ReactNode;
  detail?: string;
  icon?: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-border/70 bg-surface-elevated p-5 shadow-soft transition-all duration-300 hover:-translate-y-0.5 hover:shadow-elevated",
        className,
      )}
    >
      <div className="flex items-start justify-between gap-4">
        <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground">
          {label}
        </p>
        {icon ? (
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-accent/55 text-primary">
            {icon}
          </span>
        ) : null}
      </div>
      <p className="mt-5 text-display text-3xl leading-none text-foreground">{value}</p>
      {detail ? (
        <p className="mt-2 text-xs leading-relaxed text-muted-foreground">{detail}</p>
      ) : null}
    </div>
  );
}

export function StatusPill({
  children,
  tone = "neutral",
  className,
}: {
  children: ReactNode;
  tone?: "neutral" | "success" | "warning" | "danger";
  className?: string;
}) {
  const tones = {
    neutral: "border-border bg-surface text-muted-foreground",
    success: "border-primary/15 bg-accent/55 text-primary",
    warning: "border-gold/35 bg-gold/12 text-gold-foreground",
    danger: "border-destructive/25 bg-destructive/5 text-destructive",
  };

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.16em]",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

export function EmptyState({
  title,
  detail,
  action,
  className,
}: {
  title: string;
  detail: string;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <StateFrame className={className}>
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 max-w-md text-sm leading-relaxed text-muted-foreground">{detail}</p>
      {action ? <div className="mt-5">{action}</div> : null}
    </StateFrame>
  );
}

export function LoadingState({
  title = "Loading",
  detail = "Fetching the latest EduLife data.",
}: {
  title?: string;
  detail?: string;
}) {
  return (
    <StateFrame>
      <Loader2 className="h-5 w-5 animate-spin text-primary" aria-hidden />
      <p className="mt-3 text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 max-w-md text-sm leading-relaxed text-muted-foreground">{detail}</p>
    </StateFrame>
  );
}

export function ErrorState({
  title = "This section is unavailable",
  detail,
  onRetry,
}: {
  title?: string;
  detail: string;
  onRetry?: () => void;
}) {
  return (
    <StateFrame className="border-destructive/25 bg-destructive/5">
      <AlertCircle className="h-5 w-5 text-destructive" aria-hidden />
      <p className="mt-3 text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 max-w-md text-sm leading-relaxed text-muted-foreground">{detail}</p>
      {onRetry ? (
        <Button type="button" variant="outline" size="sm" className="mt-5" onClick={onRetry}>
          Retry
        </Button>
      ) : null}
    </StateFrame>
  );
}

export function ActionFooter({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={cn(
        "sticky bottom-4 z-10 flex flex-col gap-3 rounded-2xl border border-border/70 bg-surface-elevated/90 p-3 shadow-elevated backdrop-blur-md sm:flex-row sm:items-center sm:justify-end",
        className,
      )}
    >
      {children}
    </div>
  );
}

function StateFrame({ children, className }: { children: ReactNode; className?: string }) {
  return (
    // Loading, empty, and error states share one frame so screens do not drift visually.
    <div
      className={cn(
        "flex min-h-44 flex-col items-center justify-center rounded-2xl border border-border/70 bg-surface-elevated px-6 py-10 text-center shadow-soft",
        className,
      )}
    >
      {children}
    </div>
  );
}
