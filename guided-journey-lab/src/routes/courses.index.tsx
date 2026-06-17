import { useState, type ReactNode } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookOpen, CheckCircle2, Clock3, PlayCircle, Trash2 } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { getCourseProgress, listMyEnrollments, unenrollFromCourse } from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/courses/")({
  component: CoursesRoute,
  head: () => ({ meta: [{ title: "My Courses - EduLife" }] }),
});

type CourseTab = "all" | "in-progress" | "completed" | "not-started";

function CoursesRoute() {
  return (
    <RequireAuth>
      <CoursesPage />
    </RequireAuth>
  );
}

function CoursesPage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<CourseTab>("all");
  const [query, setQuery] = useState("");

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const progressQueries = useQueries({
    queries: (enrollmentsQuery.data ?? []).map((enrollment) => ({
      queryKey: ["progress", enrollment.courseId],
      queryFn: () => getCourseProgress(auth.getAccessToken, enrollment.courseId),
      enabled: enrollmentsQuery.isSuccess,
    })),
  });

  const unenrollMutation = useMutation({
    mutationFn: (enrollmentId: string) => unenrollFromCourse(auth.getAccessToken, enrollmentId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["enrollments"] });
    },
  });

  const progressByCourseId = new Map(
    (enrollmentsQuery.data ?? []).map((enrollment, index) => [
      enrollment.courseId,
      progressQueries[index]?.data,
    ]),
  );

  const courses = (enrollmentsQuery.data ?? []).map((enrollment) => {
    const progress = progressByCourseId.get(enrollment.courseId);
    const percent = progress ? Math.round(progress.percentComplete) : 0;
    const nextLesson = progress?.sections
      .flatMap((section) => section.lessons)
      .find((lesson) => !lesson.completed);

    let status: CourseTab = "not-started";

    if (percent >= 100) {
      status = "completed";
    } else if (percent > 0) {
      status = "in-progress";
    }

    return {
      ...enrollment,
      progress,
      percent,
      nextLesson: nextLesson?.title || "No lesson started yet",
      status,
    };
  });

  const filteredCourses = courses.filter((course) => {
    const tabMatches = activeTab === "all" || course.status === activeTab;
    const queryMatches =
      query.trim().length === 0 ||
      course.title.toLowerCase().includes(query.toLowerCase()) ||
      course.shortDescription.toLowerCase().includes(query.toLowerCase());

    return tabMatches && queryMatches;
  });

  const totals = {
    enrolled: courses.length,
    completed: courses.filter((course) => course.status === "completed").length,
    inProgress: courses.filter((course) => course.status === "in-progress").length,
    completedLessons: courses.reduce(
      (sum, course) => sum + (course.progress?.completedLessons ?? 0),
      0,
    ),
    totalLessons: courses.reduce((sum, course) => sum + (course.progress?.totalLessons ?? 0), 0),
  };

  const progressPending = progressQueries.some((queryItem) => queryItem.isLoading);
  const progressError = progressQueries.find((queryItem) => queryItem.isError)?.error;

  return (
    <AppLayout>
      <section className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <MetricCard
            title="Enrolled"
            value={String(totals.enrolled)}
            icon={<BookOpen className="h-5 w-5 text-primary" />}
          />
          <MetricCard
            title="In progress"
            value={String(totals.inProgress)}
            icon={<PlayCircle className="h-5 w-5 text-amber-500" />}
          />
          <MetricCard
            title="Completed"
            value={String(totals.completed)}
            icon={<CheckCircle2 className="h-5 w-5 text-teal-600" />}
          />
          <MetricCard
            title="Lessons done"
            value={`${totals.completedLessons}/${totals.totalLessons || 0}`}
            icon={<Clock3 className="h-5 w-5 text-muted-foreground" />}
          />
        </div>

        <div className="flex flex-wrap gap-2">
          {(["all", "in-progress", "completed", "not-started"] as const).map((tab) => (
            <button
              key={tab}
              type="button"
              onClick={() => setActiveTab(tab)}
              className={`rounded-full border px-4 py-2 text-xs font-medium capitalize transition-colors ${
                activeTab === tab
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-surface-elevated text-muted-foreground hover:text-foreground"
              }`}
            >
              {tab.replace("-", " ")}
            </button>
          ))}
        </div>

        {enrollmentsQuery.isLoading || progressPending ? (
          <StateCard title="Loading your courses..." detail="Fetching enrollments and progress." />
        ) : enrollmentsQuery.isError ? (
          <StateCard title="Courses unavailable" detail={enrollmentsQuery.error.message} />
        ) : progressError ? (
          <StateCard title="Progress unavailable" detail={progressError.message} />
        ) : filteredCourses.length === 0 ? (
          <StateCard
            title="No matching courses"
            detail="Enroll from Explore first, or clear the current search and filters."
          />
        ) : (
          <div className="grid gap-4 xl:grid-cols-2">
            {filteredCourses.map((course) => (
              <article
                key={course.enrollmentId}
                className="group relative overflow-hidden rounded-3xl hairline bg-surface-elevated p-5 shadow-soft transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:shadow-elevated"
              >
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                  <div className="h-28 w-full overflow-hidden rounded-2xl bg-muted sm:w-44">
                    {course.imageUrl ? (
                      <img
                        src={course.imageUrl}
                        alt={course.title}
                        className="h-full w-full object-cover"
                        onError={(e) => {
                          e.currentTarget.style.display = "none";
                          e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
                        }}
                      />
                    ) : null}
                    <div className="grid h-full place-items-center bg-gradient-to-br from-primary/10 to-primary-glow/10 text-primary" data-fallback="" hidden={!!course.imageUrl}>
                      <BookOpen className="h-8 w-8" />
                    </div>
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap gap-2 text-[11px] uppercase tracking-[0.16em] text-muted-foreground">
                      <span className="rounded-full bg-primary/8 px-3 py-1 text-primary">
                        {course.level.replace("_", " ")}
                      </span>
                      <span className="rounded-full bg-muted px-3 py-1">
                        {course.languageCode.toUpperCase()}
                      </span>
                    </div>
                    <h2 className="mt-3 text-lg font-semibold text-foreground">{course.title}</h2>
                    <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                      {course.shortDescription}
                    </p>

                    <div className="mt-4">
                      <div className="mb-2 flex items-center justify-between text-xs text-muted-foreground">
                        <span>
                          {course.progress?.completedLessons ?? 0} /{" "}
                          {course.progress?.totalLessons ?? 0} lessons
                        </span>
                        <span className="font-semibold text-foreground">{course.percent}%</span>
                      </div>
                      <div className="h-2 rounded-full bg-border">
                        <div
                          className="h-full rounded-full bg-gradient-primary"
                          style={{ width: `${course.percent}%` }}
                        />
                      </div>
                    </div>

                    <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <p className="text-xs text-muted-foreground">
                        Next lesson:{" "}
                        <span className="font-medium text-foreground">{course.nextLesson}</span>
                      </p>
                      <div className="flex flex-wrap items-center gap-2">
                        <Link
                          to="/courses/$courseId"
                          params={{ courseId: course.courseId }}
                          className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-4 py-2 text-xs font-semibold text-primary"
                        >
                          <BookOpen className="h-3.5 w-3.5" />
                          Open course
                        </Link>
                        <button
                          type="button"
                          onClick={() => unenrollMutation.mutate(course.enrollmentId)}
                          disabled={unenrollMutation.isPending}
                          className="inline-flex items-center gap-2 rounded-full border border-destructive/20 bg-destructive/5 px-4 py-2 text-xs font-semibold text-destructive disabled:opacity-60"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                          Unenroll
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </AppLayout>
  );
}

function MetricCard({ title, value, icon }: { title: string; value: string; icon: ReactNode }) {
  return (
    <div className="group relative overflow-hidden rounded-2xl hairline bg-surface-elevated p-5 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:shadow-elevated">
      <div className="flex items-center justify-between">
        <p className="text-[10px] uppercase tracking-[0.2em] font-mono text-muted-foreground">
          {title}
        </p>
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/8 group-hover:bg-primary/12 transition-colors duration-500">
          {icon}
        </span>
      </div>
      <p className="mt-5 text-display text-3xl lg:text-4xl text-foreground leading-none">{value}</p>
    </div>
  );
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-3xl hairline bg-surface-elevated px-6 py-12 text-center">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
    </div>
  );
}
