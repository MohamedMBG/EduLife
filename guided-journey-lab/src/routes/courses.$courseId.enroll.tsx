import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  Award,
  BookOpen,
  CheckCircle,
  Clock,
  Download,
  FileQuestion,
  GraduationCap,
  PlayCircle,
  Shield,
  UserCheck,
  Video,
} from "lucide-react";
import { getLearnerCourseDetail } from "../lib/learner-flow-data";

export const Route = createFileRoute("/courses/$courseId/enroll")({
  component: EnrollCoursePage,
  head: () => ({ meta: [{ title: "Enroll - EduLife" }] }),
});

function EnrollCoursePage() {
  const { courseId } = Route.useParams();
  const course = getLearnerCourseDetail(courseId);
  const firstLesson = course.lessons.find((lesson) => !lesson.locked) ?? course.lessons[0];

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-20 flex h-16 items-center gap-4 border-b border-border/60 bg-surface-elevated/85 px-6 backdrop-blur-md">
        <Link to="/courses/$courseId" params={{ courseId }} className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
          <ArrowLeft className="h-4 w-4" />
          Course overview
        </Link>
        <div className="ml-auto inline-flex items-center gap-2 rounded-full border border-primary/25 bg-primary/6 px-4 py-2 text-xs font-semibold text-primary">
          <Shield className="h-3.5 w-3.5" />
          Enroll before learner access
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-10 lg:px-8">
        <div className="grid gap-8 lg:grid-cols-[1.2fr_0.8fr]">
          <motion.section
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="overflow-hidden rounded-3xl border border-border/70 bg-surface-elevated"
            style={{ boxShadow: "var(--shadow-luxury)" }}
          >
            <div
              className="relative overflow-hidden px-8 py-10 text-white"
              style={{ background: `linear-gradient(135deg, ${course.gradientFrom}, ${course.gradientTo})` }}
            >
              <div className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-white/10 blur-3xl" />
              <div className="relative z-10">
                <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3.5 py-1.5 text-xs font-medium text-white/85 backdrop-blur-sm">
                  <GraduationCap className="h-3.5 w-3.5" />
                  New learner flow
                </div>
                <h1 className="text-display text-3xl leading-tight">{course.title}</h1>
                <p className="mt-2 max-w-2xl text-sm leading-relaxed text-white/75">{course.description}</p>
              </div>
            </div>

            <div className="space-y-8 px-8 py-8">
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                {[
                  { label: "Lessons", value: String(course.totalLessons), icon: BookOpen },
                  { label: "Duration", value: course.duration, icon: Clock },
                  { label: "Final exam", value: "80% pass", icon: FileQuestion },
                  { label: "Certificate", value: "Included", icon: Award },
                ].map(({ label, value, icon: Icon }) => (
                  <div key={label} className="rounded-2xl border border-border/70 bg-surface px-4 py-4">
                    <div className="mb-2 grid h-9 w-9 place-items-center rounded-xl bg-primary/8 text-primary">
                      <Icon className="h-4 w-4" strokeWidth={1.75} />
                    </div>
                    <p className="text-display text-xl font-semibold text-foreground">{value}</p>
                    <p className="mt-0.5 text-[11px] text-muted-foreground">{label}</p>
                  </div>
                ))}
              </div>

              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <span className="h-1 w-1 rounded-full bg-primary/60" />
                  <h2 className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">What happens after enrollment</h2>
                  <div className="h-px flex-1 bg-gradient-to-r from-border to-transparent" />
                </div>

                {/* This stepper explains the exact frontend flow the learner will follow after enrollment. */}
                <div className="grid gap-4">
                  {[
                    { icon: Video, title: "Watch the lesson video", body: "The player screen opens first so the learner can start studying immediately." },
                    { icon: Download, title: "Read the lesson PDF", body: "Each lesson exposes a reading tab with a PDF-style summary for review." },
                    { icon: FileQuestion, title: "Take the quiz", body: "The learner completes a lesson quiz, then moves through the rest of the course." },
                    { icon: Award, title: "Pass the final exam", body: "The final exam is server-scored in the real flow and unlocks the certificate only after a passing score." },
                  ].map(({ icon: Icon, title, body }, index) => (
                    <div key={title} className="flex gap-4 rounded-2xl border border-border/70 bg-surface px-5 py-4">
                      <div className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-primary/8 text-primary">
                        <Icon className="h-5 w-5" strokeWidth={1.75} />
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-semibold uppercase tracking-[0.18em] text-primary">Step {index + 1}</span>
                        </div>
                        <p className="mt-1 text-sm font-semibold text-foreground">{title}</p>
                        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">{body}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.section>

          <motion.aside initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.08 }} className="space-y-5">
            <div className="rounded-3xl border border-border/70 bg-surface-elevated p-6" style={{ boxShadow: "var(--shadow-soft)" }}>
              <div className="mb-4 flex items-center gap-3">
                <div className="grid h-11 w-11 place-items-center rounded-2xl bg-primary/10 text-primary">
                  <UserCheck className="h-5 w-5" strokeWidth={1.75} />
                </div>
                <div>
                  <p className="text-sm font-semibold text-foreground">Enrollment confirmation</p>
                  <p className="text-xs text-muted-foreground">This grants lesson, PDF, quiz, and exam access.</p>
                </div>
              </div>

              <div className="space-y-3 rounded-2xl border border-border/70 bg-surface px-4 py-4">
                {[
                  "Structured lessons with guided order",
                  "Reading resources for each lesson",
                  "Final exam access after course completion",
                  "Certificate only after a passing result",
                ].map((item) => (
                  <div key={item} className="flex items-start gap-2.5 text-sm text-muted-foreground">
                    <CheckCircle className="mt-0.5 h-4 w-4 shrink-0 text-primary" strokeWidth={2} />
                    <span>{item}</span>
                  </div>
                ))}
              </div>

              <div className="mt-5 space-y-3">
                <Link
                  to="/courses/$courseId/lessons/$lessonId"
                  params={{ courseId: course.id, lessonId: firstLesson.id }}
                  className="flex h-11 w-full items-center justify-center gap-2 rounded-2xl bg-primary text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90"
                >
                  <PlayCircle className="h-4 w-4" />
                  Confirm enrollment and start lesson 1
                </Link>
                <Link
                  to="/courses/$courseId"
                  params={{ courseId: course.id }}
                  className="flex h-10 w-full items-center justify-center rounded-2xl border border-border/80 text-sm font-medium text-foreground transition-colors hover:bg-accent"
                >
                  Review syllabus
                </Link>
              </div>
            </div>

            <div className="rounded-3xl border border-border/70 bg-surface-elevated p-6" style={{ boxShadow: "var(--shadow-soft)" }}>
              <div className="mb-4 flex items-center gap-3">
                <span className="h-1 w-1 rounded-full bg-primary/60" />
                <h2 className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">First lesson opens with</h2>
              </div>
              <div className="space-y-3">
                {[
                  { icon: Video, label: "Video player" },
                  { icon: Download, label: "PDF reading panel" },
                  { icon: FileQuestion, label: "Lesson quiz tab" },
                ].map(({ icon: Icon, label }) => (
                  <div key={label} className="flex items-center gap-3 rounded-2xl border border-border/70 bg-surface px-4 py-3">
                    <div className="grid h-9 w-9 place-items-center rounded-xl bg-primary/8 text-primary">
                      <Icon className="h-4 w-4" strokeWidth={1.75} />
                    </div>
                    <span className="text-sm font-medium text-foreground">{label}</span>
                  </div>
                ))}
              </div>
            </div>
          </motion.aside>
        </div>
      </main>
    </div>
  );
}
