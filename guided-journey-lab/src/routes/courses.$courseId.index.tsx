import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, BookOpen, CheckCircle2, CirclePlay, Clock3, GraduationCap, Layers3, Lock, FileText, ChevronRight, Video } from "lucide-react";
import type { ReactNode } from "react";
import { AppShell } from "../components/app/AppShell";
import {
  enrollInCourse,
  getCourseDetail,
  getCourseProgress,
  listMyEnrollments,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/courses/$courseId/")({
  component: CourseDetailRoute,
  head: () => ({ meta: [{ title: "Course Detail - EduLife" }] }),
});

function CourseDetailRoute() {
  return (
    <RequireAuth>
      <CourseDetailPage />
    </RequireAuth>
  );
}

function CourseDetailPage() {
  const { courseId } = Route.useParams();
  const auth = useAuth();
  const queryClient = useQueryClient();

  const courseQuery = useQuery({
    queryKey: ["course", courseId],
    queryFn: () => getCourseDetail(auth.getAccessToken, courseId),
  });

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const progressQuery = useQuery({
    queryKey: ["progress", courseId],
    queryFn: () => getCourseProgress(auth.getAccessToken, courseId),
    enabled: enrollmentsQuery.data?.some((enrollment) => enrollment.courseId === courseId) ?? false,
  });

  const enrollMutation = useMutation({
    mutationFn: () => enrollInCourse(auth.getAccessToken, courseId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["enrollments"] }),
        queryClient.invalidateQueries({ queryKey: ["progress", courseId] }),
      ]);
    },
  });

  const myEnrollment = (enrollmentsQuery.data ?? []).find((enrollment) => enrollment.courseId === courseId);
  const orderedLessons = (courseQuery.data?.sections ?? [])
    .flatMap((section) =>
      section.lessons.map((lesson) => ({
        ...lesson,
        sectionTitle: section.title,
        sectionOrder: section.displayOrder ?? 0,
      })),
    )
    .sort((a, b) => a.sectionOrder - b.sectionOrder || (a.displayOrder ?? 0) - (b.displayOrder ?? 0));

  const completedLessonIds = new Set(
    progressQuery.data?.sections.flatMap((section) =>
      section.lessons.filter((lesson) => lesson.completed).map((lesson) => lesson.lessonId),
    ) ?? [],
  );

  const nextLesson = orderedLessons.find((lesson) => !completedLessonIds.has(lesson.id)) ?? orderedLessons[0];

  return (
    <AppShell
      active="courses"
      user={{
        displayName: auth.session?.displayName ?? "EduLife learner",
        email: auth.session?.email ?? "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex items-center gap-3">
          <Link
            to="/courses"
            className="inline-flex items-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-xs font-semibold text-foreground"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            Back to courses
          </Link>
          <div>
            <p className="text-sm font-semibold text-foreground">Course detail</p>
            <p className="text-xs text-muted-foreground">
              Outline, progress, and lesson access for this backend course.
            </p>
          </div>
        </div>
      }
    >
      {courseQuery.isLoading || enrollmentsQuery.isLoading ? (
        <StateCard title="Loading course..." detail="Fetching the course outline and your access state." />
      ) : courseQuery.isError ? (
        <StateCard title="Course unavailable" detail={courseQuery.error.message} />
      ) : (
        <div className="space-y-6">
          <section className="rounded-3xl bg-gradient-to-br from-primary to-primary-glow px-6 py-8 text-primary-foreground shadow-elevated">
            <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
              <div className="max-w-3xl">
                <p className="inline-flex rounded-full border border-white/20 bg-white/10 px-3 py-1 text-[11px] uppercase tracking-[0.16em]">
                  {(courseQuery.data?.level ?? "").replace("_", " ")} · {(courseQuery.data?.languageCode ?? "").toUpperCase()}
                </p>
                <h1 className="mt-4 text-display text-4xl">{courseQuery.data?.title}</h1>
                <p className="mt-3 text-sm leading-relaxed text-primary-foreground/75">
                  {courseQuery.data?.description || courseQuery.data?.shortDescription}
                </p>
              </div>

              {myEnrollment ? (
                <div className="flex flex-col gap-2 sm:flex-row">
                  {nextLesson ? (
                    <Link
                      to="/learn/$courseId/$lessonId"
                      params={{ courseId, lessonId: nextLesson.id }}
                      className="inline-flex items-center gap-2 rounded-full bg-white px-5 py-3 text-sm font-semibold text-foreground shadow-elevated"
                    >
                      <CirclePlay className="h-4 w-4" />
                      {completedLessonIds.size > 0 ? "Continue learning" : "Start course"}
                    </Link>
                  ) : null}
                  <Link
                    to="/courses/$courseId/resources"
                    params={{ courseId }}
                    className="inline-flex items-center gap-2 rounded-full border border-white/30 bg-white/10 px-5 py-3 text-sm font-semibold text-primary-foreground hover:bg-white/20 transition-colors"
                  >
                    <BookOpen className="h-4 w-4" />
                    Resources
                  </Link>
                  {progressQuery.data && progressQuery.data.percentComplete >= 100 ? (
                    <Link
                      to="/courses/$courseId/exam"
                      params={{ courseId }}
                      className="inline-flex items-center gap-2 rounded-full bg-gold px-5 py-3 text-sm font-semibold text-gold-foreground shadow-gold"
                    >
                      <GraduationCap className="h-4 w-4" />
                      Take final exam
                    </Link>
                  ) : null}
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => enrollMutation.mutate()}
                  disabled={enrollMutation.isPending}
                  className="inline-flex items-center gap-2 rounded-full bg-white px-5 py-3 text-sm font-semibold text-foreground shadow-elevated disabled:opacity-60"
                >
                  <BookOpen className="h-4 w-4" />
                  {enrollMutation.isPending ? "Enrolling..." : "Enroll to unlock lessons"}
                </button>
              )}
            </div>
          </section>

          <section className="grid gap-4 md:grid-cols-3">
            <MetricCard
              title="Sections"
              value={String(courseQuery.data?.sections.length ?? 0)}
              icon={<Layers3 className="h-5 w-5 text-primary" />}
            />
            <MetricCard
              title="Lessons"
              value={String(orderedLessons.length)}
              icon={<BookOpen className="h-5 w-5 text-primary" />}
            />
            <MetricCard
              title="Completed"
              value={
                myEnrollment && progressQuery.data
                  ? `${progressQuery.data.completedLessons}/${progressQuery.data.totalLessons}`
                  : "Enroll first"
              }
              icon={<CheckCircle2 className="h-5 w-5 text-teal-600" />}
            />
          </section>

          <section className="space-y-6">
            {(courseQuery.data?.sections ?? []).map((section) => {
              const sectionLessons = section.lessons || [];
              const completedSectionLessons = sectionLessons.filter((lesson) => completedLessonIds.has(lesson.id)).length;
              const totalSectionLessons = sectionLessons.length;
              const sectionPercent = totalSectionLessons > 0 ? (completedSectionLessons / totalSectionLessons) * 100 : 0;

              return (
                <article
                  key={section.id}
                  className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft"
                >
                  {/* Section header info area */}
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-bold uppercase tracking-[0.14em] text-primary">
                          Section {section.displayOrder ?? 0}
                        </span>
                        {myEnrollment && totalSectionLessons > 0 && (
                          <span className="text-[11px] font-semibold text-muted-foreground">
                            {completedSectionLessons} of {totalSectionLessons} completed
                          </span>
                        )}
                      </div>
                      <h2 className="mt-3 text-xl font-bold text-foreground tracking-tight">{section.title}</h2>
                      {section.description && (
                        <p className="mt-2 text-sm leading-relaxed text-muted-foreground max-w-3xl">{section.description}</p>
                      )}
                    </div>

                    {/* Mini progress bar for this specific module/section */}
                    {myEnrollment && totalSectionLessons > 0 && (
                      <div className="w-full shrink-0 sm:w-40 pt-1">
                        <div className="flex justify-between text-[11px] font-bold text-foreground mb-1">
                          <span>Section Progress</span>
                          <span>{Math.round(sectionPercent)}%</span>
                        </div>
                        <div className="h-2 w-full rounded-full bg-border/60 overflow-hidden">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-primary to-primary-glow transition-all duration-500 ease-out"
                            style={{ width: `${sectionPercent}%` }}
                          />
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Lessons list with connector line (learning path timeline) */}
                  <div className="relative mt-6 space-y-4">
                    {/* The vertical connector line connecting the icons of the path */}
                    {totalSectionLessons > 1 && (
                      <div 
                        className="absolute left-[27px] top-[24px] bottom-[24px] w-[2px] bg-border/70 pointer-events-none" 
                        aria-hidden="true"
                      />
                    )}

                    {sectionLessons.map((lesson) => {
                      const completed = completedLessonIds.has(lesson.id);
                      const accessible = Boolean(myEnrollment) || lesson.preview;
                      
                      // Identify type of lesson to render matching icon
                      const isVideo = lesson.lessonType?.toUpperCase() === "VIDEO" || lesson.lessonType?.toUpperCase() === "FILM";
                      const LessonIcon = isVideo ? Video : FileText;

                      return (
                        <div
                          key={lesson.id}
                          className={`relative flex gap-4 rounded-2xl border p-4 transition-all duration-300 ${
                            completed
                              ? "border-teal-500/20 bg-teal-500/[0.02] hover:bg-teal-500/[0.04]"
                              : !accessible
                                ? "border-border/50 bg-background/50 opacity-65 select-none"
                                : "border-border bg-background hover:-translate-y-0.5 hover:shadow-soft hover:border-primary/25 hover:bg-surface-elevated/20"
                          }`}
                        >
                          {/* Left icon cell inside the timeline layout */}
                          <div 
                            className={`relative z-10 flex h-14 w-14 shrink-0 items-center justify-center rounded-xl border transition-colors ${
                              completed
                                ? "bg-teal-50 text-teal-600 border-teal-200"
                                : !accessible
                                  ? "bg-muted text-muted-foreground border-border"
                                  : "bg-primary/5 text-primary border-primary/15"
                            }`}
                          >
                            {!accessible ? (
                              <Lock className="h-5 w-5" />
                            ) : (
                              <LessonIcon className="h-5 w-5" />
                            )}
                          </div>

                          {/* Middle information column */}
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <p className={`font-semibold tracking-tight ${completed ? "text-foreground/90 line-through decoration-teal-600/30" : "text-foreground"}`}>
                                {lesson.title}
                              </p>
                              {lesson.preview && (
                                <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-primary">
                                  Preview
                                </span>
                              )}
                              {completed && (
                                <span className="rounded-full bg-teal-100/70 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-teal-700">
                                  Completed
                                </span>
                              )}
                            </div>
                            <p className="mt-1 text-sm text-muted-foreground leading-relaxed line-clamp-2">
                              {lesson.summary || "Lesson ready to open from the backend content service."}
                            </p>
                            
                            {/* Metadata row with duration & type */}
                            <div className="mt-2.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs font-medium text-muted-foreground">
                              <span className="uppercase tracking-[0.12em] text-[10px] font-bold text-muted-foreground/80">
                                {lesson.lessonType}
                              </span>
                              <span className="inline-flex items-center gap-1">
                                <Clock3 className="h-3.5 w-3.5" />
                                {lesson.estimatedDurationMinutes ?? 0} min
                              </span>
                            </div>
                          </div>

                          {/* Right action control */}
                          <div className="flex shrink-0 items-center self-stretch sm:self-center pl-2">
                            {accessible ? (
                              <Link
                                to="/learn/$courseId/$lessonId"
                                params={{ courseId, lessonId: lesson.id }}
                                className={`inline-flex items-center justify-center gap-1.5 rounded-full px-4 py-2 text-xs font-bold shadow-soft transition-all duration-200 ${
                                  completed
                                    ? "bg-muted text-foreground hover:bg-border"
                                    : "bg-primary text-primary-foreground hover:bg-primary-dark hover:shadow-elevated"
                                }`}
                              >
                                <CirclePlay className="h-4 w-4" />
                                {completed ? "Review" : "Open"}
                              </Link>
                            ) : (
                              <span className="inline-flex items-center gap-1 rounded-full border border-border/80 bg-muted/50 px-3.5 py-2 text-xs font-bold text-muted-foreground">
                                <Lock className="h-3.5 w-3.5" />
                                Locked
                              </span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </article>
              );
            })}
          </section>
        </div>
      )}
    </AppShell>
  );
}

function MetricCard({
  title,
  value,
  icon,
}: {
  title: string;
  value: string;
  icon: ReactNode;
}) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{title}</p>
        {icon}
      </div>
      <p className="mt-3 text-display text-3xl text-foreground">{value}</p>
    </div>
  );
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-10 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
    </div>
  );
}
