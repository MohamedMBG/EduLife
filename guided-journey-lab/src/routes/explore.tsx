import { useDeferredValue, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Compass, Search, Sparkles } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { enrollInCourse, listCourses, listMyEnrollments } from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/explore")({
  component: ExploreRoute,
  head: () => ({ meta: [{ title: "Explore Courses - EduLife" }] }),
});

const LEVEL_FILTERS = ["ALL", "BEGINNER", "INTERMEDIATE", "ADVANCED"] as const;
const LANGUAGE_FILTERS = ["ALL", "ar", "fr", "en"] as const;

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

function formatLevel(level: string) {
  return level.replace("_", " ").replace(/\b\w/g, (c) => c.toUpperCase());
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
    if (language === "ALL") return true;
    return course.languageCode.toLowerCase() === language.toLowerCase();
  });
  const featured = catalog[0] ?? null;

  const isLoading = coursesQuery.isLoading || enrollmentsQuery.isLoading;

  return (
    <AppLayout showSearch searchValue={query} onSearch={setQuery}>
      <div>
        {/* Page Header */}
        <header className="mb-12 sm:mb-16">
          <h1
            className="text-4xl sm:text-5xl font-light tracking-tight"
            style={{
              color: "#091426",
              fontFamily: "Montserrat, sans-serif",
              letterSpacing: "-0.02em",
            }}
          >
            Explore Courses
          </h1>
          <p
            className="mt-2 text-lg font-light max-w-2xl leading-relaxed"
            style={{ color: "#505f76" }}
          >
            Discover your path to academic excellence with our curated selection of premium
            educational content.
          </p>
        </header>

        {/* Featured Course Hero */}
        {featured && (
          <section className="mb-12 sm:mb-16">
            <Link
              to="/courses/$courseId"
              params={{ courseId: featured.id }}
              className="group relative block w-full h-[320px] sm:h-[400px] rounded-xl overflow-hidden"
              style={{ border: "1px solid #c5c6cd" }}
            >
              <div
                className="absolute inset-0 z-10"
                style={{
                  background: "linear-gradient(to right, rgba(9,20,38,0.9), rgba(9,20,38,0.2))",
                }}
              />
              {featured.imageUrl ? (
                <img
                  src={featured.imageUrl}
                  alt={featured.title}
                  className="absolute inset-0 w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                  onError={(e) => {
                    e.currentTarget.style.display = "none";
                    e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
                  }}
                />
              ) : null}
              <div
                className="absolute inset-0"
                style={{ background: "linear-gradient(135deg, #1e293b, #091426)" }}
                data-fallback=""
                hidden={!!featured.imageUrl}
              />
              <div className="absolute inset-0 z-20 p-8 sm:p-12 flex flex-col justify-center max-w-3xl">
                <div className="flex items-center gap-2 mb-4">
                  <span
                    className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-semibold uppercase tracking-widest backdrop-blur-md"
                    style={{
                      background: "rgba(255,255,255,0.1)",
                      color: "#ffffff",
                      border: "1px solid rgba(255,255,255,0.2)",
                    }}
                  >
                    <Sparkles className="h-3 w-3" />
                    Featured right now
                  </span>
                </div>
                <h2
                  className="text-2xl sm:text-4xl font-light leading-tight"
                  style={{
                    color: "#ffffff",
                    fontFamily: "Montserrat, sans-serif",
                    letterSpacing: "-0.01em",
                  }}
                >
                  {featured.title}
                </h2>
                <p
                  className="mt-2 text-sm sm:text-base font-light max-w-xl"
                  style={{ color: "rgba(255,255,255,0.8)" }}
                >
                  {featured.shortDescription}
                </p>
                <div className="mt-5 flex gap-3">
                  <span
                    className="px-4 py-2 rounded-xl text-xs font-semibold backdrop-blur-sm"
                    style={{ background: "rgba(255,255,255,0.1)", color: "#ffffff" }}
                  >
                    {formatLevel(featured.level)}
                  </span>
                  <span
                    className="px-4 py-2 rounded-xl text-xs font-semibold backdrop-blur-sm"
                    style={{ background: "rgba(255,255,255,0.1)", color: "#ffffff" }}
                  >
                    {formatLanguage(featured.languageCode)}
                  </span>
                </div>
              </div>
            </Link>
          </section>
        )}

        {/* Filter System */}
        <section
          className="mb-8"
          style={{ borderBottom: "1px solid #c5c6cd", paddingBottom: "16px" }}
        >
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:flex-wrap">
            <span
              className="text-xs font-semibold uppercase tracking-widest shrink-0"
              style={{ color: "#091426", opacity: 0.5, letterSpacing: "0.05em" }}
            >
              Filter by
            </span>

            <div className="flex flex-wrap gap-2">
              {LEVEL_FILTERS.map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setLevel(value)}
                  className="px-5 py-2 rounded-full text-xs font-semibold transition-all cursor-pointer"
                  style={
                    level === value
                      ? { background: "#091426", color: "#ffffff" }
                      : {
                          background: "transparent",
                          color: "#505f76",
                          border: "1px solid #c5c6cd",
                        }
                  }
                  aria-pressed={level === value}
                >
                  {value === "ALL" ? "All Levels" : formatLevel(value)}
                </button>
              ))}
            </div>

            <div
              className="hidden md:block self-stretch"
              style={{ width: "1px", background: "#c5c6cd", margin: "0 8px" }}
            />

            <div className="flex flex-wrap gap-2">
              {LANGUAGE_FILTERS.map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setLanguage(value)}
                  className="px-5 py-2 rounded-full text-xs font-semibold transition-all cursor-pointer"
                  style={
                    language === value
                      ? { background: "#091426", color: "#ffffff" }
                      : {
                          background: "transparent",
                          color: "#505f76",
                          border: "1px solid #c5c6cd",
                        }
                  }
                  aria-pressed={language === value}
                >
                  {value === "ALL" ? "All Languages" : formatLanguage(value)}
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* Course Grid */}
        {isLoading ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
        ) : coursesQuery.isError ? (
          <ErrorState message={coursesQuery.error.message} />
        ) : catalog.length === 0 ? (
          <EmptyState />
        ) : (
          <section className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {catalog.map((course) => {
              const enrolled = enrolledCourseIds.has(course.id);
              return (
                <CourseCard
                  key={course.id}
                  course={course}
                  enrolled={enrolled}
                  enrolling={enrollMutation.isPending}
                  onEnroll={() => enrollMutation.mutate(course.id)}
                />
              );
            })}
          </section>
        )}
      </div>
    </AppLayout>
  );
}

/* ─── Course Card ─── */
function CourseCard({
  course,
  enrolled,
  enrolling,
  onEnroll,
}: {
  course: {
    id: string;
    title: string;
    shortDescription: string;
    level: string;
    languageCode: string;
    imageUrl: string | null;
  };
  enrolled: boolean;
  enrolling: boolean;
  onEnroll: () => void;
}) {
  return (
    <article
      className="group flex flex-col rounded-xl overflow-hidden transition-all duration-300"
      style={{
        background: "#ffffff",
        border: "1px solid #c5c6cd",
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.borderColor = "#091426";
        e.currentTarget.style.boxShadow = "0 32px 64px -12px rgba(9,20,38,0.06)";
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.borderColor = "#c5c6cd";
        e.currentTarget.style.boxShadow = "none";
      }}
    >
      <Link to="/courses/$courseId" params={{ courseId: course.id }} className="block">
        <div className="relative h-48 overflow-hidden">
          {course.imageUrl ? (
            <img
              src={course.imageUrl}
              alt={course.title}
              className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
              onError={(e) => {
                e.currentTarget.style.display = "none";
                e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
              }}
            />
          ) : null}
          <div
            className="w-full h-full flex items-center justify-center"
            style={{ background: "linear-gradient(135deg, #1e293b, #091426)" }}
            data-fallback=""
            hidden={!!course.imageUrl}
          >
            <Compass className="h-10 w-10" style={{ color: "#8590a6" }} />
          </div>
          <div className="absolute top-4 left-4">
            <span
              className="px-3 py-1 rounded-xl text-[10px] font-semibold uppercase backdrop-blur"
              style={{
                background: "rgba(255,255,255,0.9)",
                color: "#091426",
                border: "1px solid #c5c6cd",
              }}
            >
              {formatLevel(course.level)}
            </span>
          </div>
        </div>
      </Link>

      <div className="flex flex-col flex-1 p-5">
        <Link to="/courses/$courseId" params={{ courseId: course.id }} className="block">
          <h3
            className="text-xl font-normal leading-snug line-clamp-2"
            style={{ color: "#091426", fontFamily: "Montserrat, sans-serif" }}
          >
            {course.title}
          </h3>
          <p className="mt-2 text-sm leading-relaxed line-clamp-3" style={{ color: "#505f76" }}>
            {course.shortDescription}
          </p>
        </Link>

        <div className="mt-auto pt-5 flex items-center justify-between">
          <div className="flex items-center gap-1.5" style={{ color: "#505f76" }}>
            <span className="text-[10px] font-semibold uppercase tracking-widest">
              {enrolled ? "In Progress" : formatLanguage(course.languageCode)}
            </span>
          </div>

          {enrolled ? (
            <Link
              to="/courses/$courseId"
              params={{ courseId: course.id }}
              className="px-5 py-2 rounded-xl text-xs font-semibold transition-all"
              style={{
                background: "#d8e3fb",
                color: "#091426",
              }}
            >
              Continue
            </Link>
          ) : (
            <button
              type="button"
              onClick={(e) => {
                e.preventDefault();
                onEnroll();
              }}
              disabled={enrolling}
              className="px-5 py-2 rounded-xl text-xs font-semibold transition-all disabled:opacity-60 cursor-pointer"
              style={{
                background: "#091426",
                color: "#ffffff",
              }}
            >
              {enrolling ? "Enrolling..." : "Enroll Now"}
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

/* ─── Skeleton Loading Card ─── */
function SkeletonCard() {
  return (
    <div
      className="rounded-xl overflow-hidden animate-pulse"
      style={{ background: "#ffffff", border: "1px solid #c5c6cd" }}
    >
      <div className="h-48" style={{ background: "#eaeef2" }} />
      <div className="p-5 space-y-3">
        <div className="h-5 rounded" style={{ background: "#eaeef2", width: "75%" }} />
        <div className="h-4 rounded" style={{ background: "#eaeef2", width: "100%" }} />
        <div className="h-4 rounded" style={{ background: "#eaeef2", width: "60%" }} />
        <div className="flex justify-between items-center pt-3">
          <div className="h-3 rounded" style={{ background: "#eaeef2", width: "25%" }} />
          <div className="h-8 rounded-xl" style={{ background: "#eaeef2", width: "90px" }} />
        </div>
      </div>
    </div>
  );
}

/* ─── Empty State ─── */
function EmptyState() {
  return (
    <div
      className="rounded-xl px-6 py-16 text-center"
      style={{ background: "#ffffff", border: "1px solid #c5c6cd" }}
    >
      <Compass className="mx-auto h-10 w-10 mb-4" style={{ color: "#c5c6cd" }} />
      <p className="text-sm font-semibold" style={{ color: "#091426" }}>
        No courses found
      </p>
      <p className="mt-2 text-sm" style={{ color: "#505f76" }}>
        Try a broader search or remove one of the active filters.
      </p>
    </div>
  );
}

/* ─── Error State ─── */
function ErrorState({ message }: { message: string }) {
  return (
    <div
      className="rounded-xl px-6 py-16 text-center"
      style={{ background: "#ffffff", border: "1px solid #c5c6cd" }}
    >
      <p className="text-sm font-semibold" style={{ color: "#091426" }}>
        Catalog unavailable
      </p>
      <p className="mt-2 text-sm" style={{ color: "#505f76" }}>
        {message}
      </p>
    </div>
  );
}
