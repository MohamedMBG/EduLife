import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import {
  Award,
  BookOpen,
  GraduationCap,
  Layers3,
  ShieldCheck,
  Users,
  UserCheck,
  Activity,
} from "lucide-react";
import { AdminShell } from "../components/app/AdminShell";
import { getAdminMetrics } from "../lib/api/client";
import { useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/admin/dashboard")({
  component: AdminDashboardRoute,
  head: () => ({ meta: [{ title: "Admin Dashboard - EduLife" }] }),
});

function AdminDashboardRoute() {
  const auth = useAuth();

  const metricsQuery = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: () => getAdminMetrics(auth.getAccessToken),
  });

  const m = metricsQuery.data;
  const firstName =
    auth.session?.displayName?.split(" ").filter(Boolean)[0] || "Admin";

  return (
    <AdminShell active="dashboard">
      {metricsQuery.isLoading ? (
        <LoadingCard />
      ) : metricsQuery.isError ? (
        <ErrorCard message={metricsQuery.error.message} onRetry={() => metricsQuery.refetch()} />
      ) : (
        <div className="space-y-8">
          {/* Hero */}
          <section className="rounded-3xl bg-[oklch(0.40_0.19_250)] px-6 py-8 text-white shadow-elevated">
            <p className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs uppercase tracking-[0.16em]">
              <ShieldCheck className="h-3.5 w-3.5" />
              Platform admin
            </p>
            <h1 className="mt-4 text-display text-4xl">Welcome back, {firstName}</h1>
            <p className="mt-2 max-w-2xl text-sm text-white/70">
              Real-time platform metrics — all data from{" "}
              <code className="rounded bg-white/10 px-1 font-mono text-xs">
                /api/v1/admin/metrics
              </code>
            </p>
          </section>

          {/* Stat grid */}
          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              title="Learners"
              value={m?.totalLearners ?? 0}
              icon={<Users className="h-5 w-5 text-[oklch(0.40_0.19_250)]" />}
            />
            <StatCard
              title="Teachers"
              value={m?.totalTeachers ?? 0}
              icon={<GraduationCap className="h-5 w-5 text-teal-600" />}
            />
            <StatCard
              title="Published courses"
              value={m?.totalCoursesPublished ?? 0}
              icon={<Layers3 className="h-5 w-5 text-[oklch(0.40_0.19_250)]" />}
            />
            <StatCard
              title="Certificates issued"
              value={m?.totalCertificates ?? 0}
              icon={<Award className="h-5 w-5 text-amber-500" />}
            />
          </section>

          {/* Second row */}
          <section className="grid gap-4 sm:grid-cols-3">
            <StatCard
              title="Active enrollments"
              value={m?.totalEnrollmentsActive ?? 0}
              icon={<Activity className="h-5 w-5 text-teal-600" />}
            />
            <StatCard
              title="Draft courses"
              value={m?.totalCoursesDraft ?? 0}
              icon={<BookOpen className="h-5 w-5 text-muted-foreground" />}
            />
            <StatCard
              title="Group admins"
              value={m?.totalGroupAdmins ?? 0}
              icon={<ShieldCheck className="h-5 w-5 text-muted-foreground" />}
            />
          </section>

          {/* Quick actions */}
          <section className="grid gap-4 md:grid-cols-2">
            <Link
              to="/admin/teacher-requests"
              className="group relative overflow-hidden rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft transition-shadow hover:shadow-elevated"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <UserCheck className="h-5 w-5 text-[oklch(0.40_0.19_250)]" />
                    <p className="text-sm font-semibold text-foreground">Teacher Applications</p>
                    {(m?.pendingTeacherRequests ?? 0) > 0 && (
                      <span className="rounded-full bg-[oklch(0.92_0.03_250)] px-2 py-0.5 text-xs font-bold text-[oklch(0.40_0.19_250)]">
                        {m!.pendingTeacherRequests} pending
                      </span>
                    )}
                  </div>
                  <p className="mt-2 text-sm text-muted-foreground">
                    Review, approve, or reject learners requesting teacher role.
                  </p>
                </div>
                <span className="text-2xl text-muted-foreground group-hover:text-foreground transition-colors">
                  →
                </span>
              </div>
            </Link>

            <div className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft opacity-60">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <Users className="h-5 w-5 text-muted-foreground" />
                    <p className="text-sm font-semibold text-foreground">User Management</p>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                      coming soon
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-muted-foreground">
                    Browse users, change roles, deactivate accounts.
                  </p>
                </div>
              </div>
            </div>
          </section>
        </div>
      )}
    </AdminShell>
  );
}

function StatCard({ title, value, icon }: { title: string; value: number; icon: ReactNode }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{title}</p>
        {icon}
      </div>
      <p className="mt-3 text-display text-3xl text-foreground">{value.toLocaleString()}</p>
    </div>
  );
}

function LoadingCard() {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-12 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">Loading admin metrics…</p>
      <p className="mt-2 text-sm text-muted-foreground">Fetching from the live backend.</p>
    </div>
  );
}

function ErrorCard({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="rounded-3xl border border-destructive/20 bg-destructive/5 px-6 py-8 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">Admin metrics unavailable</p>
      <p className="mt-2 text-sm text-muted-foreground">{message}</p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-4 rounded-full bg-foreground px-4 py-2 text-sm font-medium text-background"
      >
        Retry
      </button>
    </div>
  );
}
