import { createFileRoute, Link } from "@tanstack/react-router";
import { AnimatePresence, motion } from "framer-motion";
import {
  ArrowLeft,
  Award,
  BarChart2,
  BookOpen,
  Briefcase,
  CheckCircle,
  ChevronRight,
  Clock,
  Code2,
  Compass,
  Filter,
  GraduationCap,
  Home,
  Languages,
  Lock,
  LogOut,
  Menu,
  Monitor,
  Palette,
  PlayCircle,
  Search,
  Settings,
  Shield,
  Sparkles,
  Star,
  TrendingUp,
  X,
  Zap,
} from "lucide-react";
import { useState } from "react";
import { learnerCourseCards } from "../lib/learner-flow-data";

export const Route = createFileRoute("/courses")({
  component: CoursesPage,
  head: () => ({ meta: [{ title: "My Courses - EduLife" }] }),
});

const user = { name: "Mohamed Baghdadi", email: "m.baghdadi@example.com", initials: "MB" };
const courses = learnerCourseCards;

const navLinks = [
  { icon: Home, label: "Home", to: "/dashboard" as const },
  { icon: BookOpen, label: "My Courses", to: "/courses" as const },
  { icon: Compass, label: "Explore", to: "/explore" as const },
  { icon: Award, label: "Certificates", to: "/certificates" as const },
  { icon: Settings, label: "Settings", to: "/profile" as const },
];

type Tab = "all" | "in-progress" | "completed" | "not-started";

const tabs: { id: Tab; label: string; count: (items: typeof courses) => number }[] = [
  { id: "all", label: "All", count: (items) => items.length },
  { id: "in-progress", label: "In Progress", count: (items) => items.filter((course) => course.status === "in-progress").length },
  { id: "completed", label: "Completed", count: (items) => items.filter((course) => course.status === "completed").length },
  { id: "not-started", label: "Not Started", count: (items) => items.filter((course) => course.status === "not-started").length },
];

function subjectIcon(subjectIcon: (typeof courses)[number]["subjectIcon"]) {
  return {
    code: Code2,
    language: Languages,
    business: Briefcase,
    design: Palette,
  }[subjectIcon] ?? Monitor;
}

function courseDestination(course: (typeof courses)[number]) {
  if (course.status === "not-started") {
    return {
      to: "/courses/$courseId/enroll" as const,
      label: "Enroll",
      icon: PlayCircle,
    };
  }

  if (course.status === "completed") {
    return {
      to: "/courses/$courseId" as const,
      label: "Review",
      icon: CheckCircle,
    };
  }

  return {
    to: "/courses/$courseId" as const,
    label: "Continue",
    icon: ChevronRight,
  };
}

function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-foreground/20 backdrop-blur-sm md:hidden" onClick={onClose} />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-surface-elevated transition-transform duration-300 ease-in-out ${
          open ? "translate-x-0" : "-translate-x-full"
        } md:static md:z-auto md:translate-x-0`}
        style={{ boxShadow: "var(--shadow-luxury)" }}
      >
        <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border/60 px-6">
          <div className="relative">
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-glow">
              <GraduationCap className="h-4 w-4" />
            </span>
            <span className="absolute -inset-1 -z-10 rounded-2xl bg-primary/20 blur-md" />
          </div>
          <span className="text-display text-lg tracking-tight text-foreground">EduLife</span>
          <button className="ml-auto text-muted-foreground transition-colors hover:text-foreground md:hidden" onClick={onClose}>
            <X className="h-4 w-4" />
          </button>
        </div>

        <nav className="flex-1 space-y-0.5 px-3 py-6">
          <p className="mb-3 px-3 text-[10px] font-medium uppercase tracking-[0.18em] text-muted-foreground/60">Main</p>
          {navLinks.map(({ icon: Icon, label, to }) => {
            const active = to === "/courses";
            return (
              <Link
                key={label}
                to={to}
                className={`relative flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                  active ? "bg-primary/10 text-primary shadow-sm" : "text-muted-foreground hover:bg-accent/80 hover:text-foreground"
                }`}
              >
                <Icon className="h-4 w-4 shrink-0" strokeWidth={active ? 2 : 1.75} />
                {label}
                {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" />}
              </Link>
            );
          })}
          <div className="mt-4 border-t border-border/60 pt-4">
            <p className="mb-3 px-3 text-[10px] font-medium uppercase tracking-[0.18em] text-muted-foreground/60">Progress</p>
            <Link
              to="/level"
              className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-muted-foreground transition-all duration-200 hover:bg-accent/80 hover:text-foreground"
            >
              <Shield className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              Level & Progress
            </Link>
          </div>
        </nav>

        <div className="border-t border-border/60 p-4">
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground">
                {user.initials}
              </div>
              <span className="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-surface-elevated bg-teal" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground">{user.name}</p>
              <p className="truncate text-xs text-muted-foreground">{user.email}</p>
            </div>
            <Link to="/login" className="text-muted-foreground transition-colors hover:text-foreground" aria-label="Log out">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>
    </>
  );
}

function CourseCard({ course, index }: { course: (typeof courses)[number]; index: number }) {
  const pct = course.total > 0 ? Math.round((course.lessons / course.total) * 100) : 0;
  const xpPct = course.xpTotal > 0 ? Math.round((course.xpEarned / course.xpTotal) * 100) : 0;
  const SubjectIcon = subjectIcon(course.subjectIcon);
  const destination = courseDestination(course);
  const ActionIcon = destination.icon;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.96 }}
      transition={{ duration: 0.4, delay: index * 0.06 }}
      className={`group relative flex flex-col overflow-hidden rounded-3xl border bg-surface-elevated transition-all duration-300 hover:-translate-y-0.5 hover:shadow-luxury ${course.accentBorder}`}
    >
      <div className="relative h-44 overflow-hidden bg-muted">
        <img src={course.thumbnail} alt={course.title} className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105" loading="lazy" />
        <div
          className="absolute inset-0"
          style={{
            background: `linear-gradient(to top, ${course.gradientFrom}e0 0%, ${course.gradientFrom}55 45%, transparent 100%)`,
          }}
        />

        <div className="absolute inset-0 overflow-hidden opacity-0 transition-opacity duration-300 group-hover:opacity-100">
          <div
            className="absolute inset-0 animate-shimmer"
            style={{
              background: "linear-gradient(105deg, transparent 40%, oklch(1 0 0 / 0.12) 50%, transparent 60%)",
            }}
          />
        </div>

        <div className="absolute left-3 top-3 flex items-center gap-1.5 rounded-full border border-border/50 bg-background/85 px-2.5 py-1 backdrop-blur-md shadow-soft">
          <SubjectIcon className={`h-3 w-3 ${course.accentText}`} strokeWidth={2} />
          <span className={`text-[10px] font-semibold uppercase tracking-[0.12em] ${course.accentText}`}>{course.subject}</span>
        </div>

        <div className="absolute bottom-3 left-3 flex items-center gap-1.5 rounded-full border border-border/40 bg-background/75 px-2.5 py-1 backdrop-blur-md">
          <Clock className="h-3 w-3 text-muted-foreground" strokeWidth={2} />
          <span className="text-[10px] font-semibold text-foreground/80">{course.duration}</span>
        </div>

        <div className="absolute right-3 top-3">
          {course.status === "completed" && (
            <span className="flex items-center gap-1 rounded-full bg-primary/90 px-2.5 py-1 text-[10px] font-semibold text-primary-foreground shadow-soft backdrop-blur-sm">
              <CheckCircle className="h-3 w-3" />
              Done
            </span>
          )}
          {course.status === "not-started" && (
            <span className="flex items-center gap-1 rounded-full border border-border/60 bg-background/80 px-2.5 py-1 text-[10px] font-semibold text-muted-foreground backdrop-blur-sm">
              <Lock className="h-3 w-3" />
              New
            </span>
          )}
          {course.status === "in-progress" && (
            <span className="flex items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-semibold text-white shadow-soft backdrop-blur-sm" style={{ background: `${course.gradientFrom}cc` }}>
              <PlayCircle className="h-3 w-3" />
              {pct}%
            </span>
          )}
        </div>

        {course.status !== "not-started" && (
          <div className="absolute inset-x-0 bottom-0 h-1 bg-black/20">
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

      <div className="flex flex-1 flex-col gap-3 p-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className={`relative grid h-7 w-7 shrink-0 place-items-center rounded-full ${course.instructorColor} text-[10px] font-bold text-white shadow-soft`}>
              {course.instructorInitials}
              <span className="absolute inset-0 rounded-full ring-2 ring-border ring-offset-1 ring-offset-surface-elevated" />
            </div>
            <span className="max-w-[120px] truncate text-xs font-medium text-muted-foreground">{course.instructor}</span>
          </div>
          <div className="flex shrink-0 items-center gap-1 text-xs font-semibold text-gold">
            <Star className="h-3 w-3 fill-gold" strokeWidth={0} />
            <span className="tabular-nums">{course.rating}</span>
          </div>
        </div>

        <h3 className="text-display text-sm font-semibold leading-snug text-foreground">{course.title}</h3>

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
            <span className="ml-auto flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {course.estimatedMin}m
            </span>
          )}
        </div>

        <div>
          <div className="h-1.5 overflow-hidden rounded-full bg-amber-100 dark:bg-amber-500/10">
            <motion.div
              className="h-full rounded-full bg-gradient-gold"
              initial={{ width: 0 }}
              animate={{ width: `${xpPct}%` }}
              transition={{ duration: 1.1, delay: 0.4 + index * 0.06, ease: [0.22, 1, 0.36, 1] }}
            />
          </div>
          <p className="mt-1 text-[10px] tabular-nums text-muted-foreground">
            {course.xpEarned}/{course.xpTotal} XP earned
          </p>
        </div>

        <div className="mt-auto flex items-center justify-between gap-2 border-t border-border/60 pt-3.5">
          <p className="truncate text-[11px] text-muted-foreground">
            {course.status === "completed" ? "Course complete" : `Next: ${course.nextLesson}`}
          </p>
          <Link
            to={destination.to}
            params={{ courseId: String(course.id) }}
            className={`group/btn flex h-8 shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl px-4 text-xs font-semibold transition-all duration-200 ${
              course.status === "completed"
                ? "border border-primary/20 bg-primary/8 text-primary hover:bg-primary/15"
                : course.status === "not-started"
                ? "bg-foreground text-background shadow-soft hover:opacity-90"
                : `${course.accentBg} ${course.accentText} border ${course.accentBorder} hover:opacity-80`
            }`}
          >
            <ActionIcon className={`h-3 w-3 ${destination.label === "Continue" ? "transition-transform group-hover/btn:translate-x-0.5" : ""}`} />
            {destination.label}
          </Link>
        </div>
      </div>
    </motion.div>
  );
}

function CoursesPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<Tab>("all");
  const [query, setQuery] = useState("");

  const filtered = courses.filter(
    (course) =>
      (activeTab === "all" || course.status === activeTab) &&
      (query === "" || course.title.toLowerCase().includes(query.toLowerCase()) || course.subject.toLowerCase().includes(query.toLowerCase())),
  );

  const totalXp = courses.reduce((sum, course) => sum + course.xpEarned, 0);
  const totalLessons = courses.reduce((sum, course) => sum + course.lessons, 0);
  const totalAll = courses.reduce((sum, course) => sum + course.total, 0);
  const completed = courses.filter((course) => course.status === "completed").length;
  const inProgress = courses.filter((course) => course.status === "in-progress").length;
  const overallPct = Math.round((totalLessons / totalAll) * 100);

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="sticky top-0 z-20 flex h-16 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/80 px-6 backdrop-blur-md">
          <button className="text-muted-foreground transition-colors hover:text-foreground md:hidden" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </button>
          <Link to="/dashboard" className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
            <ArrowLeft className="h-4 w-4" />
            Home
          </Link>
          <div className="ml-auto flex items-center gap-2.5">
            <Link
              to="/level"
              className="hidden h-9 items-center gap-2 rounded-full border border-primary/25 bg-primary/6 px-4 text-xs font-semibold text-primary transition-all hover:border-primary/40 hover:bg-primary/12 sm:inline-flex"
            >
              <Shield className="h-3.5 w-3.5" />
              Level 7
            </Link>
            <div className="grid h-9 w-9 cursor-pointer place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90">
              {user.initials}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-5xl space-y-8 px-6 py-10 lg:px-8">
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.55 }}
              className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary to-primary-glow p-8 grain sm:p-10"
            >
              <div className="pointer-events-none absolute -right-12 -top-12 h-48 w-48 rounded-full bg-white/10 blur-3xl" />
              <div className="pointer-events-none absolute -bottom-10 -left-10 h-40 w-40 rounded-full bg-white/6 blur-2xl" />

              <div className="relative z-10 flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
                <div>
                  <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/10 px-3.5 py-1.5 text-xs font-medium text-primary-foreground/80 backdrop-blur-sm">
                    <Sparkles className="h-3 w-3" />
                    Structured learner flow
                  </div>
                  <h1 className="text-display text-3xl leading-tight text-primary-foreground">My Courses</h1>
                  <p className="mt-1.5 text-sm text-primary-foreground/70">
                    Enroll, study, take the quiz, pass the final exam, and unlock your certificate.
                  </p>
                </div>

                <div className="flex shrink-0 items-center gap-4">
                  <div className="text-center">
                    <p className="text-display text-2xl font-semibold tabular-nums text-white">{courses.length}</p>
                    <p className="mt-0.5 text-xs text-primary-foreground/60">Enrolled</p>
                  </div>
                  <div className="h-8 w-px bg-primary-foreground/20" />
                  <div className="text-center">
                    <p className="text-display text-2xl font-semibold tabular-nums text-white">{totalXp}</p>
                    <p className="mt-0.5 text-xs text-primary-foreground/60">XP Earned</p>
                  </div>
                  <div className="h-8 w-px bg-primary-foreground/20" />
                  <div className="text-center">
                    <p className="text-display text-2xl font-semibold tabular-nums text-white">{overallPct}%</p>
                    <p className="mt-0.5 text-xs text-primary-foreground/60">Overall</p>
                  </div>
                </div>
              </div>
            </motion.div>

            <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }} className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              {[
                { label: "Enrolled", value: String(courses.length), icon: BookOpen, accent: "text-primary", iconBg: "bg-primary/10", border: "border-primary/14", bg: "from-[oklch(0.99_0.006_140)] to-[oklch(0.975_0.010_145)]" },
                { label: "Completed", value: String(completed), icon: CheckCircle, accent: "text-teal", iconBg: "bg-teal/10", border: "border-teal/14", bg: "from-[oklch(0.99_0.008_170)] to-[oklch(0.975_0.012_185)]" },
                { label: "In Progress", value: String(inProgress), icon: PlayCircle, accent: "text-gold", iconBg: "bg-gold/10", border: "border-gold/14", bg: "from-[oklch(0.99_0.012_90)] to-[oklch(0.975_0.016_80)]" },
                { label: "XP Earned", value: String(totalXp), icon: Zap, accent: "text-amber-500", iconBg: "bg-amber-50 dark:bg-amber-500/10", border: "border-amber-200 dark:border-amber-500/14", bg: "from-[oklch(0.99_0.010_85)] to-[oklch(0.975_0.015_80)]" },
              ].map((stat, index) => {
                const Icon = stat.icon;
                return (
                  <motion.div
                    key={stat.label}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.15 + index * 0.06 }}
                    className={`flex items-center gap-3.5 rounded-2xl border ${stat.border} bg-gradient-to-br ${stat.bg} px-4 py-3.5`}
                  >
                    <div className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl ${stat.iconBg}`}>
                      <Icon className={`h-4 w-4 ${stat.accent}`} strokeWidth={1.75} />
                    </div>
                    <div>
                      <p className="text-display text-xl font-bold leading-none tabular-nums text-foreground">{stat.value}</p>
                      <p className="mt-0.5 text-[10px] text-muted-foreground">{stat.label}</p>
                    </div>
                  </motion.div>
                );
              })}
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.25 }}
              className="rounded-2xl border border-border/70 bg-surface-elevated px-6 py-5"
              style={{ boxShadow: "var(--shadow-soft)" }}
            >
              <div className="mb-3 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <BarChart2 className="h-3.5 w-3.5 text-primary" />
                  <span className="text-sm font-semibold text-foreground">Overall progress</span>
                </div>
                <div className="flex items-center gap-2">
                  <TrendingUp className="h-3 w-3 text-teal" />
                  <span className="text-sm font-bold tabular-nums text-primary">{overallPct}%</span>
                </div>
              </div>
              <div className="h-2.5 overflow-hidden rounded-full bg-border">
                <motion.div className="h-full rounded-full bg-gradient-primary" initial={{ width: 0 }} animate={{ width: `${overallPct}%` }} transition={{ duration: 1.4, delay: 0.4, ease: [0.22, 1, 0.36, 1] }} />
              </div>
              <p className="mt-2 text-xs text-muted-foreground">
                <span className="font-medium tabular-nums text-foreground">{totalLessons}</span> of <span className="font-medium tabular-nums text-foreground">{totalAll}</span> lessons completed across all courses
              </p>
            </motion.div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="flex items-center gap-1 overflow-x-auto rounded-2xl border border-border/70 bg-surface-elevated p-1" style={{ boxShadow: "var(--shadow-soft)" }}>
                {tabs.map((tab) => (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`relative flex shrink-0 items-center gap-1.5 rounded-xl px-4 py-2 text-xs font-semibold transition-all duration-200 ${
                      activeTab === tab.id ? "bg-primary text-primary-foreground shadow-soft" : "text-muted-foreground hover:bg-accent/60 hover:text-foreground"
                    }`}
                  >
                    {tab.label}
                    <span className={`text-[10px] tabular-nums ${activeTab === tab.id ? "opacity-70" : "opacity-50"}`}>{tab.count(courses)}</span>
                  </button>
                ))}
              </div>
              <div className="ml-auto flex flex-1 items-center gap-2 sm:max-w-xs">
                <div className="flex h-9 flex-1 items-center gap-2 rounded-xl border border-border/80 bg-surface-elevated px-3 transition-all hover:border-primary/30 focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-ring/15">
                  <Search className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                  <input
                    type="search"
                    placeholder="Search courses..."
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    className="flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground/50"
                  />
                </div>
                <button className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-border/80 bg-surface-elevated text-muted-foreground transition-all hover:border-primary/30 hover:text-foreground">
                  <Filter className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>

            <AnimatePresence mode="popLayout">
              {filtered.length > 0 ? (
                <motion.div key="grid" className="grid gap-5 pb-6 sm:grid-cols-2 lg:grid-cols-3">
                  {filtered.map((course, index) => (
                    <CourseCard key={course.id} course={course} index={index} />
                  ))}
                </motion.div>
              ) : (
                <motion.div key="empty" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex flex-col items-center justify-center py-24 text-center">
                  <div className="mb-4 grid h-16 w-16 place-items-center rounded-3xl bg-muted shadow-soft">
                    <BookOpen className="h-7 w-7 text-muted-foreground/40" strokeWidth={1.5} />
                  </div>
                  <p className="text-sm font-semibold text-foreground">No courses found</p>
                  <p className="mt-1 text-xs text-muted-foreground">Try a different filter or search term</p>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </main>
      </div>
    </div>
  );
}
