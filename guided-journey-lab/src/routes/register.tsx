import { useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { GraduationCap, Eye, EyeOff, ArrowRight, ArrowLeft, BookOpen, Users, ChevronRight } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { useAuth } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";
import type { UserRole } from "../lib/api/types";

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

type Step = "role" | "credentials";

interface RoleOption {
  value: UserRole;
  label: string;
  description: string;
  icon: React.ReactNode;
}

const ROLE_OPTIONS: RoleOption[] = [
  {
    value: "LEARNER",
    label: "Student",
    description: "Discover courses, track progress, and earn certificates.",
    icon: <GraduationCap className="h-6 w-6" />,
  },
  {
    value: "TEACHER",
    label: "Teacher",
    description: "Create and publish courses for learners on the platform.",
    icon: <BookOpen className="h-6 w-6" />,
  },
  {
    value: "GROUP_ADMIN",
    label: "Institute Admin",
    description: "Manage teachers and build your organization or institute.",
    icon: <Users className="h-6 w-6" />,
  },
];

function RegisterPage() {
  const [step, setStep] = useState<Step>("role");
  const [selectedRole, setSelectedRole] = useState<UserRole>("LEARNER");
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
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

    try {
      await auth.register({
        name: form.name,
        email: form.email,
        password: form.password,
        intendedRole: selectedRole,
      });
      navigate({ to: "/login" });
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "Unable to create the account.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="relative min-h-screen bg-background flex items-center justify-center px-4 py-8 overflow-hidden">
      <div className="absolute inset-0 -z-10 bg-hero-gradient" />
      <div className="absolute left-1/2 top-1/2 -z-10 h-[700px] w-[1000px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-gradient-aurora blur-3xl opacity-50 animate-glow" />

      <motion.div
        initial={{ opacity: 0, y: 28 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        className="w-full max-w-[480px]"
      >
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 mb-5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to home
        </Link>

        <div className="rounded-3xl border border-border bg-surface-elevated shadow-elevated p-8">
          <div className="group flex items-center gap-2 mb-5">
            <span className="grid place-items-center h-9 w-9 rounded-xl bg-teal text-teal-foreground">
              <GraduationCap className="h-5 w-5" />
            </span>
            <span className="text-display text-xl text-foreground opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-within:opacity-100">
              EduLife
            </span>
          </div>

          <AnimatePresence mode="wait">
            {step === "role" ? (
              <motion.div
                key="role"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <h1 className="text-display text-2xl text-foreground leading-tight">
                  I want to join as…
                </h1>
                <p className="mt-2 text-sm text-muted-foreground">
                  Choose your role. You can always request a change later.
                </p>

                <div className="mt-5 space-y-3">
                  {ROLE_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setSelectedRole(option.value)}
                      className={`w-full flex items-start gap-4 rounded-2xl border p-4 text-left transition-all ${
                        selectedRole === option.value
                          ? "border-primary bg-primary/5 ring-2 ring-primary/10"
                          : "border-border bg-surface hover:border-primary/40"
                      }`}
                    >
                      <span
                        className={`mt-0.5 grid place-items-center h-10 w-10 shrink-0 rounded-xl transition-colors ${
                          selectedRole === option.value
                            ? "bg-gradient-primary text-primary-foreground"
                            : "bg-surface-elevated text-muted-foreground"
                        }`}
                      >
                        {option.icon}
                      </span>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-foreground">{option.label}</p>
                        <p className="mt-0.5 text-xs text-muted-foreground leading-relaxed">
                          {option.description}
                        </p>
                      </div>
                      {selectedRole === option.value && (
                        <span className="mt-2 shrink-0 h-4 w-4 rounded-full bg-primary" />
                      )}
                    </button>
                  ))}
                </div>

                <button
                  type="button"
                  onClick={() => setStep("credentials")}
                  className="group mt-5 w-full h-12 inline-flex items-center justify-center gap-2 rounded-full bg-foreground text-background text-sm font-medium shadow-elevated hover:opacity-90 active:scale-[0.98] transition-all"
                >
                  Continue
                  <ChevronRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
                </button>
              </motion.div>
            ) : (
              <motion.div
                key="credentials"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.3 }}
              >
                <button
                  type="button"
                  onClick={() => setStep("role")}
                  className="inline-flex items-center gap-1.5 mb-4 text-sm text-muted-foreground hover:text-foreground transition-colors"
                >
                  <ArrowLeft className="h-3.5 w-3.5" />
                  Change role
                </button>

                <h1 className="text-display text-2xl text-foreground leading-tight">
                  Create your account
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                  Joining as{" "}
                  <span className="font-medium text-foreground">
                    {ROLE_OPTIONS.find((r) => r.value === selectedRole)?.label}
                  </span>
                </p>

                <form onSubmit={handleSubmit} className="mt-5 space-y-4">
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
                        aria-pressed={showPassword}
                      >
                        {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                      </button>
                    </div>
                  </div>

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
                        aria-pressed={showConfirm}
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
              </motion.div>
            )}
          </AnimatePresence>

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
