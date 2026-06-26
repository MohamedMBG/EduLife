import { createFileRoute } from "@tanstack/react-router";
import { useQueries, useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { AppLayout } from "../components/app/AppLayout";
import {
  getCourseProgress,
  getGamificationState,
  listMyCertificates,
  listMyEnrollments,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";
import type {
  Certificate,
  CourseProgress,
  EnrolledCourse,
  GamificationState,
} from "../lib/api/types";

// Level subcomponents
import { RankCard } from "../components/level/RankCard";
import { StreakCard } from "../components/level/StreakCard";
import { QuestsCard } from "../components/level/QuestsCard";
import { SkillTreeCard } from "../components/level/SkillTreeCard";
import { WeeklyXpCard } from "../components/level/WeeklyXpCard";
import { RecentActivityCard } from "../components/level/RecentActivityCard";
import { AchievementsCard } from "../components/level/AchievementsCard";
import { LevelSkeleton, EmptyState } from "../components/level/LevelStates";

// Types and constants
import {
  type LevelState,
  XP_LESSON_COMPLETE,
  XP_PER_CERTIFICATE_BUNDLE,
  type ActivityCollections,
} from "../components/level/level-types";

export const Route = createFileRoute("/level")({
  component: LevelRoute,
  head: () => ({ meta: [{ title: "Level & Progress — EduLife" }] }),
});

// ─── display helpers (activity feed + weekly chart only) ─────────────────────

function startOfDay(d: Date) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

function buildWeekWindow() {
  const today = startOfDay(new Date());
  const dow = today.getDay();
  const offsetToMonday = dow === 0 ? -6 : 1 - dow;
  const monday = new Date(today);
  monday.setDate(today.getDate() + offsetToMonday);

  const labels = ["M", "T", "W", "T", "F", "S", "S"];
  return labels.map((label, i) => {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    return { date: d, label, key: d.toDateString() };
  });
}

function collectActivity(
  enrollments: EnrolledCourse[],
  progresses: (CourseProgress | undefined)[],
  certificates: Certificate[],
): ActivityCollections {
  const enrollmentTitles = new Map<string, string>();
  for (const e of enrollments) enrollmentTitles.set(e.courseId, e.title);

  const lessonCompletions: ActivityCollections["lessonCompletions"] = [];

  for (const cp of progresses) {
    if (!cp) continue;
    const courseTitle = enrollmentTitles.get(cp.courseId) ?? null;
    for (const section of cp.sections) {
      for (const lesson of section.lessons) {
        if (lesson.completed && lesson.completedAt) {
          lessonCompletions.push({
            date: new Date(lesson.completedAt),
            title: lesson.title,
            courseTitle,
          });
        }
      }
    }
  }

  const certificateEvents = certificates.map((c) => ({
    date: new Date(c.issuedAt),
    title: c.courseTitle,
  }));

  return { lessonCompletions, certificateEvents };
}

// ─── state builder — authoritative values from backend, display data from activity ──

function buildLevelState(
  gamState: GamificationState,
  enrollments: EnrolledCourse[],
  progresses: (CourseProgress | undefined)[],
  certificates: Certificate[],
): LevelState {
  const { lessonCompletions, certificateEvents } = collectActivity(
    enrollments,
    progresses,
    certificates,
  );

  const isMax = gamState.level >= 10;
  // xpForNextLevel = 0 at max level; guard denominator
  const xpRequired = isMax ? 1 : Math.max(1, gamState.xpForNextLevel);
  const xpInto = gamState.xpIntoLevel;
  const xpPct = isMax ? 100 : Math.min(100, Math.round((xpInto / xpRequired) * 100));

  const earnedBadges = new Set(
    gamState.badges.filter((b) => b.unlocked).map((b) => b.id),
  );

  const totalLessons = progresses.reduce(
    (sum, cp) => sum + (cp?.completedLessons ?? 0),
    0,
  );
  const totalCertificates = certificates.length;
  const totalEnrollments = enrollments.length;

  const week = buildWeekWindow();
  const todayKey = startOfDay(new Date()).toDateString();

  const allDates = [
    ...lessonCompletions.map((l) => l.date),
    ...certificateEvents.map((c) => c.date),
  ];
  const dayKeys = new Set(allDates.map((d) => startOfDay(d).toDateString()));

  // Weekly chart: display-only approximation of XP earned per day.
  // Not authoritative — gamState.totalXp is the source of truth.
  const weeklyXp = week.map(({ date, label, key }) => {
    const lessonsCount = lessonCompletions.filter(
      (l) => startOfDay(l.date).toDateString() === key,
    ).length;
    const certCount = certificateEvents.filter(
      (c) => startOfDay(c.date).toDateString() === key,
    ).length;
    return {
      day: label,
      xp: lessonsCount * XP_LESSON_COMPLETE + certCount * XP_PER_CERTIFICATE_BUNDLE,
      date,
    };
  });
  const xpWeek = weeklyXp.reduce((sum, d) => sum + d.xp, 0);

  const todayLessons = lessonCompletions.filter(
    (l) => startOfDay(l.date).toDateString() === todayKey,
  ).length;
  const todayCerts = certificateEvents.filter(
    (c) => startOfDay(c.date).toDateString() === todayKey,
  ).length;
  const xpToday =
    todayLessons * XP_LESSON_COMPLETE + todayCerts * XP_PER_CERTIFICATE_BUNDLE;

  const questDailyDone = Math.min(3, todayLessons);
  const questCertEarned = totalCertificates > 0;

  const streakDays = week.map(({ label, key, date }) => ({
    label,
    active: dayKeys.has(key),
    today: key === todayKey && date <= new Date(),
  }));

  const recentActivity = [
    ...lessonCompletions.map((l) => ({
      kind: "lesson" as const,
      title: l.title,
      subtitle: l.courseTitle ?? "Lesson completed",
      date: l.date,
      xp: XP_LESSON_COMPLETE,
    })),
    ...certificateEvents.map((c) => ({
      kind: "certificate" as const,
      title: `Certificate · ${c.title}`,
      subtitle: "Course completed",
      date: c.date,
      xp: XP_PER_CERTIFICATE_BUNDLE,
    })),
  ]
    .sort((a, b) => b.date.getTime() - a.date.getTime())
    .slice(0, 6);

  return {
    totalXp: gamState.totalXp,
    level: gamState.level,
    xpInto,
    xpRequired,
    xpPct,
    isMax,
    totalLessons,
    totalCertificates,
    totalEnrollments,
    streak: gamState.currentStreak,
    longestStreak: gamState.longestStreak,
    xpToday,
    xpWeek,
    weeklyXp,
    streakDays,
    earnedBadges,
    questDailyDone,
    questCertEarned,
    recentActivity,
    hasAnyActivity: gamState.totalXp > 0 || certificates.length > 0,
  };
}

// ─── components ────────────────────────────────────────────────────────────────

function LevelRoute() {
  return (
    <RequireAuth>
      <LevelPage />
    </RequireAuth>
  );
}

function LevelPage() {
  const auth = useAuth();

  const gamificationQuery = useQuery({
    queryKey: ["gamification"],
    queryFn: () => getGamificationState(auth.getAccessToken),
  });

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const certificatesQuery = useQuery({
    queryKey: ["certificates"],
    queryFn: () => listMyCertificates(auth.getAccessToken),
  });

  const enrollments = enrollmentsQuery.data ?? [];

  const progressQueries = useQueries({
    queries: enrollments.map((e) => ({
      queryKey: ["progress", e.courseId],
      queryFn: () => getCourseProgress(auth.getAccessToken, e.courseId),
      enabled: enrollmentsQuery.isSuccess,
    })),
  });

  const progressLoading = progressQueries.length > 0 && progressQueries.some((q) => q.isPending);

  const firstError =
    gamificationQuery.error ??
    enrollmentsQuery.error ??
    certificatesQuery.error ??
    progressQueries.find((q) => q.error)?.error ??
    null;

  const isPending =
    gamificationQuery.isPending ||
    enrollmentsQuery.isPending ||
    certificatesQuery.isPending ||
    progressLoading;

  const state = useMemo(() => {
    if (!gamificationQuery.data) return null;
    return buildLevelState(
      gamificationQuery.data,
      enrollments,
      progressQueries.map((q) => q.data),
      certificatesQuery.data ?? [],
    );
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    gamificationQuery.data,
    enrollmentsQuery.data,
    certificatesQuery.data,
    progressQueries.map((q) => q.data).join("|"),
  ]);

  const session = auth.session;
  const shellUser = {
    displayName: session?.displayName ?? "EduLife learner",
    email: session?.email ?? "",
  };

  return (
    <AppLayout>
      <div className="mx-auto max-w-5xl px-5 lg:px-8 py-6 space-y-5">
        {firstError ? (
          <div className="rounded-3xl border border-destructive/20 bg-destructive/5 p-6 text-sm text-destructive">
            Couldn't load your progress.{" "}
            {firstError instanceof Error ? firstError.message : "Try again."}
          </div>
        ) : null}

        {isPending ? (
          <LevelSkeleton />
        ) : !state || !state.hasAnyActivity ? (
          <EmptyState />
        ) : (
          <>
            <RankCard state={state} displayName={shellUser.displayName} />

            <div className="grid md:grid-cols-[auto_1fr] gap-4">
              <StreakCard state={state} />
              <QuestsCard state={state} />
            </div>

            <SkillTreeCard state={state} />

            <div className="grid lg:grid-cols-[1fr_360px] gap-4">
              <WeeklyXpCard state={state} />
              <RecentActivityCard state={state} />
            </div>

            <AchievementsCard state={state} />
          </>
        )}
      </div>
    </AppLayout>
  );
}
