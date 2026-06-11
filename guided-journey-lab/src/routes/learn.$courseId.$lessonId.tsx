import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, BookOpen, CheckCircle2, CirclePlay, Clock3 } from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import { LessonContentRenderer } from "../components/lesson/LessonContentRenderer";
import { LessonNotes } from "../components/lesson/LessonNotes";
import {
  getCourseDetail,
  getLessonDetail,
  getCourseProgress,
  markLessonComplete,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/learn/$courseId/$lessonId")({
  component: LessonRoute,
  head: () => ({ meta: [{ title: "Lesson - EduLife" }] }),
});

function LessonRoute() {
  return (
    <RequireAuth>
      <LessonPage />
    </RequireAuth>
  );
}

function LessonPage() {
  const { courseId, lessonId } = Route.useParams();
  const auth = useAuth();
  const queryClient = useQueryClient();

  const lessonQuery = useQuery({
    queryKey: ["lesson", courseId, lessonId],
    queryFn: () => getLessonDetail(auth.getAccessToken, courseId, lessonId),
  });

  const courseQuery = useQuery({
    queryKey: ["course", courseId],
    queryFn: () => getCourseDetail(auth.getAccessToken, courseId),
  });

  const progressQuery = useQuery({
    queryKey: ["progress", courseId],
    queryFn: () => getCourseProgress(auth.getAccessToken, courseId),
  });

  const markCompleteMutation = useMutation({
    mutationFn: () => markLessonComplete(auth.getAccessToken, courseId, lessonId),
    onSuccess: async () => {
      // Lesson completion changes both lesson-level access state and course aggregates, so
      // every learner screen that reads progress is invalidated together here.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["lesson", courseId, lessonId] }),
        queryClient.invalidateQueries({ queryKey: ["progress", courseId] }),
        queryClient.invalidateQueries({ queryKey: ["courses"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
        queryClient.invalidateQueries({ queryKey: ["profile"] }),
      ]);
    },
  });

  const orderedLessons = (courseQuery.data?.sections ?? [])
    .flatMap((section) =>
      section.lessons.map((lesson) => ({
        id: lesson.id,
        title: lesson.title,
        sectionTitle: section.title,
        displayOrder: lesson.displayOrder ?? 0,
        sectionOrder: section.displayOrder ?? 0,
      })),
    )
    .sort((a, b) => a.sectionOrder - b.sectionOrder || a.displayOrder - b.displayOrder);

  const currentIndex = orderedLessons.findIndex((lesson) => lesson.id === lessonId);
  const previousLesson = currentIndex > 0 ? orderedLessons[currentIndex - 1] : null;
  const nextLesson = currentIndex >= 0 ? orderedLessons[currentIndex + 1] : null;
  const courseTitle = courseQuery.data?.title || "Course";

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
            to="/courses/$courseId"
            params={{ courseId }}
            className="inline-flex items-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-xs font-semibold text-foreground"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            Back to course
          </Link>
          <div>
            <p className="text-sm font-semibold text-foreground">{courseTitle}</p>
            <p className="text-xs text-muted-foreground">
              Lesson content loaded from the backend lesson endpoint.
            </p>
          </div>
        </div>
      }
    >
      {lessonQuery.isLoading || courseQuery.isLoading ? (
        <StateCard title="Loading lesson..." detail="Fetching lesson content and navigation state." />
      ) : lessonQuery.isError ? (
        <StateCard title="Lesson unavailable" detail={lessonQuery.error.message} />
      ) : !lessonQuery.data ? (
        <StateCard title="Lesson unavailable" detail="Lesson content could not be loaded." />
      ) : (
        <div className="space-y-6">
          <section className="rounded-3xl bg-gradient-to-br from-primary to-primary-glow px-6 py-8 text-primary-foreground shadow-elevated">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div className="max-w-3xl">
                <p className="text-xs uppercase tracking-[0.16em] text-primary-foreground/70">
                  {lessonQuery.data.sectionTitle}
                </p>
                <h1 className="mt-3 text-display text-4xl">{lessonQuery.data.title}</h1>
                <p className="mt-3 text-sm leading-relaxed text-primary-foreground/75">
                  {lessonQuery.data.summary || "Lesson content is ready below."}
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2 text-xs font-semibold">
                <span className="rounded-full bg-white/10 px-3 py-1 uppercase tracking-[0.14em]">
                  {lessonQuery.data.lessonType}
                </span>
                <span className="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-1">
                  <Clock3 className="h-3.5 w-3.5" />
                  {lessonQuery.data.durationMinutes ?? 0} min
                </span>
                {lessonQuery.data.completed && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-teal-600/20 px-3 py-1 text-teal-100">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    Completed
                  </span>
                )}
              </div>
            </div>
          </section>

          <section className="grid gap-6 xl:grid-cols-[1.35fr_0.85fr]">
            <div className="space-y-6">
              <LessonContentRenderer lesson={lessonQuery.data} />
            </div>

            <aside className="space-y-4">
              <div className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
                <p className="text-sm font-semibold text-foreground">Course progress</p>
                {progressQuery.data ? (
                  <>
                    <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
                      <span>
                        {progressQuery.data.completedLessons} / {progressQuery.data.totalLessons} lessons
                      </span>
                      <span className="font-semibold text-foreground">
                        {Math.round(progressQuery.data.percentComplete)}%
                      </span>
                    </div>
                    <div className="mt-2 h-2 rounded-full bg-border">
                      <div
                        className="h-full rounded-full bg-gradient-primary"
                        style={{ width: `${Math.round(progressQuery.data.percentComplete)}%` }}
                      />
                    </div>
                  </>
                ) : (
                  <p className="mt-3 text-sm text-muted-foreground">
                    Progress is not available yet for this course.
                  </p>
                )}
              </div>

              <div className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
                <p className="text-sm font-semibold text-foreground">Lesson actions</p>
                <div className="mt-4 space-y-3">
                  <button
                    type="button"
                    onClick={() => markCompleteMutation.mutate()}
                    disabled={lessonQuery.data.completed || markCompleteMutation.isPending}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-foreground px-4 py-2 text-xs font-semibold text-background disabled:opacity-60 disabled:pointer-events-none"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    {lessonQuery.data.completed
                      ? "Already completed"
                      : markCompleteMutation.isPending
                        ? "Saving..."
                        : "Mark complete"}
                  </button>

                  {previousLesson ? (
                    <Link
                      to="/learn/$courseId/$lessonId"
                      params={{ courseId, lessonId: previousLesson.id }}
                      className="inline-flex w-full items-center justify-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-xs font-semibold text-foreground"
                    >
                      <ArrowLeft className="h-3.5 w-3.5" />
                      Previous lesson
                    </Link>
                  ) : null}

                  {nextLesson ? (
                    <Link
                      to="/learn/$courseId/$lessonId"
                      params={{ courseId, lessonId: nextLesson.id }}
                      className="inline-flex w-full items-center justify-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-4 py-2 text-xs font-semibold text-primary"
                    >
                      <CirclePlay className="h-3.5 w-3.5" />
                      Next lesson
                    </Link>
                  ) : (
                    <Link
                      to="/certificates"
                      className="inline-flex w-full items-center justify-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-4 py-2 text-xs font-semibold text-primary"
                    >
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      View certificates
                    </Link>
                  )}

                  <Link
                    to="/courses/$courseId/resources"
                    params={{ courseId }}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-xs font-semibold text-foreground"
                  >
                    <BookOpen className="h-3.5 w-3.5" />
                    Course resources
                  </Link>
                </div>
              </div>

              <LessonNotes lessonId={lessonId} />
            </aside>
          </section>
        </div>
      )}
    </AppShell>
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
