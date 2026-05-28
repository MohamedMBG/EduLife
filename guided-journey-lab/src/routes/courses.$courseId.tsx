import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  Award,
  BarChart2,
  BookOpen,
  CheckCircle,
  ChevronRight,
  Clock,
  Compass,
  FileQuestion,
  GraduationCap,
  Home,
  Languages,
  Lock,
  LogOut,
  Menu,
  PlayCircle,
  Settings,
  Shield,
  Star,
  Users,
  Video,
  X,
  Zap,
} from "lucide-react";
import { useState } from "react";
import { getLearnerCourseDetail, getLearnerNextLesson } from "../lib/learner-flow-data";

export const Route = createFileRoute("/courses/$courseId")({
  component: CourseDetailPage,
  head: () => ({ meta: [{ title: "Course - EduLife" }] }),
});

const navLinks = [
  { icon: Home, label: "Home", to: "/dashboard" as const },
  { icon: BookOpen, label: "My Courses", to: "/courses" as const },
  { icon: Compass, label: "Explore", to: "/explore" as const },
  { icon: Award, label: "Certificates", to: "/certificates" as const },
  { icon: Settings, label: "Settings", to: "/profile" as const },
];

const user = { name: "Mohamed Baghdadi", email: "m.baghdadi@example.com", initials: "MB" };

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
            <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground">
              {user.initials}
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

function CourseDetailPage() {
  const { courseId } = Route.useParams();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const course = getLearnerCourseDetail(courseId);
  const pct = Math.round((course.completedLessons / course.totalLessons) * 100);
  const nextLesson = getLearnerNextLesson(course);
  const isNewEnrollment = course.completedLessons === 0;

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="sticky top-0 z-20 flex h-16 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/80 px-6 backdrop-blur-md">
          <button className="text-muted-foreground transition-colors hover:text-foreground md:hidden" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </button>
          <Link to="/courses" className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
            <ArrowLeft className="h-4 w-4" />
            My Courses
          </Link>
          <div className="ml-auto flex items-center gap-2.5">
            <Link
              to="/level"
              className="hidden h-9 items-center gap-2 rounded-full border border-primary/25 bg-primary/6 px-4 text-xs font-semibold text-primary transition-all hover:border-primary/40 hover:bg-primary/12 sm:inline-flex"
            >
              <Shield className="h-3.5 w-3.5" />
              Level 7
            </Link>
            <Link to="/profile" className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90">
              {user.initials}
            </Link>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-5xl space-y-8 px-6 py-10 lg:px-8">
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.55 }}
              className="relative overflow-hidden rounded-3xl grain"
              style={{ background: `linear-gradient(135deg, ${course.gradientFrom}, ${course.gradientTo})` }}
            >
              <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
              <div className="pointer-events-none absolute -bottom-12 -left-12 h-48 w-48 rounded-full bg-white/6 blur-2xl" />
              <div className="relative z-10 flex flex-col gap-6 p-8 sm:p-10 lg:flex-row">
                <div className="min-w-0 flex-1">
                  <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3.5 py-1.5 text-xs font-medium text-white/80 backdrop-blur-sm">
                    {course.subject}
                  </div>
                  <h1 className="text-display text-2xl leading-tight text-white sm:text-3xl">{course.title}</h1>
                  <p className="mt-2 max-w-xl text-sm leading-relaxed text-white/70">{course.description}</p>

                  <div className="mt-5 flex flex-wrap items-center gap-4 text-sm text-white/75">
                    <span className="flex items-center gap-1.5">
                      <Clock className="h-4 w-4" />
                      {course.duration}
                    </span>
                    <span className="flex items-center gap-1.5">
                      <Users className="h-4 w-4" />
                      {course.enrolled.toLocaleString()} enrolled
                    </span>
                    <span className="flex items-center gap-1.5">
                      <Star className="h-4 w-4 fill-white/60" strokeWidth={0} />
                      {course.rating}
                    </span>
                    <span className="flex items-center gap-1.5">
                      <Zap className="h-4 w-4" />
                      {course.xp} XP
                    </span>
                  </div>

                  <div className="mt-6 max-w-sm">
                    <div className="mb-1.5 flex items-center justify-between text-xs text-white/65">
                      <span>
                        {course.completedLessons} of {course.totalLessons} lessons
                      </span>
                      <span className="font-semibold tabular-nums">{pct}%</span>
                    </div>
                    <div className="h-1.5 overflow-hidden rounded-full bg-white/20">
                      <motion.div className="h-full rounded-full bg-white/85" initial={{ width: 0 }} animate={{ width: `${pct}%` }} transition={{ duration: 1.2, delay: 0.4, ease: [0.22, 1, 0.36, 1] }} />
                    </div>
                  </div>

                  <div className="mt-6 flex flex-wrap items-center gap-3">
                    {isNewEnrollment ? (
                      <Link
                        to="/courses/$courseId/enroll"
                        params={{ courseId: course.id }}
                        className="inline-flex h-10 items-center gap-2.5 rounded-2xl bg-white px-6 text-sm font-semibold text-foreground shadow-elevated transition-all hover:opacity-90 active:scale-[0.98]"
                      >
                        <PlayCircle className="h-4 w-4" />
                        Enroll now
                      </Link>
                    ) : nextLesson ? (
                      <Link
                        to="/courses/$courseId/lessons/$lessonId"
                        params={{ courseId: course.id, lessonId: nextLesson.id }}
                        className="inline-flex h-10 items-center gap-2.5 rounded-2xl bg-white px-6 text-sm font-semibold text-foreground shadow-elevated transition-all hover:opacity-90 active:scale-[0.98]"
                      >
                        <PlayCircle className="h-4 w-4" />
                        Continue learning
                      </Link>
                    ) : (
                      <Link
                        to="/courses/$courseId/exam"
                        params={{ courseId: course.id }}
                        className="inline-flex h-10 items-center gap-2.5 rounded-2xl bg-white px-6 text-sm font-semibold text-foreground shadow-elevated transition-all hover:opacity-90 active:scale-[0.98]"
                      >
                        <FileQuestion className="h-4 w-4" />
                        Take final exam
                      </Link>
                    )}
                    <div className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-xs font-medium text-white/80 backdrop-blur-sm">
                      <Award className="h-3.5 w-3.5" />
                      Certificate unlocked after passing the exam
                    </div>
                  </div>
                </div>

                <div className="hidden min-h-[180px] shrink-0 self-stretch overflow-hidden rounded-2xl shadow-elevated lg:block lg:w-64 xl:w-72">
                  <img src={course.thumbnail} alt={course.title} className="h-full w-full object-cover" />
                </div>
              </div>
            </motion.div>

            <div className="grid gap-8 lg:grid-cols-3">
              <div className="space-y-4 lg:col-span-2">
                <div className="flex items-center gap-3">
                  <span className="h-1 w-1 rounded-full bg-primary/60" />
                  <h2 className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">{course.totalLessons} lessons</h2>
                  <div className="h-px flex-1 bg-gradient-to-r from-border to-transparent" />
                </div>

                <div className="overflow-hidden rounded-2xl border border-border/70 bg-surface-elevated divide-y divide-border/60" style={{ boxShadow: "var(--shadow-soft)" }}>
                  {course.lessons.map((lesson, index) => {
                    const isAvailable = !lesson.locked;
                    const lessonIcon = lesson.type === "quiz" ? FileQuestion : lesson.type === "reading" ? Languages : Video;

                    return (
                      <motion.div
                        key={lesson.id}
                        initial={{ opacity: 0, x: -8 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.35, delay: index * 0.04 }}
                        className={`flex items-center gap-4 px-5 py-4 ${isAvailable ? "hover:bg-accent/40" : "opacity-50"}`}
                      >
                        <div className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl ${lesson.completed ? "bg-primary/10" : isAvailable ? "bg-border/60" : "bg-muted"}`}>
                          {lesson.completed ? <CheckCircle className="h-4 w-4 text-primary" strokeWidth={2} /> : isAvailable ? <PlayCircle className="h-4 w-4 text-muted-foreground" strokeWidth={1.75} /> : <Lock className="h-4 w-4 text-muted-foreground" strokeWidth={1.75} />}
                        </div>

                        <div className="min-w-0 flex-1">
                          <p className={`text-sm font-medium leading-snug ${isAvailable ? "text-foreground" : "text-muted-foreground"}`}>
                            {index + 1}. {lesson.title}
                          </p>
                          <p className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                            {(() => {
                              const LessonIcon = lessonIcon;
                              return <LessonIcon className="h-3 w-3" />;
                            })()}
                            {lesson.duration}
                          </p>
                        </div>

                        {isAvailable ? (
                          <Link
                            to="/courses/$courseId/lessons/$lessonId"
                            params={{ courseId: course.id, lessonId: lesson.id }}
                            className="shrink-0 flex items-center gap-1 text-xs font-semibold text-primary transition-colors hover:text-primary-glow"
                          >
                            Open
                            <ChevronRight className="h-3 w-3" />
                          </Link>
                        ) : (
                          <span className="text-xs font-medium text-muted-foreground">Locked</span>
                        )}
                      </motion.div>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-4">
                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.15 }} className="space-y-4 rounded-2xl border border-border/70 bg-surface-elevated p-5" style={{ boxShadow: "var(--shadow-soft)" }}>
                  <div className="flex items-center gap-3">
                    <span className="h-1 w-1 rounded-full bg-primary/60" />
                    <h3 className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">Instructor</h3>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-primary text-sm font-bold text-white shadow-soft">{course.instructorInitials}</div>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{course.instructor}</p>
                      <p className="mt-0.5 text-xs leading-relaxed text-muted-foreground">{course.instructorBio}</p>
                    </div>
                  </div>
                </motion.div>

                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }} className="rounded-2xl border border-border/70 bg-surface-elevated p-5" style={{ boxShadow: "var(--shadow-soft)" }}>
                  <div className="mb-4 flex items-center gap-3">
                    <span className="h-1 w-1 rounded-full bg-primary/60" />
                    <h3 className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">Course info</h3>
                  </div>
                  <div className="space-y-3">
                    {[
                      { icon: BarChart2, label: "Level", value: course.level },
                      { icon: Clock, label: "Duration", value: course.duration },
                      { icon: BookOpen, label: "Lessons", value: String(course.totalLessons) },
                      { icon: Zap, label: "XP reward", value: `${course.xp} XP` },
                      { icon: Languages, label: "Language", value: course.language },
                      { icon: Star, label: "Rating", value: String(course.rating) },
                    ].map(({ icon: Icon, label, value }) => (
                      <div key={label} className="flex items-center justify-between text-sm">
                        <span className="flex items-center gap-2 text-muted-foreground">
                          <Icon className="h-3.5 w-3.5" strokeWidth={1.75} />
                          {label}
                        </span>
                        <span className="font-medium text-foreground">{value}</span>
                      </div>
                    ))}
                  </div>
                </motion.div>

                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.25 }} className="rounded-2xl border border-primary/25 bg-primary/6 p-5">
                  <div className="mx-auto mb-3 grid h-11 w-11 place-items-center rounded-2xl bg-primary/10">
                    <Award className="h-5 w-5 text-primary" strokeWidth={1.75} />
                  </div>
                  <p className="text-center text-sm font-semibold text-foreground">Completion path</p>
                  <div className="mt-3 space-y-2 text-xs text-muted-foreground">
                    {/* This checklist clarifies the required learner flow before backend state is wired in. */}
                    {["Enroll in the course", "Watch the lesson video", "Read the PDF summary", "Finish the lesson quiz", "Pass the final exam with 80%"].map((step) => (
                      <div key={step} className="flex items-start gap-2">
                        <CheckCircle className="mt-0.5 h-3.5 w-3.5 shrink-0 text-primary" />
                        <span>{step}</span>
                      </div>
                    ))}
                  </div>
                </motion.div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
