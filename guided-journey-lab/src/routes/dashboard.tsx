import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import {
  GraduationCap, Home, BookOpen, Compass, Award, Settings,
  Search, Bell, ChevronRight, Flame, CheckCircle, PlayCircle,
  Clock, LogOut, Menu, X, Users, TrendingUp, ArrowRight, Shield,
  Sparkles, ArrowUpRight,
} from "lucide-react";

export const Route = createFileRoute("/dashboard")({
  component: DashboardPage,
  head: () => ({ meta: [{ title: "Home — EduLife" }] }),
});

// ─── Data ─────────────────────────────────────────────────────────────────────

const user = { name: "Mohamed Baghdadi", email: "m.baghdadi@example.com", initials: "MB" };

const levelData = {
  current: 7, xp: 2340, xpRequired: 3000, xpToday: 120,
};

const levelPath = [
  { n: 1,  title: "Novice"    },
  { n: 2,  title: "Curious"   },
  { n: 3,  title: "Explorer"  },
  { n: 4,  title: "Seeker"    },
  { n: 5,  title: "Thinker"   },
  { n: 6,  title: "Achiever"  },
  { n: 7,  title: "Scholar"   },
  { n: 8,  title: "Expert"    },
  { n: 9,  title: "Sage"      },
  { n: 10, title: "Master"    },
];

const stats = [
  {
    icon: Flame,       value: "12",    label: "Day streak",
    trend: "+2",  trendUp: true,
    color: "text-gold",    bg: "from-[oklch(0.99_0.012_90)] to-[oklch(0.97_0.018_80)]",
    iconBg: "bg-gold/12",  border: "border-gold/18",
  },
  {
    icon: CheckCircle, value: "2 / 3", label: "Lessons today",
    trend: "67%", trendUp: true,
    color: "text-teal",    bg: "from-[oklch(0.99_0.008_170)] to-[oklch(0.97_0.014_185)]",
    iconBg: "bg-teal/12",  border: "border-teal/18",
  },
  {
    icon: Award,       value: "1",     label: "Certificate",
    trend: "new", trendUp: true,
    color: "text-primary", bg: "from-[oklch(0.99_0.006_140)] to-[oklch(0.97_0.012_145)]",
    iconBg: "bg-primary/10", border: "border-primary/16",
  },
  {
    icon: TrendingUp,  value: "84%",   label: "Avg score",
    trend: "+6%", trendUp: true,
    color: "text-primary", bg: "from-[oklch(0.99_0.006_140)] to-[oklch(0.97_0.012_145)]",
    iconBg: "bg-primary/10", border: "border-primary/16",
  },
];

const enrolled = [
  {
    id: 1, title: "Web Development Fundamentals",     subject: "Technology",
    lessons: 24, total: 35, nextLesson: "CSS Flexbox & Grid",          estimatedMin: 18,
    gradientClass: "from-primary to-primary-glow",
    accentText: "text-primary", accentBg: "bg-primary/8", accentBorder: "border-primary/20",
    cardGradient: "from-[oklch(0.99_0.006_140)] to-[oklch(0.975_0.010_145)]",
  },
  {
    id: 2, title: "Business Communication in Arabic", subject: "Language",
    lessons: 9,  total: 30, nextLesson: "Professional Email Writing",  estimatedMin: 22,
    gradientClass: "from-gold to-[oklch(0.72_0.16_70)]",
    accentText: "text-gold",    accentBg: "bg-gold/8",    accentBorder: "border-gold/20",
    cardGradient: "from-[oklch(0.99_0.012_90)] to-[oklch(0.975_0.018_80)]",
  },
  {
    id: 3, title: "Data Analysis with Excel",         subject: "Business",
    lessons: 2,  total: 20, nextLesson: "Introduction to Pivot Tables", estimatedMin: 25,
    gradientClass: "from-teal to-[oklch(0.55_0.15_185)]",
    accentText: "text-teal",    accentBg: "bg-teal/8",    accentBorder: "border-teal/20",
    cardGradient: "from-[oklch(0.99_0.008_170)] to-[oklch(0.975_0.014_185)]",
  },
];

const explore = [
  { id: 4, title: "Python for Beginners",         subject: "Technology", duration: "12h", enrolled: 340, accentClass: "bg-primary/10 text-primary", dotColor: "bg-primary" },
  { id: 5, title: "Digital Marketing Essentials", subject: "Business",   duration: "8h",  enrolled: 210, accentClass: "bg-teal/10 text-teal",       dotColor: "bg-teal"    },
  { id: 6, title: "French for Professionals",     subject: "Language",   duration: "15h", enrolled: 180, accentClass: "bg-gold/10 text-gold",       dotColor: "bg-gold"    },
  { id: 7, title: "Graphic Design Basics",        subject: "Design",     duration: "10h", enrolled: 290, accentClass: "bg-primary/10 text-primary", dotColor: "bg-primary" },
];

const navLinks = [
  { icon: Home,     label: "Home",         to: "/dashboard" as const, active: true  },
  { icon: BookOpen, label: "My Courses",   to: "/courses"   as const, active: false },
  { icon: Compass,  label: "Explore",      to: "/explore"   as const, active: false },
  { icon: Award,    label: "Certificates", to: "/certificates" as const, active: false },
  { icon: Settings, label: "Settings",     to: "/profile"      as const, active: false },
];

// ─── Sidebar ──────────────────────────────────────────────────────────────────

function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <>
      {open && (
        <div className="fixed inset-0 z-30 bg-foreground/20 backdrop-blur-sm md:hidden" onClick={onClose} />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-surface-elevated
          transition-transform duration-300 ease-in-out
          ${open ? "translate-x-0" : "-translate-x-full"} md:translate-x-0 md:static md:z-auto`}
        style={{ boxShadow: "var(--shadow-luxury)" }}
      >
        {/* Logo */}
        <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border/60 px-6">
          <div className="relative">
            <span className="grid place-items-center h-8 w-8 rounded-xl bg-gradient-primary text-primary-foreground shadow-glow">
              <GraduationCap className="h-4 w-4" />
            </span>
            <span className="absolute -inset-1 rounded-2xl bg-primary/20 blur-md -z-10" />
          </div>
          <span className="text-display text-lg text-foreground tracking-tight">EduLife</span>
          <button className="ml-auto text-muted-foreground hover:text-foreground md:hidden transition-colors" onClick={onClose} aria-label="Close menu">
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-6 space-y-0.5">
          <p className="px-3 mb-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Main</p>
          {navLinks.map(({ icon: Icon, label, to, active }) => (
            <Link key={label} to={to}
              className={`relative w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${
                active
                  ? "bg-primary/10 text-primary shadow-sm"
                  : "text-muted-foreground hover:bg-accent/80 hover:text-foreground"
              }`}
            >
              <Icon className="h-4 w-4 shrink-0" strokeWidth={active ? 2 : 1.75} />
              {label}
              {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" />}
            </Link>
          ))}

          <div className="pt-4 mt-4 border-t border-border/60">
            <p className="px-3 mb-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Progress</p>
            <Link
              to="/level"
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:bg-accent/80 hover:text-foreground transition-all duration-200"
            >
              <Shield className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              Level & Progress
            </Link>
          </div>
        </nav>

        {/* Daily goal widget */}
        <div className="mx-3 mb-3 rounded-2xl border border-border/60 bg-gradient-to-br from-primary/6 to-primary/3 p-4">
          <div className="flex items-center justify-between mb-2.5">
            <p className="text-[10px] uppercase tracking-[0.16em] text-muted-foreground font-medium">Today's goal</p>
            <span className="text-xs font-semibold text-primary tabular-nums">2 / 3</span>
          </div>
          <div className="h-1.5 rounded-full bg-border overflow-hidden">
            <div className="h-full w-2/3 rounded-full bg-gradient-primary" />
          </div>
          <p className="mt-2 text-[11px] text-muted-foreground">1 more lesson to hit your goal</p>
        </div>

        {/* User footer */}
        <div className="border-t border-border/60 p-4">
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold">
                {user.initials}
              </div>
              <span className="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-surface-elevated bg-teal" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground">{user.name}</p>
              <p className="truncate text-xs text-muted-foreground">{user.email}</p>
            </div>
            <Link to="/login" className="shrink-0 text-muted-foreground hover:text-foreground transition-colors" aria-label="Log out">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>
    </>
  );
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

function StatCard({ stat, index }: { stat: typeof stats[0]; index: number }) {
  const Icon = stat.icon;
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.15 + index * 0.07 }}
      className={`relative flex items-center gap-4 rounded-2xl border ${stat.border} bg-gradient-to-br ${stat.bg} p-5 overflow-hidden group hover:shadow-elevated transition-all duration-300`}
    >
      <div className={`grid h-11 w-11 shrink-0 place-items-center rounded-xl ${stat.iconBg}`}>
        <Icon className={`h-5 w-5 ${stat.color}`} strokeWidth={1.75} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-baseline gap-2">
          <p className="text-xl font-semibold text-display text-foreground leading-none tabular-nums">{stat.value}</p>
          <span className={`text-[10px] font-semibold flex items-center gap-0.5 ${stat.trendUp ? "text-teal" : "text-destructive"}`}>
            <ArrowUpRight className="h-3 w-3" />
            {stat.trend}
          </span>
        </div>
        <p className="mt-1 text-xs text-muted-foreground">{stat.label}</p>
      </div>
      {/* Decorative background shape */}
      <div className={`absolute -right-4 -top-4 h-16 w-16 rounded-full ${stat.iconBg} blur-xl opacity-60 pointer-events-none`} />
    </motion.div>
  );
}

// ─── Course card ──────────────────────────────────────────────────────────────

function CourseCard({ course, index }: { course: typeof enrolled[0]; index: number }) {
  const pct = Math.round((course.lessons / course.total) * 100);
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: index * 0.08 }}
      className={`group relative rounded-2xl border ${course.accentBorder} bg-gradient-to-br ${course.cardGradient} p-6 hover:shadow-luxury hover:-translate-y-0.5 transition-all duration-300`}
    >
      {/* Top accent line */}
      <div className={`absolute top-0 left-6 right-6 h-px bg-gradient-to-r ${course.gradientClass} opacity-40`} />

      <span className={`inline-block rounded-full px-2.5 py-0.5 text-[11px] uppercase tracking-[0.14em] font-medium ${course.accentBg} ${course.accentText}`}>
        {course.subject}
      </span>
      <h3 className="mt-3 text-base text-display text-foreground leading-snug">{course.title}</h3>
      <div className="mt-4">
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-xs text-muted-foreground">{course.lessons} / {course.total} lessons</span>
          <span className={`text-xs font-semibold tabular-nums ${course.accentText}`}>{pct}%</span>
        </div>
        <div className="h-1.5 rounded-full bg-border overflow-hidden">
          <div className={`h-full rounded-full bg-gradient-to-r ${course.gradientClass} transition-all duration-500`} style={{ width: `${pct}%` }} />
        </div>
      </div>
      <div className="mt-4 pt-4 border-t border-border/60 flex items-center justify-between">
        <p className="text-xs text-muted-foreground truncate max-w-[140px]">
          <span className="text-muted-foreground/50 mr-1">Next:</span>
          {course.nextLesson}
        </p>
        <button className={`flex items-center gap-1.5 text-xs font-semibold ${course.accentText} group-hover:gap-2 transition-all`}>
          Continue <ArrowRight className="h-3 w-3" />
        </button>
      </div>
    </motion.div>
  );
}

// ─── Explore card ─────────────────────────────────────────────────────────────

function ExploreCard({ course, index }: { course: typeof explore[0]; index: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.5, delay: index * 0.07 }}
      className="group shrink-0 w-60 rounded-2xl border border-border bg-surface-elevated p-5 hover:border-primary/30 hover:shadow-luxury hover:-translate-y-0.5 transition-all duration-300 cursor-pointer"
    >
      <div className="flex items-center gap-2 mb-3">
        <span className={`h-1.5 w-1.5 rounded-full ${course.dotColor}`} />
        <span className={`inline-block rounded-full px-2.5 py-0.5 text-[11px] uppercase tracking-[0.14em] font-medium ${course.accentClass}`}>
          {course.subject}
        </span>
      </div>
      <h4 className="text-sm text-display text-foreground leading-snug">{course.title}</h4>
      <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
        <span className="flex items-center gap-1"><Clock className="h-3 w-3" />{course.duration}</span>
        <span className="flex items-center gap-1"><Users className="h-3 w-3" />{course.enrolled}</span>
      </div>
      <button className="mt-4 w-full h-8 rounded-xl border border-border/80 text-xs font-medium text-foreground hover:bg-primary hover:text-primary-foreground hover:border-primary transition-all duration-200">
        Enroll
      </button>
    </motion.div>
  );
}

// ─── Dashboard page ───────────────────────────────────────────────────────────

function DashboardPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const continueWith = enrolled[0];
  const continuePct  = Math.round((continueWith.lessons / continueWith.total) * 100);
  const hour         = new Date().getHours();
  const greeting     = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">

        {/* Top bar */}
        <header className="flex h-16 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/80 backdrop-blur-md px-6 sticky top-0 z-20">
          <button className="md:hidden text-muted-foreground hover:text-foreground transition-colors" onClick={() => setSidebarOpen(true)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex flex-1 items-center gap-2 rounded-full border border-border/80 bg-surface px-4 h-9 max-w-sm hover:border-primary/30 transition-colors focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-ring/20">
            <Search className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
            <input type="search" placeholder="Search courses..." className="flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground/50 outline-none" />
          </div>
          <div className="ml-auto flex items-center gap-2.5">
            <Link
              to="/level"
              className="inline-flex items-center gap-2 h-9 rounded-full border border-primary/25 bg-primary/6 px-4 text-xs font-semibold text-primary hover:bg-primary/12 hover:border-primary/40 transition-all"
            >
              <Shield className="h-3.5 w-3.5" />
              Level {levelData.current} · {levelPath[levelData.current - 1].title}
            </Link>
            <button className="relative grid h-9 w-9 place-items-center rounded-full border border-border/80 text-muted-foreground hover:text-foreground hover:border-primary/30 transition-all">
              <Bell className="h-4 w-4" />
              <span className="absolute top-1.5 right-1.5 h-1.5 w-1.5 rounded-full bg-gold shadow-sm" />
            </button>
            <div className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold cursor-pointer hover:opacity-90 transition-opacity shadow-soft">
              {user.initials}
            </div>
          </div>
        </header>

        {/* Content */}
        <main className="flex-1 overflow-y-auto px-6 lg:px-8 py-10 space-y-10">

          {/* ── Hero greeting ── */}
          <motion.section
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.55 }}
            className="relative rounded-3xl overflow-hidden bg-gradient-to-br from-primary to-primary-glow p-8 sm:p-10 grain"
          >
            {/* Decorative blobs */}
            <div className="absolute -top-16 -right-16 h-64 w-64 rounded-full bg-white/10 blur-3xl pointer-events-none" />
            <div className="absolute -bottom-12 -left-12 h-48 w-48 rounded-full bg-white/6 blur-2xl pointer-events-none" />
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 h-32 w-96 rounded-full bg-primary-glow/30 blur-3xl pointer-events-none" />

            <div className="relative z-10">
              <div className="inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/10 px-3.5 py-1.5 text-xs font-medium text-primary-foreground/80 mb-4 backdrop-blur-sm">
                <Sparkles className="h-3 w-3" />
                {levelPath[levelData.current - 1].title} · Level {levelData.current}
              </div>
              <h1 className="text-display text-3xl sm:text-4xl text-primary-foreground leading-tight">
                {greeting},{" "}
                <span className="text-white/90 font-medium">{user.name.split(" ")[0]}</span>
              </h1>
              <p className="mt-2 text-sm sm:text-base text-primary-foreground/70 max-w-lg">
                You're on a 12-day streak. You have 1 lesson left to hit today's goal.
              </p>
              <div className="mt-6 flex flex-wrap items-center gap-4">
                <div className="flex items-center gap-2 text-sm text-primary-foreground/80">
                  <div className="h-8 w-8 rounded-full bg-primary-foreground/15 flex items-center justify-center">
                    <Flame className="h-4 w-4" strokeWidth={1.75} />
                  </div>
                  <span>12-day streak</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-primary-foreground/80">
                  <div className="h-8 w-8 rounded-full bg-primary-foreground/15 flex items-center justify-center">
                    <TrendingUp className="h-4 w-4" strokeWidth={1.75} />
                  </div>
                  <span>{levelData.xp} XP total</span>
                </div>
              </div>
            </div>
          </motion.section>

          {/* ── Stats ── */}
          <section>
            {/* Section eyebrow */}
            <div className="flex items-center gap-3 mb-5">
              <span className="h-1 w-1 rounded-full bg-primary/60" />
              <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Overview</h2>
              <div className="flex-1 h-px bg-gradient-to-r from-border to-transparent" />
            </div>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              {stats.map((s, i) => <StatCard key={s.label} stat={s} index={i} />)}
            </div>
          </section>

          {/* ── Continue learning ── */}
          <section>
            <div className="flex items-center gap-3 mb-5">
              <span className="h-1 w-1 rounded-full bg-primary/60" />
              <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Continue learning</h2>
              <div className="flex-1 h-px bg-gradient-to-r from-border to-transparent" />
            </div>
            <motion.div
              initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.2 }}
              className={`relative rounded-3xl bg-gradient-to-br ${continueWith.gradientClass} text-primary-foreground p-8 sm:p-10 overflow-hidden`}
            >
              {/* Decorative layers */}
              <div className="absolute -top-20 -right-20 h-72 w-72 rounded-full bg-white/10 blur-3xl pointer-events-none" />
              <div className="absolute -bottom-16 -left-16 h-52 w-52 rounded-full bg-white/5 blur-2xl pointer-events-none" />
              <div className="absolute inset-0 grain pointer-events-none" />

              <div className="relative flex flex-col sm:flex-row sm:items-end justify-between gap-8">
                <div className="flex-1 min-w-0">
                  <span className="inline-block rounded-full border border-primary-foreground/25 bg-primary-foreground/10 px-3 py-0.5 text-[11px] uppercase tracking-[0.16em] text-primary-foreground/80 backdrop-blur-sm mb-3">
                    {continueWith.subject}
                  </span>
                  <h3 className="text-display text-2xl sm:text-3xl text-primary-foreground leading-tight max-w-lg">
                    {continueWith.title}
                  </h3>
                  <div className="mt-4 flex flex-wrap items-center gap-4 text-sm text-primary-foreground/75">
                    <span className="flex items-center gap-2">
                      <PlayCircle className="h-4 w-4" />
                      {continueWith.nextLesson}
                    </span>
                    <span className="flex items-center gap-2">
                      <Clock className="h-4 w-4" />
                      ~{continueWith.estimatedMin} min
                    </span>
                  </div>
                  <div className="mt-6 max-w-xs">
                    <div className="flex items-center justify-between mb-1.5 text-xs text-primary-foreground/65">
                      <span>{continueWith.lessons} of {continueWith.total} lessons</span>
                      <span className="font-semibold tabular-nums">{continuePct}%</span>
                    </div>
                    <div className="h-1.5 rounded-full bg-primary-foreground/20 overflow-hidden">
                      <motion.div
                        className="h-full rounded-full bg-primary-foreground/85"
                        initial={{ width: 0 }}
                        animate={{ width: `${continuePct}%` }}
                        transition={{ duration: 1.2, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
                      />
                    </div>
                  </div>
                </div>
                <button className="group shrink-0 inline-flex h-12 items-center gap-2.5 rounded-2xl bg-primary-foreground text-foreground px-7 text-sm font-semibold hover:scale-[1.02] active:scale-[0.98] transition-transform shadow-elevated">
                  Continue learning
                  <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
                </button>
              </div>
            </motion.div>
          </section>

          {/* ── My courses ── */}
          <section>
            <div className="flex items-center gap-3 mb-5">
              <span className="h-1 w-1 rounded-full bg-primary/60" />
              <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">My courses</h2>
              <div className="flex-1 h-px bg-gradient-to-r from-border to-transparent" />
              <Link to="/courses" className="flex items-center gap-1.5 text-xs font-medium text-primary hover:text-primary-glow transition-colors">
                View all <ChevronRight className="h-3 w-3" />
              </Link>
            </div>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {enrolled.map((course, i) => <CourseCard key={course.id} course={course} index={i} />)}
            </div>
          </section>

          {/* ── Explore ── */}
          <section className="pb-4">
            <div className="flex items-center gap-3 mb-5">
              <span className="h-1 w-1 rounded-full bg-primary/60" />
              <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Explore new</h2>
              <div className="flex-1 h-px bg-gradient-to-r from-border to-transparent" />
              <button className="flex items-center gap-1.5 text-xs font-medium text-primary hover:text-primary-glow transition-colors">
                Browse all <ChevronRight className="h-3 w-3" />
              </button>
            </div>
            <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-none -mx-1 px-1">
              {explore.map((course, i) => <ExploreCard key={course.id} course={course} index={i} />)}
            </div>
          </section>

        </main>
      </div>
    </div>
  );
}
