import { createFileRoute, Link } from "@tanstack/react-router";
import { useQueries, useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { ArrowLeft } from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import {
  getCourseProgress,
  getProfile,
  listMyCertificates,
  listMyEnrollments,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";
import type {
  Certificate,
  CourseProgress,
  EnrolledCourse,
  Profile,
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
  LevelState,
  LEVEL_THRESHOLDS,
  XP_LESSON_COMPLETE,
  XP_ENROLLMENT,
  XP_PER_CERTIFICATE_BUNDLE,
  XP_STREAK_3_BONUS,
  XP_STREAK_7_BONUS,
  ActivityCollections,
} from "../components/level/level-types";

export const Route = createFileRoute("/level")({
  component: LevelRoute,
  head: () => ({ meta: [{ title: "Level & Progress — EduLife" }] }),
});

// ─── derivation helpers ────────────────────────────────────────────────────────

function startOfDay(d: Date) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

function deriveLevel(totalXp: number) {
  let level = 1;
  for (let i = 1; i < LEVEL_THRESHOLDS.length; i++) {
    if (totalXp >= LEVEL_THRESHOLDS[i]) level = i + 1;
  }
  level = Math.min(level, LEVEL_THRESHOLDS.length);
  const isMax = level >= LEVEL_THRESHOLDS.length;
  const lower = LEVEL_THRESHOLDS[level - 1];
  const upper = isMax ? lower + 5000 : LEVEL_THRESHOLDS[level];
  const xpInto = Math.max(0, totalXp - lower);
  const xpRequired = Math.max(1, upper - lower);
  return { level, xpInto, xpRequired, isMax };
}

function computeStreak(daysSet: Set<string>) {
  if (daysSet.size === 0) return 0;
  const today = startOfDay(new Date());
  let streak = 0;
  let start = 0;

  if (!daysSet.has(today.toDateString())) {
    // streak is allowed to ignore today before midnight; keep yesterday's run alive
    start = 1;
  }

  for (let i = start; i < 365; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    if (daysSet.has(d.toDateString())) streak++;
    else break;
  }
  return streak;
}

function computeLongestStreak(dates: Date[]) {
  if (dates.length === 0) return 0;
  const uniq = Array.from(
    new Set(dates.map((d) => startOfDay(d).getTime())),
  ).sort((a, b) => a - b);
  let longest = 1;
  let current = 1;
  for (let i = 1; i < uniq.length; i++) {
    if (uniq[i] - uniq[i - 1] === 86400000) current++;
    else current = 1;
    if (current > longest) longest = current;
  }
  return longest;
}

function buildWeekWindow() {
  const today = startOfDay(new Date());
  const dow = today.getDay(); // 0=Sun..6=Sat
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

function countStreakBonuses(dates: Date[]) {
  if (dates.length === 0) return { three: 0, seven: 0 };
  const uniq = Array.from(
    new Set(dates.map((d) => startOfDay(d).getTime())),
  ).sort((a, b) => a - b);
  let three = 0;
  let seven = 0;
  let runLen = 1;
  for (let i = 1; i <= uniq.length; i++) {
    const continued = i < uniq.length && uniq[i] - uniq[i - 1] === 86400000;
    if (continued) {
      runLen++;
    } else {
      if (runLen >= 3) three++;
      if (runLen >= 7) seven++;
      runLen = 1;
    }
  }
  return { three, seven };
}

function maxRolling7DayCount(dates: Date[]) {
  if (dates.length === 0) return 0;
  const byDay = new Map<number, number>();
  for (const d of dates) {
    const t = startOfDay(d).getTime();
    byDay.set(t, (byDay.get(t) ?? 0) + 1);
  }
  const sortedDays = Array.from(byDay.keys()).sort((a, b) => a - b);
  let max = 0;
  for (const day of sortedDays) {
    let count = 0;
    for (let i = 0; i < 7; i++) {
      const k = day + i * 86400000;
      count += byDay.get(k) ?? 0;
    }
    if (count > max) max = count;
  }
  return max;
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

function deriveState(
  profile: Profile | undefined,
  enrollments: EnrolledCourse[],
  progresses: (CourseProgress | undefined)[],
  certificates: Certificate[],
): LevelState {
  const { lessonCompletions, certificateEvents } = collectActivity(
    enrollments,
    progresses,
    certificates,
  );

  const completionsCounted = lessonCompletions.length;
  const profileCompleted = profile?.completedLessons ?? 0;
  const totalLessons = Math.max(completionsCounted, profileCompleted);
  const totalCertificates = certificates.length;
  const totalEnrollments = profile?.enrolledCourses ?? enrollments.length;

  // Streak bonuses (awarded once per completed run that crossed each threshold).
  const allDates: Date[] = [
    ...lessonCompletions.map((l) => l.date),
    ...certificateEvents.map((c) => c.date),
  ];
  const { three: streak3Count, seven: streak7Count } = countStreakBonuses(allDates);

  const totalXp =
    totalLessons * XP_LESSON_COMPLETE +
    totalCertificates * XP_PER_CERTIFICATE_BUNDLE +
    totalEnrollments * XP_ENROLLMENT +
    streak3Count * XP_STREAK_3_BONUS +
    streak7Count * XP_STREAK_7_BONUS;

  const { level, xpInto, xpRequired, isMax } = deriveLevel(totalXp);
  const xpPct = Math.min(100, Math.round((xpInto / xpRequired) * 100));

  const dayKeys = new Set(allDates.map((d) => startOfDay(d).toDateString()));
  const streak = computeStreak(dayKeys);
  const longestStreak = computeLongestStreak(allDates);

  const week = buildWeekWindow();
  const weeklyXp = week.map(({ date, label, key }) => {
    const lessonsCount = lessonCompletions.filter(
      (l) => startOfDay(l.date).toDateString() === key,
    ).length;
    const certCount = certificateEvents.filter(
      (c) => startOfDay(c.date).toDateString() === key,
    ).length;
    return {
      day: label,
      xp:
        lessonsCount * XP_LESSON_COMPLETE +
        certCount * XP_PER_CERTIFICATE_BUNDLE,
      date,
    };
  });
  const xpWeek = weeklyXp.reduce((sum, d) => sum + d.xp, 0);

  const todayKey = startOfDay(new Date()).toDateString();
  const xpToday =
    lessonCompletions.filter(
      (l) => startOfDay(l.date).toDateString() === todayKey,
    ).length *
      XP_LESSON_COMPLETE +
    certificateEvents.filter(
      (c) => startOfDay(c.date).toDateString() === todayKey,
    ).length *
      XP_PER_CERTIFICATE_BUNDLE;
  const questDailyDone = Math.min(
    3,
    lessonCompletions.filter(
      (l) => startOfDay(l.date).toDateString() === todayKey,
    ).length,
  );
  const questCertEarned = totalCertificates > 0;

  const streakDays = week.map(({ label, key, date }) => ({
    label,
    active: dayKeys.has(key),
    today: key === todayKey && date <= new Date(),
  }));

  // Badge derivation — shared spec, ids must match Android XpEngine.
  const earnedBadges = new Set<string>();
  if (totalLessons >= 1) earnedBadges.add("first_flame");
  if (totalLessons >= 10) earnedBadges.add("bookworm");
  if (totalCertificates >= 1) {
    earnedBadges.add("sharp_mind");
    earnedBadges.add("graduate");
  }
  if (totalCertificates >= 3) earnedBadges.add("trophy_hunter");
  if (longestStreak >= 14) earnedBadges.add("dedicated");
  if (longestStreak >= 30) earnedBadges.add("star_learner");
  if (longestStreak >= 60) earnedBadges.add("inferno");
  if (level >= 7) earnedBadges.add("scholar");
  if (level >= 10) earnedBadges.add("master");
  if (maxRolling7DayCount(lessonCompletions.map((l) => l.date)) >= 5) {
    earnedBadges.add("on_a_roll");
  }

  // Speed Run — 3+ lessons in a single day
  {
    const byDay = new Map<string, number>();
    for (const l of lessonCompletions) {
      const k = startOfDay(l.date).toDateString();
      byDay.set(k, (byDay.get(k) ?? 0) + 1);
    }
    for (const v of byDay.values()) {
      if (v >= 3) {
        earnedBadges.add("speed_run");
        break;
      }
    }
  }

  // Recent activity feed
  const activityItems = [
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
    totalXp,
    level,
    xpInto,
    xpRequired,
    xpPct,
    isMax,
    totalLessons,
    totalCertificates,
    totalEnrollments,
    streak,
    longestStreak,
    xpToday,
    xpWeek,
    weeklyXp,
    streakDays,
    earnedBadges,
    questDailyDone,
    questCertEarned,
    recentActivity: activityItems,
    hasAnyActivity: totalLessons + totalCertificates > 0,
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

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile(auth.getAccessToken),
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

  const progressLoading =
    progressQueries.length > 0 && progressQueries.some((q) => q.isPending);

  const firstError =
    profileQuery.error ??
    enrollmentsQuery.error ??
    certificatesQuery.error ??
    progressQueries.find((q) => q.error)?.error ??
    null;

  const isPending =
    profileQuery.isPending ||
    enrollmentsQuery.isPending ||
    certificatesQuery.isPending ||
    progressLoading;

  const state = useMemo(
    () =>
      deriveState(
        profileQuery.data,
        enrollments,
        progressQueries.map((q) => q.data),
        certificatesQuery.data ?? [],
      ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      profileQuery.data,
      enrollmentsQuery.data,
      certificatesQuery.data,
      progressQueries.map((q) => q.data).join("|"),
    ],
  );

  const session = auth.session;
  const shellUser = {
    displayName: session?.displayName ?? "EduLife learner",
    email: session?.email ?? "",
  };

  return (
    <AppShell
      active="level"
      user={shellUser}
      onLogout={auth.logout}
      header={
        <>
          <Link
            to="/dashboard"
            className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Home
          </Link>
          <div className="ml-auto flex items-center gap-3">
            <div
              className={`hidden sm:flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-semibold ${
                state.streak > 0
                  ? "bg-amber-50 border border-amber-200 dark:bg-amber-500/10 dark:border-amber-500/20 text-amber-500"
                  : "bg-muted border border-border text-muted-foreground"
              }`}
            >
              🔥 {state.streak} day streak
            </div>
          </div>
        </>
      }
    >
      <div className="mx-auto max-w-5xl px-5 lg:px-8 py-6 space-y-5">
        {firstError ? (
          <div className="rounded-3xl border border-destructive/20 bg-destructive/5 p-6 text-sm text-destructive">
            Couldn't load your progress.{" "}
            {firstError instanceof Error ? firstError.message : "Try again."}
          </div>
        ) : null}

        {isPending ? (
          <LevelSkeleton />
        ) : !state.hasAnyActivity ? (
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
    </AppShell>
  );
}
