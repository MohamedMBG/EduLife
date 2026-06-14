import {
  Award,
  BookOpen,
  Shield,
  Zap,
  Target,
  Trophy,
  Crown,
  Star,
  Flame,
  Scroll,
  Sparkles,
  Compass,
  Brain,
  Gem,
  Lightbulb,
  TrendingUp,
} from "lucide-react";

// Gamification XP values — shared spec.
// Must match Android XpEngine constants. See root CLAUDE.md → ## Gamification (Shared Spec).
export const XP_LESSON_COMPLETE = 25;
export const XP_COURSE_COMPLETE = 100;
export const XP_EXAM_PASS = 150;
export const XP_CERTIFICATE = 200;
export const XP_ENROLLMENT = 10;
export const XP_DAILY_LOGIN = 5;
export const XP_STREAK_3_BONUS = 30;
export const XP_STREAK_7_BONUS = 75;

// A certificate issuance triggers three events server-side: course complete + exam pass + cert.
// Web has only the certificate signal, so it awards the bundled XP at cert time.
export const XP_PER_CERTIFICATE_BUNDLE =
  XP_CERTIFICATE + XP_EXAM_PASS + XP_COURSE_COMPLETE;

// Legacy aliases retained for backwards compatibility within this file.
export const LESSON_XP = XP_LESSON_COMPLETE;
export const CERT_XP = XP_PER_CERTIFICATE_BUNDLE;

export const LEVEL_THRESHOLDS = [
  0, 250, 600, 1100, 1800, 2700, 3900, 5500, 7500, 10000,
];

export const LEVEL_PATH = [
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

export const LEVEL_METADATA: Record<
  number,
  {
    icon: React.ComponentType<{ className?: string; strokeWidth?: number }>;
    gradient: string;
    glow: string;
    textColor: string;
    description: string;
    colorFrom: string;
    colorTo: string;
  }
> = {
  1: {
    icon: BookOpen,
    gradient: "from-slate-400 to-slate-600 dark:from-slate-500 dark:to-slate-700",
    glow: "shadow-[0_0_12px_rgba(100,116,139,0.35)]",
    textColor: "text-slate-500 dark:text-slate-400",
    description: "Embark on your learning journey. Build foundational knowledge.",
    colorFrom: "#94a3b8",
    colorTo: "#475569",
  },
  2: {
    icon: Lightbulb,
    gradient: "from-sky-400 to-blue-500 dark:from-sky-500 dark:to-blue-600",
    glow: "shadow-[0_0_12px_rgba(14,165,233,0.35)]",
    textColor: "text-sky-500 dark:text-sky-400",
    description: "Feed your curiosity with introductory topics and new ideas.",
    colorFrom: "#38bdf8",
    colorTo: "#3b82f6",
  },
  3: {
    icon: Compass,
    gradient: "from-emerald-400 to-teal-600 dark:from-emerald-500 dark:to-teal-700",
    glow: "shadow-[0_0_12px_rgba(16,185,129,0.35)]",
    textColor: "text-emerald-500 dark:text-emerald-400",
    description: "Explore intermediate concepts and expand your horizons.",
    colorFrom: "#34d399",
    colorTo: "#0d9488",
  },
  4: {
    icon: Shield,
    gradient: "from-amber-400 to-orange-500 dark:from-amber-500 dark:to-orange-600",
    glow: "shadow-[0_0_12px_rgba(245,158,11,0.35)]",
    textColor: "text-amber-500 dark:text-amber-400",
    description: "Seek deep understanding and guard your growing knowledge.",
    colorFrom: "#fbbf24",
    colorTo: "#f97316",
  },
  5: {
    icon: Brain,
    gradient: "from-orange-400 to-red-500 dark:from-orange-500 dark:to-red-600",
    glow: "shadow-[0_0_12px_rgba(249,115,22,0.35)]",
    textColor: "text-orange-500 dark:text-orange-400",
    description: "Think critically, connecting complex dots and reasoning.",
    colorFrom: "#fb923c",
    colorTo: "#ef4444",
  },
  6: {
    icon: Target,
    gradient: "from-red-400 to-rose-600 dark:from-red-500 dark:to-rose-700",
    glow: "shadow-[0_0_12px_rgba(239,68,68,0.35)]",
    textColor: "text-red-500 dark:text-red-400",
    description: "Achieve mastery through goal-oriented focus and projects.",
    colorFrom: "#f87171",
    colorTo: "#e11d48",
  },
  7: {
    icon: Scroll,
    gradient: "from-violet-400 to-fuchsia-600 dark:from-violet-500 dark:to-fuchsia-700",
    glow: "shadow-[0_0_12px_rgba(139,92,246,0.35)]",
    textColor: "text-violet-500 dark:text-violet-400",
    description: "Showcase academic expertise and dive deep into resources.",
    colorFrom: "#a78bfa",
    colorTo: "#c026d3",
  },
  8: {
    icon: Gem,
    gradient: "from-cyan-400 to-indigo-600 dark:from-cyan-500 dark:to-indigo-700",
    glow: "shadow-[0_0_15px_rgba(6,182,212,0.4)]",
    textColor: "text-cyan-500 dark:text-cyan-400",
    description: "Command advanced domains as a certified industry expert.",
    colorFrom: "#22d3ee",
    colorTo: "#4f46e5",
  },
  9: {
    icon: Zap,
    gradient: "from-yellow-400 to-amber-500 dark:from-yellow-500 dark:to-amber-600",
    glow: "shadow-[0_0_15px_rgba(234,179,8,0.45)]",
    textColor: "text-yellow-500 dark:text-yellow-400",
    description: "Harness lightning-fast intuition and sage-like wisdom.",
    colorFrom: "#facc15",
    colorTo: "#f59e0b",
  },
  10: {
    icon: Crown,
    gradient: "from-amber-400 via-yellow-500 to-purple-600 dark:from-amber-500 dark:via-yellow-600 dark:to-purple-700",
    glow: "shadow-[0_0_18px_rgba(245,158,11,0.5)]",
    textColor: "text-amber-500 dark:text-amber-400",
    description: "Achieve the ultimate pinnacle of educational mastery.",
    colorFrom: "#fbbf24",
    colorTo: "#9333ea",
  },
};

export type BadgeRarity = "common" | "rare" | "epic" | "legendary";

export const BADGE_DEFS: {
  key: string;
  icon: React.ElementType;
  title: string;
  desc: string;
  rarity: BadgeRarity;
}[] = [
  { key: "first_flame", icon: Flame, title: "First Flame", desc: "Complete your first lesson", rarity: "common" },
  { key: "bookworm", icon: BookOpen, title: "Bookworm", desc: "Complete 10 lessons", rarity: "rare" },
  { key: "speed_run", icon: Zap, title: "Speed Run", desc: "3 lessons in one day", rarity: "rare" },
  { key: "sharp_mind", icon: Target, title: "Sharp Mind", desc: "Pass any exam", rarity: "epic" },
  { key: "graduate", icon: Award, title: "Graduate", desc: "Earn your first certificate", rarity: "epic" },
  { key: "on_a_roll", icon: TrendingUp, title: "On A Roll", desc: "5 lessons in a week", rarity: "common" },
  { key: "dedicated", icon: Shield, title: "Dedicated", desc: "14-day streak", rarity: "epic" },
  { key: "star_learner", icon: Star, title: "Star Learner", desc: "30-day streak", rarity: "legendary" },
  { key: "scholar", icon: Scroll, title: "Scholar", desc: "Reach level 7", rarity: "epic" },
  { key: "master", icon: Crown, title: "Master", desc: "Reach level 10", rarity: "legendary" },
  { key: "trophy_hunter", icon: Trophy, title: "Trophy Hunter", desc: "Earn 3 certificates", rarity: "legendary" },
  { key: "inferno", icon: Flame, title: "Inferno", desc: "60-day streak", rarity: "legendary" },
];

export const rarityConfig: Record<
  BadgeRarity,
  { label: string; color: string; bg: string; border: string; glow: string }
> = {
  common: {
    label: "Common",
    color: "text-slate-500 dark:text-slate-400",
    bg: "bg-slate-500/5",
    border: "border-slate-500/10",
    glow: "",
  },
  rare: {
    label: "Rare",
    color: "text-primary dark:text-primary",
    bg: "bg-primary/5",
    border: "border-primary/10",
    glow: "shadow-[0_0_12px_rgba(20,120,60,0.1)]",
  },
  epic: {
    label: "Epic",
    color: "text-violet-500 dark:text-violet-400",
    bg: "bg-violet-500/5",
    border: "border-violet-500/10",
    glow: "shadow-[0_0_12px_rgba(139,92,246,0.12)]",
  },
  legendary: {
    label: "Legendary",
    color: "text-amber-500 dark:text-amber-400",
    bg: "bg-amber-500/5",
    border: "border-amber-500/15",
    glow: "shadow-gold shadow-[0_0_16px_rgba(245,158,11,0.15)]",
  },
};

export interface LevelState {
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

export interface ActivityCollections {
  lessonCompletions: { date: Date; title: string; courseTitle: string | null }[];
  certificateEvents: { date: Date; title: string }[];
}
