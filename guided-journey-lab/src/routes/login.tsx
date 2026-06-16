import { useEffect, useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { GraduationCap, Eye, EyeOff, ArrowUpRight, ArrowLeft, ShieldCheck } from "lucide-react";
import { motion } from "framer-motion";
import { useAuth } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/login")({
  component: LoginPage,
  validateSearch: (search: Record<string, unknown>): { redirect?: string } => {
    const redirect = typeof search.redirect === "string" ? search.redirect : undefined;
    return redirect && redirect.startsWith("/") && !redirect.startsWith("//")
      ? { redirect }
      : {};
  },
  head: () => ({
    meta: [
      { title: "Sign In — EduLife" },
      {
        name: "description",
        content: "Sign in to your EduLife account and continue your learning journey.",
      },
    ],
  }),
});

function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const auth = useAuth();
  const { redirect } = Route.useSearch();

  const [signedIn, setSignedIn] = useState(false);

  useEffect(() => {
    const destination = redirect ?? "/dashboard";
    if (auth.status === "authenticated") {
      navigate({ to: destination });
      return;
    }
    if (signedIn && auth.status === "loading") {
      navigate({ to: destination });
    }
  }, [auth.status, navigate, signedIn, redirect]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setSubmitError(null);

    try {
      await auth.login(email, password);
      setSignedIn(true);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "Unable to sign in.");
    } finally {
      setSubmitting(false);
    }
  }

  function handleEmailChange(value: string) {
    setEmail(value);
    if (submitError) setSubmitError(null);
    if (auth.error) auth.clearError();
  }

  function handlePasswordChange(value: string) {
    setPassword(value);
    if (submitError) setSubmitError(null);
    if (auth.error) auth.clearError();
  }

  return (
    <div className="relative min-h-[100dvh] grid lg:grid-cols-12 bg-background text-foreground overflow-hidden">
      <div className="noise-overlay" aria-hidden />

      {/* Left: form */}
      <div className="relative lg:col-span-7 flex items-center justify-center px-6 py-12 lg:py-20">
        <div className="absolute inset-0 -z-10 bg-hero-gradient opacity-60" />
        <div className="absolute -top-32 left-1/4 -z-10 h-[480px] w-[640px] -translate-x-1/2 rounded-full bg-gradient-aurora blur-3xl opacity-40 animate-glow" />

        <motion.div
          initial={{ opacity: 0, y: 28, filter: "blur(6px)" }}
          animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          transition={{ duration: 0.9, ease: [0.16, 1, 0.3, 1] }}
          className="w-full max-w-[440px]"
        >
          <Link
            to="/"
            className="group inline-flex items-center gap-1.5 mb-10 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-3.5 w-3.5 transition-transform duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:-translate-x-0.5" />
            Back to home
          </Link>

          <div className="bezel">
            <div className="bezel-inner p-10 lg:p-11">
              <div className="flex items-center gap-2.5">
                <span className="grid place-items-center h-10 w-10 rounded-xl bg-gradient-primary text-primary-foreground shadow-bezel">
                  <GraduationCap className="h-5 w-5" strokeWidth={1.5} />
                </span>
                <span className="text-display text-xl text-foreground">EduLife</span>
              </div>

              <h1 className="mt-10 text-display text-[clamp(2rem,4vw,2.75rem)] leading-[1.05] text-foreground">
                Welcome back.
              </h1>
              <p className="mt-3 text-sm text-muted-foreground leading-relaxed max-w-[42ch]">
                {appEnv.demoMode
                  ? "Demo mode runs entirely in the browser. Any email and password opens the learner flow."
                  : "Sign in with Firebase — we'll sync your learner profile with the backend automatically."}
              </p>

              <form onSubmit={handleSubmit} className="mt-10 space-y-5">
                <div className="space-y-1.5">
                  <label
                    htmlFor="email"
                    className="block text-[10px] uppercase tracking-[0.2em] font-mono text-muted-foreground"
                  >
                    Email
                  </label>
                  <input
                    id="email"
                    type="email"
                    required
                    autoComplete="email"
                    value={email}
                    onChange={(e) => handleEmailChange(e.target.value)}
                    placeholder="you@example.com"
                    className="w-full h-12 rounded-xl hairline bg-surface px-4 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <label
                      htmlFor="password"
                      className="text-[10px] uppercase tracking-[0.2em] font-mono text-muted-foreground"
                    >
                      Password
                    </label>
                    <Link
                      to="/forgot-password"
                      className="text-xs text-primary hover:text-primary-glow transition-colors"
                    >
                      Forgot password?
                    </Link>
                  </div>
                  <div className="relative">
                    <input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      required
                      autoComplete="current-password"
                      value={password}
                      onChange={(e) => handlePasswordChange(e.target.value)}
                      placeholder="••••••••"
                      className="w-full h-12 rounded-xl hairline bg-surface px-4 pr-12 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                      aria-label={showPassword ? "Hide password" : "Show password"}
                      aria-pressed={showPassword}
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4" strokeWidth={1.5} />
                      ) : (
                        <Eye className="h-4 w-4" strokeWidth={1.5} />
                      )}
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={submitting || (!auth.configured && !appEnv.demoMode)}
                  className="group relative w-full h-12 inline-flex items-center justify-center gap-1.5 rounded-full bg-foreground text-background pl-5 pr-1.5 text-sm font-medium shadow-bezel active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:shadow-elevated"
                >
                  <span>{submitting ? "Signing in…" : "Sign In"}</span>
                  <span className="grid h-9 w-9 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:bg-background/25 group-hover:translate-x-0.5 group-hover:-translate-y-px">
                    <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
                  </span>
                </button>

                {(submitError || auth.error) && (
                  <div
                    role="alert"
                    aria-live="polite"
                    className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
                  >
                    {submitError || auth.error}
                  </div>
                )}
              </form>

              <div className="mt-10 pt-6 border-t border-border/60 text-center">
                <p className="text-sm text-muted-foreground">
                  No account yet?{" "}
                  <Link
                    to="/register"
                    className="text-primary hover:text-primary-glow font-medium transition-colors"
                  >
                    Create one →
                  </Link>
                </p>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Right: brand panel */}
      <aside className="relative hidden lg:flex lg:col-span-5 items-center justify-center overflow-hidden bg-gradient-to-br from-primary via-primary to-primary-glow text-primary-foreground">
        <div className="absolute -top-40 -right-32 h-96 w-96 rounded-full bg-gold/30 blur-3xl" />
        <div className="absolute -bottom-40 -left-32 h-96 w-96 rounded-full bg-teal/30 blur-3xl" />
        <div
          className="absolute inset-0 opacity-[0.04] mix-blend-overlay"
          style={{
            backgroundImage:
              "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='160' height='160'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>\")",
          }}
        />

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
          className="relative max-w-[420px] px-10"
        >
          <span className="inline-flex items-center gap-2 rounded-full bg-primary-foreground/12 border border-primary-foreground/20 px-3 py-1 text-[10px] uppercase tracking-[0.2em] font-medium">
            <ShieldCheck className="h-3 w-3" strokeWidth={1.75} />
            Verified learner identity
          </span>
          <h2 className="mt-7 text-display text-[clamp(2rem,3.5vw,3rem)] leading-[1.05] max-w-[16ch]">
            One identity bridges every EduLife surface.
          </h2>
          <p className="mt-6 text-base leading-relaxed text-primary-foreground/80 max-w-[40ch]">
            The same Firebase session powers the Android app and the web — your progress,
            exam attempts, and certificates stay perfectly in sync.
          </p>

          <ul className="mt-10 space-y-3 text-sm text-primary-foreground/80">
            {[
              "Server-graded exams, never the client",
              "Cooldowns and pass thresholds enforced by the backend",
              "Verifiable certificate with a unique public link",
            ].map((line) => (
              <li key={line} className="flex items-start gap-3">
                <span className="mt-1.5 h-1 w-1 rounded-full bg-gold" />
                {line}
              </li>
            ))}
          </ul>
        </motion.div>
      </aside>
    </div>
  );
}
