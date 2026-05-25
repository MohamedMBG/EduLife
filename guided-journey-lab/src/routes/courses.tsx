import { createFileRoute, Link } from "@tanstack/react-router";
import { motion, AnimatePresence } from "framer-motion";
import {
  GraduationCap, Home, BookOpen, Compass, Award, Settings,
  LogOut, Shield, ArrowLeft, Menu, X, Search, PlayCircle,
  Clock, CheckCircle, Lock, ChevronRight, Star, Zap, BarChart2,
  Filter, Code2, Languages, Briefcase, Palette, Monitor,
  TrendingUp, Sparkles,
} from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/courses")({
  component: CoursesPage,
  head: () => ({ meta: [{ title: "My Courses — EduLife" }] }),
});

// ─── Data ─────────────────────────────────────────────────────────────────────

const user = { name: "Mohamed Baghdadi", email: "m.baghdadi@example.com", initials: "MB" };

type CourseStatus = "in-progress" | "completed" | "not-started";

const courses: {
  id: number;
  title: string;
  subject: string;
  SubjectIcon: React.ElementType;
  instructor: string;
  instructorInitials: string;
  instructorColor: string;
  thumbnail: string;
  lessons: number;
  total: number;
  nextLesson: string;
  estimatedMin: number;
  xpEarned: number;
  xpTotal: number;
  status: CourseStatus;
  rating: number;
  gradientFrom: string;
  gradientTo: string;
  accentText: string;
  accentBg: string;
  accentBorder: string;
  duration: string;
}[] = [
  {
    id: 1,
    title: "Web Development Fundamentals",
    subject: "Technology",
    SubjectIcon: Code2,
    instructor: "Khalid Moussaoui",
    instructorInitials: "KM",
    instructorColor: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 24, total: 35,
    nextLesson: "CSS Flexbox & Grid", estimatedMin: 18,
    xpEarned: 480, xpTotal: 700, status: "in-progress", rating: 4.8,
    gradientFrom: "oklch(0.38 0.16 145)", gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary", accentBg: "bg-primary/8", accentBorder: "border-primary/20",
    duration: "18h",
  },
  {
    id: 2,
    title: "Business Communication in Arabic",
    subject: "Language",
    SubjectIcon: Languages,
    instructor: "Fatima Tahiri",
    instructorInitials: "FT",
    instructorColor: "bg-gold",
    thumbnail: "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 9, total: 30,
    nextLesson: "Professional Email Writing", estimatedMin: 22,
    xpEarned: 180, xpTotal: 600, status: "in-progress", rating: 4.6,
    gradientFrom: "oklch(0.78 0.14 80)", gradientTo: "oklch(0.68 0.16 70)",
    accentText: "text-gold", accentBg: "bg-gold/8", accentBorder: "border-gold/20",
    duration: "22h",
  },
  {
    id: 3,
    title: "Data Analysis with Excel",
    subject: "Business",
    SubjectIcon: Briefcase,
    instructor: "Youssef Kettani",
    instructorInitials: "YK",
    instructorColor: "bg-teal",
    thumbnail: "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 2, total: 20,
    nextLesson: "Introduction to Pivot Tables", estimatedMin: 25,
    xpEarned: 40, xpTotal: 400, status: "in-progress", rating: 4.5,
    gradientFrom: "oklch(0.72 0.10 200)", gradientTo: "oklch(0.58 0.14 190)",
    accentText: "text-teal", accentBg: "bg-teal/8", accentBorder: "border-teal/20",
    duration: "14h",
  },
  {
    id: 4,
    title: "Introduction to Python",
    subject: "Technology",
    SubjectIcon: Monitor,
    instructor: "Omar Bennis",
    instructorInitials: "OB",
    instructorColor: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1526379095098-d400fd0bf935?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 18, total: 18,
    nextLesson: "—", estimatedMin: 0,
    xpEarned: 360, xpTotal: 360, status: "completed", rating: 4.9,
    gradientFrom: "oklch(0.38 0.16 145)", gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary", accentBg: "bg-primary/8", accentBorder: "border-primary/20",
    duration: "12h",
  },
  {
    id: 5,
    title: "Darija for Professionals",
    subject: "Language",
    SubjectIcon: Languages,
    instructor: "Nadia Alami",
    instructorInitials: "NA",
    instructorColor: "bg-gold",
    thumbnail: "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 12, total: 12,
    nextLesson: "—", estimatedMin: 0,
    xpEarned: 240, xpTotal: 240, status: "completed", rating: 4.7,
    gradientFrom: "oklch(0.78 0.14 80)", gradientTo: "oklch(0.68 0.16 70)",
    accentText: "text-gold", accentBg: "bg-gold/8", accentBorder: "border-gold/20",
    duration: "9h",
  },
  {
    id: 6,
    title: "Graphic Design Basics",
    subject: "Design",
    SubjectIcon: Palette,
    instructor: "Salma Chraibi",
    instructorInitials: "SC",
    instructorColor: "bg-violet-500",
    thumbnail: "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 0, total: 22,
    nextLesson: "Design Principles", estimatedMin: 20,
    xpEarned: 0, xpTotal: 440, status: "not-started", rating: 4.4,
    gradientFrom: "oklch(0.55 0.22 290)", gradientTo: "oklch(0.45 0.20 280)",
    accentText: "text-violet-500",
    accentBg: "bg-violet-50 dark:bg-violet-500/10",
    accentBorder: "border-violet-200 dark:border-violet-500/20",
    duration: "16h",
  },
];

const navLinks = [
  { icon: Home,     label: "Home",         to: "/dashboard" as const },
  { icon: BookOpen, label: "My Courses",   to: "/courses"   as const },
  { icon: Compass,  label: "Explore",      to: "/explore"   as const },
  { icon: Award,    label: "Certificates", to: "/dashboard" as const },
  { icon: Settings, label: "Settings",     to: "/dashboard" as const },
];

type Tab = "all" | "in-progress" | "completed" | "not-started";

const tabs: { id: Tab; label: string; count: (l: typeof courses) => number }[] = [
  { id: "all",         label: "All",         count: l => l.length },
  { id: "in-progress", label: "In Progress", count: l => l.filter(c => c.status === "in-progress").length },
  { id: "completed",   label: "Completed",   count: l => l.filter(c => c.status === "completed").length },
  { id: "not-started", label: "Not Started", count: l => l.filter(c => c.status === "not-started").length },
];

// ─── Sidebar ─────────────────────────────────────────────────────────────────

function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <>
      {open && (
        <div className="fixed inset-0 z-30 md:hidden bg-foreground/20 backdrop-blur-sm" onClick={onClose} />
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
          <button className="ml-auto md:hidden text-muted-foreground hover:text-foreground transition-colors" onClick={onClose}>
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-6 space-y-0.5">
          <p className="px-3 mb-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Main</p>
          {navLinks.map(({ icon: Icon, label, to }) => {
            const active = to === "/courses";
            return (
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
            );
          })}
          <div className="pt-4 mt-4 border-t border-border/60">
            <p className="px-3 mb-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Progress</p>
            <Link to="/level"
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:bg-accent/80 hover:text-foreground transition-all duration-200"
            >
              <Shield className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              Level & Progress
            </Link>
          </div>
        </nav>

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
            <Link to="/login" className="text-muted-foreground hover:text-foreground transition-colors">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>
    </>
  );
}

// ─── Course Card ──────────────────────────────────────────────────────────────

function CourseCard({ course, index }: { course: typeof courses[0]; index: number }) {
  const pct   = course.total > 0 ? Math.round((course.lessons / course.total) * 100) : 0;
  const xpPct = course.xpTotal > 0 ? Math.round((course.xpEarned / course.xpTotal) * 100) : 0;
  const SubjectIcon = course.SubjectIcon;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.96 }}
      transition={{ duration: 0.4, delay: index * 0.06 }}
      className={`group relative flex flex-col rounded-3xl border bg-surface-elevated overflow-hidden
        hover:shadow-luxury hover:-translate-y-0.5 transition-all duration-300 ${course.accentBorder}`}
    >
      {/* ── Thumbnail ── */}
      <div className="relative h-44 overflow-hidden bg-muted">
        <img
          src={course.thumbnail}
          alt={course.title}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          loading="lazy"
        />
        {/* Gradient overlay */}
        <div
          className="absolute inset-0"
          style={{
            background: `linear-gradient(to top, ${course.gradientFrom}e0 0%, ${course.gradientFrom}55 45%, transparent 100%)`,
          }}
        />

        {/* Shimmer on hover */}
        <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 overflow-hidden">
          <div
            className="absolute inset-0 animate-shimmer"
            style={{
              background: "linear-gradient(105deg, transparent 40%, oklch(1 0 0 / 0.12) 50%, transparent 60%)",
            }}
          />
        </div>

        {/* Subject pill — top left */}
        <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full px-2.5 py-1 bg-background/85 backdrop-blur-md border border-border/50 shadow-soft">
          <SubjectIcon className={`h-3 w-3 ${course.accentText}`} strokeWidth={2} />
          <span className={`text-[10px] font-semibold uppercase tracking-[0.12em] ${course.accentText}`}>
            {course.subject}
          </span>
        </div>

        {/* Duration badge — bottom left */}
        <div className="absolute bottom-3 left-3 flex items-center gap-1.5 rounded-full px-2.5 py-1 bg-background/75 backdrop-blur-md border border-border/40">
          <Clock className="h-3 w-3 text-muted-foreground" strokeWidth={2} />
          <span className="text-[10px] font-semibold text-foreground/80">{course.duration}</span>
        </div>

        {/* Status badge — top right */}
        <div className="absolute top-3 right-3">
          {course.status === "completed" && (
            <span className="flex items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-semibold bg-primary/90 text-primary-foreground backdrop-blur-sm shadow-soft">
              <CheckCircle className="h-3 w-3" />Done
            </span>
          )}
          {course.status === "not-started" && (
            <span className="flex items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-semibold bg-background/80 text-muted-foreground backdrop-blur-sm border border-border/60">
              <Lock className="h-3 w-3" />New
            </span>
          )}
          {course.status === "in-progress" && (
            <span
              className="flex items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-semibold backdrop-blur-sm text-white shadow-soft tabular-nums"
              style={{ background: `${course.gradientFrom}cc` }}
            >
              <PlayCircle className="h-3 w-3" />{pct}%
            </span>
          )}
        </div>

        {/* Progress bar at bottom of image */}
        {course.status !== "not-started" && (
          <div className="absolute bottom-0 inset-x-0 h-1 bg-black/20">
            <motion.div
              className="h-full"
              style={{ background: `linear-gradient(90deg, ${course.gradientFrom}, ${course.gradientTo})` }}
              initial={{ width: 0 }}
              animate={{ width: `${pct}%` }}
              transition={{ duration: 1.2, delay: 0.3 + index * 0.06, ease: [0.22, 1, 0.36, 1] }}
            />
          </div>
        )}
      </div>

      {/* ── Body ── */}
      <div className="flex flex-col flex-1 p-5 gap-3">

        {/* Instructor row */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className={`relative grid h-7 w-7 shrink-0 place-items-center rounded-full ${course.instructorColor} text-[10px] font-bold text-white shadow-soft`}>
              {course.instructorInitials}
              <span className="absolute inset-0 rounded-full ring-2 ring-border ring-offset-1 ring-offset-surface-elevated" />
            </div>
            <span className="text-xs font-medium text-muted-foreground truncate max-w-[120px]">{course.instructor}</span>
          </div>
          <div className="flex items-center gap-1 text-xs font-semibold text-gold shrink-0">
            <Star className="h-3 w-3 fill-gold" strokeWidth={0} />
            <span className="tabular-nums">{course.rating}</span>
          </div>
        </div>

        {/* Title */}
        <h3 className="text-sm font-semibold text-display text-foreground leading-snug">
          {course.title}
        </h3>

        {/* Lesson count + XP */}
        <div className="flex items-center gap-3 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <BookOpen className="h-3 w-3" />
            {course.lessons}/{course.total} lessons
          </span>
          <span className="flex items-center gap-1">
            <Zap className="h-3 w-3 text-gold" />
            {course.xpEarned} XP
          </span>
          {course.status !== "completed" && course.estimatedMin > 0 && (
            <span className="flex items-center gap-1 ml-auto">
              <Clock className="h-3 w-3" />{course.estimatedMin}m
            </span>
          )}
        </div>

        {/* XP bar */}
        <div>
          <div className="h-1.5 rounded-full bg-amber-100 dark:bg-amber-500/10 overflow-hidden">
            <motion.div
              className="h-full rounded-full bg-gradient-gold"
              initial={{ width: 0 }}
              animate={{ width: `${xpPct}%` }}
              transition={{ duration: 1.1, delay: 0.4 + index * 0.06, ease: [0.22, 1, 0.36, 1] }}
            />
          </div>
          <p className="mt-1 text-[10px] text-muted-foreground tabular-nums">{course.xpEarned}/{course.xpTotal} XP earned</p>
        </div>

        {/* Footer */}
        <div className="mt-auto pt-3.5 border-t border-border/60 flex items-center justify-between gap-2">
          <p className="text-[11px] text-muted-foreground truncate">
            {course.status === "completed" ? "Course complete" : `Next: ${course.nextLesson}`}
          </p>
          <button
            className={`shrink-0 group/btn flex items-center gap-1.5 rounded-xl px-4 h-8 text-xs font-semibold transition-all duration-200 whitespace-nowrap ${
              course.status === "completed"
                ? "bg-primary/8 text-primary hover:bg-primary/15 border border-primary/20"
                : course.status === "not-started"
                ? "bg-foreground text-background hover:opacity-90 shadow-soft"
                : `${course.accentBg} ${course.accentText} border ${course.accentBorder} hover:opacity-80`
            }`}
          >
            {course.status === "completed" ? (
              <><CheckCircle className="h-3 w-3" />Review</>
            ) : course.status === "not-started" ? (
              <><PlayCircle className="h-3 w-3" />Start</>
            ) : (
              <>Continue<ChevronRight className="h-3 w-3 group-hover/btn:translate-x-0.5 transition-transform" /></>
            )}
          </button>
        </div>
      </div>
    </motion.div>
  );
}

// ─── Main ─────────────────────────────────────────────────────────────────────

function CoursesPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeTab, setActiveTab]     = useState<Tab>("all");
  const [query, setQuery]             = useState("");

  const filtered = courses.filter(c =>
    (activeTab === "all" || c.status === activeTab) &&
    (query === "" || c.title.toLowerCase().includes(query.toLowerCase()) || c.subject.toLowerCase().includes(query.toLowerCase()))
  );

  const totalXp      = courses.reduce((s, c) => s + c.xpEarned, 0);
  const totalLessons = courses.reduce((s, c) => s + c.lessons, 0);
  const totalAll     = courses.reduce((s, c) => s + c.total, 0);
  const completed    = courses.filter(c => c.status === "completed").length;
  const inProgress   = courses.filter(c => c.status === "in-progress").length;
  const overallPct   = Math.round((totalLessons / totalAll) * 100);

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">

        {/* Top bar */}
        <header className="flex h-16 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/80 backdrop-blur-md px-6 sticky top-0 z-20">
          <button className="md:hidden text-muted-foreground hover:text-foreground transition-colors" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </button>
          <Link to="/dashboard" className="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="h-4 w-4" />
            Home
          </Link>
          <div className="ml-auto flex items-center gap-2.5">
            <Link to="/level"
              className="hidden sm:inline-flex items-center gap-2 h-9 rounded-full border border-primary/25 bg-primary/6 px-4 text-xs font-semibold text-primary hover:bg-primary/12 hover:border-primary/40 transition-all"
            >
              <Shield className="h-3.5 w-3.5" />Level 7
            </Link>
            <div className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold shadow-soft hover:opacity-90 transition-opacity cursor-pointer">
              {user.initials}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-5xl px-6 lg:px-8 py-10 space-y-8">

            {/* ── Hero banner ── */}
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.55 }}
              className="relative rounded-3xl overflow-hidden bg-gradient-to-br from-primary to-primary-glow p-8 sm:p-10 grain"
            >
              {/* Decorative blobs */}
              <div className="absolute -top-12 -right-12 h-48 w-48 rounded-full bg-white/10 blur-3xl pointer-events-none" />
              <div className="absolute -bottom-10 -left-10 h-40 w-40 rounded-full bg-white/6 blur-2xl pointer-events-none" />

              <div className="relative z-10 flex flex-col sm:flex-row sm:items-center justify-between gap-6">
                <div>
                  <div className="inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/10 px-3.5 py-1.5 text-xs font-medium text-primary-foreground/80 mb-3 backdrop-blur-sm">
                    <Sparkles className="h-3 w-3" />
                    Your Learning Journey
                  </div>
                  <h1 className="text-display text-3xl text-primary-foreground leading-tight">My Courses</h1>
                  <p className="mt-1.5 text-sm text-primary-foreground/70">
                    {inProgress} in progress · {completed} completed · {totalLessons} lessons done
                  </p>
                </div>

                {/* Hero inline stats */}
                <div className="flex items-center gap-4 shrink-0">
                  <div className="text-center">
                    <p className="text-2xl font-semibold text-display text-white tabular-nums">{courses.length}</p>
                    <p className="text-xs text-primary-foreground/60 mt-0.5">Enrolled</p>
                  </div>
                  <div className="h-8 w-px bg-primary-foreground/20" />
                  <div className="text-center">
                    <p className="text-2xl font-semibold text-display text-white tabular-nums">{totalXp}</p>
                    <p className="text-xs text-primary-foreground/60 mt-0.5">XP Earned</p>
                  </div>
                  <div className="h-8 w-px bg-primary-foreground/20" />
                  <div className="text-center">
                    <p className="text-2xl font-semibold text-display text-white tabular-nums">{overallPct}%</p>
                    <p className="text-xs text-primary-foreground/60 mt-0.5">Overall</p>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* ── STATS ROW ── */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.1 }}
              className="grid grid-cols-2 lg:grid-cols-4 gap-3"
            >
              {[
                { label: "Enrolled",    value: String(courses.length), icon: BookOpen,    accent: "text-primary",   iconBg: "bg-primary/10",  border: "border-primary/14",  bg: "from-[oklch(0.99_0.006_140)] to-[oklch(0.975_0.010_145)]" },
                { label: "Completed",   value: String(completed),       icon: CheckCircle, accent: "text-teal",      iconBg: "bg-teal/10",     border: "border-teal/14",     bg: "from-[oklch(0.99_0.008_170)] to-[oklch(0.975_0.012_185)]" },
                { label: "In Progress", value: String(inProgress),      icon: PlayCircle,  accent: "text-gold",      iconBg: "bg-gold/10",     border: "border-gold/14",     bg: "from-[oklch(0.99_0.012_90)] to-[oklch(0.975_0.016_80)]"  },
                { label: "XP Earned",   value: String(totalXp),         icon: Zap,         accent: "text-amber-500", iconBg: "bg-amber-50 dark:bg-amber-500/10", border: "border-amber-200 dark:border-amber-500/14", bg: "from-[oklch(0.99_0.010_85)] to-[oklch(0.975_0.015_80)]" },
              ].map((s, i) => {
                const Icon = s.icon;
                return (
                  <motion.div
                    key={s.label}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.15 + i * 0.06 }}
                    className={`flex items-center gap-3.5 rounded-2xl border ${s.border} bg-gradient-to-br ${s.bg} px-4 py-3.5`}
                  >
                    <div className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl ${s.iconBg}`}>
                      <Icon className={`h-4 w-4 ${s.accent}`} strokeWidth={1.75} />
                    </div>
                    <div>
                      <p className="text-xl font-bold text-display text-foreground leading-none tabular-nums">{s.value}</p>
                      <p className="text-[10px] text-muted-foreground mt-0.5">{s.label}</p>
                    </div>
                  </motion.div>
                );
              })}
            </motion.div>

            {/* Overall progress */}
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.25 }}
              className="rounded-2xl border border-border/70 bg-surface-elevated px-6 py-5"
              style={{ boxShadow: "var(--shadow-soft)" }}
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <BarChart2 className="h-3.5 w-3.5 text-primary" />
                  <span className="text-sm font-semibold text-foreground">Overall progress</span>
                </div>
                <div className="flex items-center gap-2">
                  <TrendingUp className="h-3 w-3 text-teal" />
                  <span className="text-sm font-bold text-primary tabular-nums">{overallPct}%</span>
                </div>
              </div>
              <div className="h-2.5 rounded-full bg-border overflow-hidden">
                <motion.div
                  className="h-full rounded-full bg-gradient-primary"
                  initial={{ width: 0 }}
                  animate={{ width: `${overallPct}%` }}
                  transition={{ duration: 1.4, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
                />
              </div>
              <p className="mt-2 text-xs text-muted-foreground">
                <span className="font-medium text-foreground tabular-nums">{totalLessons}</span> of{" "}
                <span className="font-medium text-foreground tabular-nums">{totalAll}</span> lessons completed across all courses
              </p>
            </motion.div>

            {/* ── TABS + SEARCH ── */}
            <div className="flex flex-col sm:flex-row sm:items-center gap-3">
              <div className="flex items-center gap-1 rounded-2xl border border-border/70 bg-surface-elevated p-1 overflow-x-auto" style={{ boxShadow: "var(--shadow-soft)" }}>
                {tabs.map(tab => (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`relative shrink-0 flex items-center gap-1.5 rounded-xl px-4 py-2 text-xs font-semibold transition-all duration-200 ${
                      activeTab === tab.id
                        ? "bg-primary text-primary-foreground shadow-soft"
                        : "text-muted-foreground hover:text-foreground hover:bg-accent/60"
                    }`}
                  >
                    {tab.label}
                    <span className={`text-[10px] tabular-nums ${activeTab === tab.id ? "opacity-70" : "opacity-50"}`}>
                      {tab.count(courses)}
                    </span>
                  </button>
                ))}
              </div>
              <div className="flex items-center gap-2 flex-1 sm:max-w-xs ml-auto">
                <div className="flex flex-1 items-center gap-2 rounded-xl border border-border/80 bg-surface-elevated px-3 h-9 hover:border-primary/30 focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-ring/15 transition-all">
                  <Search className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                  <input
                    type="search"
                    placeholder="Search courses…"
                    value={query}
                    onChange={e => setQuery(e.target.value)}
                    className="flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground/50 outline-none"
                  />
                </div>
                <button className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-border/80 bg-surface-elevated text-muted-foreground hover:text-foreground hover:border-primary/30 transition-all">
                  <Filter className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>

            {/* ── COURSE GRID ── */}
            <AnimatePresence mode="popLayout">
              {filtered.length > 0 ? (
                <motion.div key="grid" className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5 pb-6">
                  {filtered.map((course, i) => (
                    <CourseCard key={course.id} course={course} index={i} />
                  ))}
                </motion.div>
              ) : (
                <motion.div
                  key="empty"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="flex flex-col items-center justify-center py-24 text-center"
                >
                  <div className="grid h-16 w-16 place-items-center rounded-3xl bg-muted mb-4 shadow-soft">
                    <BookOpen className="h-7 w-7 text-muted-foreground/40" strokeWidth={1.5} />
                  </div>
                  <p className="text-sm font-semibold text-foreground">No courses found</p>
                  <p className="text-xs text-muted-foreground mt-1">Try a different filter or search term</p>
                </motion.div>
              )}
            </AnimatePresence>

          </div>
        </main>
      </div>
    </div>
  );
}
