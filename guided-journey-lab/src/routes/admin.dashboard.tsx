import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import {
  Activity,
  AlertCircle,
  ArrowRight,
  Award,
  BookOpen,
  CheckCircle2,
  Clock3,
  FileText,
  GraduationCap,
  Layers3,
  ShieldCheck,
  Users,
} from "lucide-react";
import { AdminShell } from "../components/app/AdminShell";
import { getAdminMetrics, listAdminTeacherRequests } from "../lib/api/client";
import type { AdminMetrics, TeacherRequestSummary } from "../lib/api/types";
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

  const pendingRequestsQuery = useQuery({
    queryKey: ["admin", "teacher-requests", "PENDING", "dashboard"],
    queryFn: () => listAdminTeacherRequests(auth.getAccessToken, "PENDING", 0, 5),
  });

  const approvedRequestsQuery = useQuery({
    queryKey: ["admin", "teacher-requests", "APPROVED", "dashboard"],
    queryFn: () => listAdminTeacherRequests(auth.getAccessToken, "APPROVED", 0, 1),
  });

  const rejectedRequestsQuery = useQuery({
    queryKey: ["admin", "teacher-requests", "REJECTED", "dashboard"],
    queryFn: () => listAdminTeacherRequests(auth.getAccessToken, "REJECTED", 0, 1),
  });

  const metrics = metricsQuery.data;
  const insights = metrics ? buildInsights(metrics) : null;
  const firstName =
    auth.session?.displayName?.split(" ").filter(Boolean)[0] || "Admin";
  const pendingRequests = pendingRequestsQuery.data?.content ?? [];

  return (
    <AdminShell active="dashboard">
      {metricsQuery.isLoading ? (
        <LoadingCard />
      ) : metricsQuery.isError ? (
        <ErrorCard message={metricsQuery.error.message} onRetry={() => metricsQuery.refetch()} />
      ) : metrics && insights ? (
        <div className="space-y-8">
          <section className="overflow-hidden rounded-[2rem] border border-[oklch(0.44_0.16_250)] bg-[oklch(0.32_0.14_250)] text-white shadow-elevated">
            <div className="grid gap-6 px-6 py-8 lg:grid-cols-[1.3fr_0.9fr] lg:px-8">
              <div>
                <p className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs uppercase tracking-[0.16em]">
                  <ShieldCheck className="h-3.5 w-3.5" />
                  Platform admin
                </p>
                <h1 className="mt-4 text-display text-4xl">Welcome back, {firstName}</h1>
                <p className="mt-3 max-w-2xl text-sm leading-relaxed text-white/72">
                  This console summarizes users, course publishing, enrollments, certificates, and
                  teacher access requests from the live backend admin endpoints.
                </p>
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <HeroMiniStat label="Total users" value={insights.totalUsers} />
                <HeroMiniStat label="Course records" value={insights.totalCourses} />
                <HeroMiniStat label="Publish rate" value={`${insights.publishRate}%`} />
                <HeroMiniStat label="Review queue" value={metrics.pendingTeacherRequests} />
              </div>
            </div>
          </section>

          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              title="Learners"
              value={metrics.totalLearners}
              detail={`${insights.learnerShare}% of platform users`}
              icon={<Users className="h-5 w-5 text-[oklch(0.40_0.19_250)]" />}
            />
            <StatCard
              title="Teachers"
              value={metrics.totalTeachers}
              detail={`${insights.teacherShare}% of users can teach`}
              icon={<GraduationCap className="h-5 w-5 text-teal-600" />}
            />
            <StatCard
              title="Active enrollments"
              value={metrics.totalEnrollmentsActive}
              detail={`${insights.enrollmentsPerLearner} per learner`}
              icon={<Activity className="h-5 w-5 text-teal-600" />}
            />
            <StatCard
              title="Certificates"
              value={metrics.totalCertificates}
              detail={`${insights.certificatesPerEnrollment}% of active enrollments`}
              icon={<Award className="h-5 w-5 text-amber-500" />}
            />
          </section>

          <section className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
            <Panel
              eyebrow="Course operations"
              title="Publishing pipeline"
              detail="Draft, published, and archived course counts from the platform course table."
              action={
                <Link
                  to="/explore"
                  className="inline-flex items-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-xs font-semibold text-foreground"
                >
                  View catalog
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              }
            >
              <div className="grid gap-4 md:grid-cols-3">
                <PipelineCard
                  label="Draft"
                  value={metrics.totalCoursesDraft}
                  detail="Needs content or approval"
                  icon={<FileText className="h-4 w-4" />}
                />
                <PipelineCard
                  label="Published"
                  value={metrics.totalCoursesPublished}
                  detail="Visible to learners"
                  icon={<Layers3 className="h-4 w-4" />}
                />
                <PipelineCard
                  label="Archived"
                  value={metrics.totalCoursesArchived}
                  detail="Hidden from catalog"
                  icon={<BookOpen className="h-4 w-4" />}
                />
              </div>
              <ProgressBar
                label="Published share"
                value={insights.publishRate}
                helper={`${metrics.totalCoursesPublished} of ${insights.totalCourses} courses are published`}
              />
            </Panel>

            <Panel
              eyebrow="Access control"
              title="Teacher review queue"
              detail="Pending teacher applications are the highest-priority admin action."
              action={
                <Link
                  to="/admin/teacher-requests"
                  className="inline-flex items-center gap-2 rounded-full bg-[oklch(0.40_0.19_250)] px-4 py-2 text-xs font-semibold text-white"
                >
                  Review requests
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              }
            >
              <div className="grid gap-3 sm:grid-cols-3">
                <QueueStat label="Pending" value={metrics.pendingTeacherRequests} tone="pending" />
                <QueueStat
                  label="Approved"
                  value={approvedRequestsQuery.data?.totalElements ?? 0}
                  tone="good"
                />
                <QueueStat
                  label="Rejected"
                  value={rejectedRequestsQuery.data?.totalElements ?? 0}
                  tone="bad"
                />
              </div>

              {pendingRequestsQuery.isLoading ? (
                <MiniState title="Loading queue..." />
              ) : pendingRequestsQuery.isError ? (
                <MiniState title={pendingRequestsQuery.error.message} />
              ) : pendingRequests.length === 0 ? (
                <MiniState title="No pending teacher applications right now." />
              ) : (
                <div className="mt-4 space-y-3">
                  {pendingRequests.map((request) => (
                    <PendingRequestRow key={request.id} request={request} />
                  ))}
                </div>
              )}
            </Panel>
          </section>

          <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
            <Panel
              eyebrow="Learner outcomes"
              title="Learning conversion"
              detail="A quick read on whether learners are moving from enrollment to certificate."
            >
              <div className="space-y-4">
                <ProgressBar
                  label="Certificates per enrollment"
                  value={insights.certificatesPerEnrollment}
                  helper={`${metrics.totalCertificates} certificates from ${metrics.totalEnrollmentsActive} active enrollments`}
                />
                <ProgressBar
                  label="Enrollments per published course"
                  value={Math.min(100, parseFloat(insights.enrollmentsPerPublishedCourse) * 10)}
                  helper={`${insights.enrollmentsPerPublishedCourse} active enrollments per published course`}
                />
              </div>
            </Panel>

            <Panel
              eyebrow="Admin notes"
              title="What needs attention"
              detail="Derived from the current metrics. No extra backend endpoint required."
            >
              <div className="grid gap-3 md:grid-cols-2">
                {buildActionItems(metrics, insights).map((item) => (
                  <ActionItem key={item.title} {...item} />
                ))}
              </div>
            </Panel>
          </section>
        </div>
      ) : null}
    </AdminShell>
  );
}

function buildInsights(metrics: AdminMetrics) {
  const totalUsers = metrics.totalLearners + metrics.totalTeachers + metrics.totalGroupAdmins;
  const totalCourses =
    metrics.totalCoursesDraft + metrics.totalCoursesPublished + metrics.totalCoursesArchived;

  return {
    totalUsers,
    totalCourses,
    learnerShare: percent(metrics.totalLearners, totalUsers),
    teacherShare: percent(metrics.totalTeachers, totalUsers),
    publishRate: percent(metrics.totalCoursesPublished, totalCourses),
    enrollmentsPerLearner: ratio(metrics.totalEnrollmentsActive, metrics.totalLearners),
    enrollmentsPerPublishedCourse: ratio(
      metrics.totalEnrollmentsActive,
      metrics.totalCoursesPublished,
    ),
    certificatesPerEnrollment: percent(metrics.totalCertificates, metrics.totalEnrollmentsActive),
  };
}

function buildActionItems(metrics: AdminMetrics, insights: ReturnType<typeof buildInsights>) {
  return [
    {
      title:
        metrics.pendingTeacherRequests > 0
          ? "Teacher approvals waiting"
          : "Teacher approvals clear",
      detail:
        metrics.pendingTeacherRequests > 0
          ? `${metrics.pendingTeacherRequests} request(s) need review before teachers can publish content.`
          : "No pending teacher applications right now.",
      icon:
        metrics.pendingTeacherRequests > 0 ? (
          <AlertCircle className="h-4 w-4" />
        ) : (
          <CheckCircle2 className="h-4 w-4" />
        ),
      tone: metrics.pendingTeacherRequests > 0 ? "warning" : "good",
    },
    {
      title: "Course publishing health",
      detail:
        insights.publishRate >= 50
          ? `${insights.publishRate}% of courses are published.`
          : "Most courses are still draft or archived. Check content approval flow.",
      icon: <Layers3 className="h-4 w-4" />,
      tone: insights.publishRate >= 50 ? "good" : "warning",
    },
    {
      title: "Learner activity",
      detail:
        metrics.totalEnrollmentsActive > 0
          ? `${metrics.totalEnrollmentsActive} active enrollments across ${metrics.totalLearners} learners.`
          : "No active enrollments yet. Course discovery may need attention.",
      icon: <Activity className="h-4 w-4" />,
      tone: metrics.totalEnrollmentsActive > 0 ? "good" : "warning",
    },
    {
      title: "Certificate proof",
      detail:
        metrics.totalCertificates > 0
          ? `${metrics.totalCertificates} certificates issued after successful exams.`
          : "No certificates issued yet. Learners may not have reached exams.",
      icon: <Award className="h-4 w-4" />,
      tone: metrics.totalCertificates > 0 ? "good" : "neutral",
    },
  ] as const;
}

function percent(value: number, total: number) {
  if (total <= 0) return 0;
  return Math.round((value / total) * 100);
}

function ratio(value: number, total: number) {
  if (total <= 0) return "0.0";
  return (value / total).toFixed(1);
}

function HeroMiniStat({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-2xl border border-white/15 bg-white/10 p-4">
      <p className="text-xs text-white/65">{label}</p>
      <p className="mt-2 text-display text-3xl text-white">
        {typeof value === "number" ? value.toLocaleString() : value}
      </p>
    </div>
  );
}

function StatCard({
  title,
  value,
  detail,
  icon,
}: {
  title: string;
  value: number;
  detail: string;
  icon: ReactNode;
}) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{title}</p>
        {icon}
      </div>
      <p className="mt-3 text-display text-3xl text-foreground">{value.toLocaleString()}</p>
      <p className="mt-1 text-xs text-muted-foreground">{detail}</p>
    </div>
  );
}

function Panel({
  eyebrow,
  title,
  detail,
  action,
  children,
}: {
  eyebrow: string;
  title: string;
  detail: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-muted-foreground">
            {eyebrow}
          </p>
          <h2 className="mt-2 text-xl font-semibold text-foreground">{title}</h2>
          <p className="mt-1 max-w-2xl text-sm leading-relaxed text-muted-foreground">{detail}</p>
        </div>
        {action}
      </div>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function PipelineCard({
  label,
  value,
  detail,
  icon,
}: {
  label: string;
  value: number;
  detail: string;
  icon: ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-border bg-background p-4">
      <div className="flex items-center justify-between text-muted-foreground">
        <p className="text-sm">{label}</p>
        {icon}
      </div>
      <p className="mt-3 text-display text-3xl text-foreground">{value.toLocaleString()}</p>
      <p className="mt-1 text-xs text-muted-foreground">{detail}</p>
    </div>
  );
}

function ProgressBar({ label, value, helper }: { label: string; value: number; helper: string }) {
  const safeValue = Math.max(0, Math.min(100, value));

  return (
    <div>
      <div className="flex items-center justify-between gap-3 text-sm">
        <p className="font-medium text-foreground">{label}</p>
        <p className="text-muted-foreground">{safeValue}%</p>
      </div>
      <div className="mt-2 h-2 rounded-full bg-border">
        <div
          className="h-full rounded-full bg-[oklch(0.40_0.19_250)]"
          style={{ width: `${safeValue}%` }}
        />
      </div>
      <p className="mt-2 text-xs text-muted-foreground">{helper}</p>
    </div>
  );
}

function QueueStat({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: "pending" | "good" | "bad";
}) {
  const toneClass =
    tone === "good"
      ? "bg-teal-500/10 text-teal-700"
      : tone === "bad"
        ? "bg-destructive/10 text-destructive"
        : "bg-[oklch(0.92_0.03_250)] text-[oklch(0.40_0.19_250)]";

  return (
    <div className={`rounded-2xl px-4 py-3 ${toneClass}`}>
      <p className="text-xs font-medium">{label}</p>
      <p className="mt-1 text-display text-2xl">{value.toLocaleString()}</p>
    </div>
  );
}

function PendingRequestRow({ request }: { request: TeacherRequestSummary }) {
  return (
    <div className="rounded-2xl border border-border bg-background px-4 py-3">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-foreground">{request.userEmail}</p>
          <p className="mt-1 text-xs text-muted-foreground">
            Requested {formatDate(request.requestedAt)}
          </p>
        </div>
        <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-[oklch(0.92_0.03_250)] px-2.5 py-1 text-xs font-bold uppercase tracking-wide text-[oklch(0.40_0.19_250)]">
          <Clock3 className="h-3 w-3" />
          Pending
        </span>
      </div>
      {request.motivation && (
        <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
          {request.motivation}
        </p>
      )}
    </div>
  );
}

function ActionItem({
  title,
  detail,
  icon,
  tone,
}: {
  title: string;
  detail: string;
  icon: ReactNode;
  tone: "good" | "warning" | "neutral";
}) {
  const toneClass =
    tone === "good"
      ? "bg-teal-500/10 text-teal-700"
      : tone === "warning"
        ? "bg-amber-500/10 text-amber-700"
        : "bg-muted text-muted-foreground";

  return (
    <div className="rounded-2xl border border-border bg-background p-4">
      <span className={`inline-flex h-9 w-9 items-center justify-center rounded-xl ${toneClass}`}>
        {icon}
      </span>
      <p className="mt-3 text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-1 text-sm leading-relaxed text-muted-foreground">{detail}</p>
    </div>
  );
}

function MiniState({ title }: { title: string }) {
  return (
    <div className="mt-4 rounded-2xl border border-border bg-background px-4 py-5 text-sm text-muted-foreground">
      {title}
    </div>
  );
}

function LoadingCard() {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-12 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">Loading admin metrics...</p>
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

function formatDate(value: string | null) {
  if (!value) return "unknown date";
  return value.substring(0, 10);
}
