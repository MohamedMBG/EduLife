import { createFileRoute } from "@tanstack/react-router";
import { useQueries, useQuery } from "@tanstack/react-query";
import {
  Activity,
  Award,
  BarChart3,
  BookOpen,
  CheckCircle2,
  GraduationCap,
  Layers3,
  Users,
} from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import {
  getGroupCohortAnalytics,
  getStudentAnalyticsSummary,
  getStudentProgressTrend,
  getTeacherAnalytics,
  getTeacherCohortAnalytics,
  listMyGroups,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";
import type {
  Funnel,
  GroupCohortAnalytics,
  MonthCount,
  StudentAnalyticsSummary,
  StudentProgressTrend,
  TeacherAnalytics,
  TeacherCohortAnalytics,
} from "../lib/api/types";

export const Route = createFileRoute("/analytics")({
  component: AnalyticsRoute,
  head: () => ({ meta: [{ title: "Analytics - EduLife" }] }),
});

function AnalyticsRoute() {
  return (
    <RequireAuth>
      <AnalyticsPage />
    </RequireAuth>
  );
}

function AnalyticsPage() {
  const auth = useAuth();
  const role = auth.session?.role;
  const isTeacher = role === "TEACHER";
  const isGroupAdmin = role === "GROUP_ADMIN";

  return (
    <AppShell
      active="analytics"
      user={{
        displayName: auth.session?.displayName || "EduLife user",
        email: auth.session?.email || "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">Analytics</p>
          <p className="text-xs text-muted-foreground">
            Server-scoped learning metrics from the EduLife backend.
          </p>
        </div>
      }
    >
      {isTeacher ? (
        <TeacherAnalyticsPanel />
      ) : isGroupAdmin ? (
        <GroupAnalyticsPanel />
      ) : (
        <StudentAnalyticsPanel />
      )}
    </AppShell>
  );
}

function StudentAnalyticsPanel() {
  const auth = useAuth();
  const summaryQuery = useQuery({
    queryKey: ["analytics", "student", "summary"],
    queryFn: () => getStudentAnalyticsSummary(auth.getAccessToken),
  });
  const trendQuery = useQuery({
    queryKey: ["analytics", "student", "trend"],
    queryFn: () => getStudentProgressTrend(auth.getAccessToken),
  });

  if (summaryQuery.isLoading || trendQuery.isLoading) {
    return <StatePanel title="Loading analytics..." detail="Fetching your own learning metrics." />;
  }

  if (summaryQuery.isError) {
    return <ErrorPanel message={summaryQuery.error.message} onRetry={() => summaryQuery.refetch()} />;
  }

  if (trendQuery.isError) {
    return <ErrorPanel message={trendQuery.error.message} onRetry={() => trendQuery.refetch()} />;
  }

  return (
    <DashboardFrame
      eyebrow="Learner analytics"
      title="My learning progress"
      detail="These counts are scoped by the backend to your authenticated user. No user id is sent by the web client."
    >
      <StudentSummaryGrid summary={summaryQuery.data} />
      <section className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <Panel title="Lesson trend" detail="Lessons completed by month.">
          <MonthRows rows={trendQuery.data?.lessonsByMonth ?? []} empty="No completed lessons yet." />
        </Panel>
        <Panel title="Outcome snapshot" detail="Exam and certificate progress from the secure backend.">
          <OutcomeSnapshot summary={summaryQuery.data} trend={trendQuery.data} />
        </Panel>
      </section>
    </DashboardFrame>
  );
}

function TeacherAnalyticsPanel() {
  const auth = useAuth();
  const coursesQuery = useQuery({
    queryKey: ["analytics", "teacher", "courses"],
    queryFn: () => getTeacherAnalytics(auth.getAccessToken),
  });
  const cohortsQuery = useQuery({
    queryKey: ["analytics", "teacher", "cohorts"],
    queryFn: () => getTeacherCohortAnalytics(auth.getAccessToken),
  });

  if (coursesQuery.isLoading || cohortsQuery.isLoading) {
    return <StatePanel title="Loading analytics..." detail="Fetching owned-course metrics." />;
  }

  if (coursesQuery.isError) {
    return <ErrorPanel message={coursesQuery.error.message} onRetry={() => coursesQuery.refetch()} />;
  }

  if (cohortsQuery.isError) {
    return <ErrorPanel message={cohortsQuery.error.message} onRetry={() => cohortsQuery.refetch()} />;
  }

  const courses = coursesQuery.data?.courses ?? [];

  return (
    <DashboardFrame
      eyebrow="Teacher analytics"
      title="Owned-course performance"
      detail="The backend filters every metric to courses authored by your account. The web app never sends a teacher id."
    >
      <TeacherSummaryGrid analytics={coursesQuery.data} cohorts={cohortsQuery.data} />
      <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <Panel title="Learner funnel" detail="Enrollment grains moving through the course journey.">
          <FunnelRows funnel={cohortsQuery.data?.funnel} />
        </Panel>
        <Panel title="Enrollment cohorts" detail="Active enrollments grouped by month.">
          <MonthRows rows={cohortsQuery.data?.enrollmentCohorts ?? []} empty="No enrollments yet." />
        </Panel>
      </section>
      <Panel title="Course performance" detail="One row per course returned by the backend.">
        {courses.length === 0 ? (
          <EmptyText>No owned courses yet.</EmptyText>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="text-xs uppercase tracking-[0.14em] text-muted-foreground">
                <tr>
                  <th className="py-3 pr-4 font-semibold">Course</th>
                  <th className="py-3 pr-4 font-semibold">Status</th>
                  <th className="py-3 pr-4 font-semibold">Enrollments</th>
                  <th className="py-3 pr-4 font-semibold">Completion</th>
                  <th className="py-3 pr-4 font-semibold">Pass rate</th>
                  <th className="py-3 pr-4 font-semibold">Certificates</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {courses.map((course) => (
                  <tr key={course.courseId}>
                    <td className="py-4 pr-4 font-medium text-foreground">{course.title}</td>
                    <td className="py-4 pr-4 text-muted-foreground">{course.status}</td>
                    <td className="py-4 pr-4 text-foreground">{formatCount(course.activeEnrollments)}</td>
                    <td className="py-4 pr-4 text-foreground">
                      {formatPercent(course.completionRatePercent)}
                    </td>
                    <td className="py-4 pr-4 text-foreground">{formatPercent(course.passRatePercent)}</td>
                    <td className="py-4 pr-4 text-foreground">{formatCount(course.certificatesIssued)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </DashboardFrame>
  );
}

function GroupAnalyticsPanel() {
  const auth = useAuth();
  const groupsQuery = useQuery({
    queryKey: ["groups", "analytics-list"],
    queryFn: () => listMyGroups(auth.getAccessToken),
  });

  const groupQueries = useQueries({
    queries: (groupsQuery.data ?? []).map((group) => ({
      queryKey: ["analytics", "group", group.id],
      // The backend re-checks ownership for every groupId; the web client does not decide scope.
      queryFn: () => getGroupCohortAnalytics(auth.getAccessToken, group.id),
      enabled: groupsQuery.isSuccess,
    })),
  });

  if (groupsQuery.isLoading || groupQueries.some((query) => query.isLoading)) {
    return <StatePanel title="Loading group analytics..." detail="Fetching your owned groups." />;
  }

  if (groupsQuery.isError) {
    return <ErrorPanel message={groupsQuery.error.message} onRetry={() => groupsQuery.refetch()} />;
  }

  const firstGroupError = groupQueries.find((query) => query.isError);
  if (firstGroupError?.error) {
    return (
      <ErrorPanel
        message={firstGroupError.error.message}
        onRetry={() => groupQueries.forEach((query) => query.refetch())}
      />
    );
  }

  const groups = groupQueries.map((query) => query.data).filter(Boolean) as GroupCohortAnalytics[];
  const totalMembers = groups.reduce((sum, group) => sum + group.memberCount, 0);
  const totalCourses = groups.reduce((sum, group) => sum + group.courseCount, 0);

  return (
    <DashboardFrame
      eyebrow="Group analytics"
      title="Cohort health"
      detail="Every group card is authorized by the backend against your group ownership."
    >
      <section className="grid gap-4 sm:grid-cols-3">
        <MetricCard title="Groups" value={groups.length} icon={<Users className="h-5 w-5" />} />
        <MetricCard title="Members" value={totalMembers} icon={<GraduationCap className="h-5 w-5" />} />
        <MetricCard title="Assigned courses" value={totalCourses} icon={<BookOpen className="h-5 w-5" />} />
      </section>
      {groups.length === 0 ? (
        <StatePanel title="No groups yet" detail="Create a group before analytics can be computed." />
      ) : (
        <section className="grid gap-5 lg:grid-cols-2">
          {groups.map((group) => (
            <Panel
              key={group.groupId}
              title={group.groupName}
              detail={`${group.memberCount} members · ${group.courseCount} assigned courses`}
            >
              <FunnelRows funnel={group.funnel} />
            </Panel>
          ))}
        </section>
      )}
    </DashboardFrame>
  );
}

function DashboardFrame({
  eyebrow,
  title,
  detail,
  children,
}: {
  eyebrow: string;
  title: string;
  detail: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mx-auto max-w-7xl space-y-7">
      <section className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft">
        <p className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.16em] text-primary">
          <BarChart3 className="h-4 w-4" />
          {eyebrow}
        </p>
        <h1 className="mt-3 text-display text-3xl text-foreground">{title}</h1>
        <p className="mt-2 max-w-3xl text-sm leading-relaxed text-muted-foreground">{detail}</p>
      </section>
      {children}
    </div>
  );
}

function StudentSummaryGrid({ summary }: { summary?: StudentAnalyticsSummary }) {
  return (
    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
      <MetricCard title="Active courses" value={summary?.activeEnrollments ?? 0} icon={<BookOpen className="h-5 w-5" />} />
      <MetricCard title="Lessons done" value={summary?.lessonsCompleted ?? 0} icon={<CheckCircle2 className="h-5 w-5" />} />
      <MetricCard title="Exam attempts" value={summary?.examAttempts ?? 0} icon={<Activity className="h-5 w-5" />} />
      <MetricCard title="Passed exams" value={summary?.examsPassed ?? 0} icon={<GraduationCap className="h-5 w-5" />} />
      <MetricCard title="Certificates" value={summary?.certificatesEarned ?? 0} icon={<Award className="h-5 w-5" />} />
    </section>
  );
}

function TeacherSummaryGrid({
  analytics,
  cohorts,
}: {
  analytics?: TeacherAnalytics;
  cohorts?: TeacherCohortAnalytics;
}) {
  return (
    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <MetricCard title="Courses" value={analytics?.totalCourses ?? 0} icon={<Layers3 className="h-5 w-5" />} />
      <MetricCard title="Enrolled" value={cohorts?.funnel.enrolled ?? 0} icon={<Users className="h-5 w-5" />} />
      <MetricCard title="Completed" value={cohorts?.funnel.completed ?? 0} icon={<CheckCircle2 className="h-5 w-5" />} />
      <MetricCard title="Certified" value={cohorts?.funnel.certified ?? 0} icon={<Award className="h-5 w-5" />} />
    </section>
  );
}

function OutcomeSnapshot({
  summary,
  trend,
}: {
  summary?: StudentAnalyticsSummary;
  trend?: StudentProgressTrend;
}) {
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <MiniStat label="Lessons total" value={trend?.totalLessons ?? 0} />
      <MiniStat label="Pass rate" value={summary?.examAttempts ? formatPercent(((summary.examsPassed ?? 0) / summary.examAttempts) * 100) : "0.0%"} />
      <MiniStat label="Certificates" value={summary?.certificatesEarned ?? 0} />
    </div>
  );
}

function MetricCard({ title, value, icon }: { title: string; value: number; icon: React.ReactNode }) {
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
  if (rows.length === 0) return <EmptyText>{empty}</EmptyText>;

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

function MiniStat({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-2xl border border-border bg-background p-4">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-2 text-display text-2xl text-foreground">
        {typeof value === "number" ? formatCount(value) : value}
      </p>
    </div>
  );
}

function StatePanel({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="mx-auto max-w-3xl rounded-3xl border border-border bg-surface-elevated px-6 py-10 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
    </div>
  );
}

function ErrorPanel({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="mx-auto max-w-3xl rounded-3xl border border-destructive/20 bg-destructive/5 px-6 py-8 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">Analytics unavailable</p>
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

function EmptyText({ children }: { children: React.ReactNode }) {
  return <p className="rounded-2xl border border-border bg-background px-4 py-5 text-sm text-muted-foreground">{children}</p>;
}

function formatCount(value: number) {
  return Math.max(0, value).toLocaleString();
}

function formatPercent(value: number) {
  return `${Math.max(0, value).toFixed(1)}%`;
}

function barPercent(value: number, total: number) {
  if (total <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((value / total) * 100)));
}
