import type { InputHTMLAttributes, ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import {
  ArrowLeft,
  ArrowUpRight,
  Check,
  Eye,
  EyeOff,
  GraduationCap,
  ShieldCheck,
} from "lucide-react";
import { motion } from "framer-motion";
import { cn } from "../../lib/utils";

type AuthRoute = "/" | "/login" | "/register" | "/forgot-password";

interface AuthShellProps {
  children: ReactNode;
  backTo?: AuthRoute;
  backLabel?: string;
  cardClassName?: string;
}

export function AuthShell({
  children,
  backTo = "/",
  backLabel = "Back to home",
  cardClassName,
}: AuthShellProps) {
  return (
    <div className="relative grid min-h-[100dvh] overflow-hidden bg-background text-foreground lg:grid-cols-12">
      <div className="noise-overlay" aria-hidden />

      <main className="relative flex items-center justify-center px-5 py-10 sm:px-6 lg:col-span-7 lg:py-20">
        <div className="absolute inset-0 -z-10 bg-hero-gradient opacity-60" />
        <div className="absolute -top-32 left-1/4 -z-10 h-[480px] w-[640px] -translate-x-1/2 rounded-full bg-gradient-aurora opacity-40 blur-3xl animate-glow" />

        <motion.div
          // Auth screens must remain readable from SSR if hydration is delayed by Vite or Firebase.
          initial={false}
          animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          transition={{ duration: 0.9, ease: [0.16, 1, 0.3, 1] }}
          className={cn("w-full max-w-[520px]", cardClassName)}
        >
          <AuthBackLink to={backTo}>{backLabel}</AuthBackLink>
          {children}
        </motion.div>
      </main>

      <AuthBrandPanel />
    </div>
  );
}

export function AuthCard({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <section className="bezel">
      <div className={cn("bezel-inner p-7 sm:p-9 lg:p-11", className)}>{children}</div>
    </section>
  );
}

export function AuthBrandPanel() {
  const proofPoints = [
    "Server-graded exams, never the client",
    "Cooldowns and pass thresholds enforced by the backend",
    "Verifiable certificate with a unique public link",
  ];

  return (
    <aside className="relative hidden items-center justify-center overflow-hidden bg-gradient-to-br from-primary via-primary to-primary-glow text-primary-foreground lg:col-span-5 lg:flex">
      <div className="absolute -right-32 -top-40 h-96 w-96 rounded-full bg-gold/30 blur-3xl" />
      <div className="absolute -bottom-40 -left-32 h-96 w-96 rounded-full bg-teal/30 blur-3xl" />
      <div
        className="absolute inset-0 opacity-[0.04] mix-blend-overlay"
        style={{
          backgroundImage:
            "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='160' height='160'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>\")",
        }}
      />

      <motion.div
        // Keep the brand proof visible before client hydration completes.
        initial={false}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 1, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
        className="relative max-w-[420px] px-10"
      >
        <span className="inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/12 px-3 py-1 text-[10px] font-medium uppercase tracking-[0.2em]">
          <ShieldCheck className="h-3 w-3" strokeWidth={1.75} />
          Verified learner identity
        </span>
        <h2 className="mt-7 max-w-[16ch] text-display text-[clamp(2rem,3.5vw,3rem)] leading-[1.05]">
          One identity bridges every EduLife surface.
        </h2>
        <p className="mt-6 max-w-[40ch] text-base leading-relaxed text-primary-foreground/80">
          The same Firebase session powers the Android app and the web. Your progress, exam
          attempts, and certificates stay perfectly in sync.
        </p>

        <ul className="mt-10 space-y-3 text-sm text-primary-foreground/80">
          {proofPoints.map((line) => (
            <li key={line} className="flex items-start gap-3">
              <span className="mt-1.5 h-1 w-1 rounded-full bg-gold" />
              {line}
            </li>
          ))}
        </ul>
      </motion.div>
    </aside>
  );
}

export function AuthBackLink({ to, children }: { to: AuthRoute; children: ReactNode }) {
  return (
    <Link
      to={to}
      className="group mb-8 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground focus-ring sm:mb-10"
    >
      <ArrowLeft className="h-3.5 w-3.5 transition-transform duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:-translate-x-0.5" />
      {children}
    </Link>
  );
}

export function AuthLogo() {
  return (
    <div className="flex items-center gap-2.5">
      <span className="grid h-10 w-10 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-bezel">
        <GraduationCap className="h-5 w-5" strokeWidth={1.5} />
      </span>
      <span className="text-display text-xl text-foreground">EduLife</span>
    </div>
  );
}

interface AuthInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string | null;
}

export function AuthInput({ label, id, error, className, ...props }: AuthInputProps) {
  const inputId = id ?? props.name;

  return (
    <div className="space-y-1.5">
      <label
        htmlFor={inputId}
        className="block text-[10px] font-mono uppercase tracking-[0.2em] text-muted-foreground"
      >
        {label}
      </label>
      <input
        id={inputId}
        className={cn(
          "h-12 w-full rounded-xl hairline bg-surface px-4 text-sm text-foreground outline-none transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] placeholder:text-muted-foreground/50 focus:border-primary focus:ring-2 focus:ring-primary/15",
          error && "border-destructive focus:border-destructive focus:ring-destructive/10",
          className,
        )}
        aria-invalid={error ? true : undefined}
        aria-describedby={error && inputId ? `${inputId}-error` : undefined}
        {...props}
      />
      {error ? (
        <p id={inputId ? `${inputId}-error` : undefined} className="text-xs text-destructive">
          {error}
        </p>
      ) : null}
    </div>
  );
}

interface AuthPasswordInputProps extends Omit<AuthInputProps, "type"> {
  visible: boolean;
  onToggleVisible: () => void;
  labelAction?: ReactNode;
}

export function AuthPasswordInput({
  label,
  id,
  error,
  className,
  visible,
  onToggleVisible,
  labelAction,
  ...props
}: AuthPasswordInputProps) {
  const inputId = id ?? props.name;

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <label
          htmlFor={inputId}
          className="block text-[10px] font-mono uppercase tracking-[0.2em] text-muted-foreground"
        >
          {label}
        </label>
        {labelAction}
      </div>
      <div className="relative">
        <input
          id={inputId}
          type={visible ? "text" : "password"}
          className={cn(
            "h-12 w-full rounded-xl hairline bg-surface px-4 pr-12 text-sm text-foreground outline-none transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] placeholder:text-muted-foreground/50 focus:border-primary focus:ring-2 focus:ring-primary/15",
            error && "border-destructive focus:border-destructive focus:ring-destructive/10",
            className,
          )}
          aria-invalid={error ? true : undefined}
          aria-describedby={error && inputId ? `${inputId}-error` : undefined}
          {...props}
        />
        <button
          type="button"
          onClick={onToggleVisible}
          className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground focus-ring"
          aria-label={visible ? "Hide password" : "Show password"}
          aria-pressed={visible}
        >
          {visible ? (
            <EyeOff className="h-4 w-4" strokeWidth={1.5} />
          ) : (
            <Eye className="h-4 w-4" strokeWidth={1.5} />
          )}
        </button>
      </div>
      {error ? (
        <p id={inputId ? `${inputId}-error` : undefined} className="text-xs text-destructive">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export function AuthPrimaryButton({
  children,
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      className={cn(
        "group relative inline-flex h-12 w-full items-center justify-center gap-1.5 rounded-full bg-primary pl-5 pr-1.5 text-sm font-medium text-primary-foreground shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:shadow-elevated active:scale-[0.98] disabled:pointer-events-none disabled:opacity-40",
        className,
      )}
      {...props}
    >
      <span>{children}</span>
      <span className="grid h-9 w-9 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:-translate-y-px group-hover:translate-x-0.5 group-hover:bg-background/25">
        <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
      </span>
    </button>
  );
}

export function AuthFooterLink({
  prefix,
  to,
  children,
}: {
  prefix: string;
  to: AuthRoute;
  children: ReactNode;
}) {
  return (
    <div className="mt-8 border-t border-border/60 pt-6 text-center">
      <p className="text-sm text-muted-foreground">
        {prefix}{" "}
        <Link
          to={to}
          className="font-medium text-primary transition-colors hover:text-primary-glow"
        >
          {children}
        </Link>
      </p>
    </div>
  );
}

interface RoleOptionCardProps {
  label: string;
  description: string;
  icon: ReactNode;
  selected: boolean;
  onSelect: () => void;
}

export function RoleOptionCard({
  label,
  description,
  icon,
  selected,
  onSelect,
}: RoleOptionCardProps) {
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      className={cn(
        "group flex w-full items-start gap-4 rounded-2xl border p-4 text-left transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] focus-ring",
        selected
          ? "border-primary bg-primary/5 ring-2 ring-primary/10"
          : "border-border/70 bg-surface hover:border-primary/40 hover:bg-surface-elevated",
      )}
    >
      <span
        className={cn(
          "mt-0.5 grid h-10 w-10 shrink-0 place-items-center rounded-xl transition-colors",
          selected
            ? "bg-gradient-primary text-primary-foreground shadow-bezel"
            : "bg-surface-elevated text-muted-foreground",
        )}
      >
        {icon}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-semibold text-foreground">{label}</span>
        <span className="mt-0.5 block text-xs leading-relaxed text-muted-foreground">
          {description}
        </span>
      </span>
      <span
        className={cn(
          "mt-2 grid h-5 w-5 shrink-0 place-items-center rounded-full border transition-all",
          selected
            ? "border-primary bg-primary text-primary-foreground"
            : "border-border bg-surface-elevated text-transparent group-hover:border-primary/40",
        )}
        aria-hidden
      >
        <Check className="h-3 w-3" strokeWidth={2} />
      </span>
    </button>
  );
}
