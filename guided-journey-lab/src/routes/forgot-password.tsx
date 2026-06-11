import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { AlertTriangle, ArrowLeft, ArrowRight, CheckCircle2, GraduationCap } from "lucide-react";
import { getFirebaseAuth, getFirebaseAuthModule } from "../lib/auth/firebase";
import { getReadableAuthError } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/forgot-password")({
  component: ForgotPasswordPage,
  head: () => ({
    meta: [
      { title: "Reset Password — EduLife" },
      {
        name: "description",
        content: "Reset your EduLife account password via Firebase.",
      },
    ],
  }),
});

function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    if (appEnv.demoMode) {
      setError("Password reset is not available in website demo mode.");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const [auth, firebaseAuth] = await Promise.all([
        getFirebaseAuth(),
        getFirebaseAuthModule(),
      ]);
      await firebaseAuth.sendPasswordResetEmail(auth, email.trim());
      setSent(true);
    } catch (err) {
      // Swallow account-enumeration codes — always pretend the email was sent so an attacker
      // cannot probe which addresses are registered.
      const code = err && typeof err === "object" && "code" in err ? String(err.code) : "";
      if (code === "auth/user-not-found" || code === "auth/invalid-credential") {
        setSent(true);
      } else {
        setError(getReadableAuthError(err));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4">
      <div className="absolute inset-0 -z-10 bg-hero-gradient" />
      <div className="absolute left-1/2 top-1/2 -z-10 h-[700px] w-[1000px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-gradient-aurora opacity-50 blur-3xl animate-glow" />

      <motion.div
        initial={{ opacity: 0, y: 28 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        className="w-full max-w-[420px]"
      >
        <Link
          to="/login"
          className="mb-8 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to sign in
        </Link>

        <div className="rounded-3xl border border-border bg-surface-elevated p-10 shadow-elevated">
          <div className="group mb-8 flex items-center gap-2">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-teal text-teal-foreground">
              <GraduationCap className="h-5 w-5" />
            </span>
            <span className="text-display text-xl text-foreground opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-within:opacity-100">
              EduLife
            </span>
          </div>

          <h1 className="text-display text-3xl leading-tight text-foreground">
            Reset password
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
            Enter your email — Firebase will send a reset link.
          </p>

          {sent ? (
            <div className="mt-8 flex items-start gap-3 rounded-2xl border border-primary/20 bg-primary/8 p-4 text-sm text-foreground">
              <CheckCircle2 className="mt-0.5 h-4 w-4 text-primary" />
              <div>
                <p className="font-semibold">Check your inbox.</p>
                <p className="mt-1 text-muted-foreground">
                  If an account exists for {email}, a reset email is on its way.
                </p>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="mt-8 space-y-5">
              <div className="space-y-1.5">
                <label
                  htmlFor="email"
                  className="block text-xs uppercase tracking-[0.16em] text-muted-foreground"
                >
                  Email
                </label>
                <input
                  id="email"
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  className="h-12 w-full rounded-xl border border-border bg-surface px-4 text-sm text-foreground outline-none transition-all placeholder:text-muted-foreground/50 focus:border-primary focus:ring-2 focus:ring-primary/10"
                />
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="group inline-flex h-12 w-full items-center justify-center gap-2 rounded-full bg-foreground text-sm font-medium text-background shadow-elevated transition-all hover:opacity-90 active:scale-[0.98] disabled:opacity-60 disabled:pointer-events-none"
              >
                {submitting ? "Sending..." : "Send reset link"}
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </button>

              {error ? (
                <div
                  role="alert"
                  aria-live="polite"
                  className="flex items-start gap-2 rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
                >
                  <AlertTriangle className="mt-0.5 h-4 w-4" />
                  <p>{error}</p>
                </div>
              ) : null}
            </form>
          )}

          <div className="mt-8 border-t border-border pt-6 text-center">
            <p className="text-sm text-muted-foreground">
              Remembered it?{" "}
              <Link
                to="/login"
                className="font-medium text-primary transition-colors hover:text-primary-glow"
              >
                Sign in →
              </Link>
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
