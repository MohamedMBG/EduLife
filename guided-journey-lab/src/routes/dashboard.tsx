import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  BookOpen,
  BrainCircuit,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Compass,
  MoreHorizontal,
  Sparkles,
  type LucideIcon,
} from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import {
  enrollInCourse,
  getCourseProgress,
  getProfile,
  getStudentAnalyticsSummary,
  listCourses,
  listMyEnrollments,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";
import type { CourseProgress, EnrolledCourse, StudentAnalyticsSummary } from "../lib/api/types";

export const Route = createFileRoute("/dashboard")({
  component: DashboardRoute,
  head: () => ({ meta: [{ title: "My Learning - EduLife" }] }),
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
  // Non-learner roles keep their dedicated workspaces, so this route never mixes admin,
  // teacher, or group-admin data into the learner dashboard.
  const isLearner = !isAdmin && !isTeacher && !isGroupAdmin;

  const isClient = typeof window !== "undefined";
  const [plannerProgress, setPlannerProgress] = useState({ completed: 0, target: 10 });

  useEffect(() => {
    if (!isClient) return;

    // The planner is currently stored locally; the dashboard reads it without changing
    // the backend contract while still keeping the daily-goal card tied to real user action.
    const completedValue = localStorage.getItem("edulife_planner_completed_hours");
    const targetValue = localStorage.getItem("edulife_planner_target_hours");
    const completed = completedValue ? parseFloat(completedValue) : 0;
    const target = targetValue ? parseInt(targetValue, 10) : 10;
    setPlannerProgress({
      completed: Number.isFinite(completed) ? completed : 0,
      target: Number.isFinite(target) && target > 0 ? target : 10,
    });
  }, [isClient]);

  useEffect(() => {
    if (isAdmin) {
      navigate({ to: "/admin/dashboard" });
    } else if (isTeacher) {
      navigate({ to: "/teach" });
    } else if (isGroupAdmin) {
      navigate({ to: "/groups" });
    }
  }, [isAdmin, isGroupAdmin, isTeacher, navigate]);

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile(auth.getAccessToken),
    enabled: isLearner,
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

  const analyticsQuery = useQuery({
    queryKey: ["analytics", "student", "summary", "dashboard"],
    queryFn: () => getStudentAnalyticsSummary(auth.getAccessToken),
    enabled: isLearner,
  });

  const progressQueries = useQueries({
    queries: (enrollmentsQuery.data ?? []).slice(0, 4).map((enrollment) => ({
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
        queryClient.invalidateQueries({ queryKey: ["analytics", "student"] }),
      ]);
    },
  });

  const profile = profileQuery.data;
  const enrollments = useMemo(() => enrollmentsQuery.data ?? [], [enrollmentsQuery.data]);
  const progressByCourseId = useMemo(
    () =>
      new Map(
        enrollments
          .slice(0, 4)
          .map((enrollment, index) => [enrollment.courseId, progressQueries[index]?.data]),
      ),
    [enrollments, progressQueries],
  );
  const activePaths = enrollments
    .slice(0, 3)
    .map((enrollment) => buildActivePath(enrollment, progressByCourseId.get(enrollment.courseId)));
  const activePath = activePaths[0];
  const enrolledCourseIds = new Set(enrollments.map((enrollment) => enrollment.courseId));
  const suggestedCourses = (exploreQuery.data?.content ?? []).filter(
    (course) => !enrolledCourseIds.has(course.id),
  );
  const firstName =
    profile?.displayName?.split(" ").filter(Boolean)[0] ||
    auth.session?.displayName.split(" ").filter(Boolean)[0] ||
    "learner";
  const metrics = buildMetrics({
    profile,
    analytics: analyticsQuery.data,
    enrollments,
    activePaths,
    plannerProgress,
  });
  const dailyGoal = buildDailyGoal(plannerProgress);
  const careerGoal = inferCareerGoal(activePath?.title, suggestedCourses[0]?.title);

  if (!isLearner) {
    return <RedirectingScreen />;
  }

  const loadingDashboard = profileQuery.isLoading || enrollmentsQuery.isLoading;
  const dashboardError =
    profileQuery.isError || enrollmentsQuery.isError
      ? profileQuery.error?.message || enrollmentsQuery.error?.message
      : null;

  return (
    <AppLayout>
      <div>
        {loadingDashboard ? (
          <DashboardSkeleton />
        ) : dashboardError ? (
          <StatePanel
            title="Dashboard unavailable"
            detail={dashboardError}
            actionLabel="Retry"
            onAction={() => {
              void profileQuery.refetch();
              void enrollmentsQuery.refetch();
            }}
          />
        ) : (
          <>
            <DashboardHero
              firstName={firstName}
              activePath={activePath}
              careerGoal={careerGoal}
              completedLessons={metrics.completedLessons}
            />

            <section className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
              <div className="space-y-16">
                <section className="grid gap-5 md:grid-cols-3">
                  {metrics.cards.map((card) => (
                    <DashboardMetricCard key={card.label} {...card} />
                  ))}
                </section>

                <section>
                  <div className="mb-5 flex items-end justify-between gap-4">
                    <div>
                      <h2 className="text-2xl font-extrabold tracking-0 text-[#091426] sm:text-3xl">
                        Active Paths
                      </h2>
                    </div>
                    <Link
                      to="/courses"
                      className="group inline-flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.18em] text-[#091426] transition-colors duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:text-[#505f76]"
                    >
                      View all
                      <ArrowRight className="h-3.5 w-3.5 transition-transform duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] group-hover:translate-x-1" />
                    </Link>
                  </div>

                  {activePaths.length === 0 ? (
                    <EmptyLearningState />
                  ) : (
                    <div className="space-y-6">
                      {activePaths.map((path, index) => (
                        <ActivePathCard key={path.enrollmentId} path={path} priority={index} />
                      ))}
                    </div>
                  )}
                </section>
              </div>

              <aside className="space-y-6 lg:sticky lg:top-24 lg:self-start">
                <CareerPathWidget
                  careerGoal={careerGoal}
                  activePathTitle={activePath?.title}
                  percent={activePath?.percent ?? 0}
                />
                <DailyGoalWidget goal={dailyGoal} />
              </aside>
            </section>

            <section className="mt-16">
              <DiscoverStrip
                isLoading={exploreQuery.isLoading}
                error={exploreQuery.isError ? exploreQuery.error.message : null}
                suggestions={suggestedCourses.slice(0, 3)}
                isEnrolling={enrollMutation.isPending}
                onEnroll={(courseId) => enrollMutation.mutate(courseId)}
              />
            </section>
          </>
        )}
      </div>
    </AppLayout>
  );
}

interface ActivePath {
  enrollmentId: string;
  courseId: string;
  title: string;
  shortDescription: string;
  level: string;
  languageCode: string;
  imageUrl: string | null;
  percent: number;
  completedLessons: number;
  totalLessons: number;
  nextLessonId: string | null;
  nextLessonTitle: string;
  nextSectionTitle: string | null;
  minutesCompleted: number;
}

function buildActivePath(enrollment: EnrolledCourse, progress?: CourseProgress): ActivePath {
  const lessons =
    progress?.sections.flatMap((section) =>
      section.lessons.map((lesson) => ({ ...lesson, sectionTitle: section.title })),
    ) ?? [];
  const nextLesson = lessons.find((lesson) => !lesson.completed);
  const completedMinutes = lessons
    .filter((lesson) => lesson.completed)
    .reduce((sum, lesson) => sum + (lesson.durationMinutes ?? 0), 0);

  return {
    enrollmentId: enrollment.enrollmentId,
    courseId: enrollment.courseId,
    title: enrollment.title,
    shortDescription: enrollment.shortDescription,
    level: enrollment.level || "Guided",
    languageCode: enrollment.languageCode,
    imageUrl: enrollment.imageUrl,
    percent: Math.round(progress?.percentComplete ?? 0),
    completedLessons: progress?.completedLessons ?? 0,
    totalLessons: progress?.totalLessons ?? 0,
    nextLessonId: nextLesson?.lessonId ?? null,
    nextLessonTitle:
      nextLesson?.title ||
      (progress && progress.percentComplete >= 100 ? "Final exam" : "Course overview"),
    nextSectionTitle: nextLesson?.sectionTitle ?? null,
    minutesCompleted: completedMinutes,
  };
}

function buildMetrics({
  profile,
  analytics,
  enrollments,
  activePaths,
  plannerProgress,
}: {
  profile:
    | { enrolledCourses?: number; completedLessons?: number; certificates?: number }
    | undefined;
  analytics: StudentAnalyticsSummary | undefined;
  enrollments: EnrolledCourse[];
  activePaths: ActivePath[];
  plannerProgress: { completed: number; target: number };
}) {
  const enrolledCourses = Math.max(
    analytics?.activeEnrollments ?? 0,
    profile?.enrolledCourses ?? 0,
    enrollments.length,
  );
  const completedLessons = Math.max(
    analytics?.lessonsCompleted ?? 0,
    profile?.completedLessons ?? 0,
    activePaths.reduce((sum, path) => sum + path.completedLessons, 0),
  );
  const certificates = Math.max(analytics?.certificatesEarned ?? 0, profile?.certificates ?? 0);
  const completedMinutes = activePaths.reduce((sum, path) => sum + path.minutesCompleted, 0);
  const hours = Math.max(plannerProgress.completed, completedMinutes / 60);
  // No gamification score endpoint exists yet, so this progress score is derived from
  // server analytics/profile counts instead of inventing a separate mock data source.
  const skillScore =
    completedLessons * 20 +
    (analytics?.examsPassed ?? certificates) * 120 +
    certificates * 200 +
    enrolledCourses * 15;

  return {
    completedLessons,
    cards: [
      {
        label: "Courses",
        value: String(enrolledCourses),
        status: activePaths.length > 0 ? `${activePaths.length} active now` : "Ready to enroll",
        Icon: BookOpen,
        tone: "blue" as const,
      },
      {
        label: "Hours",
        value: hours > 0 ? formatHours(hours) : "0",
        status: hours > 0 ? "tracked study" : `${plannerProgress.target}h weekly target`,
        Icon: Clock3,
        tone: "amber" as const,
      },
      {
        label: "Progress Score",
        value: String(skillScore),
        status:
          certificates > 0
            ? `${certificates} certificate${certificates === 1 ? "" : "s"}`
            : "building evidence",
        Icon: BrainCircuit,
        tone: "violet" as const,
      },
    ],
  };
}

function buildDailyGoal(plannerProgress: { completed: number; target: number }) {
  const percent = Math.min(
    100,
    Math.round((plannerProgress.completed / plannerProgress.target) * 100),
  );
  const remainingHours = Math.max(0, plannerProgress.target - plannerProgress.completed);
  const remainingMinutes = Math.round(remainingHours * 60);
  return {
    percent,
    remaining:
      remainingMinutes >= 60
        ? `${(remainingMinutes / 60).toFixed(remainingMinutes % 60 === 0 ? 0 : 1)} hrs to go`
        : `${remainingMinutes} mins to go`,
    completedDays: Math.min(7, Math.max(0, Math.ceil((percent / 100) * 7))),
  };
}

function inferCareerGoal(activeTitle?: string, suggestionTitle?: string) {
  const source = `${activeTitle ?? ""} ${suggestionTitle ?? ""}`.toLowerCase();

  if (source.includes("data") || source.includes("network") || source.includes("ai")) {
    return "Senior Data Lead";
  }
  if (source.includes("design") || source.includes("ui")) {
    return "Product Design Lead";
  }
  if (source.includes("web") || source.includes("java") || source.includes("spring")) {
    return "Full Stack Engineer";
  }
  return "your next certificate";
}

function DashboardHero({
  firstName,
  activePath,
  careerGoal,
  completedLessons,
}: {
  firstName: string;
  activePath?: ActivePath;
  careerGoal: string;
  completedLessons: number;
}) {
  const focusTitle = activePath?.nextLessonTitle ?? "Choose your next path";
  const focusSubtitle = activePath?.nextSectionTitle ?? activePath?.title ?? "Course discovery";
  const momentumCopy =
    completedLessons > 0
      ? `You have completed ${completedLessons} lessons. Continue your path to ${careerGoal}.`
      : "Your learner workspace is ready. Choose a course and start the guided flow.";

  return (
    <section className="relative overflow-hidden rounded-[8px] border border-[#dfe3e7] bg-[radial-gradient(circle_at_0%_0%,#ffffff_0%,#f0f4f8_48%,#eaeef2_100%)] px-6 py-10 shadow-[0_22px_70px_-56px_rgba(9,20,38,0.45)] sm:px-10 lg:px-12 lg:py-12">
      <div className="pointer-events-none absolute inset-0 opacity-[0.035] grain" />
      <div className="relative grid gap-8 lg:grid-cols-[minmax(0,1fr)_310px] lg:items-center">
        <div>
          <span className="inline-flex items-center gap-2 rounded-full bg-white/70 px-4 py-1.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-[#091426] shadow-[inset_0_0_0_1px_rgba(197,198,205,0.5)]">
            <span className="h-2 w-2 rounded-full bg-[#22c55e]" />
            Top learner momentum
          </span>
          <h1 className="mt-7 max-w-[14ch] text-[clamp(2.5rem,6vw,4rem)] font-light leading-[1.05] tracking-0 text-[#091426]">
            Welcome back, {firstName}.
          </h1>
          <p className="mt-5 max-w-2xl text-base font-light leading-8 text-[#505f76] sm:text-lg">
            {momentumCopy}
          </p>
          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <HeroResumeButton activePath={activePath} />
            <Link
              to="/planner"
              className="inline-flex h-12 items-center justify-center rounded-[4px] border border-[#dfe3e7] bg-white/50 px-8 text-[12px] font-bold uppercase tracking-[0.16em] text-[#091426] transition-all duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:-translate-y-0.5 hover:bg-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#091426]"
            >
              View Schedule
            </Link>
          </div>
        </div>

        <div className="hidden justify-center lg:flex">
          <div className="rotate-3 rounded-[16px] border border-white/70 bg-white/72 p-1.5 shadow-[0_36px_90px_-48px_rgba(9,20,38,0.42)]">
            <div className="grid h-64 w-64 place-items-center rounded-[12px] bg-white/76 px-8 text-center shadow-[inset_0_1px_1px_rgba(255,255,255,0.9)]">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#505f76]">
                  Today's Focus
                </p>
                <p className="mt-3 text-2xl font-extrabold leading-tight text-[#091426]">
                  {focusTitle}
                </p>
                <p className="mt-2 text-xs font-medium text-[#505f76]">{focusSubtitle}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function HeroResumeButton({ activePath }: { activePath?: ActivePath }) {
  const className =
    "inline-flex h-12 items-center justify-center rounded-[4px] bg-[#091426] px-8 text-[12px] font-bold uppercase tracking-[0.16em] text-white shadow-[0_18px_44px_-28px_rgba(9,20,38,0.72)] transition-all duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:-translate-y-0.5 hover:bg-[#1e293b] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#091426]";

  if (!activePath) {
    return (
      <Link to="/explore" className={className}>
        Browse Catalog
      </Link>
    );
  }

  if (activePath.nextLessonId) {
    return (
      <Link
        to="/learn/$courseId/$lessonId"
        params={{ courseId: activePath.courseId, lessonId: activePath.nextLessonId }}
        className={className}
      >
        Resume Session
      </Link>
    );
  }

  return (
    <Link to="/courses/$courseId" params={{ courseId: activePath.courseId }} className={className}>
      Resume Session
    </Link>
  );
}

function DashboardMetricCard({
  label,
  value,
  status,
  Icon,
  tone,
}: {
  label: string;
  value: string;
  status: string;
  Icon: LucideIcon;
  tone: "blue" | "amber" | "violet";
}) {
  const toneClass = {
    blue: "bg-[#edf4ff] text-[#2563eb]",
    amber: "bg-[#fff7ed] text-[#f97316]",
    violet: "bg-[#f6edff] text-[#7c3aed]",
  }[tone];

  return (
    <article className="group rounded-[8px] border border-[#dfe3e7] bg-white p-6 shadow-[0_18px_50px_-42px_rgba(9,20,38,0.38)] transition-all duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:-translate-y-0.5 hover:border-[#c5c6cd]">
      <div className="flex items-start justify-between">
        <span className={`grid h-12 w-12 place-items-center rounded-[4px] ${toneClass}`}>
          <Icon className="h-5 w-5" strokeWidth={1.8} />
        </span>
        <Sparkline />
      </div>
      <p className="mt-5 text-[12px] font-semibold uppercase tracking-[0.18em] text-[#091426]">
        {label}
      </p>
      <div className="mt-2 flex flex-wrap items-baseline gap-2">
        <span className="font-mono text-3xl tracking-0 text-[#171c1f]">{value}</span>
        <span className="text-[12px] font-semibold text-[#505f76]">{status}</span>
      </div>
    </article>
  );
}

function ActivePathCard({ path, priority }: { path: ActivePath; priority: number }) {
  return (
    <article className="group grid overflow-hidden rounded-[8px] border border-[#dfe3e7] bg-white shadow-[0_20px_58px_-48px_rgba(9,20,38,0.46)] transition-all duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:-translate-y-0.5 hover:border-[#c5c6cd] md:grid-cols-[190px_minmax(0,1fr)_184px]">
      <div className="relative h-52 overflow-hidden bg-[#091426] md:h-full">
        {path.imageUrl ? (
          <img
            src={path.imageUrl}
            alt={path.title}
            className="h-full w-full object-cover transition-transform duration-700 ease-[cubic-bezier(0.32,0.72,0,1)] group-hover:scale-105"
            onError={(e) => {
              e.currentTarget.style.display = "none";
              e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
            }}
          />
        ) : null}
        <div className="grid h-full place-items-center bg-[radial-gradient(circle_at_30%_20%,#1e293b,#091426)] text-white" data-fallback="" hidden={!!path.imageUrl}>
          <BookOpen className="h-12 w-12 opacity-75" strokeWidth={1.4} />
        </div>
        <div className="absolute inset-0 bg-[#091426]/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
      </div>

      <div className="flex min-w-0 flex-col justify-center p-6">
        <div className="flex flex-wrap items-center gap-3">
          <span className="rounded-full bg-[#f0f4f8] px-3 py-1 text-[10px] font-bold uppercase tracking-[0.18em] text-[#091426]">
            {path.level.replaceAll("_", " ")}
          </span>
          <span className="text-xs font-medium text-[#8590a6]">
            {formatLanguage(path.languageCode)} learning path
          </span>
        </div>
        <h3 className="mt-4 max-w-[23ch] text-2xl font-extrabold leading-tight text-[#091426]">
          {path.title}
        </h3>
        <div className="mt-5">
          <div className="h-1.5 overflow-hidden rounded-full bg-[#dfe3e7]">
            <div
              className="h-full rounded-full bg-[#091426] transition-[width] duration-700 ease-[cubic-bezier(0.32,0.72,0,1)]"
              style={{ width: `${path.percent}%` }}
            />
          </div>
          <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs">
            <span className="font-medium text-[#505f76]">{path.percent}% complete</span>
            <span className="font-bold uppercase tracking-0 text-[#091426]">
              Next: {path.nextLessonTitle}
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-center border-t border-[#dfe3e7] bg-[#f6fafe]/70 p-6 md:border-l md:border-t-0">
        <PathResumeButton path={path} filled={priority === 0} />
      </div>
    </article>
  );
}

function PathResumeButton({ path, filled }: { path: ActivePath; filled: boolean }) {
  const className = `inline-flex h-12 w-full items-center justify-center rounded-[4px] px-8 text-[12px] font-bold uppercase tracking-[0.16em] transition-all duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#091426] ${
    filled
      ? "bg-[#091426] text-white shadow-[0_18px_44px_-30px_rgba(9,20,38,0.72)] hover:bg-[#1e293b]"
      : "border border-[#091426] bg-white text-[#091426] hover:bg-[#f0f4f8]"
  }`;

  if (path.nextLessonId) {
    return (
      <Link
        to="/learn/$courseId/$lessonId"
        params={{ courseId: path.courseId, lessonId: path.nextLessonId }}
        className={className}
      >
        Resume
      </Link>
    );
  }

  return (
    <Link to="/courses/$courseId" params={{ courseId: path.courseId }} className={className}>
      Resume
    </Link>
  );
}

function CareerPathWidget({
  careerGoal,
  activePathTitle,
  percent,
}: {
  careerGoal: string;
  activePathTitle?: string;
  percent: number;
}) {
  const aheadText =
    percent > 0
      ? `Your velocity in ${activePathTitle ?? "your active course"} is shaping a focused path toward this outcome.`
      : "Enroll in a course to turn advisor recommendations into a tracked career path.";

  return (
    <section className="relative overflow-hidden rounded-[8px] bg-[#091426] p-8 text-white shadow-[0_28px_68px_-40px_rgba(9,20,38,0.72)]">
      <div className="absolute right-0 top-0 h-16 w-16 rounded-bl-[14px] bg-white/6" />
      <span className="inline-flex rounded-full border border-white/20 bg-white/10 px-3 py-1 text-[10px] font-bold uppercase tracking-[0.18em]">
        AI Career Path
      </span>
      <h3 className="mt-5 text-2xl font-extrabold leading-tight">On track for {careerGoal}</h3>
      <p className="mt-5 text-sm font-light leading-7 text-white/72">{aheadText}</p>
      <div className="mt-7 space-y-4 border-t border-white/12 pt-6">
        <ChecklistRow done label="Prerequisites Met" />
        <ChecklistRow label="Portfolio Review Pending" />
      </div>
      <Link
        to="/advisor"
        className="group mt-8 inline-flex items-center gap-3 text-[12px] font-bold uppercase tracking-[0.16em] text-white transition-colors hover:text-white/75"
      >
        Full Trajectory
        <ArrowRight className="h-4 w-4 transition-transform duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] group-hover:translate-x-1" />
      </Link>
    </section>
  );
}

function DailyGoalWidget({
  goal,
}: {
  goal: { percent: number; remaining: string; completedDays: number };
}) {
  const days = ["M", "T", "W", "T", "F", "S", "S"];

  return (
    <section className="rounded-[8px] border border-[#dfe3e7] bg-white p-8 shadow-[0_18px_50px_-42px_rgba(9,20,38,0.35)]">
      <div className="flex items-center justify-between">
        <h3 className="text-[12px] font-bold uppercase tracking-[0.18em] text-[#091426]">
          Daily Goal
        </h3>
        <MoreHorizontal className="h-5 w-5 text-[#8590a6]" strokeWidth={1.8} />
      </div>
      <div className="mt-8 flex justify-center">
        <ProgressRing percent={goal.percent} />
      </div>
      <div className="mt-7 text-center">
        <p className="text-xl font-extrabold text-[#091426]">{goal.remaining}</p>
        <p className="mt-3 text-xs leading-6 text-[#505f76]">Keep your study cadence visible.</p>
      </div>
      <div className="mt-8 flex justify-between gap-2">
        {days.map((day, index) => (
          <span
            key={`${day}-${index}`}
            className={`grid h-8 w-8 place-items-center rounded-[10px] text-[10px] font-bold ${
              index < goal.completedDays ? "bg-[#091426] text-white" : "bg-[#e4e9ed] text-[#505f76]"
            }`}
          >
            {day}
          </span>
        ))}
      </div>
    </section>
  );
}

function ProgressRing({ percent }: { percent: number }) {
  const radius = 76;
  const stroke = 6;
  const normalizedRadius = radius - stroke / 2;
  const circumference = 2 * Math.PI * normalizedRadius;
  const offset = circumference - (percent / 100) * circumference;

  return (
    <div className="relative h-40 w-40" role="img" aria-label={`${percent}% complete`}>
      <svg className="h-full w-full -rotate-90">
        <circle
          cx="80"
          cy="80"
          r={normalizedRadius}
          fill="transparent"
          stroke="#dfe3e7"
          strokeWidth={stroke}
        />
        <circle
          cx="80"
          cy="80"
          r={normalizedRadius}
          fill="transparent"
          stroke="#091426"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          strokeWidth={stroke}
          style={{ transition: "stroke-dashoffset 700ms cubic-bezier(0.32,0.72,0,1)" }}
        />
      </svg>
      <div className="absolute inset-0 grid place-items-center text-center">
        <div>
          <p className="font-mono text-3xl font-black text-[#091426]">{percent}%</p>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.08em] text-[#505f76]">
            Complete
          </p>
        </div>
      </div>
    </div>
  );
}

function DiscoverStrip({
  isLoading,
  error,
  suggestions,
  isEnrolling,
  onEnroll,
}: {
  isLoading: boolean;
  error: string | null;
  suggestions: Array<{
    id: string;
    title: string;
    shortDescription: string;
    imageUrl: string | null;
  }>;
  isEnrolling: boolean;
  onEnroll: (courseId: string) => void;
}) {
  if (isLoading) {
    return <StatePanel title="Loading catalog suggestions" detail="Fetching published courses." />;
  }

  if (error) {
    return <StatePanel title="Catalog unavailable" detail={error} />;
  }

  if (suggestions.length === 0) {
    return (
      <StatePanel
        title="No new recommendations"
        detail="You are already enrolled in the available published courses."
      />
    );
  }

  return (
    <section className="border-t border-[#dfe3e7] pt-10">
      <div className="mb-5 flex items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-[#505f76]">
            Live Catalog
          </p>
          <h2 className="mt-2 text-2xl font-extrabold text-[#091426]">Recommended next moves</h2>
        </div>
        <Link
          to="/explore"
          className="text-[11px] font-bold uppercase tracking-[0.18em] text-[#091426] hover:text-[#505f76]"
        >
          Catalog
        </Link>
      </div>
      <div className="grid gap-5 md:grid-cols-3">
        {suggestions.map((course) => (
          <article
            key={course.id}
            className="overflow-hidden rounded-[8px] border border-[#dfe3e7] bg-white transition-all duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:-translate-y-0.5 hover:shadow-[0_18px_50px_-42px_rgba(9,20,38,0.38)]"
          >
            <div className="h-36 bg-[#091426]">
              {course.imageUrl ? (
                <img
                  src={course.imageUrl}
                  alt={course.title}
                  className="h-full w-full object-cover"
                  onError={(e) => {
                    e.currentTarget.style.display = "none";
                    e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
                  }}
                />
              ) : null}
              <div className="grid h-full place-items-center text-white/80" data-fallback="" hidden={!!course.imageUrl}>
                <Compass className="h-9 w-9" strokeWidth={1.5} />
              </div>
            </div>
            <div className="p-5">
              <h3 className="text-lg font-extrabold leading-tight text-[#091426]">
                {course.title}
              </h3>
              <p className="mt-2 line-clamp-2 text-sm leading-6 text-[#505f76]">
                {course.shortDescription}
              </p>
              <div className="mt-5 flex flex-wrap gap-2">
                <Link
                  to="/courses/$courseId"
                  params={{ courseId: course.id }}
                  className="rounded-[4px] border border-[#dfe3e7] px-3 py-2 text-xs font-bold text-[#091426] hover:bg-[#f0f4f8]"
                >
                  Outline
                </Link>
                <button
                  type="button"
                  onClick={() => onEnroll(course.id)}
                  disabled={isEnrolling}
                  className="rounded-[4px] bg-[#091426] px-3 py-2 text-xs font-bold text-white disabled:opacity-60"
                >
                  Enroll
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function EmptyLearningState() {
  return (
    <div className="rounded-[8px] border border-[#dfe3e7] bg-white px-6 py-10 text-center shadow-[0_18px_50px_-42px_rgba(9,20,38,0.35)]">
      <span className="mx-auto grid h-12 w-12 place-items-center rounded-[8px] bg-[#f0f4f8] text-[#091426]">
        <Compass className="h-5 w-5" strokeWidth={1.7} />
      </span>
      <h3 className="mt-4 text-lg font-extrabold text-[#091426]">No active enrollment yet</h3>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[#505f76]">
        Browse the catalog and enroll in a course to start the learner flow.
      </p>
      <Link
        to="/explore"
        className="mt-5 inline-flex h-11 items-center justify-center rounded-[4px] bg-[#091426] px-6 text-[12px] font-bold uppercase tracking-[0.16em] text-white"
      >
        Browse Catalog
      </Link>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-8">
      <div className="h-[344px] animate-pulse rounded-[8px] border border-[#dfe3e7] bg-[#eaeef2]" />
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-6">
          <div className="grid gap-5 md:grid-cols-3">
            {[0, 1, 2].map((item) => (
              <div key={item} className="h-40 animate-pulse rounded-[8px] bg-white" />
            ))}
          </div>
          {[0, 1].map((item) => (
            <div key={item} className="h-44 animate-pulse rounded-[8px] bg-white" />
          ))}
        </div>
        <div className="space-y-6">
          <div className="h-80 animate-pulse rounded-[8px] bg-[#091426]/90" />
          <div className="h-96 animate-pulse rounded-[8px] bg-white" />
        </div>
      </div>
    </div>
  );
}

function StatePanel({
  title,
  detail,
  actionLabel,
  onAction,
}: {
  title: string;
  detail: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <div className="rounded-[8px] border border-[#dfe3e7] bg-white px-6 py-12 text-center shadow-[0_18px_50px_-42px_rgba(9,20,38,0.35)]">
      <p className="text-sm font-extrabold text-[#091426]">{title}</p>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[#505f76]">{detail}</p>
      {actionLabel && onAction ? (
        <button
          type="button"
          onClick={onAction}
          className="mt-5 rounded-[4px] bg-[#091426] px-5 py-2.5 text-xs font-bold uppercase tracking-[0.16em] text-white"
        >
          {actionLabel}
        </button>
      ) : null}
    </div>
  );
}

function ChecklistRow({ done, label }: { done?: boolean; label: string }) {
  return (
    <div className="flex items-center gap-3">
      {done ? (
        <CheckCircle2 className="h-4.5 w-4.5 text-[#34d399]" strokeWidth={1.8} />
      ) : (
        <span className="h-4 w-4 rounded-full border border-white/40" />
      )}
      <span className={`text-sm font-semibold ${done ? "text-white" : "text-white/62"}`}>
        {label}
      </span>
    </div>
  );
}

function Sparkline() {
  return (
    <span className="relative h-6 w-16 overflow-hidden rounded-full bg-[#f6fafe]">
      <span className="absolute left-0 top-1/2 h-px w-full bg-[#091426]/16" />
      <span className="absolute left-2 top-[11px] h-px w-12 rotate-[-2deg] bg-[#091426]/24" />
    </span>
  );
}

function RedirectingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f6fafe] px-4">
      <div className="rounded-[8px] border border-[#dfe3e7] bg-white px-6 py-5 text-center shadow-[0_18px_50px_-42px_rgba(9,20,38,0.35)]">
        <p className="text-sm font-semibold text-[#091426]">Taking you to your workspace...</p>
        <p className="mt-2 text-xs text-[#505f76]">
          Each role opens its own portal: teaching, groups, or the admin console.
        </p>
      </div>
    </div>
  );
}

function formatHours(hours: number) {
  return hours % 1 === 0 ? String(hours) : hours.toFixed(1);
}

function formatLanguage(languageCode: string) {
  const normalized = languageCode.toLowerCase();
  if (normalized === "en") return "English";
  if (normalized === "fr") return "French";
  if (normalized === "ar" || normalized === "darija") return "Darija";
  return languageCode.toUpperCase();
}
