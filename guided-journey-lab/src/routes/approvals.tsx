import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookOpen, CheckCircle2, Clock } from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import { listCmsCourses, publishCmsCourse } from "../lib/api/client";
import { RequireCourseApprover, useAuth } from "../lib/auth/auth-context";
import type { CmsCourse } from "../lib/api/types";

export const Route = createFileRoute("/approvals")({
  component: ApprovalsRoute,
  head: () => ({ meta: [{ title: "Course Approvals - EduLife" }] }),
});

function ApprovalsRoute() {
  return (
    <RequireCourseApprover>
      <ApprovalsPage />
    </RequireCourseApprover>
  );
}

function ApprovalsPage() {
  const auth = useAuth();

  // For GROUP_ADMIN the backend scopes this list to courses authored by teachers
  // inside their groups — exactly the review queue this page needs.
  const coursesQuery = useQuery({
    queryKey: ["cms", "courses"],
    queryFn: () => listCmsCourses(auth.getAccessToken),
  });

  const courses = coursesQuery.data ?? [];
  const pending = courses.filter((course) => course.status === "DRAFT");
  const published = courses.filter((course) => course.status === "PUBLISHED");

  return (
    <AppShell
      active="approvals"
      user={{
        displayName: auth.session?.displayName || "EduLife group admin",
        email: auth.session?.email || "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">Course Approvals</p>
          <p className="text-xs text-muted-foreground">
            Review and publish courses submitted by the teachers in your groups.
          </p>
        </div>
      }
    >
      <div className="mx-auto max-w-4xl space-y-8">
        {coursesQuery.isLoading ? (
          <StateCard title="Loading review queue..." detail="Fetching courses from your teachers." />
        ) : coursesQuery.isError ? (
          <StateCard title="Approvals unavailable" detail={coursesQuery.error.message} />
        ) : (
          <>
            <section className="space-y-4">
              <div className="flex items-center gap-3">
                <span className="grid h-9 w-9 place-items-center rounded-xl bg-gold/15 text-gold">
                  <Clock className="h-4 w-4" />
                </span>
                <h1 className="text-display text-2xl text-foreground">
                  Pending review ({pending.length})
                </h1>
              </div>
              {pending.length === 0 ? (
                <StateCard
                  title="Nothing waiting for review"
                  detail="When a teacher in one of your groups creates a course, it shows up here as a draft."
                />
              ) : (
                pending.map((course) => <ApprovalCard key={course.id} course={course} />)
              )}
            </section>

            <section className="space-y-4">
              <div className="flex items-center gap-3">
                <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/10 text-primary">
                  <CheckCircle2 className="h-4 w-4" />
                </span>
                <h2 className="text-display text-2xl text-foreground">
                  Published ({published.length})
                </h2>
              </div>
              {published.length === 0 ? (
                <p className="text-sm text-muted-foreground">No published courses from your teachers yet.</p>
              ) : (
                <div className="space-y-2">
                  {published.map((course) => (
                    <div
                      key={course.id}
                      className="flex items-center gap-3 rounded-xl border border-border/60 bg-surface px-4 py-2.5"
                    >
                      <BookOpen className="h-4 w-4 text-muted-foreground" />
                      <p className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">
                        {course.title}
                      </p>
                      <span className="truncate text-xs text-muted-foreground">
                        {course.createdByEmail ?? "unknown author"}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </AppShell>
  );
}

function ApprovalCard({ course }: { course: CmsCourse }) {
  const auth = useAuth();
  const queryClient = useQueryClient();

  const publishMutation = useMutation({
    mutationFn: () => publishCmsCourse(auth.getAccessToken, course.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cms", "courses"] }),
  });

  return (
    <article className="rounded-3xl border border-border bg-card p-6 shadow-soft">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h3 className="text-display text-xl text-foreground">{course.title}</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            By {course.createdByEmail ?? "unknown author"}
            {course.level ? ` · ${course.level}` : ""} · {course.languageCode.toUpperCase()}
          </p>
          <p className="mt-2 line-clamp-3 text-sm text-muted-foreground">
            {course.shortDescription || course.description}
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            if (window.confirm(`Publish "${course.title}"? Learners will see it in the catalog.`)) {
              publishMutation.mutate();
            }
          }}
          disabled={publishMutation.isPending}
          className="inline-flex h-10 items-center gap-2 rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90 disabled:opacity-40"
        >
          <CheckCircle2 className="h-4 w-4" />
          {publishMutation.isPending ? "Publishing..." : "Approve & publish"}
        </button>
      </div>
      {publishMutation.isError && (
        <p role="alert" className="mt-3 rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {publishMutation.error.message}
        </p>
      )}
    </article>
  );
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-10 text-center shadow-soft">
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="mt-2 text-xs text-muted-foreground">{detail}</p>
    </div>
  );
}
