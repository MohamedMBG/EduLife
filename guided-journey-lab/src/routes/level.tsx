import { createFileRoute, Link } from "@tanstack/react-router";
import { useQueries, useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { motion } from "framer-motion";
import {
  Award,
  BookOpen,
  Shield,
  Zap,
  Target,
  Trophy,
  Crown,
  Lock,
  Star,
  Flame,
  CheckCircle,
  ArrowLeft,
  TrendingUp,
  Swords,
  Scroll,
  Sparkles,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
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

export const Route = createFileRoute("/level")({
  component: LevelRoute,
  head: () => ({ meta: [{ title: "Level & Progress — EduLife" }] }),
});

// ─── XP economy ────────────────────────────────────────────────────────────────

const LESSON_XP = 50;
const CERT_XP = 500;
const LEVEL_THRESHOLDS = [
  0, 250, 600, 1100, 1800, 2700, 3900, 5500, 7500, 10000,
];

const LEVEL_PATH = [
  { n: 1, title: "Novice", icon: "⚪" },
  { n: 2, title: "Curious", icon: "🔵" },
  { n: 3, title: "Explorer", icon: "🟢" },
  { n: 4, title: "Seeker", icon: "🟡" },
  { n: 5, title: "Thinker", icon: "🟠" },
  { n: 6, title: "Achiever", icon: "🔴" },
  { n: 7, title: "Scholar", icon: "🟣" },
  { n: 8, title: "Expert", icon: "💎" },
  { n: 9, title: "Sage", icon: "⚡" },
  { n: 10, title: "Master", icon: "👑" },
];

type BadgeRarity = "common" | "rare" | "epic" | "legendary";

const BADGE_DEFS: {
  key: string;
  icon: React.ElementType;
  title: string;
  desc: string;
  rarity: BadgeRarity;
}[] = [
  { key: "first-flame", icon: Flame, title: "First Flame", desc: "Complete your first lesson", rarity: "common" },
  { key: "bookworm", icon: BookOpen, title: "Bookworm", desc: "Complete 10 lessons", rarity: "rare" },
  { key: "speed-run", icon: Zap, title: "Speed Run", desc: "3 lessons in one day", rarity: "rare" },
  { key: "sharp-mind", icon: Target, title: "Sharp Mind", desc: "Pass any exam", rarity: "epic" },
  { key: "graduate", icon: Award, title: "Graduate", desc: "Earn your first certificate", rarity: "epic" },
  { key: "on-a-roll", icon: TrendingUp, title: "On A Roll", desc: "5 lessons in a week", rarity: "common" },
  { key: "dedicated", icon: Shield, title: "Dedicated", desc: "14-day streak", rarity: "epic" },
  { key: "star-learner", icon: Star, title: "Star Learner", desc: "30-day streak", rarity: "legendary" },
  { key: "scholar", icon: Scroll, title: "Scholar", desc: "Reach level 7", rarity: "epic" },
  { key: "master", icon: Crown, title: "Master", desc: "Reach level 10", rarity: "legendary" },
  { key: "trophy-hunter", icon: Trophy, title: "Trophy Hunter", desc: "Earn 3 certificates", rarity: "legendary" },
  { key: "inferno", icon: Flame, title: "Inferno", desc: "60-day streak", rarity: "legendary" },
];

const rarityConfig: Record<
  BadgeRarity,
  { label: string; color: string; bg: string; border: string; glow: string }
> = {
  common: {
    label: "Common",
    color: "text-slate-500",
    bg: "bg-slate-50 dark:bg-slate-500/10",
    border: "border-slate-200 dark:border-slate-500/20",
    glow: "",
  },
  rare: {
    label: "Rare",
    color: "text-primary",
    bg: "bg-primary/6",
    border: "border-primary/20",
    glow: "shadow-[0_0_12px_-2px_oklch(0.50_0.21_145/0.2)]",
  },
  epic: {
    label: "Epic",
    color: "text-violet-500",
    bg: "bg-violet-50 dark:bg-violet-500/10",
    border: "border-violet-200 dark:border-violet-500/20",
    glow: "shadow-[0_0_14px_-2px_oklch(0.55_0.22_290/0.25)]",
  },
  legendary: {
    label: "Legendary",
    color: "text-amber-500",
    bg: "bg-amber-50 dark:bg-amber-500/10",
    border: "border-amber-200 dark:border-amber-500/20",
    glow: "shadow-[0_0_16px_-2px_oklch(0.78_0.14_80/0.3)]",
  },
};

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

interface LevelState {
  totalXp: number;
  level: number;
  xpInto: number;
  xpRequired: number;
  xpPct: number;
  isMax: boolean;
  totalLessons: number;
  totalCertificates: number;
  totalEnrollments: number;
  streak: number;
  longestStreak: number;
  xpToday: number;
  xpWeek: number;
  weeklyXp: { day: string; xp: number; date: Date }[];
  streakDays: { label: string; active: boolean; today: boolean }[];
  earnedBadges: Set<string>;
  questDailyDone: number;
  questCertEarned: boolean;
  recentActivity: {
    kind: "lesson" | "certificate";
    title: string;
    subtitle: string;
    date: Date;
    xp: number;
  }[];
  hasAnyActivity: boolean;
}

interface ActivityCollections {
  lessonCompletions: { date: Date; title: string; courseTitle: string | null }[];
  certificateEvents: { date: Date; title: string }[];
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

  // Progress endpoint is the source of truth for completion timestamps. Fall back to the
  // profile counter when no per-course progress has loaded yet so the rank card is not blank
  // for users with completed lessons in unloaded courses.
  const completionsCounted = lessonCompletions.length;
  const profileCompleted = profile?.completedLessons ?? 0;
  const totalLessons = Math.max(completionsCounted, profileCompleted);
  const totalCertificates = certificates.length;
  const totalEnrollments = profile?.enrolledCourses ?? enrollments.length;

  const totalXp = totalLessons * LESSON_XP + totalCertificates * CERT_XP;
  const { level, xpInto, xpRequired, isMax } = deriveLevel(totalXp);
  const xpPct = Math.min(100, Math.round((xpInto / xpRequired) * 100));

  const allDates: Date[] = [
    ...lessonCompletions.map((l) => l.date),
    ...certificateEvents.map((c) => c.date),
  ];
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
      xp: lessonsCount * LESSON_XP + certCount * CERT_XP,
      date,
    };
  });
  const xpWeek = weeklyXp.reduce((sum, d) => sum + d.xp, 0);

  const todayKey = startOfDay(new Date()).toDateString();
  const xpToday =
    lessonCompletions.filter(
      (l) => startOfDay(l.date).toDateString() === todayKey,
    ).length *
      LESSON_XP +
    certificateEvents.filter(
      (c) => startOfDay(c.date).toDateString() === todayKey,
    ).length *
      CERT_XP;
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

  // Badge derivation
  const earnedBadges = new Set<string>();
  if (totalLessons >= 1) earnedBadges.add("first-flame");
  if (totalLessons >= 10) earnedBadges.add("bookworm");
  if (totalCertificates >= 1) {
    earnedBadges.add("sharp-mind");
    earnedBadges.add("graduate");
  }
  if (totalCertificates >= 3) earnedBadges.add("trophy-hunter");
  if (longestStreak >= 14) earnedBadges.add("dedicated");
  if (longestStreak >= 30) earnedBadges.add("star-learner");
  if (longestStreak >= 60) earnedBadges.add("inferno");
  if (level >= 7) earnedBadges.add("scholar");
  if (level >= 10) earnedBadges.add("master");
  if (maxRolling7DayCount(lessonCompletions.map((l) => l.date)) >= 5) {
    earnedBadges.add("on-a-roll");
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
        earnedBadges.add("speed-run");
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
      xp: LESSON_XP,
    })),
    ...certificateEvents.map((c) => ({
      kind: "certificate" as const,
      title: `Certificate · ${c.title}`,
      subtitle: "Course completed",
      date: c.date,
      xp: CERT_XP,
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

// ─── Rank card ────────────────────────────────────────────────────────────────

function RankCard({
  state,
  displayName,
}: {
  state: LevelState;
  displayName: string;
}) {
  const currentLv = LEVEL_PATH[state.level - 1];
  const nextLv = state.isMax ? null : LEVEL_PATH[state.level];
  const xpRemaining = Math.max(0, state.xpRequired - state.xpInto);
  const earnedCount = state.earnedBadges.size;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.92 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
      className="relative rounded-3xl border border-primary/20 bg-gradient-to-br from-primary/8 via-background to-primary/4 p-8 overflow-hidden"
    >
      <div className="absolute -right-6 -top-6 opacity-[0.04] pointer-events-none">
        <Shield strokeWidth={0.5} className="w-48 h-48 text-primary" />
      </div>

      <div className="relative flex flex-col sm:flex-row sm:items-center gap-6">
        <div className="relative shrink-0">
          <motion.div
            className="absolute inset-[-8px] rounded-full border-2 border-primary/30"
            animate={{ scale: [1, 1.08, 1], opacity: [0.6, 0.15, 0.6] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />
          <motion.div
            className="absolute inset-[-18px] rounded-full border border-primary/15"
            animate={{ scale: [1, 1.1, 1], opacity: [0.4, 0.08, 0.4] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut", delay: 0.4 }}
          />

          <div className="relative w-28 h-28">
            <svg
              viewBox="0 0 112 112"
              className="absolute inset-0 w-full h-full"
              style={{ transform: "rotate(-90deg)" }}
            >
              <circle
                cx="56"
                cy="56"
                r="48"
                fill="none"
                strokeWidth="5"
                className="stroke-primary/12"
              />
              <motion.circle
                cx="56"
                cy="56"
                r="48"
                fill="none"
                strokeWidth="5"
                strokeLinecap="round"
                className="stroke-primary"
                strokeDasharray={`${2 * Math.PI * 48}`}
                initial={{ strokeDashoffset: 2 * Math.PI * 48 }}
                animate={{
                  strokeDashoffset:
                    2 * Math.PI * 48 * (1 - state.xpPct / 100),
                }}
                transition={{ duration: 2, delay: 0.5, ease: [0.22, 1, 0.36, 1] }}
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-display text-4xl leading-none text-foreground">
                {state.level}
              </span>
              <span className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-0.5">
                Level
              </span>
            </div>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-0.5">
            <span className="text-lg">{currentLv.icon}</span>
            <p className="text-xs uppercase tracking-[0.3em] font-semibold text-muted-foreground">
              {displayName.split(" ")[0]} · Current Rank
            </p>
          </div>
          <h1
            className="text-display leading-none text-foreground mb-1"
            style={{ fontSize: "clamp(2rem,5vw,3.5rem)" }}
          >
            {currentLv.title}
          </h1>
          <p className="text-sm text-muted-foreground mb-4">
            {state.totalXp.toLocaleString()} total XP
            <span className="mx-2 text-border">·</span>
            <span
              className={
                state.streak > 0
                  ? "text-amber-500 font-medium"
                  : "text-muted-foreground"
              }
            >
              🔥 {state.streak}-day streak
            </span>
          </p>

          <div className="max-w-sm">
            <div className="flex justify-between text-xs text-muted-foreground mb-1.5">
              <span>
                <span className="font-semibold text-foreground">
                  {state.xpInto.toLocaleString()}
                </span>
                <span> / {state.xpRequired.toLocaleString()} XP</span>
              </span>
              <span className="font-medium text-primary">
                {state.isMax ? "MAX" : `${state.xpPct}%`}
              </span>
            </div>
            <div className="relative h-3 rounded-full overflow-hidden bg-primary/10">
              {[25, 50, 75].map((pct) => (
                <div
                  key={pct}
                  className="absolute top-0 bottom-0 w-px bg-background/60 z-10"
                  style={{ left: `${pct}%` }}
                />
              ))}
              <motion.div
                className="absolute inset-y-0 left-0 rounded-full bg-gradient-primary"
                initial={{ width: 0 }}
                animate={{ width: `${state.xpPct}%` }}
                transition={{ duration: 1.8, delay: 0.6, ease: [0.22, 1, 0.36, 1] }}
              />
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              {state.isMax ? (
                <>Max rank reached. Keep stacking XP!</>
              ) : (
                <>
                  {xpRemaining.toLocaleString()} XP to{" "}
                  <strong className="text-foreground">{nextLv?.title}</strong>{" "}
                  {nextLv?.icon}
                </>
              )}
            </p>
          </div>
        </div>

        <div className="flex sm:flex-col gap-3">
          {[
            {
              label: "Lessons",
              value: state.totalLessons.toString(),
              icon: BookOpen,
              color: "text-primary",
            },
            {
              label: "This Week",
              value: `+${state.xpWeek}`,
              icon: Zap,
              color: "text-teal",
            },
            {
              label: "Badges",
              value: `${state.earnedBadges.size}/${BADGE_DEFS.length}`,
              icon: Shield,
              color: "text-amber-500",
            },
          ].map((s) => {
            const Icon = s.icon;
            return (
              <div
                key={s.label}
                className="flex items-center gap-2 rounded-2xl border border-border bg-surface-elevated px-3 py-2 min-w-[110px]"
              >
                <Icon
                  className={`h-4 w-4 shrink-0 ${s.color}`}
                  strokeWidth={1.75}
                />
                <div>
                  <p className="text-[10px] text-muted-foreground leading-none">
                    {s.label}
                  </p>
                  <p className="text-sm font-bold text-foreground leading-tight">
                    {s.value}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
      {/* dev-only: keep earnedCount referenced for future stat surface */}
      <span className="sr-only">{earnedCount} badges earned</span>
    </motion.div>
  );
}

// ─── Streak card ──────────────────────────────────────────────────────────────

function StreakCard({ state }: { state: LevelState }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.2 }}
      className="rounded-3xl border border-border bg-surface-elevated p-5"
    >
      <div className="flex items-center gap-2 mb-4">
        <Flame className="h-4 w-4 text-amber-500" strokeWidth={1.75} />
        <p className="text-xs uppercase tracking-[0.25em] font-semibold text-muted-foreground">
          This Week
        </p>
      </div>
      <div className="flex items-end gap-2">
        {state.streakDays.map((d, i) => (
          <div key={i} className="flex flex-col items-center gap-1.5">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.3 + i * 0.07, type: "spring", bounce: 0.4 }}
              className={`w-9 h-9 rounded-xl flex items-center justify-center text-base transition-all ${
                d.active
                  ? "bg-amber-50 border-2 border-amber-300 dark:bg-amber-500/15 dark:border-amber-500/40"
                  : "bg-muted border-2 border-border"
              } ${d.today ? "ring-2 ring-primary ring-offset-1" : ""}`}
            >
              {d.active ? "🔥" : "·"}
            </motion.div>
            <span className="text-[10px] text-muted-foreground font-medium">
              {d.label}
            </span>
          </div>
        ))}
      </div>
      <div className="mt-4 pt-4 border-t border-border flex items-center justify-between text-xs">
        <span className="text-muted-foreground">Longest</span>
        <span className="font-bold text-foreground">
          {state.longestStreak} days {state.longestStreak >= 7 ? "🏆" : ""}
        </span>
      </div>
    </motion.div>
  );
}

// ─── Quests card ──────────────────────────────────────────────────────────────

function QuestsCard({ state }: { state: LevelState }) {
  const quests = [
    {
      icon: Swords,
      title: "Daily Warrior",
      desc: "Complete 3 lessons today",
      progress: state.questDailyDone,
      total: 3,
      xp: 50,
      color: "text-primary",
      bg: "bg-primary/8 border-primary/20",
    },
    {
      icon: Scroll,
      title: "Knowledge Seeker",
      desc: "Earn any course certificate",
      progress: state.questCertEarned ? 1 : 0,
      total: 1,
      xp: 75,
      color: "text-amber-500",
      bg: "bg-amber-50 border-amber-200 dark:bg-amber-500/10 dark:border-amber-500/20",
    },
    {
      icon: Sparkles,
      title: "Streak Keeper",
      desc: "Study 7 days in a row",
      progress: Math.min(state.streak, 7),
      total: 7,
      xp: 120,
      color: "text-teal",
      bg: "bg-teal/8 border-teal/20",
    },
  ];

  const doneCount = quests.filter((q) => q.progress >= q.total).length;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.25 }}
      className="rounded-3xl border border-border bg-surface-elevated p-5"
    >
      <div className="flex items-center gap-2 mb-4">
        <Swords className="h-4 w-4 text-primary" strokeWidth={1.75} />
        <p className="text-xs uppercase tracking-[0.25em] font-semibold text-muted-foreground">
          Quests
        </p>
        <span className="ml-auto text-xs font-semibold text-primary bg-primary/8 border border-primary/20 rounded-full px-2 py-0.5">
          {doneCount}/{quests.length} done
        </span>
      </div>
      <div className="space-y-3">
        {quests.map((q, i) => {
          const Icon = q.icon;
          const pct = Math.round((q.progress / q.total) * 100);
          const done = q.progress >= q.total;
          return (
            <motion.div
              key={q.title}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.35 + i * 0.08 }}
              className={`flex items-center gap-3 rounded-2xl border p-3 transition-all ${
                done ? "opacity-60" : ""
              } ${q.bg}`}
            >
              <div className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-background/80">
                <Icon className={`h-4 w-4 ${q.color}`} strokeWidth={1.75} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-xs font-semibold text-foreground">{q.title}</p>
                  {done && <CheckCircle className="h-3 w-3 text-teal shrink-0" />}
                </div>
                <div className="h-1.5 rounded-full bg-background/60 overflow-hidden">
                  <motion.div
                    className={`h-full rounded-full ${q.color.replace("text-", "bg-")}`}
                    initial={{ width: 0 }}
                    animate={{ width: `${pct}%` }}
                    transition={{ duration: 0.9, delay: 0.5 + i * 0.1 }}
                  />
                </div>
                <p className="text-[10px] text-muted-foreground mt-0.5">
                  {q.desc} · {q.progress}/{q.total}
                </p>
              </div>
              <div className="shrink-0 text-right">
                <span className={`text-xs font-bold ${q.color}`}>+{q.xp}</span>
                <p className="text-[9px] text-muted-foreground">XP</p>
              </div>
            </motion.div>
          );
        })}
      </div>
    </motion.div>
  );
}

// ─── Skill tree ───────────────────────────────────────────────────────────────

function SkillTreeCard({ state }: { state: LevelState }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.3 }}
      className="rounded-3xl border border-border bg-surface-elevated p-6 overflow-hidden"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Scroll className="h-4 w-4 text-primary" strokeWidth={1.75} />
          <p className="text-xs uppercase tracking-[0.28em] font-semibold text-muted-foreground">
            Skill Tree
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <div className="h-2.5 w-2.5 rounded-full bg-gradient-primary" />
            Mastered
          </div>
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <div className="h-2.5 w-2.5 rounded-full border-2 border-primary bg-primary/10" />
            Current
          </div>
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <div className="h-2.5 w-2.5 rounded-full bg-muted border border-border" />
            Locked
          </div>
        </div>
      </div>

      <div className="overflow-x-auto pb-1">
        <div className="relative min-w-max" style={{ height: 180 }}>
          {LEVEL_PATH.map((lv, i) => {
            const done = lv.n < state.level;
            const current = lv.n === state.level;
            const locked = lv.n > state.level;
            const top = i % 2 === 0 ? 16 : 88;
            const x = i * 96 + 8;

            return (
              <div key={lv.n}>
                {i < LEVEL_PATH.length - 1 && (
                  <svg
                    className="absolute pointer-events-none"
                    style={{ left: x + 28, top: 0, width: 96, height: 180, overflow: "visible" }}
                    viewBox="0 0 96 180"
                  >
                    <path
                      d={`M 28 ${top + 28} C 62 ${top + 28} 34 ${
                        (i + 1) % 2 === 0 ? 44 : 116
                      } 68 ${(i + 1) % 2 === 0 ? 44 : 116}`}
                      fill="none"
                      strokeWidth="2"
                      strokeDasharray="5 4"
                      className={done ? "stroke-primary/40" : "stroke-border"}
                    />
                  </svg>
                )}

                <motion.div
                  initial={{ opacity: 0, scale: 0.5 }}
                  animate={{ opacity: 1, scale: current ? 1.12 : 1 }}
                  transition={{ duration: 0.45, delay: 0.1 + i * 0.07, ease: [0.22, 1, 0.36, 1] }}
                  className="absolute flex flex-col items-center gap-1.5"
                  style={{ left: x, top, width: 64 }}
                >
                  {done && (
                    <div className="text-[9px] font-bold text-primary bg-primary/10 rounded-full px-1.5 py-0.5 -mb-0.5">
                      ✓ done
                    </div>
                  )}
                  {current && (
                    <div className="text-[9px] font-bold text-primary animate-pulse -mb-0.5">
                      ▶ you
                    </div>
                  )}
                  {locked && (
                    <div className="text-[9px] text-muted-foreground/40 -mb-0.5 invisible">·</div>
                  )}

                  <div className="relative">
                    {current && (
                      <>
                        <motion.div
                          className="absolute inset-[-8px] rounded-2xl border-2 border-primary/30"
                          animate={{ scale: [1, 1.15, 1], opacity: [0.7, 0.15, 0.7] }}
                          transition={{ duration: 2.5, repeat: Infinity }}
                        />
                        <motion.div
                          className="absolute inset-[-14px] rounded-2xl border border-primary/15"
                          animate={{ scale: [1, 1.2, 1], opacity: [0.4, 0.05, 0.4] }}
                          transition={{ duration: 2.5, repeat: Infinity, delay: 0.3 }}
                        />
                      </>
                    )}
                    <div
                      className={`relative grid h-14 w-14 rounded-2xl border-2 place-items-center text-2xl transition-all ${
                        done
                          ? "bg-gradient-primary border-primary/30 shadow-[0_4px_16px_-2px_oklch(0.50_0.21_145/0.35)]"
                          : current
                          ? "bg-primary/8 border-primary shadow-[0_4px_16px_-2px_oklch(0.50_0.21_145/0.3)]"
                          : "bg-muted/60 border-border/60"
                      }`}
                    >
                      {locked ? (
                        <Lock
                          className="h-5 w-5 text-muted-foreground/30"
                          strokeWidth={1.5}
                        />
                      ) : (
                        <span>{lv.icon}</span>
                      )}
                      <div
                        className={`absolute -bottom-1.5 -right-1.5 grid h-5 w-5 place-items-center rounded-full text-[9px] font-bold border ${
                          done || current
                            ? "bg-primary text-primary-foreground border-primary/30"
                            : "bg-muted text-muted-foreground/40 border-border"
                        }`}
                      >
                        {lv.n}
                      </div>
                    </div>
                  </div>

                  <p
                    className={`text-[10px] font-bold leading-none text-center ${
                      done
                        ? "text-primary"
                        : current
                        ? "text-foreground"
                        : "text-muted-foreground/35"
                    }`}
                  >
                    {lv.title}
                  </p>
                </motion.div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="mt-4 flex items-center gap-3">
        <div className="flex-1 h-1.5 rounded-full bg-primary/10 overflow-hidden">
          <motion.div
            className="h-full rounded-full bg-gradient-primary"
            initial={{ width: 0 }}
            animate={{
              width: `${((state.level - 1) / LEVEL_PATH.length) * 100}%`,
            }}
            transition={{ duration: 1.2, delay: 0.8, ease: [0.22, 1, 0.36, 1] }}
          />
        </div>
        <span className="text-xs font-semibold text-primary shrink-0">
          {state.level - 1}/{LEVEL_PATH.length}
        </span>
      </div>
    </motion.div>
  );
}

// ─── Weekly XP chart ──────────────────────────────────────────────────────────

function WeeklyXpCard({ state }: { state: LevelState }) {
  const maxDayXp = Math.max(0, ...state.weeklyXp.map((d) => d.xp));

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.35 }}
      className="rounded-3xl border border-border bg-surface-elevated p-6"
    >
      <div className="flex items-start justify-between mb-6">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] font-semibold text-muted-foreground mb-1">
            XP this week
          </p>
          <p className="text-display text-4xl leading-none text-foreground">
            +{state.xpWeek}
          </p>
        </div>
        <span className="flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold bg-teal/8 text-teal border border-teal/20">
          <TrendingUp className="h-3 w-3" />
          {state.xpToday > 0 ? `+${state.xpToday} today` : "no XP today"}
        </span>
      </div>
      <ResponsiveContainer width="100%" height={150}>
        <BarChart data={state.weeklyXp} barSize={24} barCategoryGap="30%">
          <defs>
            <linearGradient id="barActive" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="oklch(0.50 0.21 142)" stopOpacity={1} />
              <stop offset="100%" stopColor="oklch(0.34 0.16 148)" stopOpacity={0.7} />
            </linearGradient>
            <linearGradient id="barInactive" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="oklch(0.50 0.21 142)" stopOpacity={0.18} />
              <stop offset="100%" stopColor="oklch(0.34 0.16 148)" stopOpacity={0.04} />
            </linearGradient>
          </defs>
          <XAxis
            dataKey="day"
            axisLine={false}
            tickLine={false}
            tick={{ fontSize: 11, fill: "oklch(0.5 0.02 145)", fontFamily: "Figtree" }}
            dy={6}
          />
          <YAxis hide />
          <Tooltip
            cursor={false}
            content={({ active, payload }) =>
              active && payload?.length ? (
                <div className="rounded-xl px-3 py-2 text-xs font-semibold bg-surface-elevated border border-border text-foreground shadow-elevated">
                  +{payload[0].value} XP
                </div>
              ) : null
            }
          />
          <Bar dataKey="xp" radius={[6, 6, 3, 3]}>
            {state.weeklyXp.map((entry, i) => (
              <Cell
                key={i}
                fill={
                  entry.xp > 0 && entry.xp === maxDayXp
                    ? "url(#barActive)"
                    : "url(#barInactive)"
                }
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </motion.div>
  );
}

// ─── Recent activity ──────────────────────────────────────────────────────────

function formatRelative(date: Date) {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const days = Math.floor(diffMs / 86400000);
  if (days <= 0) return "today";
  if (days === 1) return "1 day ago";
  if (days < 7) return `${days} days ago`;
  if (days < 30) return `${Math.floor(days / 7)} wk ago`;
  return date.toLocaleDateString();
}

function RecentActivityCard({ state }: { state: LevelState }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.4 }}
      className="rounded-3xl border border-border bg-surface-elevated overflow-hidden"
    >
      <div className="flex items-center justify-between px-5 py-4 border-b border-border">
        <div className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-amber-500" strokeWidth={1.75} />
          <p className="text-xs uppercase tracking-[0.2em] font-semibold text-muted-foreground">
            Recent Activity
          </p>
        </div>
        <span className="text-xs text-muted-foreground">+{state.totalXp} XP</span>
      </div>

      {state.recentActivity.length === 0 ? (
        <div className="px-5 py-8 text-xs text-muted-foreground text-center">
          No activity yet. Finish a lesson to start earning XP.
        </div>
      ) : (
        <div className="divide-y divide-border">
          {state.recentActivity.map((item, i) => {
            const Icon = item.kind === "certificate" ? Award : CheckCircle;
            const accent =
              item.kind === "certificate" ? "text-amber-500" : "text-primary";
            return (
              <motion.div
                key={`${item.kind}-${item.date.getTime()}-${i}`}
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.3, delay: 0.45 + i * 0.04 }}
                className="flex items-center gap-3 px-4 py-3"
              >
                <div
                  className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl bg-muted ${accent}`}
                >
                  <Icon className="h-4 w-4" strokeWidth={1.75} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-semibold truncate text-foreground">
                    {item.title}
                  </p>
                  <p className="text-[10px] text-muted-foreground/70 truncate">
                    {item.subtitle} · {formatRelative(item.date)}
                  </p>
                </div>
                <p className={`text-xs font-bold tabular-nums ${accent}`}>
                  +{item.xp}
                </p>
              </motion.div>
            );
          })}
        </div>
      )}
    </motion.div>
  );
}

// ─── Achievements ─────────────────────────────────────────────────────────────

function AchievementsCard({ state }: { state: LevelState }) {
  const earnedCount = state.earnedBadges.size;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.45 }}
    >
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-amber-500" strokeWidth={1.75} />
          <p className="text-xs uppercase tracking-[0.28em] font-semibold text-muted-foreground">
            Achievements
          </p>
        </div>
        <div className="flex items-center gap-3">
          {(["common", "rare", "epic", "legendary"] as BadgeRarity[]).map((r) => (
            <span
              key={r}
              className={`hidden sm:inline text-[10px] font-semibold ${rarityConfig[r].color}`}
            >
              {rarityConfig[r].label}
            </span>
          ))}
          <span className="text-xs text-muted-foreground">
            <span className="font-bold text-foreground">{earnedCount}</span>/
            {BADGE_DEFS.length}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
        {BADGE_DEFS.map((badge, i) => {
          const earned = state.earnedBadges.has(badge.key);
          const Icon = badge.icon;
          const r = rarityConfig[badge.rarity];
          return (
            <motion.div
              key={badge.key}
              initial={{ opacity: 0, scale: 0.88 }}
              animate={{ opacity: earned ? 1 : 0.4, scale: 1 }}
              transition={{ duration: 0.35, delay: 0.08 + i * 0.03 }}
              whileHover={earned ? { scale: 1.04, y: -3 } : {}}
              className={`relative flex flex-col gap-3 rounded-2xl border p-4 transition-all ${
                earned ? `${r.bg} ${r.border} ${r.glow}` : "bg-muted/30 border-border"
              }`}
            >
              <div className="flex items-center justify-between">
                <div
                  className={`grid h-9 w-9 place-items-center rounded-xl ${
                    earned ? "bg-background/80" : "bg-muted"
                  }`}
                >
                  <Icon
                    className={`h-4 w-4 ${earned ? r.color : "text-muted-foreground"}`}
                    strokeWidth={1.75}
                  />
                </div>
                <span
                  className={`text-[9px] uppercase tracking-wider font-bold ${
                    earned ? r.color : "text-muted-foreground/40"
                  }`}
                >
                  {badge.rarity}
                </span>
              </div>
              <div>
                <p
                  className={`text-xs font-bold leading-tight ${
                    earned ? "text-foreground" : "text-muted-foreground"
                  }`}
                >
                  {badge.title}
                </p>
                <p className="text-[10px] mt-0.5 leading-tight text-muted-foreground/60">
                  {badge.desc}
                </p>
              </div>
              {earned ? (
                <CheckCircle
                  className={`absolute top-2.5 right-2.5 h-3 w-3 ${r.color} opacity-70`}
                />
              ) : (
                <Lock className="absolute top-2.5 right-2.5 h-3 w-3 text-muted-foreground/30" />
              )}
            </motion.div>
          );
        })}
      </div>
    </motion.div>
  );
}

// ─── Loading / empty states ───────────────────────────────────────────────────

function LevelSkeleton() {
  return (
    <div className="space-y-5">
      <div className="rounded-3xl border border-border bg-surface-elevated h-48 animate-pulse" />
      <div className="grid md:grid-cols-2 gap-4">
        <div className="rounded-3xl border border-border bg-surface-elevated h-40 animate-pulse" />
        <div className="rounded-3xl border border-border bg-surface-elevated h-40 animate-pulse" />
      </div>
      <div className="rounded-3xl border border-border bg-surface-elevated h-56 animate-pulse" />
      <div className="grid lg:grid-cols-[1fr_360px] gap-4">
        <div className="rounded-3xl border border-border bg-surface-elevated h-56 animate-pulse" />
        <div className="rounded-3xl border border-border bg-surface-elevated h-56 animate-pulse" />
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated p-10 text-center">
      <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-primary/10 text-primary mb-4">
        <Shield className="h-6 w-6" strokeWidth={1.5} />
      </div>
      <h2 className="text-display text-2xl text-foreground mb-2">
        Start earning XP
      </h2>
      <p className="text-sm text-muted-foreground max-w-sm mx-auto mb-5">
        Finish your first lesson to unlock levels, streaks, and achievements.
        Every completed lesson is worth {LESSON_XP} XP and each certificate adds{" "}
        {CERT_XP} XP.
      </p>
      <Link
        to="/explore"
        className="inline-flex items-center gap-2 rounded-2xl bg-gradient-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-glow"
      >
        Explore courses
      </Link>
    </div>
  );
}
