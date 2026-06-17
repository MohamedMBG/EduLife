import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ClipboardList, FileText, Layers3, Plus, Trash2, Video, X } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import {
  ApiClientError,
  createCmsLesson,
  createCmsSection,
  deleteCmsLesson,
  deleteCmsSection,
  getCmsExam,
  listCmsCourses,
  listCmsLessons,
  listCmsSections,
} from "../lib/api/client";
import { RequireTeacher, useAuth } from "../lib/auth/auth-context";
import type { CmsLesson, CmsLessonType, CmsSection } from "../lib/api/types";

export const Route = createFileRoute("/teach/$courseId")({
  component: ManageCourseRoute,
  head: () => ({ meta: [{ title: "Manage Course - EduLife" }] }),
});

function ManageCourseRoute() {
  return (
    <RequireTeacher>
      <ManageCoursePage />
    </RequireTeacher>
  );
}

function ManageCoursePage() {
  const auth = useAuth();
  const { courseId } = Route.useParams();
  const [addingSection, setAddingSection] = useState(false);

  // The CMS has no single-course endpoint; the owned-courses list is small and already cached.
  const coursesQuery = useQuery({
    queryKey: ["cms", "courses"],
    queryFn: () => listCmsCourses(auth.getAccessToken),
  });
  const course = coursesQuery.data?.find((candidate) => candidate.id === courseId);

  const sectionsQuery = useQuery({
    queryKey: ["cms", "sections", courseId],
    queryFn: () => listCmsSections(auth.getAccessToken, courseId),
  });
  const sections = sectionsQuery.data ?? [];

  const lessonQueries = useQueries({
    queries: sections.map((section) => ({
      queryKey: ["cms", "lessons", section.id],
      queryFn: () => listCmsLessons(auth.getAccessToken, section.id),
      enabled: sectionsQuery.isSuccess,
    })),
  });

  const examQuery = useQuery({
    queryKey: ["cms", "exam", courseId],
    queryFn: () => getCmsExam(auth.getAccessToken, courseId),
    retry: (failureCount, error) => {
      if (error instanceof ApiClientError && (error.status === 404 || error.status === 403)) {
        return false;
      }
      return failureCount < 2;
    },
  });
  const hasExam = !!examQuery.data;

  return (
    <AppLayout>
      <div className="mx-auto max-w-4xl space-y-8">
        <Link
          to="/teach"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to Teaching Studio
        </Link>

        {coursesQuery.isLoading || sectionsQuery.isLoading ? (
          <StateCard title="Loading course content..." detail="Fetching sections and lessons." />
        ) : coursesQuery.isError ? (
          <StateCard title="Course unavailable" detail={coursesQuery.error.message} />
        ) : !course ? (
          <StateCard
            title="Course not found"
            detail="This course does not exist or belongs to another teacher."
          />
        ) : sectionsQuery.isError ? (
          <StateCard title="Sections unavailable" detail={sectionsQuery.error.message} />
        ) : (
          <>
            <section className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h1 className="text-display text-3xl text-foreground">{course.title}</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                  {course.status === "DRAFT"
                    ? "Draft — an admin publishes it once the content is ready."
                    : `Status: ${course.status}`}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setAddingSection((open) => !open)}
                className="inline-flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-elevated transition-all hover:opacity-90 active:scale-[0.98]"
              >
                {addingSection ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
                {addingSection ? "Close" : "Add section"}
              </button>
            </section>

            <Link
              to="/teach/$courseId/exam"
              params={{ courseId }}
              className="inline-flex h-11 items-center gap-2 rounded-full border border-border px-5 text-sm font-medium text-foreground transition-colors hover:bg-accent hover:border-primary/40"
            >
              <ClipboardList className="h-4 w-4 text-primary" />
              {hasExam ? "Manage final exam" : "Create final exam"}
            </Link>

            {addingSection && (
              <CreateSectionCard
                courseId={courseId}
                nextOrder={sections.length + 1}
                onDone={() => setAddingSection(false)}
              />
            )}

            {sections.length === 0 && !addingSection ? (
              <StateCard
                title="No sections yet"
                detail="Add the first section to start structuring this course."
              />
            ) : (
              <div className="space-y-5">
                {sections.map((section, index) => (
                  <SectionCard
                    key={section.id}
                    courseId={courseId}
                    section={section}
                    lessons={lessonQueries[index]?.data ?? []}
                    lessonsLoading={lessonQueries[index]?.isLoading ?? false}
                  />
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </AppLayout>
  );
}

function SectionCard({
  courseId,
  section,
  lessons,
  lessonsLoading,
}: {
  courseId: string;
  section: CmsSection;
  lessons: CmsLesson[];
  lessonsLoading: boolean;
}) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [addingLesson, setAddingLesson] = useState(false);

  const deleteSectionMutation = useMutation({
    mutationFn: () => deleteCmsSection(auth.getAccessToken, courseId, section.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cms", "sections", courseId] }),
  });

  const deleteLessonMutation = useMutation({
    mutationFn: (lessonId: string) => deleteCmsLesson(auth.getAccessToken, section.id, lessonId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cms", "lessons", section.id] }),
  });

  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-soft">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/10 text-primary">
            <Layers3 className="h-4 w-4" />
          </span>
          <div>
            <h2 className="text-display text-lg text-foreground">
              {section.displayOrder}. {section.title}
            </h2>
            {section.description && (
              <p className="text-xs text-muted-foreground">{section.description}</p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setAddingLesson((open) => !open)}
            className="inline-flex h-9 items-center gap-1.5 rounded-full border border-border px-3 text-xs font-medium text-foreground transition-colors hover:bg-accent"
          >
            {addingLesson ? <X className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
            {addingLesson ? "Close" : "Lesson"}
          </button>
          <button
            type="button"
            onClick={() => {
              if (window.confirm(`Delete section "${section.title}" and all its lessons?`)) {
                deleteSectionMutation.mutate();
              }
            }}
            disabled={deleteSectionMutation.isPending}
            className="grid h-9 w-9 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive disabled:opacity-40"
            aria-label="Delete section"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {(deleteSectionMutation.isError || deleteLessonMutation.isError) && (
        <p
          role="alert"
          className="mt-3 rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          {deleteSectionMutation.error?.message || deleteLessonMutation.error?.message}
        </p>
      )}

      {addingLesson && (
        <div className="mt-4">
          <CreateLessonCard
            sectionId={section.id}
            nextOrder={lessons.length + 1}
            onDone={() => setAddingLesson(false)}
          />
        </div>
      )}

      <div className="mt-4 space-y-2">
        {lessonsLoading ? (
          <p className="text-xs text-muted-foreground">Loading lessons...</p>
        ) : lessons.length === 0 ? (
          <p className="text-xs text-muted-foreground">No lessons in this section yet.</p>
        ) : (
          lessons.map((lesson) => (
            <div
              key={lesson.id}
              className="flex items-center gap-3 rounded-xl border border-border/60 bg-surface px-4 py-2.5"
            >
              <span className="text-muted-foreground">
                {lesson.lessonType === "VIDEO" ? (
                  <Video className="h-4 w-4" />
                ) : (
                  <FileText className="h-4 w-4" />
                )}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-foreground">
                  {lesson.displayOrder}. {lesson.title}
                </p>
                <p className="text-[11px] text-muted-foreground">
                  {lesson.lessonType}
                  {lesson.estimatedDurationMinutes
                    ? ` · ${lesson.estimatedDurationMinutes} min`
                    : ""}
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  if (window.confirm(`Delete lesson "${lesson.title}"?`)) {
                    deleteLessonMutation.mutate(lesson.id);
                  }
                }}
                disabled={deleteLessonMutation.isPending}
                className="grid h-8 w-8 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive disabled:opacity-40"
                aria-label={`Delete lesson ${lesson.title}`}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </div>
          ))
        )}
      </div>
    </section>
  );
}

function CreateSectionCard({
  courseId,
  nextOrder,
  onDone,
}: {
  courseId: string;
  nextOrder: number;
  onDone: () => void;
}) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  const createMutation = useMutation({
    mutationFn: () =>
      createCmsSection(auth.getAccessToken, courseId, {
        title: title.trim(),
        description: description.trim() || undefined,
        displayOrder: nextOrder,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["cms", "sections", courseId] });
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
      <h2 className="text-display text-lg text-foreground">New section</h2>
      <input
        required
        maxLength={255}
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        placeholder="Section title"
        className="w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
      />
      <input
        value={description}
        onChange={(event) => setDescription(event.target.value)}
        placeholder="Optional description"
        className="w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
      />
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
          className="inline-flex h-10 items-center rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90 disabled:opacity-40"
        >
          {createMutation.isPending ? "Adding..." : `Add as section ${nextOrder}`}
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

function CreateLessonCard({
  sectionId,
  nextOrder,
  onDone,
}: {
  sectionId: string;
  nextOrder: number;
  onDone: () => void;
}) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [lessonType, setLessonType] = useState<CmsLessonType>("ARTICLE");
  const [duration, setDuration] = useState("");
  const [contentUrl, setContentUrl] = useState("");
  const [contentBody, setContentBody] = useState("");

  const createMutation = useMutation({
    mutationFn: () =>
      createCmsLesson(auth.getAccessToken, sectionId, {
        title: title.trim(),
        lessonType,
        estimatedDurationMinutes: duration ? Number(duration) : undefined,
        displayOrder: nextOrder,
        preview: false,
        contentUrl: lessonType === "ARTICLE" ? undefined : contentUrl.trim() || undefined,
        contentBody: lessonType === "ARTICLE" ? contentBody.trim() || undefined : undefined,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["cms", "lessons", sectionId] });
      onDone();
    },
  });

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        createMutation.mutate();
      }}
      className="space-y-3 rounded-2xl border border-border bg-surface p-4"
    >
      <input
        required
        maxLength={255}
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        placeholder="Lesson title"
        className="w-full h-10 rounded-xl border border-input bg-surface-elevated px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
      />
      <div className="grid gap-3 sm:grid-cols-2">
        <select
          value={lessonType}
          onChange={(event) => setLessonType(event.target.value as CmsLessonType)}
          className="w-full h-10 rounded-xl border border-input bg-surface-elevated px-3 text-sm text-foreground outline-none focus:border-primary transition-all"
        >
          <option value="ARTICLE">Article</option>
          <option value="VIDEO">Video</option>
          <option value="RESOURCE">Resource</option>
        </select>
        <input
          type="number"
          min={1}
          value={duration}
          onChange={(event) => setDuration(event.target.value)}
          placeholder="Duration (minutes)"
          className="w-full h-10 rounded-xl border border-input bg-surface-elevated px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
        />
      </div>
      {lessonType === "ARTICLE" ? (
        <textarea
          rows={4}
          value={contentBody}
          onChange={(event) => setContentBody(event.target.value)}
          placeholder="Article content (markdown supported)"
          className="w-full rounded-xl border border-input bg-surface-elevated px-4 py-3 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
        />
      ) : (
        <input
          type="url"
          value={contentUrl}
          onChange={(event) => setContentUrl(event.target.value)}
          placeholder={lessonType === "VIDEO" ? "Video URL" : "Resource URL"}
          className="w-full h-10 rounded-xl border border-input bg-surface-elevated px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
        />
      )}
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
          className="inline-flex h-9 items-center rounded-full bg-primary px-4 text-xs font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90 disabled:opacity-40"
        >
          {createMutation.isPending ? "Adding..." : `Add as lesson ${nextOrder}`}
        </button>
        <button
          type="button"
          onClick={onDone}
          className="text-xs text-muted-foreground transition-colors hover:text-foreground"
        >
          Cancel
        </button>
      </div>
    </form>
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
