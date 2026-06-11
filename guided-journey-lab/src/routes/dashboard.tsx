import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useEffect } from "react";
import {
  ArrowRight,
  Award,
  BookOpen,
  CheckCircle2,
  Compass,
  GraduationCap,
  Layers3,
  ShieldCheck,
  UserCircle2,
  Users,
} from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import {
  getAdminMetrics,
  enrollInCourse,
  getCourseProgress,
  getProfile,
  listCourses,
  listMyEnrollments,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/dashboard")({
  component: DashboardRoute,
  head: () => ({ meta: [{ title: "Dashboard - EduLife" }] }),
});

function DashboardRoute() {
  return (
    <RequireAuth>
      <DashboardPage />
    </RequireAuth>
  );
}

function DashboardPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isAdmin = auth.session?.role === "ADMIN";

  // Redirect admin users to the dedicated admin dashboard.
  useEffect(() => {
    if (isAdmin) {
      navigate({ to: "/admin/dashboard" });
    }
  }, [isAdmin, navigate]);
  const isTeacher = auth.session?.role === "TEACHER";
  const dashboardTitle = isAdmin
    ? "Platform dashboard"
    : isTeacher
      ? "Teacher dashboard"
      : "Learner dashboard";
  const dashboardDetail = isAdmin
    ? "Admin metrics and learner activity come from the Spring Boot backend."
    : "Profile, enrollments, and discovery all come from the Spring Boot backend.";

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile(auth.getAccessToken),
  });

  const adminMetricsQuery = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: () => getAdminMetrics(auth.getAccessToken),
    // The backend enforces ADMIN on this endpoint; the UI mirrors that rule to avoid
    // expected 403 errors for normal learner sessions.
    enabled: isAdmin,
  });

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const exploreQuery = useQuery({
    queryKey: ["courses", "dashboard"],
    queryFn: () => listCourses(auth.getAccessToken, { size: 6 }),
  });

  const progressQueries = useQueries({
    queries: (enrollmentsQuery.data ?? []).slice(0, 3).map((enrollment) => ({
      queryKey: ["progress", enrollment.courseId],
      queryFn: () => getCourseProgress(auth.getAccessToken, enrollment.courseId),
      enabled: enrollmentsQuery.isSuccess,
    })),
  });

  const enrollMutation = useMutation({
    mutationFn: (courseId: string) => enrollInCourse(auth.getAccessToken, courseId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["enrollments"] }),
        queryClient.invalidateQueries({ queryKey: ["profile"] }),
      ]);
    },
  });

  const profile = profileQuery.data;
  const enrollments = enrollmentsQuery.data ?? [];
  const enrolledCourseIds = new Set(enrollments.map((enrollment) => enrollment.courseId));
  const suggestedCourses = (exploreQuery.data?.content ?? []).filter(
    (course) => !enrolledCourseIds.has(course.id),
  );
  const activeCourse = enrollments[0];
  const activeProgress = progressQueries[0]?.data;
  const firstName =
    profile?.displayName?.split(" ").filter(Boolean)[0] ||
    auth.session?.displayName.split(" ").filter(Boolean)[0] ||
    "learner";
  const adminMetrics = adminMetricsQuery.data;

  return (
    <AppShell
      active="dashboard"
      user={{
        displayName: profile?.displayName || auth.session?.displayName || "EduLife learner",
        email: profile?.email || auth.session?.email || "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">{dashboardTitle}</p>
          <p className="text-xs text-muted-foreground">{dashboardDetail}</p>
        </div>
      }
    >
      {profileQuery.isLoading ||
      enrollmentsQuery.isLoading ||
      (isAdmin && adminMetricsQuery.isLoading) ? (
        <StateCard
          title="Loading dashboard..."
          detail="Syncing your learner profile and courses."
        />
      ) : profileQuery.isError ? (
        <StateCard title="Dashboard unavailable" detail={profileQuery.error.message} />
      ) : enrollmentsQuery.isError ? (
        <StateCard title="Enrollments unavailable" detail={enrollmentsQuery.error.message} />
      ) : isAdmin && adminMetricsQuery.isError ? (
        <StateCard title="Admin metrics unavailable" detail={adminMetricsQuery.error.message} />
      ) : (
        <div className="space-y-8">
          <section className="rounded-3xl bg-gradient-to-br from-primary to-primary-glow px-6 py-8 text-primary-foreground shadow-elevated">
            <p className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs uppercase tracking-[0.16em]">
              {isAdmin ? (
                <ShieldCheck className="h-3.5 w-3.5" />
              ) : (
                <GraduationCap className="h-3.5 w-3.5" />
              )}
              {isAdmin ? "Authenticated admin" : "Authenticated learner"}
            </p>
            <h1 className="mt-4 text-display text-4xl">Welcome back, {firstName}</h1>
            <p className="mt-2 max-w-2xl text-sm text-primary-foreground/75">
              {isAdmin
                ? "Your admin session is backed by Firebase auth plus `/api/v1/auth/sync`, then authorized by backend RBAC before metrics load."
                : "Your session is backed by Firebase auth plus `/api/v1/auth/sync`, so the website is now using the same identity bridge as the Android app."}
            </p>
          </section>

          {isAdmin ? (
            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <MetricCard
                title="Learners"
                value={String(adminMetrics?.totalLearners ?? 0)}
                icon={<Users className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Teachers"
                value={String(adminMetrics?.totalTeachers ?? 0)}
                icon={<ShieldCheck className="h-5 w-5 text-teal-600" />}
              />
              <MetricCard
                title="Published courses"
                value={String(adminMetrics?.totalCoursesPublished ?? 0)}
                icon={<Layers3 className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Certificates"
                value={String(adminMetrics?.totalCertificates ?? 0)}
                icon={<GraduationCap className="h-5 w-5 text-amber-500" />}
              />
            </section>
          ) : (
            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <MetricCard
                title="Display name"
                value={profile?.displayName || "Not set"}
                icon={<UserCircle2 className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Enrolled courses"
                value={String(profile?.enrolledCourses ?? 0)}
                icon={<BookOpen className="h-5 w-5 text-primary" />}
              />
              <MetricCard
                title="Completed lessons"
                value={String(profile?.completedLessons ?? 0)}
                icon={<CheckCircle2 className="h-5 w-5 text-teal-600" />}
              />
              <MetricCard
                title="Certificates"
                value={String(profile?.certificates ?? 0)}
                icon={<GraduationCap className="h-5 w-5 text-amber-500" />}
              />
            </section>
          )}

          <section className="grid gap-6 xl:grid-cols-[1.3fr_0.9fr]">
            <div className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-foreground">Continue learning</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    The next step in your current backend enrollment.
                  </p>
                </div>
                {activeCourse ? (
                  <Link
                    to="/courses/$courseId"
                    params={{ courseId: activeCourse.courseId }}
                    className="rounded-full border border-primary/20 bg-primary/8 px-4 py-2 text-xs font-semibold text-primary"
                  >
                    Open course
                  </Link>
                ) : (
                  <Link
                    to="/explore"
                    className="rounded-full border border-primary/20 bg-primary/8 px-4 py-2 text-xs font-semibold text-primary"
                  >
                    Browse catalog
                  </Link>
                )}
              </div>

              {activeCourse && activeProgress ? (
                <div className="mt-5 rounded-3xl bg-muted/40 p-5">
                  <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Current course
                  </p>
                  <h2 className="mt-2 text-xl font-semibold text-foreground">
                    {activeCourse.title}
                  </h2>
                  <p className="mt-2 text-sm text-muted-foreground">
                    {activeCourse.shortDescription}
                  </p>
                  <div className="mt-4">
                    <div className="mb-2 flex items-center justify-between text-xs text-muted-foreground">
                      <span>
                        {activeProgress.completedLessons} / {activeProgress.totalLessons} lessons
                      </span>
                      <span className="font-semibold text-foreground">
                        {Math.round(activeProgress.percentComplete)}%
                      </span>
                    </div>
                    <div className="h-2 rounded-full bg-border">
                      <div
                        className="h-full rounded-full bg-gradient-primary"
                        style={{ width: `${Math.round(activeProgress.percentComplete)}%` }}
                      />
                    </div>
                  </div>
                </div>
              ) : (
                <StateCard
                  title={
                    isAdmin
                      ? "No learner enrollment on this admin account"
                      : "No active enrollment yet"
                  }
                  detail={
                    isAdmin
                      ? "Admin accounts can still inspect the live catalog, but they are not required to have learner progress."
                      : "Browse the catalog and enroll in a course to start the learner flow."
                  }
                />
              )}
            </div>

            <div className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-foreground">Profile snapshot</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Loaded from `/api/v1/profile`.
                  </p>
                </div>
              </div>
              <dl className="mt-5 space-y-4 text-sm">
                <div>
                  <dt className="text-muted-foreground">Email</dt>
                  <dd className="mt-1 font-medium text-foreground">{profile?.email}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Bio</dt>
                  <dd className="mt-1 text-foreground">{profile?.bio || "No bio added yet."}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Role</dt>
                  <dd className="mt-1 font-medium text-foreground">{auth.session?.role}</dd>
                </div>
              </dl>
            </div>
          </section>

          <section className="space-y-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-foreground">
                  Recommended from the live catalog
                </p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Suggestions use the same published course endpoint as Explore.
                </p>
              </div>
              <Link
                to="/certificates"
                className="inline-flex items-center gap-2 rounded-full border border-border bg-surface-elevated px-4 py-2 text-xs font-semibold text-foreground"
              >
                Certificates
                <Award className="h-3.5 w-3.5" />
              </Link>
              <Link
                to="/explore"
                className="inline-flex items-center gap-2 rounded-full border border-border bg-surface-elevated px-4 py-2 text-xs font-semibold text-foreground"
              >
                Browse all
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>

            {exploreQuery.isLoading ? (
              <StateCard title="Loading suggestions..." detail="Fetching published courses." />
            ) : exploreQuery.isError ? (
              <StateCard title="Suggestions unavailable" detail={exploreQuery.error.message} />
            ) : suggestedCourses.length === 0 ? (
              <StateCard
                title="No suggestions yet"
                detail="You are already enrolled in the available seed courses."
              />
            ) : (
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {suggestedCourses.slice(0, 3).map((course) => (
                  <article
                    key={course.id}
                    className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft"
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
                          <Compass className="h-8 w-8" />
                        </div>
                      )}
                    </div>
                    <h3 className="mt-4 text-lg font-semibold text-foreground">{course.title}</h3>
                    <p className="mt-2 text-sm text-muted-foreground">{course.shortDescription}</p>
                    <div className="mt-5 flex flex-wrap gap-2">
                      <Link
                        to="/courses/$courseId"
                        params={{ courseId: course.id }}
                        className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-4 py-2 text-xs font-semibold text-primary"
                      >
                        View outline
                      </Link>
                      <button
                        type="button"
                        onClick={() => enrollMutation.mutate(course.id)}
                        disabled={enrollMutation.isPending}
                        className="inline-flex items-center gap-2 rounded-full bg-foreground px-4 py-2 text-xs font-semibold text-background disabled:opacity-60"
                      >
                        Enroll now
                        <ArrowRight className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </AppShell>
  );
}

function MetricCard({ title, value, icon }: { title: string; value: string; icon: ReactNode }) {
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
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-8 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
    </div>
  );
}
