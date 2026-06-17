import { useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { AnimatePresence, motion } from "framer-motion";
import { ArrowLeft, BookOpen, GraduationCap, Users } from "lucide-react";
import {
  AuthCard,
  AuthFooterLink,
  AuthInput,
  AuthLogo,
  AuthPasswordInput,
  AuthPrimaryButton,
  AuthShell,
  RoleOptionCard,
} from "../components/auth/AuthComponents";
import { useAuth } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";
import type { UserRole } from "../lib/api/types";

export const Route = createFileRoute("/register")({
  component: RegisterPage,
  head: () => ({
    meta: [
      { title: "Create Account | EduLife" },
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
    icon: <GraduationCap className="h-6 w-6" strokeWidth={1.5} />,
  },
  {
    value: "TEACHER",
    label: "Teacher",
    description: "Create and publish courses for learners on the platform.",
    icon: <BookOpen className="h-6 w-6" strokeWidth={1.5} />,
  },
  {
    value: "GROUP_ADMIN",
    label: "Institute Admin",
    description: "Manage teachers and build your organization or institute.",
    icon: <Users className="h-6 w-6" strokeWidth={1.5} />,
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

  const selectedRoleLabel = ROLE_OPTIONS.find((role) => role.value === selectedRole)?.label;
  const passwordMismatch = form.confirm.length > 0 && form.confirm !== form.password;

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    if (submitError) setSubmitError(null);
    if (auth.error) auth.clearError();
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (passwordMismatch) return;

    setSubmitting(true);
    setSubmitError(null);

    try {
      // The selected role remains a registration intent; AuthContext and backend sync still decide
      // the persisted role instead of trusting arbitrary client identity data.
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
    <AuthShell cardClassName="max-w-[560px]">
      <AuthCard>
        <AuthLogo />

        <AnimatePresence mode="wait" initial={false}>
          {step === "role" ? (
            <motion.div
              key="role"
              initial={false}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -18 }}
              transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
            >
              <h1 className="mt-10 text-display text-[clamp(2rem,4vw,2.75rem)] leading-[1.05] text-foreground">
                I want to join as...
              </h1>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
                Choose your role. You can always request a change later.
              </p>

              <div className="mt-8 space-y-3">
                {ROLE_OPTIONS.map((option) => (
                  <RoleOptionCard
                    key={option.value}
                    label={option.label}
                    description={option.description}
                    icon={option.icon}
                    selected={selectedRole === option.value}
                    onSelect={() => setSelectedRole(option.value)}
                  />
                ))}
              </div>

              <AuthPrimaryButton
                type="button"
                className="mt-6"
                onClick={() => setStep("credentials")}
              >
                Continue
              </AuthPrimaryButton>
            </motion.div>
          ) : (
            <motion.div
              key="credentials"
              initial={{ opacity: 0, x: 18 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 18 }}
              transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
            >
              <button
                type="button"
                onClick={() => setStep("role")}
                className="group mt-8 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground focus-ring"
              >
                <ArrowLeft className="h-3.5 w-3.5 transition-transform duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:-translate-x-0.5" />
                Change role
              </button>

              <h1 className="mt-6 text-display text-[clamp(2rem,4vw,2.75rem)] leading-[1.05] text-foreground">
                Create your account
              </h1>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                Joining as <span className="font-medium text-foreground">{selectedRoleLabel}</span>
              </p>

              <form onSubmit={handleSubmit} className="mt-8 space-y-4">
                <AuthInput
                  id="name"
                  name="name"
                  label="Full name"
                  type="text"
                  required
                  autoComplete="name"
                  value={form.name}
                  onChange={handleChange}
                  placeholder="Mohamed Baghdadi"
                />

                <AuthInput
                  id="email"
                  name="email"
                  label="Email"
                  type="email"
                  required
                  autoComplete="email"
                  value={form.email}
                  onChange={handleChange}
                  placeholder="you@example.com"
                />

                <AuthPasswordInput
                  id="password"
                  name="password"
                  label="Password"
                  required
                  autoComplete="new-password"
                  minLength={8}
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Min. 8 characters"
                  visible={showPassword}
                  onToggleVisible={() => setShowPassword((v) => !v)}
                />

                <AuthPasswordInput
                  id="confirm"
                  name="confirm"
                  label="Confirm password"
                  required
                  autoComplete="new-password"
                  value={form.confirm}
                  onChange={handleChange}
                  placeholder="Repeat your password"
                  visible={showConfirm}
                  onToggleVisible={() => setShowConfirm((v) => !v)}
                  error={passwordMismatch ? "Passwords do not match." : null}
                />

                <AuthPrimaryButton
                  type="submit"
                  disabled={
                    passwordMismatch || submitting || (!auth.configured && !appEnv.demoMode)
                  }
                >
                  {submitting ? "Creating account..." : "Create account"}
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
            </motion.div>
          )}
        </AnimatePresence>

        <AuthFooterLink prefix="Already have an account?" to="/login">
          Sign in →
        </AuthFooterLink>
      </AuthCard>
    </AuthShell>
  );
}
