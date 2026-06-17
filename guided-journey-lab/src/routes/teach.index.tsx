import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookOpen, CheckCircle2, FileEdit, Plus, X } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { createCmsCourse, listCmsCourses } from "../lib/api/client";
import { RequireTeacher, useAuth } from "../lib/auth/auth-context";
import type { CmsCourse, CourseStatus } from "../lib/api/types";

export const Route = createFileRoute("/teach/")({
  component: TeachRoute,
  head: () => ({ meta: [{ title: "Teaching Studio - EduLife" }] }),
});

function TeachRoute() {
  return (
    <RequireTeacher>
      <TeachPage />
    </RequireTeacher>
  );
}

const STATUS_STYLES: Record<CourseStatus, string> = {
  DRAFT: "bg-secondary text-secondary-foreground",
  PUBLISHED: "bg-primary/10 text-primary",
  ARCHIVED: "bg-muted text-muted-foreground",
};

function TeachPage() {
  const auth = useAuth();
  const [creating, setCreating] = useState(false);

  const coursesQuery = useQuery({
    queryKey: ["cms", "courses"],
    queryFn: () => listCmsCourses(auth.getAccessToken),
  });

  const courses = coursesQuery.data ?? [];
  const drafts = courses.filter((course) => course.status === "DRAFT").length;
  const published = courses.filter((course) => course.status === "PUBLISHED").length;

  return (
    <AppLayout>
      <div className="mx-auto max-w-5xl space-y-8">
        <section className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-display text-3xl text-foreground">Your courses</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {published} published · {drafts} draft{drafts === 1 ? "" : "s"} — publishing requires
              an admin review.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setCreating((open) => !open)}
            className="inline-flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-elevated transition-all hover:opacity-90 active:scale-[0.98]"
          >
            {creating ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
            {creating ? "Close" : "New course"}
          </button>
        </section>

        {creating && (
          <CreateCourseCard
            onDone={() => {
              setCreating(false);
            }}
          />
        )}

        {coursesQuery.isLoading ? (
          <StateCard title="Loading your courses..." detail="Fetching the CMS course list." />
        ) : coursesQuery.isError ? (
          <StateCard title="Courses unavailable" detail={coursesQuery.error.message} />
        ) : courses.length === 0 ? (
          <StateCard
            title="No courses yet"
            detail="Create your first course, add sections and lessons, then ask an admin to publish it."
          />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {courses.map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  );
}

function CourseCard({ course }: { course: CmsCourse }) {
  return (
    <Link
      to="/teach/$courseId"
      params={{ courseId: course.id }}
      className="group rounded-3xl border border-border bg-card p-6 shadow-soft transition-all hover:border-primary/40 hover:shadow-elevated"
    >
      <div className="flex items-start justify-between gap-3">
        <span className="grid h-10 w-10 place-items-center rounded-xl bg-primary/10 text-primary">
          <BookOpen className="h-5 w-5" />
        </span>
        <span
          className={`rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide ${STATUS_STYLES[course.status]}`}
        >
          {course.status}
        </span>
      </div>
      <h2 className="mt-4 text-display text-xl text-foreground group-hover:text-primary transition-colors">
        {course.title}
      </h2>
      <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">
        {course.shortDescription || course.description}
      </p>
      <p className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-primary">
        <FileEdit className="h-3.5 w-3.5" />
        Manage content
      </p>
    </Link>
  );
}

function CreateCourseCard({ onDone }: { onDone: () => void }) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [shortDescription, setShortDescription] = useState("");
  const [description, setDescription] = useState("");
  const [languageCode, setLanguageCode] = useState("en");
  const [level, setLevel] = useState("");

  const createMutation = useMutation({
    mutationFn: () =>
      createCmsCourse(auth.getAccessToken, {
        title: title.trim(),
        shortDescription: shortDescription.trim() || undefined,
        description: description.trim(),
        languageCode: languageCode.trim(),
        level: level.trim() || undefined,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["cms", "courses"] });
      onDone();
    },
  });

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        createMutation.mutate();
      }}
      className="space-y-4 rounded-3xl border border-border bg-surface-elevated p-6 shadow-elevated"
    >
      <h2 className="text-display text-xl text-foreground">New course</h2>

      <Field label="Title" required>
        <input
          required
          maxLength={255}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="e.g. Practical Spring Boot"
          className="w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
        />
      </Field>

      <Field label="Short description">
        <input
          maxLength={500}
          value={shortDescription}
          onChange={(event) => setShortDescription(event.target.value)}
          placeholder="One-line summary shown in the catalog"
          className="w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
        />
      </Field>

      <Field label="Description" required>
        <textarea
          required
          rows={4}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="What will learners be able to do after this course?"
          className="w-full rounded-xl border border-input bg-surface px-4 py-3 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
        />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Language code" required>
          <input
            required
            maxLength={10}
            value={languageCode}
            onChange={(event) => setLanguageCode(event.target.value)}
            placeholder="en"
            className="w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
          />
        </Field>
        <Field label="Level / category">
          <input
            value={level}
            onChange={(event) => setLevel(event.target.value)}
            placeholder="e.g. Beginner"
            className="w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
          />
        </Field>
      </div>

      {createMutation.isError && (
        <p
          role="alert"
          className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          {createMutation.error.message}
        </p>
      )}

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={createMutation.isPending}
          className="inline-flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90 disabled:opacity-40"
        >
          <CheckCircle2 className="h-4 w-4" />
          {createMutation.isPending ? "Creating..." : "Create draft"}
        </button>
        <button
          type="button"
          onClick={onDone}
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}

function Field({
  label,
  required,
  children,
}: {
  label: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <label className="block space-y-1.5">
      <span className="block text-xs uppercase tracking-[0.16em] text-muted-foreground">
        {label}
        {required ? " *" : ""}
      </span>
      {children}
    </label>
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
