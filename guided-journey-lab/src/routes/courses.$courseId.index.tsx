import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, BookOpen, CheckCircle2, CirclePlay, Clock3, GraduationCap, Layers3 } from "lucide-react";
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

          <section className="space-y-4">
            {(courseQuery.data?.sections ?? []).map((section) => (
              <article
                key={section.id}
                className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                      Section {section.displayOrder ?? 0}
                    </p>
                    <h2 className="mt-2 text-lg font-semibold text-foreground">{section.title}</h2>
                    {section.description && (
                      <p className="mt-2 text-sm text-muted-foreground">{section.description}</p>
                    )}
                  </div>
                </div>

                <div className="mt-5 space-y-3">
                  {section.lessons.map((lesson) => {
                    const completed = completedLessonIds.has(lesson.id);
                    const accessible = Boolean(myEnrollment) || lesson.preview;

                    return (
                      <div
                        key={lesson.id}
                        className="flex flex-col gap-3 rounded-2xl border border-border bg-background p-4 sm:flex-row sm:items-center sm:justify-between"
                      >
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="font-medium text-foreground">{lesson.title}</p>
                            {lesson.preview && (
                              <span className="rounded-full bg-primary/8 px-2.5 py-1 text-[11px] font-semibold text-primary">
                                Preview
                              </span>
                            )}
                            {completed && (
                              <span className="rounded-full bg-teal-50 px-2.5 py-1 text-[11px] font-semibold text-teal-700">
                                Completed
                              </span>
                            )}
                          </div>
                          <p className="mt-1 text-sm text-muted-foreground">
                            {lesson.summary || "Lesson ready to open from the backend content service."}
                          </p>
                          <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                            <span className="uppercase tracking-[0.14em]">{lesson.lessonType}</span>
                            <span className="inline-flex items-center gap-1">
                              <Clock3 className="h-3.5 w-3.5" />
                              {lesson.estimatedDurationMinutes ?? 0} min
                            </span>
                          </div>
                        </div>

                        {accessible ? (
                          <Link
                            to="/learn/$courseId/$lessonId"
                            params={{ courseId, lessonId: lesson.id }}
                            className="inline-flex items-center justify-center gap-2 rounded-full bg-foreground px-4 py-2 text-xs font-semibold text-background"
                          >
                            <CirclePlay className="h-3.5 w-3.5" />
                            {completed ? "Review lesson" : "Open lesson"}
                          </Link>
                        ) : (
                          <span className="rounded-full border border-border bg-muted px-4 py-2 text-xs font-semibold text-muted-foreground">
                            Enroll to unlock
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>
              </article>
            ))}
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
