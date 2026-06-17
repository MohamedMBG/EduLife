import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  Activity,
  Award,
  BarChart3,
  BookOpen,
  CheckCircle2,
  GraduationCap,
  Users,
} from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { getPlatformAnalytics, getPlatformCohortAnalytics } from "../lib/api/client";
import { useAuth } from "../lib/auth/auth-context";
import type { Funnel, MonthCount, PlatformAnalytics } from "../lib/api/types";

export const Route = createFileRoute("/admin/analytics")({
  component: AdminAnalyticsRoute,
  head: () => ({ meta: [{ title: "Admin Analytics - EduLife" }] }),
});

function AdminAnalyticsRoute() {
  const auth = useAuth();
  const metricsQuery = useQuery({
    queryKey: ["analytics", "platform", "summary"],
    queryFn: () => getPlatformAnalytics(auth.getAccessToken),
  });
  const cohortsQuery = useQuery({
    queryKey: ["analytics", "platform", "cohorts"],
    queryFn: () => getPlatformCohortAnalytics(auth.getAccessToken),
  });

  return (
    <AppLayout>
      {metricsQuery.isLoading || cohortsQuery.isLoading ? (
        <StateCard title="Loading platform analytics..." detail="Fetching admin-only analytics." />
      ) : metricsQuery.isError ? (
        <ErrorCard message={metricsQuery.error.message} onRetry={() => metricsQuery.refetch()} />
      ) : cohortsQuery.isError ? (
        <ErrorCard message={cohortsQuery.error.message} onRetry={() => cohortsQuery.refetch()} />
      ) : (
        <div className="space-y-8">
          <section className="rounded-[2rem] border border-primary/20 bg-primary px-6 py-8 text-white shadow-elevated">
            <p className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs uppercase tracking-[0.16em]">
              <BarChart3 className="h-3.5 w-3.5" />
              Platform analytics
            </p>
            <h1 className="mt-4 text-display text-4xl">Learning outcomes dashboard</h1>
            <p className="mt-3 max-w-3xl text-sm leading-relaxed text-white/72">
              Global aggregates from `/api/v1/analytics/platform` and cohort funnels from
              `/api/v1/analytics/platform/cohorts`. Access is protected by admin RBAC on the
              backend.
            </p>
          </section>

          <PlatformMetricGrid metrics={metricsQuery.data} />

          <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
            <Panel
              title="Learner funnel"
              detail="Enrollment grains moving through the full learner loop."
            >
              <FunnelRows funnel={cohortsQuery.data?.funnel} />
            </Panel>
            <Panel title="Enrollment cohorts" detail="Active enrollments grouped by month.">
              <MonthRows
                rows={cohortsQuery.data?.enrollmentCohorts ?? []}
                empty="No enrollments yet."
              />
            </Panel>
          </section>

          <Panel
            title="Certificate trend"
            detail="Certificates issued after passed exams, grouped by month."
          >
            <MonthRows
              rows={cohortsQuery.data?.certificateTrend ?? []}
              empty="No certificates issued yet."
            />
          </Panel>
        </div>
      )}
    </AppLayout>
  );
}

function PlatformMetricGrid({ metrics }: { metrics?: PlatformAnalytics }) {
  return (
    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <MetricCard
        title="Learners"
        value={metrics?.learners ?? 0}
        icon={<Users className="h-5 w-5" />}
      />
      <MetricCard
        title="Teachers"
        value={metrics?.teachers ?? 0}
        icon={<GraduationCap className="h-5 w-5" />}
      />
      <MetricCard
        title="Published courses"
        value={metrics?.coursesPublished ?? 0}
        icon={<BookOpen className="h-5 w-5" />}
      />
      <MetricCard
        title="Active enrollments"
        value={metrics?.activeEnrollments ?? 0}
        icon={<Activity className="h-5 w-5" />}
      />
      <MetricCard
        title="Exam attempts"
        value={metrics?.totalExamAttempts ?? 0}
        icon={<CheckCircle2 className="h-5 w-5" />}
      />
      <MetricCard
        title="Exams passed"
        value={metrics?.totalExamsPassed ?? 0}
        icon={<GraduationCap className="h-5 w-5" />}
      />
      <MetricCard
        title="Certificates"
        value={metrics?.totalCertificates ?? 0}
        icon={<Award className="h-5 w-5" />}
      />
      <MetricCard
        title="Admins"
        value={metrics?.admins ?? 0}
        icon={<BarChart3 className="h-5 w-5" />}
      />
    </section>
  );
}

function MetricCard({
  title,
  value,
  icon,
}: {
  title: string;
  value: number;
  icon: React.ReactNode;
}) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-muted-foreground">{title}</p>
        <span className="text-primary">{icon}</span>
      </div>
      <p className="mt-3 text-display text-3xl text-foreground">{formatCount(value)}</p>
    </div>
  );
}

function Panel({
  title,
  detail,
  children,
}: {
  title: string;
  detail: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft">
      <h2 className="text-lg font-semibold text-foreground">{title}</h2>
      <p className="mt-1 text-sm text-muted-foreground">{detail}</p>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function FunnelRows({ funnel }: { funnel?: Funnel }) {
  const safe = funnel ?? { enrolled: 0, started: 0, completed: 0, passed: 0, certified: 0 };
  const rows = [
    ["Enrolled", safe.enrolled],
    ["Started lessons", safe.started],
    ["Completed course", safe.completed],
    ["Passed exam", safe.passed],
    ["Certified", safe.certified],
  ] as const;

  return (
    <div className="space-y-3">
      {rows.map(([label, value]) => (
        <div key={label}>
          <div className="flex items-center justify-between gap-4 text-sm">
            <p className="text-muted-foreground">{label}</p>
            <p className="font-medium text-foreground">{formatCount(value)}</p>
          </div>
          <div className="mt-2 h-2 rounded-full bg-border">
            <div
              className="h-full rounded-full bg-primary"
              style={{ width: `${barPercent(value, safe.enrolled)}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

function MonthRows({ rows, empty }: { rows: MonthCount[]; empty: string }) {
  if (rows.length === 0) {
    return (
      <p className="rounded-2xl border border-border bg-background px-4 py-5 text-sm text-muted-foreground">
        {empty}
      </p>
    );
  }

  const max = Math.max(...rows.map((row) => row.count), 1);
  return (
    <div className="space-y-3">
      {rows.map((row) => (
        <div key={row.month}>
          <div className="flex items-center justify-between gap-4 text-sm">
            <p className="text-muted-foreground">{row.month}</p>
            <p className="font-medium text-foreground">{formatCount(row.count)}</p>
          </div>
          <div className="mt-2 h-2 rounded-full bg-border">
            <div
              className="h-full rounded-full bg-primary"
              style={{ width: `${barPercent(row.count, max)}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-12 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
    </div>
  );
}

function ErrorCard({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="rounded-3xl border border-destructive/20 bg-destructive/5 px-6 py-8 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">Analytics unavailable</p>
      <p className="mt-2 text-sm text-muted-foreground">{message}</p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-4 rounded-full bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
      >
        Retry
      </button>
    </div>
  );
}

function formatCount(value: number) {
  return Math.max(0, value).toLocaleString();
}

function barPercent(value: number, total: number) {
  if (total <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((value / total) * 100)));
}
