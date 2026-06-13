import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Award, ArrowRight, CalendarDays, FileBadge2 } from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import { listMyCertificates, listMyEnrollments } from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/certificates/")({
  component: CertificatesRoute,
  head: () => ({ meta: [{ title: "Certificates - EduLife" }] }),
});

function CertificatesRoute() {
  return (
    <RequireAuth>
      <CertificatesPage />
    </RequireAuth>
  );
}

function CertificatesPage() {
  const auth = useAuth();

  const certificatesQuery = useQuery({
    queryKey: ["certificates"],
    queryFn: () => listMyCertificates(auth.getAccessToken),
  });

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const courseTitles = new Map(
    (enrollmentsQuery.data ?? []).map((enrollment) => [enrollment.courseId, enrollment.title]),
  );

  return (
    <AppShell
      active="certificates"
      user={{
        displayName: auth.session?.displayName ?? "EduLife learner",
        email: auth.session?.email ?? "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">Certificates</p>
          <p className="text-xs text-muted-foreground">
            Certificates earned after passing backend-scored course exams.
          </p>
        </div>
      }
    >
      {certificatesQuery.isLoading || enrollmentsQuery.isLoading ? (
        <StateCard title="Loading certificates..." detail="Fetching your verified certificate history." />
      ) : certificatesQuery.isError ? (
        <StateCard title="Certificates unavailable" detail={certificatesQuery.error.message} />
      ) : (certificatesQuery.data ?? []).length === 0 ? (
        <StateCard
          title="No certificates yet"
          detail="Pass a final exam first, then your certificate will appear here."
        />
      ) : (
        <section className="grid gap-4 lg:grid-cols-2">
          {(certificatesQuery.data ?? []).map((certificate) => (
            <article
              key={certificate.id}
              className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-3 py-1 text-[11px] uppercase tracking-[0.16em] text-primary">
                    <Award className="h-3.5 w-3.5" />
                    Verified certificate
                  </p>
                  <h2 className="mt-4 text-xl font-semibold text-foreground">
                    {certificate.courseTitle || courseTitles.get(certificate.courseId) || "Course completed"}
                  </h2>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-gradient-primary text-primary-foreground shadow-soft">
                  <FileBadge2 className="h-6 w-6" />
                </div>
              </div>

              <dl className="mt-5 grid gap-4 sm:grid-cols-2">
                <div className="rounded-2xl bg-muted/50 p-4">
                  <dt className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Certificate number
                  </dt>
                  <dd className="mt-2 text-sm font-medium text-foreground">
                    {certificate.certificateNumber}
                  </dd>
                </div>
                <div className="rounded-2xl bg-muted/50 p-4">
                  <dt className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    <CalendarDays className="h-3.5 w-3.5" />
                    Issued at
                  </dt>
                  <dd className="mt-2 text-sm font-medium text-foreground">
                    {new Date(certificate.issuedAt).toLocaleDateString()}
                  </dd>
                </div>
              </dl>

              <div className="mt-5 flex justify-end">
                <Link
                  to="/certificates/$certificateId"
                  params={{ certificateId: certificate.id }}
                  className="inline-flex items-center gap-2 rounded-full bg-gradient-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow-soft"
                >
                  View certificate
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </div>
            </article>
          ))}
        </section>
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
