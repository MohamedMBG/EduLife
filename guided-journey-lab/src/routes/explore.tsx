import { createFileRoute, Link } from "@tanstack/react-router";
import { motion, AnimatePresence } from "framer-motion";
import { useState, useMemo } from "react";
import {
  GraduationCap, Home, BookOpen, Compass, Award, Settings,
  LogOut, Shield, Search, Menu, X, Star, Clock, Users,
  Zap, ChevronRight, Layers, Code2, Languages, Briefcase,
  Palette, Monitor, FlaskConical, Music2, Camera, Dumbbell,
  Sparkles, SlidersHorizontal, Globe,
} from "lucide-react";

export const Route = createFileRoute("/explore")({
  component: ExplorePage,
  head: () => ({ meta: [{ title: "Explore Courses — EduLife" }] }),
});

// ─── Types ────────────────────────────────────────────────────────────────────

type Level = "Beginner" | "Intermediate" | "Advanced";
type Lang  = "Arabic" | "French" | "English" | "Darija";

interface Course {
  id: number;
  title: string;
  shortDescription: string;
  subject: string;
  SubjectIcon: React.ElementType;
  instructor: string;
  instructorInitials: string;
  instructorBg: string;
  thumbnail: string;
  lessonCount: number;
  duration: string;
  enrolled: number;
  rating: number;
  level: Level;
  language: Lang;
  xp: number;
  featured?: boolean;
  gradientFrom: string;
  gradientTo: string;
  accentText: string;
  accentBg: string;
  accentBorder: string;
  dotColor: string;
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const user = { name: "Mohamed Baghdadi", email: "m.baghdadi@example.com", initials: "MB" };

const courses: Course[] = [
  {
    id: 1,
    title: "Web Development Fundamentals",
    shortDescription: "HTML, CSS and JavaScript from the ground up. Build your first real websites.",
    subject: "Technology", SubjectIcon: Code2,
    instructor: "Khalid Moussaoui", instructorInitials: "KM", instructorBg: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 35, duration: "18h", enrolled: 1240, rating: 4.8,
    level: "Beginner", language: "English", xp: 700, featured: true,
    gradientFrom: "oklch(0.38 0.16 145)", gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary", accentBg: "bg-primary/8", accentBorder: "border-primary/20",
    dotColor: "bg-primary",
  },
  {
    id: 2,
    title: "Business Communication in Arabic",
    shortDescription: "Write clear emails, lead meetings, and negotiate in formal Arabic.",
    subject: "Language", SubjectIcon: Languages,
    instructor: "Fatima Tahiri", instructorInitials: "FT", instructorBg: "bg-gold",
    thumbnail: "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 30, duration: "22h", enrolled: 870, rating: 4.6,
    level: "Intermediate", language: "Arabic", xp: 600,
    gradientFrom: "oklch(0.74 0.17 77)", gradientTo: "oklch(0.64 0.20 68)",
    accentText: "text-gold", accentBg: "bg-gold/8", accentBorder: "border-gold/20",
    dotColor: "bg-gold",
  },
  {
    id: 3,
    title: "Data Analysis with Excel",
    shortDescription: "Pivot tables, VLOOKUP, dashboards — master the tool used in every Moroccan office.",
    subject: "Business", SubjectIcon: Briefcase,
    instructor: "Youssef Kettani", instructorInitials: "YK", instructorBg: "bg-teal",
    thumbnail: "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 20, duration: "14h", enrolled: 650, rating: 4.5,
    level: "Beginner", language: "French", xp: 400,
    gradientFrom: "oklch(0.66 0.15 194)", gradientTo: "oklch(0.52 0.17 187)",
    accentText: "text-teal", accentBg: "bg-teal/8", accentBorder: "border-teal/20",
    dotColor: "bg-teal",
  },
  {
    id: 4,
    title: "Python for Beginners",
    shortDescription: "Loops, functions, files and APIs. No prior coding experience needed.",
    subject: "Technology", SubjectIcon: Monitor,
    instructor: "Omar Bennis", instructorInitials: "OB", instructorBg: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1526379095098-d400fd0bf935?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 28, duration: "16h", enrolled: 2100, rating: 4.9,
    level: "Beginner", language: "English", xp: 560,
    gradientFrom: "oklch(0.38 0.16 145)", gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary", accentBg: "bg-primary/8", accentBorder: "border-primary/20",
    dotColor: "bg-primary",
  },
  {
    id: 5,
    title: "French for Professionals",
    shortDescription: "Business French for the Moroccan job market — written and spoken.",
    subject: "Language", SubjectIcon: Globe,
    instructor: "Nadia Alami", instructorInitials: "NA", instructorBg: "bg-gold",
    thumbnail: "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 36, duration: "20h", enrolled: 980, rating: 4.7,
    level: "Intermediate", language: "French", xp: 720,
    gradientFrom: "oklch(0.74 0.17 77)", gradientTo: "oklch(0.64 0.20 68)",
    accentText: "text-gold", accentBg: "bg-gold/8", accentBorder: "border-gold/20",
    dotColor: "bg-gold",
  },
  {
    id: 6,
    title: "Graphic Design Basics",
    shortDescription: "Typography, color theory, and layout fundamentals using free tools.",
    subject: "Design", SubjectIcon: Palette,
    instructor: "Salma Chraibi", instructorInitials: "SC", instructorBg: "bg-violet-500",
    thumbnail: "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 22, duration: "12h", enrolled: 760, rating: 4.4,
    level: "Beginner", language: "Darija", xp: 440,
    gradientFrom: "oklch(0.55 0.22 290)", gradientTo: "oklch(0.45 0.20 280)",
    accentText: "text-violet-500", accentBg: "bg-violet-500/8", accentBorder: "border-violet-500/20",
    dotColor: "bg-violet-500",
  },
  {
    id: 7,
    title: "Digital Marketing Essentials",
    shortDescription: "SEO, social media strategy, and paid ads — all in one guided path.",
    subject: "Business", SubjectIcon: Layers,
    instructor: "Hamid Rafiq", instructorInitials: "HR", instructorBg: "bg-teal",
    thumbnail: "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 26, duration: "15h", enrolled: 1050, rating: 4.6,
    level: "Beginner", language: "English", xp: 520,
    gradientFrom: "oklch(0.66 0.15 194)", gradientTo: "oklch(0.52 0.17 187)",
    accentText: "text-teal", accentBg: "bg-teal/8", accentBorder: "border-teal/20",
    dotColor: "bg-teal",
  },
  {
    id: 8,
    title: "Machine Learning with Python",
    shortDescription: "Linear models, trees, and neural networks — hands-on with real datasets.",
    subject: "Technology", SubjectIcon: FlaskConical,
    instructor: "Zineb Benali", instructorInitials: "ZB", instructorBg: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 42, duration: "30h", enrolled: 540, rating: 4.8,
    level: "Advanced", language: "English", xp: 1200, featured: true,
    gradientFrom: "oklch(0.38 0.16 145)", gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary", accentBg: "bg-primary/8", accentBorder: "border-primary/20",
    dotColor: "bg-primary",
  },
  {
    id: 9,
    title: "Photography & Visual Storytelling",
    shortDescription: "Frame, expose, and edit. Turn a phone camera into a professional tool.",
    subject: "Design", SubjectIcon: Camera,
    instructor: "Leila Fassi", instructorInitials: "LF", instructorBg: "bg-violet-500",
    thumbnail: "https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 18, duration: "11h", enrolled: 430, rating: 4.5,
    level: "Beginner", language: "Darija", xp: 360,
    gradientFrom: "oklch(0.55 0.22 290)", gradientTo: "oklch(0.45 0.20 280)",
    accentText: "text-violet-500", accentBg: "bg-violet-500/8", accentBorder: "border-violet-500/20",
    dotColor: "bg-violet-500",
  },
  {
    id: 10,
    title: "Fitness & Nutrition Science",
    shortDescription: "Evidence-based training and meal planning — no gym required.",
    subject: "Health", SubjectIcon: Dumbbell,
    instructor: "Rachid Oualid", instructorInitials: "RO", instructorBg: "bg-rose-500",
    thumbnail: "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 24, duration: "13h", enrolled: 890, rating: 4.7,
    level: "Beginner", language: "Arabic", xp: 480,
    gradientFrom: "oklch(0.60 0.22 15)", gradientTo: "oklch(0.50 0.20 10)",
    accentText: "text-rose-500", accentBg: "bg-rose-500/8", accentBorder: "border-rose-500/20",
    dotColor: "bg-rose-500",
  },
  {
    id: 11,
    title: "Oud for Beginners",
    shortDescription: "Learn the fundamentals of maqam, posture, and classical Moroccan repertoire.",
    subject: "Music", SubjectIcon: Music2,
    instructor: "Amine Hasnaoui", instructorInitials: "AH", instructorBg: "bg-amber-500",
    thumbnail: "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 20, duration: "10h", enrolled: 320, rating: 4.6,
    level: "Beginner", language: "Darija", xp: 400,
    gradientFrom: "oklch(0.74 0.17 77)", gradientTo: "oklch(0.64 0.20 68)",
    accentText: "text-amber-500", accentBg: "bg-amber-500/8", accentBorder: "border-amber-500/20",
    dotColor: "bg-amber-500",
  },
  {
    id: 12,
    title: "Advanced SQL & Databases",
    shortDescription: "Window functions, CTEs, query optimization — real production patterns.",
    subject: "Technology", SubjectIcon: Code2,
    instructor: "Mehdi Tazi", instructorInitials: "MT", instructorBg: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=600&h=340&fit=crop&auto=format&q=80",
    lessonCount: 32, duration: "20h", enrolled: 390, rating: 4.7,
    level: "Advanced", language: "French", xp: 900,
    gradientFrom: "oklch(0.38 0.16 145)", gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary", accentBg: "bg-primary/8", accentBorder: "border-primary/20",
    dotColor: "bg-primary",
  },
];

const CATEGORIES = ["All", "Technology", "Language", "Business", "Design", "Health", "Music"] as const;
const LEVELS     = ["All levels", "Beginner", "Intermediate", "Advanced"] as const;
const LANGUAGES  = ["All languages", "Arabic", "Darija", "French", "English"] as const;

type CategoryFilter = typeof CATEGORIES[number];
type LevelFilter    = typeof LEVELS[number];
type LangFilter     = typeof LANGUAGES[number];

const navLinks = [
  { icon: Home,     label: "Home",         to: "/dashboard" as const },
  { icon: BookOpen, label: "My Courses",   to: "/courses"   as const },
  { icon: Compass,  label: "Explore",      to: "/explore"   as const },
  { icon: Award,    label: "Certificates", to: "/dashboard" as const },
  { icon: Settings, label: "Settings",     to: "/dashboard" as const },
];

// ─── Sidebar ──────────────────────────────────────────────────────────────────

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

        <nav className="flex-1 px-3 py-6 space-y-0.5">
          <p className="px-3 mb-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Main</p>
          {navLinks.map(({ icon: Icon, label, to }) => {
            const active = to === "/explore";
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
            <Link to="/login" className="text-muted-foreground hover:text-foreground transition-colors" aria-label="Log out">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>
    </>
  );
}

// ─── Course card ──────────────────────────────────────────────────────────────

function CourseCard({ course, index }: { course: Course; index: number }) {
  const SubjectIcon = course.SubjectIcon;
  return (
    <motion.div
      layout
      key={course.id}
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.96 }}
      transition={{ duration: 0.38, delay: index * 0.05 }}
      className={`group relative flex flex-col rounded-3xl border bg-surface-elevated overflow-hidden
        hover:shadow-luxury hover:-translate-y-0.5 transition-all duration-300 ${course.accentBorder}`}
    >
      {/* Thumbnail */}
      <div className="relative h-44 overflow-hidden bg-muted shrink-0">
        <img
          src={course.thumbnail}
          alt={course.title}
          loading="lazy"
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
        <div
          className="absolute inset-0"
          style={{ background: `linear-gradient(to top, ${course.gradientFrom}dd 0%, ${course.gradientFrom}44 50%, transparent 100%)` }}
        />

        {/* Shimmer */}
        <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 overflow-hidden">
          <div className="absolute inset-0 animate-shimmer"
            style={{ background: "linear-gradient(105deg, transparent 40%, oklch(1 0 0 / 0.12) 50%, transparent 60%)" }}
          />
        </div>

        {/* Subject pill */}
        <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full px-2.5 py-1 bg-background/85 backdrop-blur-md border border-border/50 shadow-soft">
          <SubjectIcon className={`h-3 w-3 ${course.accentText}`} strokeWidth={2} />
          <span className={`text-[10px] font-semibold uppercase tracking-[0.12em] ${course.accentText}`}>{course.subject}</span>
        </div>

        {/* Level badge */}
        <div className="absolute top-3 right-3">
          <span className={`rounded-full px-2.5 py-1 text-[10px] font-semibold backdrop-blur-md border shadow-soft
            ${course.level === "Beginner"
              ? "bg-teal/80 text-white border-teal/40"
              : course.level === "Intermediate"
              ? "bg-gold/80 text-white border-gold/40"
              : "bg-primary/80 text-white border-primary/40"}`}
          >
            {course.level}
          </span>
        </div>

        {/* Duration bottom-left */}
        <div className="absolute bottom-3 left-3 flex items-center gap-1.5 rounded-full px-2.5 py-1 bg-background/75 backdrop-blur-md border border-border/40">
          <Clock className="h-3 w-3 text-muted-foreground" strokeWidth={2} />
          <span className="text-[10px] font-semibold text-foreground/80">{course.duration}</span>
        </div>

        {/* XP bottom-right */}
        <div className="absolute bottom-3 right-3 flex items-center gap-1 rounded-full px-2.5 py-1 bg-background/75 backdrop-blur-md border border-border/40">
          <Zap className="h-3 w-3 text-gold" strokeWidth={2} />
          <span className="text-[10px] font-semibold text-foreground/80">{course.xp} XP</span>
        </div>
      </div>

      {/* Body */}
      <div className="flex flex-col flex-1 p-5 gap-3">
        {/* Instructor + rating */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className={`grid h-6 w-6 shrink-0 place-items-center rounded-full ${course.instructorBg} text-[9px] font-bold text-white`}>
              {course.instructorInitials}
            </div>
            <span className="text-xs text-muted-foreground truncate max-w-[110px]">{course.instructor}</span>
          </div>
          <div className="flex items-center gap-1 text-xs font-semibold text-gold shrink-0">
            <Star className="h-3 w-3 fill-gold" strokeWidth={0} />
            <span className="tabular-nums">{course.rating}</span>
          </div>
        </div>

        {/* Title */}
        <h3 className="text-sm font-semibold text-display text-foreground leading-snug line-clamp-2">
          {course.title}
        </h3>

        {/* Description */}
        <p className="text-xs text-muted-foreground leading-relaxed line-clamp-2">{course.shortDescription}</p>

        {/* Meta row */}
        <div className="flex items-center gap-3 text-xs text-muted-foreground mt-auto">
          <span className="flex items-center gap-1">
            <BookOpen className="h-3 w-3" />
            {course.lessonCount} lessons
          </span>
          <span className="flex items-center gap-1">
            <Users className="h-3 w-3" />
            {course.enrolled >= 1000 ? `${(course.enrolled / 1000).toFixed(1)}k` : course.enrolled}
          </span>
          <span className={`ml-auto text-[10px] font-medium px-2 py-0.5 rounded-full ${
            course.language === "Arabic" || course.language === "Darija"
              ? "bg-gold/10 text-gold"
              : course.language === "French"
              ? "bg-teal/10 text-teal"
              : "bg-primary/8 text-primary"
          }`}>
            {course.language}
          </span>
        </div>

        {/* Enroll button */}
        <button className={`mt-1 w-full h-9 rounded-xl border text-xs font-semibold transition-all duration-200
          ${course.accentBg} ${course.accentText} ${course.accentBorder}
          hover:opacity-80 active:scale-[0.98]`}
        >
          Enroll — Free
        </button>
      </div>
    </motion.div>
  );
}

// ─── Featured card ────────────────────────────────────────────────────────────

function FeaturedCard({ course }: { course: Course }) {
  const SubjectIcon = course.SubjectIcon;
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.55 }}
      className="relative rounded-3xl overflow-hidden grain"
      style={{ background: `linear-gradient(135deg, ${course.gradientFrom}, ${course.gradientTo})` }}
    >
      <div className="absolute -top-20 -right-20 h-64 w-64 rounded-full bg-white/10 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-12 -left-12 h-48 w-48 rounded-full bg-white/6 blur-2xl pointer-events-none" />

      <div className="relative z-10 flex flex-col sm:flex-row gap-6 p-8 sm:p-10">
        <div className="flex-1 min-w-0">
          <div className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3.5 py-1.5 text-xs font-medium text-white/80 mb-4 backdrop-blur-sm">
            <Sparkles className="h-3 w-3" />
            Featured Course
          </div>
          <h2 className="text-display text-2xl sm:text-3xl text-white leading-tight">{course.title}</h2>
          <p className="mt-2 text-sm text-white/70 max-w-md leading-relaxed">{course.shortDescription}</p>
          <div className="mt-5 flex flex-wrap items-center gap-4 text-sm text-white/75">
            <span className="flex items-center gap-2"><SubjectIcon className="h-4 w-4" />{course.subject}</span>
            <span className="flex items-center gap-2"><Clock className="h-4 w-4" />{course.duration}</span>
            <span className="flex items-center gap-2"><Users className="h-4 w-4" />{course.enrolled.toLocaleString()} enrolled</span>
            <span className="flex items-center gap-2"><Zap className="h-4 w-4" />{course.xp} XP</span>
          </div>
          <div className="mt-6 flex items-center gap-3">
            <button className="inline-flex h-10 items-center gap-2 rounded-full bg-white text-foreground px-6 text-sm font-semibold hover:opacity-90 active:scale-[0.98] transition-all shadow-elevated">
              Enroll — Free
              <ChevronRight className="h-4 w-4" />
            </button>
            <div className="flex items-center gap-1 text-white font-semibold text-sm">
              <Star className="h-4 w-4 fill-white" strokeWidth={0} />
              {course.rating}
            </div>
          </div>
        </div>

        <div className="sm:w-52 lg:w-64 shrink-0 rounded-2xl overflow-hidden shadow-elevated hidden sm:block self-stretch">
          <img src={course.thumbnail} alt={course.title} className="w-full h-full object-cover" />
        </div>
      </div>
    </motion.div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

function ExplorePage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [query,       setQuery]       = useState("");
  const [category,   setCategory]    = useState<CategoryFilter>("All");
  const [level,      setLevel]       = useState<LevelFilter>("All levels");
  const [language,   setLanguage]    = useState<LangFilter>("All languages");
  const [filtersOpen, setFiltersOpen] = useState(false);

  const featured = courses.find(c => c.featured) ?? courses[0];

  const filtered = useMemo(() => courses.filter(c => {
    const q = query.toLowerCase();
    if (q && !c.title.toLowerCase().includes(q) && !c.subject.toLowerCase().includes(q) && !c.shortDescription.toLowerCase().includes(q)) return false;
    if (category !== "All" && c.subject !== category) return false;
    if (level !== "All levels" && c.level !== level) return false;
    if (language !== "All languages" && c.language !== language) return false;
    return true;
  }), [query, category, level, language]);

  const hasFilters = category !== "All" || level !== "All levels" || language !== "All languages" || query !== "";

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">

        {/* Top bar */}
        <header className="flex h-16 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/80 backdrop-blur-md px-6 sticky top-0 z-20">
          <button className="md:hidden text-muted-foreground hover:text-foreground transition-colors" onClick={() => setSidebarOpen(true)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </button>

          {/* Search */}
          <div className="flex flex-1 items-center gap-2 rounded-full border border-border/80 bg-surface px-4 h-9 max-w-sm hover:border-primary/30 transition-colors focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-ring/20">
            <Search className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
            <input
              type="search"
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder="Search courses..."
              className="flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground/50 outline-none"
            />
          </div>

          <div className="ml-auto flex items-center gap-2.5">
            <button
              onClick={() => setFiltersOpen(v => !v)}
              className={`hidden sm:inline-flex items-center gap-2 h-9 rounded-full border px-4 text-xs font-medium transition-all ${
                filtersOpen || hasFilters
                  ? "border-primary/40 bg-primary/8 text-primary"
                  : "border-border/80 bg-surface text-muted-foreground hover:text-foreground hover:border-primary/30"
              }`}
            >
              <SlidersHorizontal className="h-3.5 w-3.5" />
              Filters
              {hasFilters && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
            </button>
            <Link to="/level"
              className="hidden sm:inline-flex items-center gap-2 h-9 rounded-full border border-primary/25 bg-primary/6 px-4 text-xs font-semibold text-primary hover:bg-primary/12 hover:border-primary/40 transition-all"
            >
              <Shield className="h-3.5 w-3.5" />
              Level 7
            </Link>
            <div className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold shadow-soft hover:opacity-90 transition-opacity cursor-pointer">
              {user.initials}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-6xl px-6 lg:px-8 py-10 space-y-8">

            {/* Featured */}
            <FeaturedCard course={featured} />

            {/* Category chips */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
                {CATEGORIES.map(cat => (
                  <button
                    key={cat}
                    onClick={() => setCategory(cat)}
                    className={`shrink-0 h-9 rounded-full px-4 text-sm font-medium transition-all duration-200 ${
                      category === cat
                        ? "bg-foreground text-background shadow-soft"
                        : "border border-border/80 bg-surface text-muted-foreground hover:text-foreground hover:border-primary/30"
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>

              {/* Expandable filter row */}
              <AnimatePresence>
                {filtersOpen && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    transition={{ duration: 0.22 }}
                    className="overflow-hidden"
                  >
                    <div className="flex flex-wrap gap-2 pt-1">
                      {LEVELS.map(lv => (
                        <button key={lv} onClick={() => setLevel(lv)}
                          className={`h-8 rounded-full px-3.5 text-xs font-medium transition-all border ${
                            level === lv
                              ? "bg-teal/10 text-teal border-teal/30"
                              : "border-border/70 text-muted-foreground hover:text-foreground"
                          }`}
                        >
                          {lv}
                        </button>
                      ))}
                      <div className="w-px h-8 bg-border/60 mx-1 self-center" />
                      {LANGUAGES.map(lang => (
                        <button key={lang} onClick={() => setLanguage(lang)}
                          className={`h-8 rounded-full px-3.5 text-xs font-medium transition-all border ${
                            language === lang
                              ? "bg-gold/10 text-gold border-gold/30"
                              : "border-border/70 text-muted-foreground hover:text-foreground"
                          }`}
                        >
                          {lang}
                        </button>
                      ))}
                      {hasFilters && (
                        <button
                          onClick={() => { setCategory("All"); setLevel("All levels"); setLanguage("All languages"); setQuery(""); }}
                          className="h-8 rounded-full px-3.5 text-xs font-medium border border-destructive/30 text-destructive hover:bg-destructive/5 transition-all"
                        >
                          Clear all
                        </button>
                      )}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Results header */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="h-1 w-1 rounded-full bg-primary/60" />
                <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">
                  {filtered.length} {filtered.length === 1 ? "course" : "courses"}
                  {category !== "All" ? ` in ${category}` : ""}
                </h2>
              </div>
              {/* Mobile filters toggle */}
              <button
                onClick={() => setFiltersOpen(v => !v)}
                className={`sm:hidden flex items-center gap-1.5 text-xs font-medium transition-colors ${
                  filtersOpen || hasFilters ? "text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                <SlidersHorizontal className="h-3.5 w-3.5" />
                Filters
              </button>
            </div>

            {/* Grid */}
            <AnimatePresence mode="popLayout">
              {filtered.length > 0 ? (
                <motion.div
                  layout
                  className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5 pb-10"
                >
                  {filtered.map((c, i) => (
                    <CourseCard key={c.id} course={c} index={i} />
                  ))}
                </motion.div>
              ) : (
                <motion.div
                  key="empty"
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex flex-col items-center justify-center py-24 text-center"
                >
                  <div className="grid h-16 w-16 place-items-center rounded-2xl bg-muted text-muted-foreground mb-4">
                    <Search className="h-7 w-7" strokeWidth={1.5} />
                  </div>
                  <p className="text-base font-medium text-foreground">No courses found</p>
                  <p className="mt-1 text-sm text-muted-foreground">Try different filters or a broader search term.</p>
                  <button
                    onClick={() => { setCategory("All"); setLevel("All levels"); setLanguage("All languages"); setQuery(""); }}
                    className="mt-5 h-9 rounded-full border border-border px-5 text-sm font-medium text-foreground hover:bg-accent transition-colors"
                  >
                    Clear filters
                  </button>
                </motion.div>
              )}
            </AnimatePresence>

          </div>
        </main>
      </div>
    </div>
  );
}
