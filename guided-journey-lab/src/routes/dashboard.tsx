import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import {
  ArrowUpRight,
  Award,
  BookOpen,
  BrainCircuit,
  CalendarDays,
  CheckCircle2,
  Compass,
  GraduationCap,
  Layers3,
  ShieldCheck,
  Sparkles,
  Target,
  UserCircle2,
  Users,
} from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import {
  getAdminMetrics,
  enrollInCourse,
  getCourseProgress,
  getProfile,
  listCourses,
  listMyEnrollments,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/dashboard")({
  component: DashboardRoute,
  head: () => ({ meta: [{ title: "Dashboard - EduLife" }] }),
});

function DashboardRoute() {
  return (
    <RequireAuth>
      <DashboardPage />
    </RequireAuth>
  );
}

function DashboardPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isAdmin = auth.session?.role === "ADMIN";
  const isTeacher = auth.session?.role === "TEACHER";
  const isGroupAdmin = auth.session?.role === "GROUP_ADMIN";
  // Only learners actually live on /dashboard; every other role is redirected to its own
  // portal below, so we never run the learner queries or render the learner UI for them.
  const isLearner = !isAdmin && !isTeacher && !isGroupAdmin;

  // Load study planner progress from localStorage
  const isClient = typeof window !== "undefined";
  const [plannerProgress, setPlannerProgress] = useState({ completed: 0, target: 10 });

  useEffect(() => {
    if (isClient) {
      const compVal = localStorage.getItem("edulife_planner_completed_hours");
      const targetVal = localStorage.getItem("edulife_planner_target_hours");
      const completed = compVal ? parseFloat(compVal) : 0;
      const target = targetVal ? parseInt(targetVal, 10) : 10;
      setPlannerProgress({ completed, target });
    }
  }, [isClient]);

  // Each role has its own home: admins the admin console, teachers the Teaching Studio,
  // group admins their groups dashboard.
  useEffect(() => {
    if (isAdmin) {
      navigate({ to: "/admin/dashboard" });
    } else if (isTeacher) {
      navigate({ to: "/teach" });
    } else if (isGroupAdmin) {
      navigate({ to: "/groups" });
    }
  }, [isAdmin, isGroupAdmin, isTeacher, navigate]);
  const dashboardTitle = isAdmin
    ? "Platform dashboard"
    : isTeacher
      ? "Teacher dashboard"
      : "Learner dashboard";
  const dashboardDetail = isAdmin
    ? "Admin metrics and learner activity come from the Spring Boot backend."
    : "Profile, enrollments, and discovery all come from the Spring Boot backend.";

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile(auth.getAccessToken),
    enabled: isLearner,
  });

  const adminMetricsQuery = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: () => getAdminMetrics(auth.getAccessToken),
    // Admins have their own AdminShell at /admin/dashboard and are redirected there, so this
    // dashboard never needs the metrics call. Kept disabled to avoid a wasted request.
    enabled: false,
  });

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
    enabled: isLearner,
  });

  const exploreQuery = useQuery({
    queryKey: ["courses", "dashboard"],
    queryFn: () => listCourses(auth.getAccessToken, { size: 6 }),
    enabled: isLearner,
  });

  const progressQueries = useQueries({
    queries: (enrollmentsQuery.data ?? []).slice(0, 3).map((enrollment) => ({
      queryKey: ["progress", enrollment.courseId],
      queryFn: () => getCourseProgress(auth.getAccessToken, enrollment.courseId),
      enabled: enrollmentsQuery.isSuccess,
    })),
  });

  const enrollMutation = useMutation({
    mutationFn: (courseId: string) => enrollInCourse(auth.getAccessToken, courseId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["enrollments"] }),
        queryClient.invalidateQueries({ queryKey: ["profile"] }),
      ]);
    },
  });

  const profile = profileQuery.data;
  const enrollments = enrollmentsQuery.data ?? [];
  const enrolledCourseIds = new Set(enrollments.map((enrollment) => enrollment.courseId));
  const suggestedCourses = (exploreQuery.data?.content ?? []).filter(
    (course) => !enrolledCourseIds.has(course.id),
  );
  const activeCourse = enrollments[0];
  const activeProgress = progressQueries[0]?.data;
  const firstName =
    profile?.displayName?.split(" ").filter(Boolean)[0] ||
    auth.session?.displayName.split(" ").filter(Boolean)[0] ||
    "learner";
  const adminMetrics = adminMetricsQuery.data;

  // Non-learners are being redirected to their own portal — show a clean hand-off screen instead
  // of flashing the learner dashboard (and skip every learner query above).
  if (!isLearner) {
    return <RedirectingScreen />;
  }

  return (
    <AppShell
      active="dashboard"
      user={{
        displayName: profile?.displayName || auth.session?.displayName || "EduLife learner",
        email: profile?.email || auth.session?.email || "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">{dashboardTitle}</p>
          <p className="text-xs text-muted-foreground">{dashboardDetail}</p>
        </div>
      }
    >
      {profileQuery.isLoading ||
      enrollmentsQuery.isLoading ||
      (isAdmin && adminMetricsQuery.isLoading) ? (
        <StateCard
          title="Loading dashboard..."
          detail="Syncing your learner profile and courses."
        />
      ) : profileQuery.isError ? (
        <StateCard title="Dashboard unavailable" detail={profileQuery.error.message} />
      ) : enrollmentsQuery.isError ? (
        <StateCard title="Enrollments unavailable" detail={enrollmentsQuery.error.message} />
      ) : isAdmin && adminMetricsQuery.isError ? (
        <StateCard title="Admin metrics unavailable" detail={adminMetricsQuery.error.message} />
      ) : (
        <div className="space-y-8">
          <section className="relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-primary via-primary to-primary-glow px-7 py-9 lg:px-10 lg:py-11 text-primary-foreground shadow-luxury">
            <div className="absolute -top-24 -right-24 h-72 w-72 rounded-full bg-gold/25 blur-3xl" />
            <div className="absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-teal/20 blur-3xl" />
            <div className="relative">
              <p className="inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/10 px-3 py-1 text-[10px] uppercase tracking-[0.2em] font-medium">
                {isAdmin ? (
                  <ShieldCheck className="h-3 w-3" strokeWidth={1.75} />
                ) : (
                  <GraduationCap className="h-3 w-3" strokeWidth={1.75} />
                )}
                {isAdmin ? "Authenticated admin" : "Authenticated learner"}
              </p>
              <h1 className="mt-5 text-display text-[clamp(2rem,3.5vw,3rem)] leading-[1.02]">
                Welcome back,{" "}
                <span className="italic font-normal text-gold">{firstName}</span>.
              </h1>
              <p className="mt-3 max-w-2xl text-sm text-primary-foreground/80 leading-relaxed">
                {isAdmin
                  ? "Your admin session is backed by Firebase auth and authorized by backend RBAC before metrics load."
                  : "Your session is backed by Firebase auth — the same identity bridge powers the Android app and web."}
              </p>
            </div>
          </section>

          {isAdmin ? (
            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <MetricCard
                title="Learners"
                value={String(adminMetrics?.totalLearners ?? 0)}
                icon={<Users className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Teachers"
                value={String(adminMetrics?.totalTeachers ?? 0)}
                icon={<ShieldCheck className="h-5 w-5 text-teal-600" />}
              />
              <MetricCard
                title="Published courses"
                value={String(adminMetrics?.totalCoursesPublished ?? 0)}
                icon={<Layers3 className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Certificates"
                value={String(adminMetrics?.totalCertificates ?? 0)}
                icon={<GraduationCap className="h-5 w-5 text-amber-500" />}
              />
            </section>
          ) : (
            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <MetricCard
                title="Display name"
                value={profile?.displayName || "Not set"}
                icon={<UserCircle2 className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Enrolled courses"
                value={String(profile?.enrolledCourses ?? 0)}
                icon={<BookOpen className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Completed lessons"
                value={String(profile?.completedLessons ?? 0)}
                icon={<CheckCircle2 className="h-5 w-5 text-teal-600" />}
              />
              <MetricCard
                title="Certificates"
                value={String(profile?.certificates ?? 0)}
                icon={<GraduationCap className="h-5 w-5 text-amber-500" />}
              />
            </section>
          )}

          {!isAdmin && (
            <div className="grid gap-5 md:grid-cols-2">
              <QuickActionCard
                eyebrow="/01 · Advisor"
                Icon={BrainCircuit}
                title="Not sure what to take?"
                detail="Tell the Career Advisor your goal. It scans the live catalog and explains the single best course path."
                ctaLabel="Open advisor"
                to="/advisor"
                accent="primary"
              />
              <QuickActionCard
                eyebrow="/02 · Study planner"
                Icon={CalendarDays}
                title="Weekly study planner"
                detail={
                  plannerProgress.completed > 0
                    ? `${plannerProgress.completed.toFixed(1)} of ${plannerProgress.target} hours done this week — keep the streak alive.`
                    : "Set a weekly hours target and track your study cadence."
                }
                ctaLabel="Open planner"
                to="/planner"
                accent="gold"
                progress={
                  plannerProgress.completed > 0
                    ? {
                        current: plannerProgress.completed,
                        target: plannerProgress.target,
                      }
                    : undefined
                }
              />
            </div>
          )}

          <section className="grid gap-5 xl:grid-cols-[1.4fr_0.8fr]">
            <div className="relative overflow-hidden rounded-[1.75rem] hairline bg-surface-elevated p-7 lg:p-8">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-muted-foreground">
                    /03 · Continue learning
                  </span>
                  <h2 className="mt-3 text-display text-2xl text-foreground leading-tight">
                    Pick up where you left off
                  </h2>
                </div>
                {activeCourse ? (
                  <Link
                    to="/courses/$courseId"
                    params={{ courseId: activeCourse.courseId }}
                    className="group inline-flex h-10 items-center gap-1 rounded-full bg-foreground text-background pl-4 pr-1 text-xs font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98]"
                  >
                    <span>Open course</span>
                    <span className="grid h-8 w-8 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-x-0.5 group-hover:-translate-y-px group-hover:bg-background/25">
                      <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
                    </span>
                  </Link>
                ) : (
                  <Link
                    to="/explore"
                    className="group inline-flex h-10 items-center gap-1 rounded-full bg-foreground text-background pl-4 pr-1 text-xs font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98]"
                  >
                    <span>Browse catalog</span>
                    <span className="grid h-8 w-8 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-x-0.5 group-hover:-translate-y-px group-hover:bg-background/25">
                      <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
                    </span>
                  </Link>
                )}
              </div>

              {activeCourse && activeProgress ? (
                <div className="mt-7 grid lg:grid-cols-[1fr_180px] gap-7 items-end">
                  <div>
                    <p className="text-[10px] font-mono uppercase tracking-[0.2em] text-primary">
                      Currently studying
                    </p>
                    <h3 className="mt-2 text-display text-2xl text-foreground leading-tight max-w-[28ch]">
                      {activeCourse.title}
                    </h3>
                    <p className="mt-3 text-sm text-muted-foreground leading-relaxed max-w-[55ch]">
                      {activeCourse.shortDescription}
                    </p>
                    <div className="mt-6 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="inline-flex items-center gap-1.5">
                        <span className="h-1.5 w-1.5 rounded-full bg-teal" />
                        {activeProgress.completedLessons} / {activeProgress.totalLessons} lessons
                      </span>
                      <span className="h-3 w-px bg-border" />
                      <span className="inline-flex items-center gap-1.5">
                        <Target className="h-3 w-3" strokeWidth={1.75} />
                        Pass at 80% on exam
                      </span>
                    </div>
                  </div>

                  {/* Ring progress */}
                  <ProgressRing percent={Math.round(activeProgress.percentComplete)} />
                </div>
              ) : (
                <div className="mt-7 rounded-2xl hairline bg-surface px-6 py-8 text-center">
                  <span className="grid h-12 w-12 mx-auto place-items-center rounded-2xl bg-primary/8 text-primary">
                    <Compass className="h-5 w-5" strokeWidth={1.5} />
                  </span>
                  <p className="mt-4 text-sm font-medium text-foreground">
                    No active enrollment yet
                  </p>
                  <p className="mt-1.5 text-xs text-muted-foreground max-w-[40ch] mx-auto leading-relaxed">
                    Browse the catalog and enroll in a course to kick off the learner flow.
                  </p>
                </div>
              )}
            </div>

            <div className="relative overflow-hidden rounded-[1.75rem] hairline bg-surface-elevated p-7 lg:p-8">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-muted-foreground">
                    /04 · Profile
                  </span>
                  <h2 className="mt-3 text-display text-xl text-foreground leading-tight">
                    Your learner snapshot
                  </h2>
                </div>
                <Link
                  to="/profile"
                  className="grid h-9 w-9 place-items-center rounded-full hairline bg-surface text-muted-foreground transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:text-foreground"
                  aria-label="Open profile"
                >
                  <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
                </Link>
              </div>

              <dl className="mt-6 space-y-5">
                <SnapshotRow label="Email" value={profile?.email ?? "—"} mono />
                <SnapshotRow
                  label="Bio"
                  value={profile?.bio || "No bio added yet."}
                  muted={!profile?.bio}
                />
                <SnapshotRow label="Role" value={auth.session?.role ?? "—"} pill />
              </dl>
            </div>
          </section>

          <section className="space-y-5">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-muted-foreground">
                  /05 · Discover
                </span>
                <h2 className="mt-3 text-display text-2xl text-foreground leading-tight">
                  Pulled from the live catalog
                </h2>
                <p className="mt-1.5 text-sm text-muted-foreground max-w-[55ch]">
                  Same published-course endpoint that powers Explore — never seeded or
                  mocked.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Link
                  to="/certificates"
                  className="inline-flex items-center gap-1.5 rounded-full hairline bg-surface-elevated px-4 py-2 text-xs font-medium text-foreground/85 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:text-foreground hover:shadow-soft"
                >
                  <Award className="h-3.5 w-3.5" strokeWidth={1.5} />
                  Certificates
                </Link>
                <Link
                  to="/explore"
                  className="group inline-flex h-9 items-center gap-1 rounded-full bg-foreground text-background pl-3.5 pr-1 text-xs font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98]"
                >
                  <span>Browse all</span>
                  <span className="grid h-7 w-7 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-x-0.5 group-hover:-translate-y-px group-hover:bg-background/25">
                    <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
                  </span>
                </Link>
              </div>
            </div>

            {exploreQuery.isLoading ? (
              <StateCard title="Loading suggestions…" detail="Fetching published courses." />
            ) : exploreQuery.isError ? (
              <StateCard title="Suggestions unavailable" detail={exploreQuery.error.message} />
            ) : suggestedCourses.length === 0 ? (
              <StateCard
                title="No suggestions yet"
                detail="You are already enrolled in the available seed courses."
              />
            ) : (
              <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                {suggestedCourses.slice(0, 3).map((course, i) => (
                  <article
                    key={course.id}
                    className="group relative flex flex-col overflow-hidden rounded-[1.5rem] hairline bg-surface-elevated transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:shadow-elevated"
                  >
                    <div className="relative aspect-[16/10] overflow-hidden bg-muted">
                      {course.imageUrl ? (
                        <img
                          src={course.imageUrl}
                          alt={course.title}
                          className="h-full w-full object-cover transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.04]"
                        />
                      ) : (
                        <div className="grid h-full place-items-center bg-gradient-to-br from-primary/10 to-primary-glow/10 text-primary">
                          <Compass className="h-9 w-9 opacity-60" strokeWidth={1.25} />
                        </div>
                      )}
                      <span className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-background/85 hairline px-2.5 py-1 text-[10px] font-mono uppercase tracking-[0.18em] text-foreground backdrop-blur-sm">
                        <Sparkles className="h-2.5 w-2.5 text-primary" strokeWidth={2} />
                        Pick {i + 1}
                      </span>
                    </div>
                    <div className="flex flex-col flex-1 p-5">
                      <h3 className="text-display text-lg leading-snug text-foreground">
                        {course.title}
                      </h3>
                      <p className="mt-2 text-sm text-muted-foreground leading-relaxed line-clamp-3">
                        {course.shortDescription}
                      </p>
                      <div className="mt-auto pt-5 flex flex-wrap items-center gap-2">
                        <Link
                          to="/courses/$courseId"
                          params={{ courseId: course.id }}
                          className="inline-flex items-center gap-1.5 rounded-full hairline bg-surface px-3.5 py-2 text-xs font-medium text-foreground/85 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:text-foreground hover:-translate-y-0.5"
                        >
                          Outline
                          <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
                        </Link>
                        <button
                          type="button"
                          onClick={() => enrollMutation.mutate(course.id)}
                          disabled={enrollMutation.isPending}
                          className="group/btn relative inline-flex items-center gap-1 rounded-full bg-foreground text-background pl-3.5 pr-1 py-1 h-9 text-xs font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98] disabled:opacity-60 disabled:pointer-events-none"
                        >
                          <span>Enroll</span>
                          <span className="grid h-7 w-7 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover/btn:translate-x-0.5 group-hover/btn:-translate-y-px group-hover/btn:bg-background/25">
                            <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
                          </span>
                        </button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </AppShell>
  );
}

function MetricCard({ title, value, icon }: { title: string; value: string; icon: ReactNode }) {
  return (
    <div className="group relative overflow-hidden rounded-2xl hairline bg-surface-elevated p-5 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:shadow-elevated">
      <div className="flex items-center justify-between">
        <p className="text-[10px] uppercase tracking-[0.2em] font-mono text-muted-foreground">
          {title}
        </p>
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/8 group-hover:bg-primary/12 transition-colors duration-500">
          {icon}
        </span>
      </div>
      <p className="mt-5 text-display text-3xl lg:text-4xl text-foreground leading-none">
        {value}
      </p>
    </div>
  );
}

function QuickActionCard({
  eyebrow,
  Icon,
  title,
  detail,
  ctaLabel,
  to,
  accent,
  progress,
}: {
  eyebrow: string;
  Icon: typeof BrainCircuit;
  title: string;
  detail: string;
  ctaLabel: string;
  to: "/advisor" | "/planner";
  accent: "primary" | "gold";
  progress?: { current: number; target: number };
}) {
  const pct = progress
    ? Math.min(100, Math.round((progress.current / progress.target) * 100))
    : 0;
  return (
    <section className="group relative flex flex-col justify-between overflow-hidden rounded-[1.75rem] hairline bg-surface-elevated p-6 lg:p-7 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:shadow-elevated">
      <div
        className={`pointer-events-none absolute -top-20 -right-12 h-44 w-44 rounded-full blur-3xl opacity-50 ${
          accent === "gold" ? "bg-gold/25" : "bg-primary/15"
        }`}
      />
      <div className="relative flex items-start justify-between gap-4">
        <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-muted-foreground">
          {eyebrow}
        </span>
        <div className="bezel">
          <span
            className={`bezel-inner grid h-11 w-11 place-items-center ${
              accent === "gold"
                ? "bg-gradient-gold text-gold-foreground"
                : "bg-gradient-primary text-primary-foreground"
            }`}
          >
            <Icon className="h-4.5 w-4.5" strokeWidth={1.5} />
          </span>
        </div>
      </div>

      <div className="relative mt-6">
        <h3 className="text-display text-xl leading-snug text-foreground">{title}</h3>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground max-w-[42ch]">
          {detail}
        </p>

        {progress && (
          <div className="mt-5">
            <div className="mb-1.5 flex items-center justify-between text-[11px] font-mono uppercase tracking-[0.16em] text-muted-foreground">
              <span>
                {progress.current.toFixed(1)} / {progress.target} h
              </span>
              <span className="text-foreground font-semibold">{pct}%</span>
            </div>
            <div className="h-1.5 rounded-full bg-border overflow-hidden">
              <div
                className={`h-full rounded-full ${
                  accent === "gold" ? "bg-gradient-gold" : "bg-gradient-primary"
                }`}
                style={{ width: `${pct}%` }}
              />
            </div>
          </div>
        )}
      </div>

      <div className="relative mt-6 flex justify-end">
        <Link
          to={to}
          className="group/btn inline-flex h-10 items-center gap-1 rounded-full bg-foreground text-background pl-4 pr-1 text-xs font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98]"
        >
          <span>{ctaLabel}</span>
          <span className="grid h-8 w-8 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover/btn:translate-x-0.5 group-hover/btn:-translate-y-px group-hover/btn:bg-background/25">
            <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
          </span>
        </Link>
      </div>
    </section>
  );
}

function ProgressRing({ percent }: { percent: number }) {
  const radius = 62;
  const stroke = 10;
  const norm = radius - stroke / 2;
  const circumference = 2 * Math.PI * norm;
  const offset = circumference - (percent / 100) * circumference;
  return (
    <div className="relative grid place-items-center mx-auto lg:mx-0">
      <svg width={radius * 2} height={radius * 2} className="-rotate-90">
        <circle
          cx={radius}
          cy={radius}
          r={norm}
          fill="none"
          stroke="var(--color-border)"
          strokeWidth={stroke}
        />
        <circle
          cx={radius}
          cy={radius}
          r={norm}
          fill="none"
          stroke="url(#ring-grad)"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ transition: "stroke-dashoffset 0.9s cubic-bezier(0.16,1,0.3,1)" }}
        />
        <defs>
          <linearGradient id="ring-grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="oklch(0.28 0.20 158)" />
            <stop offset="100%" stopColor="oklch(0.60 0.24 148)" />
          </linearGradient>
        </defs>
      </svg>
      <div className="absolute inset-0 grid place-items-center text-center">
        <div>
          <p className="text-display text-3xl text-foreground leading-none">{percent}%</p>
          <p className="mt-1 text-[10px] font-mono uppercase tracking-[0.18em] text-muted-foreground">
            complete
          </p>
        </div>
      </div>
    </div>
  );
}

function SnapshotRow({
  label,
  value,
  mono,
  muted,
  pill,
}: {
  label: string;
  value: string;
  mono?: boolean;
  muted?: boolean;
  pill?: boolean;
}) {
  return (
    <div>
      <dt className="text-[10px] font-mono uppercase tracking-[0.18em] text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1.5">
        {pill ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/8 hairline px-3 py-1 text-[11px] font-mono uppercase tracking-[0.16em] text-primary">
            <span className="h-1 w-1 rounded-full bg-primary" />
            {value}
          </span>
        ) : (
          <span
            className={`block text-sm leading-relaxed ${
              muted ? "text-muted-foreground" : "text-foreground"
            } ${mono ? "font-mono text-[13px]" : ""}`}
          >
            {value}
          </span>
        )}
      </dd>
    </div>
  );
}

function RedirectingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-5 text-center shadow-elevated">
        <p className="text-sm font-medium text-foreground">Taking you to your workspace…</p>
        <p className="mt-2 text-xs text-muted-foreground">
          Each role opens its own portal — teaching, groups, or the admin console.
        </p>
      </div>
    </div>
  );
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-2xl hairline bg-surface-elevated px-6 py-10 text-center">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground leading-relaxed max-w-md mx-auto">
        {detail}
      </p>
    </div>
  );
}
