import { useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { GraduationCap, Eye, EyeOff, ArrowRight, ArrowLeft } from "lucide-react";
import { motion } from "framer-motion";
import { useAuth } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/register")({
  component: RegisterPage,
  head: () => ({
    meta: [
      { title: "Create Account — EduLife" },
      {
        name: "description",
        content: "Create your EduLife account and start your guided learning journey.",
      },
    ],
  }),
});

function RegisterPage() {
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const auth = useAuth();

  const passwordMismatch = form.confirm.length > 0 && form.confirm !== form.password;

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (passwordMismatch) return;

    setSubmitting(true);
    setSubmitError(null);
    setSuccessMessage(null);

    try {
      await auth.register({
        name: form.name,
        email: form.email,
        password: form.password,
      });
      setSuccessMessage(
        "Account created. Verify your email first, then sign in so the backend can unlock your learner routes.",
      );
      navigate({ to: "/login" });
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "Unable to create the account.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="relative h-screen bg-background flex items-center justify-center px-4 overflow-hidden">
      {/* Background */}
      <div className="absolute inset-0 -z-10 bg-hero-gradient" />
      <div className="absolute left-1/2 top-1/2 -z-10 h-[700px] w-[1000px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-gradient-aurora blur-3xl opacity-50 animate-glow" />

      <motion.div
        initial={{ opacity: 0, y: 28 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        className="w-full max-w-[440px]"
      >
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 mb-5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to home
        </Link>

        <div className="rounded-3xl border border-border bg-surface-elevated shadow-elevated p-8">
          {/* Logo */}
          <div className="flex items-center gap-2 mb-5">
            <span className="grid place-items-center h-9 w-9 rounded-xl bg-gradient-primary text-primary-foreground">
              <GraduationCap className="h-5 w-5" />
            </span>
            <span className="text-display text-xl text-foreground">EduLife</span>
          </div>

          <h1 className="text-display text-3xl text-foreground leading-tight">
            Start your journey
          </h1>
          <p className="mt-2 text-sm text-muted-foreground leading-relaxed">
            {appEnv.demoMode
              ? "Create a local demo account to explore the website without Firebase or backend services."
              : "Create your Firebase account first. Verified email is required before the backend allows course access."}
          </p>

          <form onSubmit={handleSubmit} className="mt-5 space-y-4">
            {/* Full name */}
            <div className="space-y-1">
              <label
                htmlFor="name"
                className="block text-xs uppercase tracking-[0.16em] text-muted-foreground"
              >
                Full Name
              </label>
              <input
                id="name"
                name="name"
                type="text"
                required
                autoComplete="name"
                value={form.name}
                onChange={handleChange}
                placeholder="Mohamed Baghdadi"
                className="w-full h-12 rounded-xl border border-border bg-surface px-4 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
              />
            </div>

            {/* Email */}
            <div className="space-y-1">
              <label
                htmlFor="email"
                className="block text-xs uppercase tracking-[0.16em] text-muted-foreground"
              >
                Email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                required
                autoComplete="email"
                value={form.email}
                onChange={handleChange}
                placeholder="you@example.com"
                className="w-full h-12 rounded-xl border border-border bg-surface px-4 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
              />
            </div>

            {/* Password */}
            <div className="space-y-1">
              <label
                htmlFor="password"
                className="block text-xs uppercase tracking-[0.16em] text-muted-foreground"
              >
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  required
                  autoComplete="new-password"
                  minLength={8}
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Min. 8 characters"
                  className="w-full h-12 rounded-xl border border-border bg-surface px-4 pr-12 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            {/* Confirm password */}
            <div className="space-y-1">
              <label
                htmlFor="confirm"
                className="block text-xs uppercase tracking-[0.16em] text-muted-foreground"
              >
                Confirm Password
              </label>
              <div className="relative">
                <input
                  id="confirm"
                  name="confirm"
                  type={showConfirm ? "text" : "password"}
                  required
                  autoComplete="new-password"
                  value={form.confirm}
                  onChange={handleChange}
                  placeholder="Repeat your password"
                  className={`w-full h-12 rounded-xl border bg-surface px-4 pr-12 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:ring-2 transition-all ${
                    passwordMismatch
                      ? "border-destructive focus:border-destructive focus:ring-destructive/10"
                      : "border-border focus:border-primary focus:ring-primary/10"
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirm((v) => !v)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  aria-label={showConfirm ? "Hide password" : "Show password"}
                >
                  {showConfirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {passwordMismatch && (
                <p className="text-xs text-destructive">Passwords do not match.</p>
              )}
            </div>

            <button
              type="submit"
              disabled={passwordMismatch || submitting || (!auth.configured && !appEnv.demoMode)}
              className="group w-full h-12 inline-flex items-center justify-center gap-2 rounded-full bg-foreground text-background text-sm font-medium shadow-elevated hover:opacity-90 active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none transition-all"
            >
              {submitting ? "Creating account..." : "Create Account"}
              <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
            </button>

            {(submitError || auth.error || successMessage) && (
              <div
                className={`rounded-2xl border px-4 py-3 text-sm ${
                  submitError || auth.error
                    ? "border-destructive/20 bg-destructive/5 text-destructive"
                    : "border-primary/20 bg-primary/5 text-primary"
                }`}
              >
                {submitError || auth.error || successMessage}
              </div>
            )}
          </form>

          <div className="mt-5 pt-5 border-t border-border text-center">
            <p className="text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link
                to="/login"
                className="text-primary hover:text-primary-glow font-medium transition-colors"
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
