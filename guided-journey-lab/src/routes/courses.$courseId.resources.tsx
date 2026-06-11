import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, BookOpen, Download, ExternalLink, FileText, Film, Video } from "lucide-react";
import type { ReactNode } from "react";
import { AppShell } from "../components/app/AppShell";
import { getCourseDetail } from "../lib/api/client";
import type { LessonSummary } from "../lib/api/types";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/courses/$courseId/resources")({
  component: ResourcesRoute,
  head: () => ({ meta: [{ title: "Course Resources - EduLife" }] }),
});

function ResourcesRoute() {
  return (
    <RequireAuth>
      <ResourcesPage />
    </RequireAuth>
  );
}

interface FlatLesson extends LessonSummary {
  sectionTitle: string;
  sectionOrder: number;
}

function ResourcesPage() {
  const { courseId } = Route.useParams();
  const auth = useAuth();

  const courseQuery = useQuery({
    queryKey: ["course", courseId],
    queryFn: () => getCourseDetail(auth.getAccessToken, courseId),
  });

  const flatLessons: FlatLesson[] = (courseQuery.data?.sections ?? [])
    .flatMap((section) =>
      section.lessons.map((lesson) => ({
        ...lesson,
        sectionTitle: section.title,
        sectionOrder: section.displayOrder ?? 0,
      })),
    )
    .sort((a, b) => a.sectionOrder - b.sectionOrder || (a.displayOrder ?? 0) - (b.displayOrder ?? 0));

  // Course summary endpoint does not ship contentUrl per lesson; grouping by lessonType and
  // linking to the lesson player is the closest fit until a dedicated resources endpoint exists.
  const grouped = groupByKind(flatLessons);

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
            <p className="text-sm font-semibold text-foreground">Study resources</p>
            <p className="text-xs text-muted-foreground">
              Videos, PDFs, and reference materials for this course.
            </p>
          </div>
        </div>
      }
    >
      {courseQuery.isLoading ? (
        <StateCard title="Loading resources..." detail="Fetching course outline from the backend." />
      ) : courseQuery.isError ? (
        <StateCard title="Resources unavailable" detail={courseQuery.error.message} />
      ) : flatLessons.length === 0 ? (
        <StateCard
          title="No lessons yet"
          detail="This course does not have any lessons configured. Check back soon."
        />
      ) : (
        <div className="space-y-8">
          <ResourceSection
            title="Videos"
            icon={<Video className="h-4 w-4 text-primary" />}
            empty="No video lessons in this course yet."
            lessons={grouped.videos}
            courseId={courseId}
          />
          <ResourceSection
            title="PDFs and downloads"
            icon={<FileText className="h-4 w-4 text-primary" />}
            empty="No downloadable documents linked to this course yet."
            lessons={grouped.documents}
            courseId={courseId}
          />
          <ResourceSection
            title="Articles and reading"
            icon={<BookOpen className="h-4 w-4 text-primary" />}
            empty="No reading materials in this course yet."
            lessons={grouped.articles}
            courseId={courseId}
          />
        </div>
      )}
    </AppShell>
  );
}

function ResourceSection({
  title,
  icon,
  empty,
  lessons,
  courseId,
}: {
  title: string;
  icon: ReactNode;
  empty: string;
  lessons: FlatLesson[];
  courseId: string;
}) {
  return (
    <section className="space-y-4">
      <div className="flex items-center gap-2">
        {icon}
        <h2 className="text-display text-xl text-foreground">{title}</h2>
        <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] font-semibold text-muted-foreground">
          {lessons.length}
        </span>
      </div>

      {lessons.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-surface px-5 py-6 text-sm text-muted-foreground">
          {empty}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {lessons.map((lesson) => (
            <ResourceCard key={lesson.id} courseId={courseId} lesson={lesson} />
          ))}
        </div>
      )}
    </section>
  );
}

function ResourceCard({ courseId, lesson }: { courseId: string; lesson: FlatLesson }) {
  return (
    <article className="flex h-full flex-col rounded-2xl border border-border bg-surface-elevated p-5 shadow-soft transition-all hover:shadow-elevated">
      <div className="flex items-center gap-2">
        <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-primary">
          {lesson.lessonType}
        </span>
        {lesson.preview ? (
          <span className="rounded-full bg-gold/15 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-gold-foreground">
            Preview
          </span>
        ) : null}
      </div>

      <p className="mt-3 text-xs uppercase tracking-[0.14em] text-muted-foreground">
        {lesson.sectionTitle}
      </p>
      <h3 className="mt-1 text-base font-semibold text-foreground">{lesson.title}</h3>
      {lesson.summary ? (
        <p className="mt-2 line-clamp-3 text-sm text-muted-foreground">{lesson.summary}</p>
      ) : null}

      <div className="mt-auto pt-5">
        <Link
          to="/learn/$courseId/$lessonId"
          params={{ courseId, lessonId: lesson.id }}
          className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-foreground px-4 py-2 text-xs font-semibold text-background"
        >
          {iconForKind(lesson.lessonType)}
          Open lesson
        </Link>
      </div>
    </article>
  );
}

function iconForKind(lessonType: string): ReactNode {
  const upper = lessonType?.toUpperCase() ?? "";
  if (upper === "VIDEO") return <Film className="h-3.5 w-3.5" />;
  if (upper === "ARTICLE") return <BookOpen className="h-3.5 w-3.5" />;
  if (upper === "RESOURCE") return <Download className="h-3.5 w-3.5" />;
  return <ExternalLink className="h-3.5 w-3.5" />;
}

function groupByKind(lessons: FlatLesson[]) {
  const videos: FlatLesson[] = [];
  const documents: FlatLesson[] = [];
  const articles: FlatLesson[] = [];

  for (const lesson of lessons) {
    const upper = (lesson.lessonType || "").toUpperCase();
    if (upper === "VIDEO") {
      videos.push(lesson);
    } else if (upper === "RESOURCE") {
      documents.push(lesson);
    } else if (upper === "ARTICLE") {
      articles.push(lesson);
    } else {
      articles.push(lesson);
    }
  }

  return { videos, documents, articles };
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-10 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
    </div>
  );
}
