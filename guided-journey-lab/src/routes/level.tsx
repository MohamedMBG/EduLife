import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import {
  GraduationCap, Home, BookOpen, Compass, Award, Settings,
  LogOut, Shield, Zap, Target, Trophy, Crown, Lock,
  Star, Flame, CheckCircle, ArrowLeft, TrendingUp, Menu, X,
  ChevronUp, ChevronDown, Minus, Swords, Scroll, Sparkles,
} from "lucide-react";
import { useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from "recharts";

export const Route = createFileRoute("/level")({
  component: LevelPage,
  head: () => ({ meta: [{ title: "Level & Progress — EduLife" }] }),
});

// ─── Data ─────────────────────────────────────────────────────────────────────

const user = { name: "Mohamed Baghdadi", email: "m.baghdadi@example.com", initials: "MB" };

const levelData = {
  current: 7, xp: 2340, xpRequired: 3000,
  xpToday: 120, xpWeek: 680, totalXp: 12340,
  streak: 14, longestStreak: 21,
};

const levelPath = [
  { n: 1,  title: "Novice",   icon: "⚪" },
  { n: 2,  title: "Curious",  icon: "🔵" },
  { n: 3,  title: "Explorer", icon: "🟢" },
  { n: 4,  title: "Seeker",   icon: "🟡" },
  { n: 5,  title: "Thinker",  icon: "🟠" },
  { n: 6,  title: "Achiever", icon: "🔴" },
  { n: 7,  title: "Scholar",  icon: "🟣" },
  { n: 8,  title: "Expert",   icon: "💎" },
  { n: 9,  title: "Sage",     icon: "⚡" },
  { n: 10, title: "Master",   icon: "👑" },
];

const leaderboard = [
  { rank: 1, name: "Salma Benali",      initials: "SB", level: 9, title: "Sage",     weeklyXp: 1240, delta: 0,  isUser: false },
  { rank: 2, name: "Youssef Amrani",   initials: "YA", level: 8, title: "Expert",   weeklyXp: 980,  delta: 1,  isUser: false },
  { rank: 3, name: "Mohamed Baghdadi", initials: "MB", level: 7, title: "Scholar",  weeklyXp: 680,  delta: -1, isUser: true  },
  { rank: 4, name: "Fatima Zahra",     initials: "FZ", level: 6, title: "Achiever", weeklyXp: 540,  delta: 2,  isUser: false },
  { rank: 5, name: "Omar Kettani",     initials: "OK", level: 5, title: "Thinker",  weeklyXp: 420,  delta: 0,  isUser: false },
  { rank: 6, name: "Nadia Bensalem",   initials: "NB", level: 4, title: "Seeker",   weeklyXp: 210,  delta: -1, isUser: false },
  { rank: 7, name: "Karim Hajji",      initials: "KH", level: 3, title: "Explorer", weeklyXp: 160,  delta: 0,  isUser: false },
  { rank: 8, name: "Amina Cherkaoui",  initials: "AC", level: 2, title: "Curious",  weeklyXp: 90,   delta: -2, isUser: false },
];

type BadgeRarity = "common" | "rare" | "epic" | "legendary";

const badges: {
  icon: React.ElementType;
  title: string;
  desc: string;
  earned: boolean;
  rarity: BadgeRarity;
}[] = [
  { icon: Flame,       title: "First Flame",   desc: "Complete your first lesson",  earned: true,  rarity: "common"    },
  { icon: BookOpen,    title: "Bookworm",       desc: "Complete 10 lessons",         earned: true,  rarity: "rare"      },
  { icon: Zap,         title: "Speed Run",      desc: "3 lessons in one day",        earned: true,  rarity: "rare"      },
  { icon: Target,      title: "Sharp Mind",     desc: "Score 90%+ on an exam",       earned: true,  rarity: "epic"      },
  { icon: Award,       title: "Graduate",       desc: "Earn your first certificate", earned: false, rarity: "epic"      },
  { icon: Star,        title: "Star Learner",   desc: "Maintain a 30-day streak",    earned: false, rarity: "legendary" },
  { icon: Trophy,      title: "Top Score",      desc: "Rank #1 on the leaderboard",  earned: false, rarity: "legendary" },
  { icon: Crown,       title: "Master",         desc: "Reach level 10",              earned: false, rarity: "legendary" },
  { icon: TrendingUp,  title: "On A Roll",      desc: "5 lessons in a week",         earned: false, rarity: "common"    },
  { icon: CheckCircle, title: "Perfectionist",  desc: "Score 100% on any exam",      earned: false, rarity: "rare"      },
  { icon: Shield,      title: "Dedicated",      desc: "30 consecutive days",         earned: false, rarity: "epic"      },
  { icon: Flame,       title: "Inferno",        desc: "60-day streak",               earned: false, rarity: "legendary" },
];

const quests = [
  { icon: Swords,   title: "Daily Warrior",  desc: "Complete 3 lessons today",  progress: 2, total: 3, xp: 50,  color: "text-primary",   bg: "bg-primary/8 border-primary/20"   },
  { icon: Scroll,   title: "Knowledge Seeker", desc: "Finish 1 quiz with 80%+", progress: 0, total: 1, xp: 75,  color: "text-amber-500", bg: "bg-amber-50 border-amber-200 dark:bg-amber-500/10 dark:border-amber-500/20" },
  { icon: Sparkles, title: "Streak Keeper",  desc: "Study 7 days in a row",     progress: 6, total: 7, xp: 120, color: "text-teal",      bg: "bg-teal/8 border-teal/20"         },
];

const weeklyXp = [
  { day: "M", xp: 80  },
  { day: "T", xp: 120 },
  { day: "W", xp: 60  },
  { day: "T", xp: 200 },
  { day: "F", xp: 40  },
  { day: "S", xp: 140 },
  { day: "S", xp: 40  },
];

const streakDays = ["M","T","W","T","F","S","S"];

const navLinks = [
  { icon: Home,     label: "Home",         to: "/dashboard" as const },
  { icon: BookOpen, label: "My Courses",   to: "/courses"   as const },
  { icon: Compass,  label: "Explore",      to: "/explore"   as const },
  { icon: Award,    label: "Certificates", to: "/certificates" as const },
  { icon: Settings, label: "Settings",     to: "/profile"      as const },
];

const rarityConfig: Record<BadgeRarity, { label: string; color: string; bg: string; border: string; glow: string }> = {
  common:    { label: "Common",    color: "text-slate-500",  bg: "bg-slate-50 dark:bg-slate-500/10",     border: "border-slate-200 dark:border-slate-500/20", glow: "" },
  rare:      { label: "Rare",      color: "text-primary",    bg: "bg-primary/6",                         border: "border-primary/20",                         glow: "shadow-[0_0_12px_-2px_oklch(0.50_0.21_145/0.2)]" },
  epic:      { label: "Epic",      color: "text-violet-500", bg: "bg-violet-50 dark:bg-violet-500/10",   border: "border-violet-200 dark:border-violet-500/20", glow: "shadow-[0_0_14px_-2px_oklch(0.55_0.22_290/0.25)]" },
  legendary: { label: "Legendary", color: "text-amber-500",  bg: "bg-amber-50 dark:bg-amber-500/10",     border: "border-amber-200 dark:border-amber-500/20",  glow: "shadow-[0_0_16px_-2px_oklch(0.78_0.14_80/0.3)]"  },
};

const xpPct       = Math.round((levelData.xp / levelData.xpRequired) * 100);
const currentLv   = levelPath[levelData.current - 1];
const nextLv      = levelPath[levelData.current];
const maxDayXp    = Math.max(...weeklyXp.map(d => d.xp));
const earnedCount = badges.filter(b => b.earned).length;

// ─── Rank Card ────────────────────────────────────────────────────────────────

function RankCard() {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.92 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
      className="relative rounded-3xl border border-primary/20 bg-gradient-to-br from-primary/8 via-background to-primary/4 p-8 overflow-hidden"
    >
      {/* Decorative bg shield */}
      <div className="absolute -right-6 -top-6 opacity-[0.04] pointer-events-none">
        <Shield strokeWidth={0.5} className="w-48 h-48 text-primary" />
      </div>

      <div className="relative flex flex-col sm:flex-row sm:items-center gap-6">
        {/* Level circle */}
        <div className="relative shrink-0">
          {/* Pulse ring */}
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

          {/* SVG ring */}
          <div className="relative w-28 h-28">
            <svg viewBox="0 0 112 112" className="absolute inset-0 w-full h-full" style={{ transform: "rotate(-90deg)" }}>
              <circle cx="56" cy="56" r="48" fill="none" strokeWidth="5" className="stroke-primary/12" />
              <motion.circle
                cx="56" cy="56" r="48"
                fill="none" strokeWidth="5"
                strokeLinecap="round"
                className="stroke-primary"
                strokeDasharray={`${2 * Math.PI * 48}`}
                initial={{ strokeDashoffset: 2 * Math.PI * 48 }}
                animate={{ strokeDashoffset: (2 * Math.PI * 48) * (1 - xpPct / 100) }}
                transition={{ duration: 2, delay: 0.5, ease: [0.22, 1, 0.36, 1] }}
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-display text-4xl leading-none text-foreground">{levelData.current}</span>
              <span className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-0.5">Level</span>
            </div>
          </div>
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-0.5">
            <span className="text-lg">{currentLv.icon}</span>
            <p className="text-xs uppercase tracking-[0.3em] font-semibold text-muted-foreground">Current Rank</p>
          </div>
          <h1 className="text-display leading-none text-foreground mb-1" style={{ fontSize: "clamp(2rem,5vw,3.5rem)" }}>
            {currentLv.title}
          </h1>
          <p className="text-sm text-muted-foreground mb-4">
            {levelData.totalXp.toLocaleString()} total XP
            <span className="mx-2 text-border">·</span>
            <span className="text-amber-500 font-medium">🔥 {levelData.streak}-day streak</span>
          </p>

          {/* XP Bar */}
          <div className="max-w-sm">
            <div className="flex justify-between text-xs text-muted-foreground mb-1.5">
              <span>
                <span className="font-semibold text-foreground">{levelData.xp.toLocaleString()}</span>
                <span> / {levelData.xpRequired.toLocaleString()} XP</span>
              </span>
              <span className="font-medium text-primary">{xpPct}%</span>
            </div>
            <div className="relative h-3 rounded-full overflow-hidden bg-primary/10">
              {/* Milestone ticks */}
              {[25, 50, 75].map(pct => (
                <div key={pct} className="absolute top-0 bottom-0 w-px bg-background/60 z-10" style={{ left: `${pct}%` }} />
              ))}
              <motion.div
                className="absolute inset-y-0 left-0 rounded-full bg-gradient-primary"
                initial={{ width: 0 }}
                animate={{ width: `${xpPct}%` }}
                transition={{ duration: 1.8, delay: 0.6, ease: [0.22, 1, 0.36, 1] }}
              />
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              {(levelData.xpRequired - levelData.xp).toLocaleString()} XP to <strong className="text-foreground">{nextLv.title}</strong> {nextLv.icon}
            </p>
          </div>
        </div>

        {/* Right stat stack */}
        <div className="flex sm:flex-col gap-3">
          {[
            { label: "Global Rank", value: "#3",                  icon: Trophy, color: "text-amber-500" },
            { label: "This Week",   value: `+${levelData.xpWeek}`, icon: Zap,   color: "text-primary"  },
            { label: "Badges",      value: `${earnedCount}/${badges.length}`, icon: Shield, color: "text-teal" },
          ].map(s => {
            const Icon = s.icon;
            return (
              <div key={s.label} className="flex items-center gap-2 rounded-2xl border border-border bg-surface-elevated px-3 py-2 min-w-[100px]">
                <Icon className={`h-4 w-4 shrink-0 ${s.color}`} strokeWidth={1.75} />
                <div>
                  <p className="text-[10px] text-muted-foreground leading-none">{s.label}</p>
                  <p className="text-sm font-bold text-foreground leading-tight">{s.value}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </motion.div>
  );
}

// ─── Sidebar ─────────────────────────────────────────────────────────────────

function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <>
      {open && (
        <div className="fixed inset-0 z-30 md:hidden bg-foreground/20 backdrop-blur-sm" onClick={onClose} />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-border bg-surface-elevated
          transition-transform duration-300 ease-in-out
          ${open ? "translate-x-0" : "-translate-x-full"} md:translate-x-0 md:static md:z-auto`}
      >
        <div className="flex h-16 shrink-0 items-center gap-2 border-b border-border px-6">
          <span className="grid place-items-center h-8 w-8 rounded-lg bg-gradient-primary text-primary-foreground">
            <GraduationCap className="h-4 w-4" />
          </span>
          <span className="text-display text-lg text-foreground">EduLife</span>
          <button className="ml-auto md:hidden text-muted-foreground" onClick={onClose}>
            <X className="h-4 w-4" />
          </button>
        </div>
        <nav className="flex-1 px-3 py-6 space-y-1">
          {navLinks.map(({ icon: Icon, label, to }) => (
            <Link key={label} to={to}
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
            >
              <Icon className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              {label}
            </Link>
          ))}
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium bg-primary/8 text-primary border border-primary/15">
            <Shield className="h-4 w-4 shrink-0" strokeWidth={2} />
            Level & Progress
          </div>
        </nav>
        <div className="mt-auto border-t border-border p-4">
          <div className="flex items-center gap-3">
            <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold">
              {user.initials}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground">{user.name}</p>
              <p className="truncate text-xs text-muted-foreground">{user.email}</p>
            </div>
            <Link to="/login" className="text-muted-foreground hover:text-foreground transition-colors">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>
    </>
  );
}

// ─── Main ─────────────────────────────────────────────────────────────────────

function LevelPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">

        {/* Top bar */}
        <header className="flex h-16 shrink-0 items-center gap-4 border-b border-border bg-surface-elevated px-6">
          <button className="md:hidden text-muted-foreground hover:text-foreground transition-colors" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </button>
          <Link to="/dashboard" className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="h-4 w-4" />
            Back to Home
          </Link>
          <div className="ml-auto flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-1.5 rounded-full bg-amber-50 border border-amber-200 dark:bg-amber-500/10 dark:border-amber-500/20 px-3 py-1.5 text-sm font-semibold text-amber-500">
              🔥 {levelData.streak} day streak
            </div>
            <div className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold">
              {user.initials}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-5xl px-5 lg:px-8 py-6 space-y-5">

            {/* ── RANK CARD ── */}
            <RankCard />

            {/* ── STREAK + QUESTS ── */}
            <div className="grid md:grid-cols-[auto_1fr] gap-4">

              {/* Streak calendar */}
              <motion.div
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.2 }}
                className="rounded-3xl border border-border bg-surface-elevated p-5"
              >
                <div className="flex items-center gap-2 mb-4">
                  <Flame className="h-4 w-4 text-amber-500" strokeWidth={1.75} />
                  <p className="text-xs uppercase tracking-[0.25em] font-semibold text-muted-foreground">This Week</p>
                </div>
                <div className="flex items-end gap-2">
                  {streakDays.map((day, i) => {
                    const active = i < (levelData.streak % 7 || 7);
                    const isToday = i === (new Date().getDay() + 6) % 7;
                    return (
                      <div key={i} className="flex flex-col items-center gap-1.5">
                        <motion.div
                          initial={{ scale: 0 }}
                          animate={{ scale: 1 }}
                          transition={{ delay: 0.3 + i * 0.07, type: "spring", bounce: 0.4 }}
                          className={`w-9 h-9 rounded-xl flex items-center justify-center text-base transition-all ${
                            active
                              ? "bg-amber-50 border-2 border-amber-300 dark:bg-amber-500/15 dark:border-amber-500/40"
                              : "bg-muted border-2 border-border"
                          } ${isToday ? "ring-2 ring-primary ring-offset-1" : ""}`}
                        >
                          {active ? "🔥" : "·"}
                        </motion.div>
                        <span className="text-[10px] text-muted-foreground font-medium">{day}</span>
                      </div>
                    );
                  })}
                </div>
                <div className="mt-4 pt-4 border-t border-border flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Longest</span>
                  <span className="font-bold text-foreground">{levelData.longestStreak} days 🏆</span>
                </div>
              </motion.div>

              {/* Daily quests */}
              <motion.div
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.25 }}
                className="rounded-3xl border border-border bg-surface-elevated p-5"
              >
                <div className="flex items-center gap-2 mb-4">
                  <Swords className="h-4 w-4 text-primary" strokeWidth={1.75} />
                  <p className="text-xs uppercase tracking-[0.25em] font-semibold text-muted-foreground">Daily Quests</p>
                  <span className="ml-auto text-xs font-semibold text-primary bg-primary/8 border border-primary/20 rounded-full px-2 py-0.5">
                    {quests.filter(q => q.progress >= q.total).length}/{quests.length} done
                  </span>
                </div>
                <div className="space-y-3">
                  {quests.map((q, i) => {
                    const Icon = q.icon;
                    const pct  = Math.round((q.progress / q.total) * 100);
                    const done = q.progress >= q.total;
                    return (
                      <motion.div
                        key={q.title}
                        initial={{ opacity: 0, x: -10 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.35 + i * 0.08 }}
                        className={`flex items-center gap-3 rounded-2xl border p-3 transition-all ${done ? "opacity-60" : ""} ${q.bg}`}
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
                              className="h-full rounded-full bg-current opacity-70"
                              style={{ color: "currentColor" }}
                              initial={{ width: 0 }}
                              animate={{ width: `${pct}%` }}
                              transition={{ duration: 0.9, delay: 0.5 + i * 0.1 }}
                            >
                              <div className={`h-full rounded-full ${q.color.replace("text-","bg-")}`} />
                            </motion.div>
                          </div>
                          <p className="text-[10px] text-muted-foreground mt-0.5">{q.desc} · {q.progress}/{q.total}</p>
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
            </div>

            {/* ── SKILL TREE ── */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.3 }}
              className="rounded-3xl border border-border bg-surface-elevated p-6 overflow-hidden"
            >
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-2">
                  <Scroll className="h-4 w-4 text-primary" strokeWidth={1.75} />
                  <p className="text-xs uppercase tracking-[0.28em] font-semibold text-muted-foreground">Skill Tree</p>
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <div className="h-2.5 w-2.5 rounded-full bg-gradient-primary" />Mastered
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <div className="h-2.5 w-2.5 rounded-full border-2 border-primary bg-primary/10" />Current
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <div className="h-2.5 w-2.5 rounded-full bg-muted border border-border" />Locked
                  </div>
                </div>
              </div>

              {/* Two-row staggered path */}
              <div className="overflow-x-auto pb-1">
                <div className="relative min-w-max" style={{ height: 180 }}>
                  {levelPath.map((lv, i) => {
                    const done    = lv.n < levelData.current;
                    const current = lv.n === levelData.current;
                    const locked  = lv.n > levelData.current;
                    const top     = i % 2 === 0 ? 16 : 88;
                    const x       = i * 96 + 8;

                    return (
                      <div key={lv.n}>
                        {/* Connector line to next */}
                        {i < levelPath.length - 1 && (
                          <svg
                            className="absolute pointer-events-none"
                            style={{ left: x + 28, top: 0, width: 96, height: 180, overflow: "visible" }}
                            viewBox="0 0 96 180"
                          >
                            <path
                              d={`M 28 ${top + 28} C 62 ${top + 28} 34 ${(i + 1) % 2 === 0 ? 44 : 116} 68 ${(i + 1) % 2 === 0 ? 44 : 116}`}
                              fill="none"
                              strokeWidth="2"
                              strokeDasharray="5 4"
                              className={done ? "stroke-primary/40" : "stroke-border"}
                            />
                          </svg>
                        )}

                        {/* Node */}
                        <motion.div
                          initial={{ opacity: 0, scale: 0.5 }}
                          animate={{ opacity: 1, scale: current ? 1.12 : 1 }}
                          transition={{ duration: 0.45, delay: 0.1 + i * 0.07, ease: [0.22, 1, 0.36, 1] }}
                          className="absolute flex flex-col items-center gap-1.5"
                          style={{ left: x, top, width: 64 }}
                        >
                          {/* XP reward label above (done nodes) */}
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
                              {locked
                                ? <Lock className="h-5 w-5 text-muted-foreground/30" strokeWidth={1.5} />
                                : <span className={done ? "grayscale-0" : ""}>{lv.icon}</span>
                              }
                              {/* Level badge */}
                              <div
                                className={`absolute -bottom-1.5 -right-1.5 grid h-5 w-5 place-items-center rounded-full text-[9px] font-bold border ${
                                  done
                                    ? "bg-primary text-primary-foreground border-primary/30"
                                    : current
                                    ? "bg-primary text-primary-foreground border-primary/30"
                                    : "bg-muted text-muted-foreground/40 border-border"
                                }`}
                              >
                                {lv.n}
                              </div>
                            </div>
                          </div>

                          <p className={`text-[10px] font-bold leading-none text-center ${
                            done ? "text-primary" : current ? "text-foreground" : "text-muted-foreground/35"
                          }`}>
                            {lv.title}
                          </p>
                        </motion.div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Progress bar */}
              <div className="mt-4 flex items-center gap-3">
                <div className="flex-1 h-1.5 rounded-full bg-primary/10 overflow-hidden">
                  <motion.div
                    className="h-full rounded-full bg-gradient-primary"
                    initial={{ width: 0 }}
                    animate={{ width: `${((levelData.current - 1) / levelPath.length) * 100}%` }}
                    transition={{ duration: 1.2, delay: 0.8, ease: [0.22, 1, 0.36, 1] }}
                  />
                </div>
                <span className="text-xs font-semibold text-primary shrink-0">
                  {levelData.current - 1}/{levelPath.length}
                </span>
              </div>
            </motion.div>

            {/* ── XP CHART + LEADERBOARD ── */}
            <div className="grid lg:grid-cols-[1fr_360px] gap-4">

              {/* XP Chart */}
              <motion.div
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.35 }}
                className="rounded-3xl border border-border bg-surface-elevated p-6"
              >
                <div className="flex items-start justify-between mb-6">
                  <div>
                    <p className="text-xs uppercase tracking-[0.2em] font-semibold text-muted-foreground mb-1">XP this week</p>
                    <p className="text-display text-4xl leading-none text-foreground">+{levelData.xpWeek}</p>
                  </div>
                  <span className="flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold bg-teal/8 text-teal border border-teal/20">
                    <TrendingUp className="h-3 w-3" />+12%
                  </span>
                </div>
                <ResponsiveContainer width="100%" height={150}>
                  <BarChart data={weeklyXp} barSize={24} barCategoryGap="30%">
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
                    <XAxis dataKey="day" axisLine={false} tickLine={false}
                      tick={{ fontSize: 11, fill: "oklch(0.5 0.02 145)", fontFamily: "Figtree" }} dy={6}
                    />
                    <YAxis hide />
                    <Tooltip cursor={false}
                      content={({ active, payload }) =>
                        active && payload?.length ? (
                          <div className="rounded-xl px-3 py-2 text-xs font-semibold bg-surface-elevated border border-border text-foreground shadow-elevated">
                            +{payload[0].value} XP
                          </div>
                        ) : null
                      }
                    />
                    <Bar dataKey="xp" radius={[6, 6, 3, 3]}>
                      {weeklyXp.map((entry, i) => (
                        <Cell key={i} fill={entry.xp === maxDayXp ? "url(#barActive)" : "url(#barInactive)"} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </motion.div>

              {/* Leaderboard */}
              <motion.div
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.4 }}
                className="rounded-3xl border border-border bg-surface-elevated overflow-hidden"
              >
                <div className="flex items-center justify-between px-5 py-4 border-b border-border">
                  <div className="flex items-center gap-2">
                    <Trophy className="h-4 w-4 text-amber-500" strokeWidth={1.75} />
                    <p className="text-xs uppercase tracking-[0.2em] font-semibold text-muted-foreground">Leaderboard</p>
                  </div>
                  <span className="text-xs text-muted-foreground">This week</span>
                </div>

                <div className="divide-y divide-border">
                  {leaderboard.map((entry, i) => {
                    const DeltaIcon = entry.delta > 0 ? ChevronUp : entry.delta < 0 ? ChevronDown : Minus;
                    const deltaColor = entry.delta > 0 ? "text-teal" : entry.delta < 0 ? "text-red-400" : "text-muted-foreground/40";
                    return (
                      <motion.div
                        key={entry.name}
                        initial={{ opacity: 0, x: 10 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.3, delay: 0.45 + i * 0.04 }}
                        className={`flex items-center gap-3 px-4 py-3 transition-colors ${entry.isUser ? "bg-primary/5" : ""}`}
                      >
                        <div className="flex items-center gap-1 w-8 shrink-0">
                          <span className="text-sm">
                            {entry.rank <= 3
                              ? ["🥇","🥈","🥉"][entry.rank - 1]
                              : <span className="text-xs font-bold tabular-nums text-muted-foreground/50">{entry.rank}</span>}
                          </span>
                        </div>
                        <div
                          className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-[10px] font-bold border ${
                            entry.isUser
                              ? "bg-gradient-primary text-primary-foreground border-primary/30"
                              : "bg-muted text-muted-foreground border-border"
                          }`}
                        >
                          {entry.initials}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-semibold truncate text-foreground">
                            {entry.name.split(" ")[0]}
                            {entry.isUser && (
                              <span className="ml-1.5 text-[9px] font-normal px-1.5 py-0.5 rounded-full bg-primary/10 text-primary">you</span>
                            )}
                          </p>
                          <p className="text-[10px] text-muted-foreground/60">Lv {entry.level}</p>
                        </div>
                        <DeltaIcon className={`h-3.5 w-3.5 shrink-0 ${deltaColor}`} strokeWidth={2.5} />
                        <p className="text-xs font-semibold text-muted-foreground tabular-nums w-10 text-right">{entry.weeklyXp}</p>
                      </motion.div>
                    );
                  })}
                </div>
              </motion.div>
            </div>

            {/* ── ACHIEVEMENTS ── */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.45 }}
            >
              <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-2">
                  <Sparkles className="h-4 w-4 text-amber-500" strokeWidth={1.75} />
                  <p className="text-xs uppercase tracking-[0.28em] font-semibold text-muted-foreground">Achievements</p>
                </div>
                <div className="flex items-center gap-3">
                  {(["common","rare","epic","legendary"] as BadgeRarity[]).map(r => (
                    <span key={r} className={`hidden sm:inline text-[10px] font-semibold ${rarityConfig[r].color}`}>
                      {rarityConfig[r].label}
                    </span>
                  ))}
                  <span className="text-xs text-muted-foreground">
                    <span className="font-bold text-foreground">{earnedCount}</span>/{badges.length}
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
                {badges.map((badge, i) => {
                  const Icon = badge.icon;
                  const r = rarityConfig[badge.rarity];
                  return (
                    <motion.div
                      key={badge.title}
                      initial={{ opacity: 0, scale: 0.88 }}
                      animate={{ opacity: badge.earned ? 1 : 0.4, scale: 1 }}
                      transition={{ duration: 0.35, delay: 0.08 + i * 0.03 }}
                      whileHover={badge.earned ? { scale: 1.04, y: -3 } : {}}
                      className={`relative flex flex-col gap-3 rounded-2xl border p-4 transition-all ${
                        badge.earned
                          ? `${r.bg} ${r.border} ${r.glow}`
                          : "bg-muted/30 border-border"
                      }`}
                    >
                      {/* Rarity tag */}
                      <div className="flex items-center justify-between">
                        <div className={`grid h-9 w-9 place-items-center rounded-xl ${badge.earned ? "bg-background/80" : "bg-muted"}`}>
                          <Icon className={`h-4 w-4 ${badge.earned ? r.color : "text-muted-foreground"}`} strokeWidth={1.75} />
                        </div>
                        <span className={`text-[9px] uppercase tracking-wider font-bold ${badge.earned ? r.color : "text-muted-foreground/40"}`}>
                          {badge.rarity}
                        </span>
                      </div>
                      <div>
                        <p className={`text-xs font-bold leading-tight ${badge.earned ? "text-foreground" : "text-muted-foreground"}`}>
                          {badge.title}
                        </p>
                        <p className="text-[10px] mt-0.5 leading-tight text-muted-foreground/60">
                          {badge.desc}
                        </p>
                      </div>
                      {badge.earned
                        ? <CheckCircle className={`absolute top-2.5 right-2.5 h-3 w-3 ${r.color} opacity-70`} />
                        : <Lock className="absolute top-2.5 right-2.5 h-3 w-3 text-muted-foreground/30" />
                      }
                    </motion.div>
                  );
                })}
              </div>
            </motion.div>

          </div>
        </main>
      </div>
    </div>
  );
}
