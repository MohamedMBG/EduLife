import { createFileRoute, Link } from "@tanstack/react-router";
import { useQueries, useQuery } from "@tanstack/react-query";
import {
  Activity,
  Award,
  BarChart3,
  BookOpen,
  CheckCircle2,
  Clock,
  Download,
  FileText,
  GraduationCap,
  Layers3,
  Monitor,
  MoreVertical,
  PlayCircle,
  Radio,
  Share2,
  ShieldCheck,
  Users,
  type LucideIcon,
} from "lucide-react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AppLayout } from "../components/app/AppLayout";
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
  head: () => ({ meta: [{ title: "Academic Command Center — EduLife" }] }),
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
    <AppLayout>
      {isTeacher ? (
        <TeacherAnalyticsPanel />
      ) : isGroupAdmin ? (
        <GroupAnalyticsPanel />
      ) : (
        <StudentAnalyticsPanel />
      )}
    </AppLayout>
  );
}

/* ================================================================
   STUDENT — Academic Command Center
   ================================================================ */

function StudentAnalyticsPanel() {
  const auth = useAuth();
  const summaryQ = useQuery({
    queryKey: ["analytics", "student", "summary"],
    queryFn: () => getStudentAnalyticsSummary(auth.getAccessToken),
  });
  const trendQ = useQuery({
    queryKey: ["analytics", "student", "trend"],
    queryFn: () => getStudentProgressTrend(auth.getAccessToken),
  });

  if (summaryQ.isLoading || trendQ.isLoading) return <CommandCenterSkeleton />;

  if (summaryQ.isError) {
    return (
      <CommandCenterError message={summaryQ.error.message} onRetry={() => summaryQ.refetch()} />
    );
  }
  if (trendQ.isError) {
    return <CommandCenterError message={trendQ.error.message} onRetry={() => trendQ.refetch()} />;
  }

  const summary = summaryQ.data;
  const trend = trendQ.data;

  return (
    <div className="space-y-8">
      <CommandCenterHeader lastUpdatedAt={summaryQ.dataUpdatedAt} />
      <KpiCardsRow summary={summary} trend={trend} />
      <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <SkillGrowthPanel trend={trend} />
        <CareerAlignmentPanel summary={summary} trend={trend} />
      </div>
      <MilestonesSection summary={summary} />
      <CommandCenterFooter />
    </div>
  );
}

/* ---------- Header ---------- */

function CommandCenterHeader({ lastUpdatedAt }: { lastUpdatedAt?: number }) {
  const time = lastUpdatedAt
    ? new Date(lastUpdatedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    : new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

  return (
    <section className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <div className="flex flex-wrap items-baseline gap-3">
          <h1 className="text-3xl font-light tracking-tight text-foreground sm:text-4xl">
            Academic Command Center
          </h1>
          <span className="rounded-md border border-border/60 bg-muted/60 px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">
            v2.4.0
          </span>
        </div>
        <p className="mt-2 text-sm text-muted-foreground">
          Real-time performance analytics &amp; career projection.
        </p>
        <p className="mt-1.5 flex items-center gap-1.5 text-xs text-muted-foreground/70">
          <Clock className="h-3.5 w-3.5" strokeWidth={1.5} />
          Last Updated: Today, {time}
        </p>
      </div>
      <button
        type="button"
        disabled
        className="inline-flex shrink-0 items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-xs font-bold uppercase tracking-[0.08em] text-primary-foreground shadow-soft transition-opacity disabled:cursor-not-allowed disabled:opacity-80"
        aria-label="Download full report (coming soon)"
        title="Coming soon"
      >
        <Download className="h-4 w-4" strokeWidth={1.75} />
        Download Full Report
      </button>
    </section>
  );
}

/* ---------- KPI Cards ---------- */

interface KpiDef {
  icon: LucideIcon;
  label: string;
  value: number | string;
  helper: string;
  trend: { label: string; positive: boolean | null } | null;
}

function KpiCardsRow({
  summary,
  trend,
}: {
  summary?: StudentAnalyticsSummary;
  trend?: StudentProgressTrend;
}) {
  const s = summary ?? {
    activeEnrollments: 0,
    lessonsCompleted: 0,
    examAttempts: 0,
    examsPassed: 0,
    certificatesEarned: 0,
  };
  const months = trend?.lessonsByMonth ?? [];

  let lessonTrend: KpiDef["trend"] = null;
  if (months.length >= 2) {
    const last = months[months.length - 1].count;
    const prev = months[months.length - 2].count;
    if (prev > 0) {
      const pct = Math.round(((last - prev) / prev) * 100);
      lessonTrend =
        pct > 0
          ? { label: `${pct}%`, positive: true }
          : pct < 0
            ? { label: `${Math.abs(pct)}%`, positive: false }
            : { label: "0%", positive: null };
    } else if (last > 0) {
      lessonTrend = { label: `+${last}`, positive: true };
    }
  }

  const successRate = s.examAttempts > 0 ? Math.round((s.examsPassed / s.examAttempts) * 100) : 0;

  const kpis: KpiDef[] = [
    {
      icon: Monitor,
      label: "ACTIVE COURSES",
      value: s.activeEnrollments,
      helper: "In progress",
      trend: s.activeEnrollments > 0 ? { label: `+${s.activeEnrollments}`, positive: true } : null,
    },
    {
      icon: CheckCircle2,
      label: "LESSONS DONE",
      value: s.lessonsCompleted,
      helper: months.length > 0 ? "This month" : "Total",
      trend: lessonTrend,
    },
    {
      icon: Radio,
      label: "EXAM ATTEMPTS",
      value: s.examAttempts,
      helper: "Lifetime",
      trend: s.examAttempts > 0 ? { label: "0%", positive: null } : null,
    },
    {
      icon: ShieldCheck,
      label: "SUCCESS RATE",
      value: `${successRate}%`,
      helper: "Avg Score",
      trend:
        s.examAttempts > 0
          ? { label: `${successRate}%`, positive: successRate >= 50 ? true : false }
          : null,
    },
    {
      icon: FileText,
      label: "CERTIFICATES",
      value: s.certificatesEarned,
      helper: "Verified",
      trend: s.certificatesEarned > 0 ? { label: "NEW", positive: true } : null,
    },
  ];

  return (
    <section
      className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5"
      aria-label="Key performance indicators"
    >
      {kpis.map((k) => (
        <KpiCard key={k.label} {...k} />
      ))}
    </section>
  );
}

function KpiCard({ icon: Icon, label, value, helper, trend }: KpiDef) {
  return (
    <div className="rounded-xl border border-border/60 bg-surface-elevated p-5 shadow-soft">
      <div className="flex items-center justify-between">
        <Icon className="h-5 w-5 text-muted-foreground/70" strokeWidth={1.5} />
        {trend && (
          <span
            className={
              trend.positive === true
                ? "text-[11px] font-semibold text-emerald-600"
                : trend.positive === false
                  ? "text-[11px] font-semibold text-red-500"
                  : "text-[11px] font-semibold text-muted-foreground"
            }
          >
            {trend.positive === true && "↗"}
            {trend.positive === false && "↘"}
            {trend.positive === null && "→"}
            {trend.label}
          </span>
        )}
      </div>
      <p className="mt-4 text-[10px] font-bold uppercase tracking-[0.12em] text-primary">{label}</p>
      <p className="mt-1 text-2xl font-light text-foreground sm:text-3xl">
        {typeof value === "number" ? formatCount(value) : value}
      </p>
      <p className="mt-0.5 text-xs text-muted-foreground">{helper}</p>
    </div>
  );
}

/* ---------- Skill Growth & Projection ---------- */

function SkillGrowthPanel({ trend }: { trend?: StudentProgressTrend }) {
  const months = trend?.lessonsByMonth ?? [];
  const avgCount =
    months.length > 0 ? months.reduce((sum, m) => sum + m.count, 0) / months.length : 0;

  const chartData = months.map((m, i) => ({
    month: formatMonthShort(m.month),
    actual: m.count,
    target: Math.round(avgCount * 0.4 + avgCount * 1.2 * ((i + 1) / months.length)),
  }));

  return (
    <section
      className="rounded-xl border border-border/60 bg-surface-elevated p-6 shadow-soft"
      aria-label="Skill growth chart"
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-foreground">Skill Growth &amp; Projection</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Comparative progress against curriculum benchmarks.
          </p>
        </div>
        <div className="flex items-center gap-5 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <span className="inline-block h-2 w-2 rounded-full bg-foreground" />
            Actual
          </span>
          <span className="flex items-center gap-1.5">
            <span className="inline-block h-2 w-2 rounded-full border border-muted-foreground" />
            Target
          </span>
        </div>
      </div>

      <div className="mt-6 h-[280px]">
        {chartData.length === 0 ? (
          <div className="flex h-full items-center justify-center rounded-lg border border-dashed border-border bg-muted/20">
            <p className="text-sm text-muted-foreground">Complete lessons to see growth trends.</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 8, right: 12, bottom: 0, left: -12 }}>
              <CartesianGrid
                strokeDasharray="4 4"
                stroke="var(--border)"
                strokeOpacity={0.4}
                vertical={false}
              />
              <XAxis
                dataKey="month"
                tick={{ fontSize: 11, fill: "var(--muted-foreground)" }}
                tickLine={false}
                axisLine={false}
              />
              <YAxis
                tick={{ fontSize: 11, fill: "var(--muted-foreground)" }}
                tickLine={false}
                axisLine={false}
                allowDecimals={false}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--surface-elevated)",
                  border: "1px solid var(--border)",
                  borderRadius: "8px",
                  fontSize: "12px",
                }}
                labelStyle={{ color: "var(--foreground)", fontWeight: 600 }}
              />
              <Line
                type="monotone"
                dataKey="target"
                stroke="var(--border)"
                strokeDasharray="6 4"
                strokeWidth={1.5}
                dot={false}
                name="Target"
              />
              <Line
                type="monotone"
                dataKey="actual"
                stroke="var(--foreground)"
                strokeWidth={2}
                dot={{
                  r: 4,
                  fill: "var(--foreground)",
                  stroke: "var(--surface-elevated)",
                  strokeWidth: 2,
                }}
                activeDot={{
                  r: 6,
                  fill: "var(--foreground)",
                  stroke: "var(--surface-elevated)",
                  strokeWidth: 3,
                }}
                name="Actual"
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </section>
  );
}

/* ---------- Career Alignment ---------- */

function CareerAlignmentPanel({
  summary,
  trend,
}: {
  summary?: StudentAnalyticsSummary;
  trend?: StudentProgressTrend;
}) {
  const radarValues = computeRadarValues(summary, trend);
  const overallScore = Math.round(
    (radarValues.reduce((a, b) => a + b, 0) / radarValues.length) * 100,
  );

  const totalLessons = trend?.totalLessons ?? 1;
  const lessonRatio = (summary?.lessonsCompleted ?? 0) / Math.max(totalLessons, 1);
  const examRatio =
    (summary?.examAttempts ?? 0) > 0 ? (summary?.examsPassed ?? 0) / summary!.examAttempts : 0;
  const courseRatio = Math.min(1, (summary?.activeEnrollments ?? 0) / 5);

  const skills = [
    { label: "Technical Proficiency", level: skillLevel(lessonRatio) },
    { label: "Problem Solving", level: skillLevel(examRatio) },
    { label: "System Design", level: skillLevel(courseRatio) },
  ];

  const note =
    overallScore >= 70
      ? "Strong alignment with your learning goals. Consider exploring advanced topics to reach full proficiency."
      : overallScore >= 40
        ? "Good momentum. Focus on completing active courses and attempting exams to strengthen your profile."
        : "You're getting started. Complete more lessons and attempt exams to build your academic profile.";

  return (
    <section className="flex flex-col rounded-xl border border-border/60 bg-surface-elevated p-6 shadow-soft">
      <div>
        <h2 className="text-lg font-semibold text-foreground">Career Alignment</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Profile mapping to:{" "}
          <span className="font-semibold text-foreground">Your Learning Path</span>
        </p>
      </div>

      <div className="my-5 flex justify-center">
        <RadarChart values={radarValues} score={overallScore} />
      </div>

      <div className="space-y-3 border-t border-border/50 pt-4">
        {skills.map((s) => (
          <div key={s.label} className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">{s.label}</span>
            <span className="font-semibold text-foreground">{s.level}</span>
          </div>
        ))}
      </div>

      <div className="mt-4 rounded-lg bg-muted/40 px-4 py-3">
        <p className="text-xs leading-relaxed text-muted-foreground">
          <span className="font-semibold italic text-foreground">Advisor&apos;s Note: </span>
          {note}
        </p>
      </div>

      <Link
        to="/advisor"
        className="mt-4 block w-full rounded-lg border border-border/70 py-2.5 text-center text-[11px] font-bold uppercase tracking-[0.12em] text-foreground transition-colors hover:bg-muted/60"
      >
        View Career Roadmap
      </Link>
    </section>
  );
}

function RadarChart({ values, score }: { values: number[]; score: number }) {
  const size = 180;
  const cx = size / 2;
  const cy = size / 2;
  const maxR = 65;
  const n = 5;
  const step = (2 * Math.PI) / n;
  const off = -Math.PI / 2;

  function vtx(i: number, r: number): string {
    const a = off + i * step;
    return `${cx + r * Math.cos(a)},${cy + r * Math.sin(a)}`;
  }

  function ring(r: number): string {
    return Array.from({ length: n }, (_, i) => vtx(i, r)).join(" ");
  }

  const dataPoints = values.map((v, i) => vtx(i, maxR * Math.max(v, 0.06)));

  return (
    <svg
      viewBox={`0 0 ${size} ${size}`}
      className="w-full max-w-[180px]"
      role="img"
      aria-label={`Career alignment score: ${score}%`}
    >
      {[0.25, 0.5, 0.75, 1].map((l) => (
        <polygon
          key={l}
          points={ring(maxR * l)}
          fill="none"
          stroke="var(--border)"
          strokeWidth={0.6}
          strokeOpacity={0.35}
        />
      ))}
      {Array.from({ length: n }, (_, i) => {
        const a = off + i * step;
        return (
          <line
            key={i}
            x1={cx}
            y1={cy}
            x2={cx + maxR * Math.cos(a)}
            y2={cy + maxR * Math.sin(a)}
            stroke="var(--border)"
            strokeWidth={0.6}
            strokeOpacity={0.35}
          />
        );
      })}
      <polygon
        points={dataPoints.join(" ")}
        fill="var(--muted)"
        fillOpacity={0.55}
        stroke="var(--foreground)"
        strokeWidth={1.2}
        strokeOpacity={0.6}
      />
      <text
        x={cx}
        y={cy}
        textAnchor="middle"
        dominantBaseline="central"
        fill="var(--foreground)"
        style={{ fontSize: 26, fontWeight: 300, fontFamily: "Montserrat, sans-serif" }}
      >
        {score}%
      </text>
    </svg>
  );
}

/* ---------- Academic Milestones ---------- */

function MilestonesSection({ summary }: { summary?: StudentAnalyticsSummary }) {
  const s = summary ?? {
    activeEnrollments: 0,
    lessonsCompleted: 0,
    examAttempts: 0,
    examsPassed: 0,
    certificatesEarned: 0,
  };
  const hasActivity = s.lessonsCompleted > 0 || s.examsPassed > 0 || s.certificatesEarned > 0;

  return (
    <section aria-label="Academic milestones">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-foreground">Academic Milestones</h2>
        <span className="text-sm font-medium text-muted-foreground">View History</span>
      </div>
      <div className="mt-2 h-px bg-border/50" />

      {!hasActivity ? (
        <div className="mt-6 rounded-xl border border-dashed border-border bg-muted/15 px-6 py-12 text-center">
          <GraduationCap className="mx-auto h-8 w-8 text-muted-foreground/40" strokeWidth={1.25} />
          <p className="mt-3 text-sm font-medium text-muted-foreground">No milestones yet</p>
          <p className="mt-1 text-xs text-muted-foreground/70">
            Complete lessons, pass exams, and earn certificates to build your academic timeline.
          </p>
        </div>
      ) : (
        <div className="mt-4 space-y-3">
          {s.certificatesEarned > 0 && (
            <MilestoneRow
              type="CERTIFICATION"
              title={`${s.certificatesEarned} Certificate${s.certificatesEarned > 1 ? "s" : ""} Earned`}
              description="Identity verified and issued by EduLife."
              actionIcon={Award}
            />
          )}
          {s.examsPassed > 0 && (
            <MilestoneRow
              type="EXAM"
              title={`${s.examsPassed} Exam${s.examsPassed > 1 ? "s" : ""} Passed`}
              description={`Completed with ${s.examAttempts} total attempt${s.examAttempts > 1 ? "s" : ""}.`}
              actionIcon={Share2}
            />
          )}
          {s.lessonsCompleted > 0 && (
            <MilestoneRow
              type="MODULE"
              title={`${s.lessonsCompleted} Lesson${s.lessonsCompleted > 1 ? "s" : ""} Completed`}
              description={`Across ${s.activeEnrollments} active enrollment${s.activeEnrollments !== 1 ? "s" : ""}.`}
              actionIcon={PlayCircle}
            />
          )}
        </div>
      )}
    </section>
  );
}

function MilestoneRow({
  type,
  title,
  description,
  actionIcon: ActionIcon,
}: {
  type: "EXAM" | "CERTIFICATION" | "MODULE";
  title: string;
  description: string;
  actionIcon: LucideIcon;
}) {
  const pill =
    type === "EXAM"
      ? "bg-accent/60 text-accent-foreground"
      : type === "CERTIFICATION"
        ? "bg-primary/8 text-primary"
        : "bg-muted text-muted-foreground";

  return (
    <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-surface-elevated px-5 py-4 shadow-soft">
      <span
        className={`shrink-0 rounded-md px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.08em] ${pill}`}
      >
        {type}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-foreground">{title}</p>
        <p className="truncate text-xs text-muted-foreground">{description}</p>
      </div>
      <div className="flex shrink-0 items-center gap-1">
        <button
          type="button"
          className="grid h-8 w-8 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          aria-label={`${type} action`}
        >
          <ActionIcon className="h-4 w-4" strokeWidth={1.5} />
        </button>
        <button
          type="button"
          className="grid h-8 w-8 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          aria-label="More options"
        >
          <MoreVertical className="h-4 w-4" strokeWidth={1.5} />
        </button>
      </div>
    </div>
  );
}

/* ---------- Loading / Error / Footer ---------- */

function CommandCenterSkeleton() {
  return (
    <div className="space-y-8">
      <div className="space-y-3">
        <div className="h-10 w-80 animate-pulse rounded-lg bg-muted" />
        <div className="h-4 w-64 animate-pulse rounded bg-muted" />
        <div className="h-3 w-40 animate-pulse rounded bg-muted" />
      </div>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {Array.from({ length: 5 }, (_, i) => (
          <div key={i} className="rounded-xl border border-border/40 bg-surface-elevated p-5">
            <div className="h-5 w-5 animate-pulse rounded bg-muted" />
            <div className="mt-4 h-3 w-20 animate-pulse rounded bg-muted" />
            <div className="mt-2 h-8 w-12 animate-pulse rounded bg-muted" />
            <div className="mt-1 h-3 w-16 animate-pulse rounded bg-muted" />
          </div>
        ))}
      </div>
      <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <div className="rounded-xl border border-border/40 bg-surface-elevated p-6">
          <div className="h-5 w-48 animate-pulse rounded bg-muted" />
          <div className="mt-2 h-3 w-72 animate-pulse rounded bg-muted" />
          <div className="mt-6 h-[280px] animate-pulse rounded-lg bg-muted/40" />
        </div>
        <div className="rounded-xl border border-border/40 bg-surface-elevated p-6">
          <div className="h-5 w-32 animate-pulse rounded bg-muted" />
          <div className="mt-2 h-3 w-48 animate-pulse rounded bg-muted" />
          <div className="mx-auto mt-8 h-40 w-40 animate-pulse rounded-full bg-muted/40" />
        </div>
      </div>
    </div>
  );
}

function CommandCenterError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="mx-auto max-w-2xl rounded-xl border border-destructive/20 bg-destructive/5 px-8 py-12 text-center">
      <Activity className="mx-auto h-8 w-8 text-destructive/50" strokeWidth={1.25} />
      <p className="mt-4 text-sm font-semibold text-foreground">Analytics unavailable</p>
      <p className="mt-2 text-sm text-muted-foreground">{message}</p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-5 rounded-lg bg-primary px-5 py-2 text-sm font-medium text-primary-foreground shadow-soft transition-transform hover:scale-[1.02] active:scale-[0.98]"
      >
        Retry
      </button>
    </div>
  );
}

function CommandCenterFooter() {
  return (
    <footer className="mt-4 border-t border-border/40 pb-2 pt-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-foreground">EduLife</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            © {new Date().getFullYear()} EduLife
          </p>
        </div>
        <div className="flex items-center gap-6 text-xs text-muted-foreground">
          <span>Privacy</span>
          <span>Terms</span>
          <span>Contact</span>
        </div>
      </div>
    </footer>
  );
}

/* ================================================================
   TEACHER PANEL (unchanged)
   ================================================================ */

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
    return (
      <ErrorPanel message={coursesQuery.error.message} onRetry={() => coursesQuery.refetch()} />
    );
  }

  if (cohortsQuery.isError) {
    return (
      <ErrorPanel message={cohortsQuery.error.message} onRetry={() => cohortsQuery.refetch()} />
    );
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
          <MonthRows
            rows={cohortsQuery.data?.enrollmentCohorts ?? []}
            empty="No enrollments yet."
          />
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
                    <td className="py-4 pr-4 text-foreground">
                      {formatCount(course.activeEnrollments)}
                    </td>
                    <td className="py-4 pr-4 text-foreground">
                      {formatPercent(course.completionRatePercent)}
                    </td>
                    <td className="py-4 pr-4 text-foreground">
                      {formatPercent(course.passRatePercent)}
                    </td>
                    <td className="py-4 pr-4 text-foreground">
                      {formatCount(course.certificatesIssued)}
                    </td>
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

function TeacherSummaryGrid({
  analytics,
  cohorts,
}: {
  analytics?: TeacherAnalytics;
  cohorts?: TeacherCohortAnalytics;
}) {
  return (
    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <MetricCard
        title="Courses"
        value={analytics?.totalCourses ?? 0}
        icon={<Layers3 className="h-5 w-5" />}
      />
      <MetricCard
        title="Enrolled"
        value={cohorts?.funnel.enrolled ?? 0}
        icon={<Users className="h-5 w-5" />}
      />
      <MetricCard
        title="Completed"
        value={cohorts?.funnel.completed ?? 0}
        icon={<CheckCircle2 className="h-5 w-5" />}
      />
      <MetricCard
        title="Certified"
        value={cohorts?.funnel.certified ?? 0}
        icon={<Award className="h-5 w-5" />}
      />
    </section>
  );
}

/* ================================================================
   GROUP PANEL (unchanged)
   ================================================================ */

function GroupAnalyticsPanel() {
  const auth = useAuth();
  const groupsQuery = useQuery({
    queryKey: ["groups", "analytics-list"],
    queryFn: () => listMyGroups(auth.getAccessToken),
  });

  const groupQueries = useQueries({
    queries: (groupsQuery.data ?? []).map((group) => ({
      queryKey: ["analytics", "group", group.id],
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
        <MetricCard
          title="Members"
          value={totalMembers}
          icon={<GraduationCap className="h-5 w-5" />}
        />
        <MetricCard
          title="Assigned courses"
          value={totalCourses}
          icon={<BookOpen className="h-5 w-5" />}
        />
      </section>
      {groups.length === 0 ? (
        <StatePanel
          title="No groups yet"
          detail="Create a group before analytics can be computed."
        />
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

/* ================================================================
   SHARED COMPONENTS (used by teacher / group panels)
   ================================================================ */

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
        className="mt-4 rounded-full bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
      >
        Retry
      </button>
    </div>
  );
}

function EmptyText({ children }: { children: React.ReactNode }) {
  return (
    <p className="rounded-2xl border border-border bg-background px-4 py-5 text-sm text-muted-foreground">
      {children}
    </p>
  );
}

/* ================================================================
   UTILITIES
   ================================================================ */

function computeRadarValues(
  summary?: StudentAnalyticsSummary,
  trend?: StudentProgressTrend,
): number[] {
  if (!summary) return [0, 0, 0, 0, 0];
  const totalLessons = Math.max(trend?.totalLessons ?? 1, 1);
  const lessonRatio = Math.min(1, summary.lessonsCompleted / totalLessons);
  const examRatio = summary.examAttempts > 0 ? summary.examsPassed / summary.examAttempts : 0;
  const courseEngagement = Math.min(1, summary.activeEnrollments / 5);
  const certRatio = Math.min(1, summary.certificatesEarned / 3);
  const months = trend?.lessonsByMonth ?? [];
  const activeMonths = months.filter((m) => m.count > 0).length;
  const consistency = months.length > 0 ? activeMonths / months.length : 0;
  return [courseEngagement, lessonRatio, examRatio, certRatio, consistency];
}

function skillLevel(ratio: number): string {
  if (ratio >= 0.9) return "Expert";
  if (ratio >= 0.7) return "Advanced";
  if (ratio >= 0.5) return "Intermediate";
  if (ratio >= 0.25) return "Developing";
  return "Beginner";
}

function formatMonthShort(monthStr: string): string {
  const names = [
    "JAN",
    "FEB",
    "MAR",
    "APR",
    "MAY",
    "JUN",
    "JUL",
    "AUG",
    "SEP",
    "OCT",
    "NOV",
    "DEC",
  ];
  const parts = monthStr.split("-");
  if (parts.length === 2) {
    const idx = parseInt(parts[1], 10) - 1;
    if (idx >= 0 && idx < 12) return names[idx];
  }
  const lower = monthStr.toLowerCase().slice(0, 3);
  const idx = [
    "jan",
    "feb",
    "mar",
    "apr",
    "may",
    "jun",
    "jul",
    "aug",
    "sep",
    "oct",
    "nov",
    "dec",
  ].indexOf(lower);
  if (idx >= 0) return names[idx];
  return monthStr.slice(0, 3).toUpperCase();
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
