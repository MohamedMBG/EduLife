import { useDeferredValue, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Compass, Search, Sparkles, Users } from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import { enrollInCourse, listCourses, listMyEnrollments } from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/explore")({
  component: ExploreRoute,
  head: () => ({ meta: [{ title: "Explore Courses - EduLife" }] }),
});

const LEVEL_FILTERS = ["ALL", "BEGINNER", "INTERMEDIATE", "ADVANCED"] as const;
const LANGUAGE_FILTERS = ["ALL", "ar", "fr", "en", "darija"] as const;

function formatLanguage(languageCode: string) {
  switch (languageCode.toLowerCase()) {
    case "ar":
      return "Arabic";
    case "fr":
      return "French";
    case "darija":
      return "Darija";
    default:
      return "English";
  }
}

function ExploreRoute() {
  return (
    <RequireAuth>
      <ExplorePage />
    </RequireAuth>
  );
}

function ExplorePage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [level, setLevel] = useState<(typeof LEVEL_FILTERS)[number]>("ALL");
  const [language, setLanguage] = useState<(typeof LANGUAGE_FILTERS)[number]>("ALL");
  const deferredQuery = useDeferredValue(query);

  const coursesQuery = useQuery({
    queryKey: ["courses", deferredQuery, level],
    queryFn: () =>
      listCourses(auth.getAccessToken, {
        q: deferredQuery || undefined,
        category: level === "ALL" ? undefined : level,
        size: 100,
      }),
  });

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const enrollMutation = useMutation({
    mutationFn: (courseId: string) => enrollInCourse(auth.getAccessToken, courseId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["enrollments"] });
    },
  });

  const enrolledCourseIds = new Set((enrollmentsQuery.data ?? []).map((item) => item.courseId));
  const catalog = (coursesQuery.data?.content ?? []).filter((course) => {
    if (language === "ALL") {
      return true;
    }

    return course.languageCode.toLowerCase() === language.toLowerCase();
  });
  const featured = catalog[0] ?? null;

  return (
    <AppShell
      active="explore"
      user={{
        displayName: auth.session?.displayName ?? "EduLife learner",
        email: auth.session?.email ?? "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-foreground">Explore published courses</p>
            <p className="text-xs text-muted-foreground">
              Real data from `/api/v1/courses`, protected by your Firebase session.
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-full border border-border/80 bg-surface px-4 py-2">
            <Search className="h-4 w-4 text-muted-foreground" />
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search courses..."
              className="w-full min-w-0 bg-transparent text-sm outline-none sm:w-64"
            />
          </div>
        </div>
      }
    >
      <section className="space-y-6">
        {featured && (
          <div className="rounded-3xl bg-gradient-to-br from-primary to-primary-glow px-6 py-8 text-primary-foreground shadow-elevated">
            <div className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs uppercase tracking-[0.16em]">
              <Sparkles className="h-3.5 w-3.5" />
              Featured right now
            </div>
            <h1 className="mt-4 text-display text-3xl">{featured.title}</h1>
            <p className="mt-2 max-w-2xl text-sm text-primary-foreground/75">
              {featured.shortDescription}
            </p>
            <div className="mt-5 flex flex-wrap items-center gap-3 text-xs text-primary-foreground/80">
              <span className="rounded-full bg-white/10 px-3 py-1">
                {featured.level.replace("_", " ")}
              </span>
              <span className="rounded-full bg-white/10 px-3 py-1">
                {formatLanguage(featured.languageCode)}
              </span>
            </div>
          </div>
        )}

        <div className="flex flex-wrap gap-2">
          {LEVEL_FILTERS.map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setLevel(value)}
              className={`rounded-full border px-4 py-2 text-xs font-medium transition-colors ${
                level === value
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-surface-elevated text-muted-foreground hover:text-foreground"
              }`}
            >
              {value === "ALL" ? "All levels" : value.replace("_", " ")}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap gap-2">
          {LANGUAGE_FILTERS.map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setLanguage(value)}
              className={`rounded-full border px-4 py-2 text-xs font-medium transition-colors ${
                language === value
                  ? "border-foreground bg-foreground text-background"
                  : "border-border bg-surface-elevated text-muted-foreground hover:text-foreground"
              }`}
            >
              {value === "ALL" ? "All languages" : formatLanguage(value)}
            </button>
          ))}
        </div>

        {coursesQuery.isLoading || enrollmentsQuery.isLoading ? (
          <StateCard title="Loading catalog..." detail="Fetching courses and your enrollment state." />
        ) : coursesQuery.isError ? (
          <StateCard title="Catalog unavailable" detail={coursesQuery.error.message} />
        ) : catalog.length === 0 ? (
          <StateCard
            title="No courses found"
            detail="Try a broader search or remove one of the active filters."
          />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {catalog.map((course) => {
              const enrolled = enrolledCourseIds.has(course.id);

              return (
                <article
                  key={course.id}
                  className="flex h-full flex-col rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft"
                >
                  <div className="aspect-[16/9] overflow-hidden rounded-2xl bg-muted">
                    {course.imageUrl ? (
                      <img
                        src={course.imageUrl}
                        alt={course.title}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="grid h-full place-items-center bg-gradient-to-br from-primary/10 to-primary-glow/10 text-primary">
                        <Compass className="h-10 w-10" />
                      </div>
                    )}
                  </div>

                  <div className="mt-4 flex flex-wrap gap-2 text-[11px] uppercase tracking-[0.16em] text-muted-foreground">
                    <span className="rounded-full bg-primary/8 px-3 py-1 text-primary">
                      {course.level.replace("_", " ")}
                    </span>
                    <span className="rounded-full bg-muted px-3 py-1">
                      {formatLanguage(course.languageCode)}
                    </span>
                  </div>

                  <h2 className="mt-4 text-lg font-semibold text-foreground">{course.title}</h2>
                  <p className="mt-2 flex-1 text-sm leading-relaxed text-muted-foreground">
                    {course.shortDescription}
                  </p>

                  <div className="mt-5 flex items-center justify-between gap-3">
                    <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                      <Users className="h-3.5 w-3.5" />
                      Published
                    </span>
                    {enrolled ? (
                      <Link
                        to="/courses"
                        className="rounded-full border border-primary/20 bg-primary/8 px-4 py-2 text-xs font-semibold text-primary"
                      >
                        In my courses
                      </Link>
                    ) : (
                      <button
                        type="button"
                        onClick={() => enrollMutation.mutate(course.id)}
                        disabled={enrollMutation.isPending}
                        className="rounded-full bg-foreground px-4 py-2 text-xs font-semibold text-background disabled:opacity-60"
                      >
                        {enrollMutation.isPending ? "Enrolling..." : "Enroll now"}
                      </button>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
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
