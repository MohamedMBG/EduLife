import { useEffect, useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import {
  AuthCard,
  AuthFooterLink,
  AuthInput,
  AuthLogo,
  AuthPasswordInput,
  AuthPrimaryButton,
  AuthShell,
} from "../components/auth/AuthComponents";
import { useAuth } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/login")({
  component: LoginPage,
  validateSearch: (search: Record<string, unknown>): { redirect?: string } => {
    const redirect = typeof search.redirect === "string" ? search.redirect : undefined;
    return redirect && redirect.startsWith("/") && !redirect.startsWith("//") ? { redirect } : {};
  },
  head: () => ({
    meta: [
      { title: "Sign In | EduLife" },
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
    // The loading transition after Firebase sign-in should still move the user forward once
    // AuthContext starts syncing with the backend session.
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
    <AuthShell cardClassName="max-w-[440px]">
      <AuthCard>
        <AuthLogo />

        <h1 className="mt-10 text-display text-[clamp(2rem,4vw,2.75rem)] leading-[1.05] text-foreground">
          Welcome back.
        </h1>
        <p className="mt-3 max-w-[42ch] text-sm leading-relaxed text-muted-foreground">
          {appEnv.demoMode
            ? "Demo mode runs entirely in the browser. Any email and password opens the learner flow."
            : "Sign in with Firebase. We'll sync your learner profile with the backend automatically."}
        </p>

        <form onSubmit={handleSubmit} className="mt-10 space-y-5">
          <AuthInput
            id="email"
            label="Email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => handleEmailChange(e.target.value)}
            placeholder="you@example.com"
          />

          <AuthPasswordInput
            id="password"
            label="Password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => handlePasswordChange(e.target.value)}
            placeholder="••••••••"
            visible={showPassword}
            onToggleVisible={() => setShowPassword((v) => !v)}
            labelAction={
              <Link
                to="/forgot-password"
                className="text-xs text-primary transition-colors hover:text-primary-glow focus-ring"
              >
                Forgot password?
              </Link>
            }
          />

          <AuthPrimaryButton
            type="submit"
            disabled={submitting || (!auth.configured && !appEnv.demoMode)}
          >
            {submitting ? "Signing in..." : "Sign in"}
          </AuthPrimaryButton>

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

        <AuthFooterLink prefix="No account yet?" to="/register">
          Create one →
        </AuthFooterLink>
      </AuthCard>
    </AuthShell>
  );
}
