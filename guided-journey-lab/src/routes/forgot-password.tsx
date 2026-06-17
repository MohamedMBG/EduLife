import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { AlertTriangle, CheckCircle2 } from "lucide-react";
import {
  AuthCard,
  AuthFooterLink,
  AuthInput,
  AuthLogo,
  AuthPrimaryButton,
  AuthShell,
} from "../components/auth/AuthComponents";
import { getReadableAuthError } from "../lib/auth/auth-context";
import { getFirebaseAuth, getFirebaseAuthModule } from "../lib/auth/firebase";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/forgot-password")({
  component: ForgotPasswordPage,
  head: () => ({
    meta: [
      { title: "Reset Password | EduLife" },
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
      const [auth, firebaseAuth] = await Promise.all([getFirebaseAuth(), getFirebaseAuthModule()]);
      await firebaseAuth.sendPasswordResetEmail(auth, email.trim());
      setSent(true);
    } catch (err) {
      // Keep the reset response ambiguous so attackers cannot check which emails exist.
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
    <AuthShell backTo="/login" backLabel="Back to sign in" cardClassName="max-w-[440px]">
      <AuthCard>
        <AuthLogo />

        <h1 className="mt-10 text-display text-[clamp(2rem,4vw,2.75rem)] leading-[1.05] text-foreground">
          Reset password
        </h1>
        <p className="mt-3 max-w-[42ch] text-sm leading-relaxed text-muted-foreground">
          Enter your email. Firebase will send a reset link.
        </p>

        {sent ? (
          <div className="mt-8 flex items-start gap-3 rounded-2xl border border-primary/20 bg-primary/8 p-4 text-sm text-foreground">
            <CheckCircle2 className="mt-0.5 h-4 w-4 text-primary" strokeWidth={1.75} />
            <div>
              <p className="font-semibold">Check your inbox.</p>
              <p className="mt-1 text-muted-foreground">
                If an account exists for {email}, a reset email is on its way.
              </p>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-8 space-y-5">
            <AuthInput
              id="email"
              label="Email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
            />

            <AuthPrimaryButton type="submit" disabled={submitting}>
              {submitting ? "Sending..." : "Send reset link"}
            </AuthPrimaryButton>

            {error ? (
              <div
                role="alert"
                aria-live="polite"
                className="flex items-start gap-2 rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
              >
                <AlertTriangle className="mt-0.5 h-4 w-4" strokeWidth={1.75} />
                <p>{error}</p>
              </div>
            ) : null}
          </form>
        )}

        <AuthFooterLink prefix="Remembered it?" to="/login">
          Sign in →
        </AuthFooterLink>
      </AuthCard>
    </AuthShell>
  );
}
