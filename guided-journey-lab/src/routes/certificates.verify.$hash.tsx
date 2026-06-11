import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  Award,
  CalendarDays,
  CheckCircle2,
  GraduationCap,
  Hash,
  Home,
  User,
  XCircle,
} from "lucide-react";
import { verifyCertificate } from "../lib/api/client";
import { ApiClientError } from "../lib/api/client";

export const Route = createFileRoute("/certificates/verify/$hash")({
  component: VerifyCertificateRoute,
  head: () => ({ meta: [{ title: "Certificate Verification — EduLife" }] }),
});

function VerifyCertificateRoute() {
  const { hash } = Route.useParams();

  const query = useQuery({
    queryKey: ["certificate-verify", hash],
    queryFn: () => verifyCertificate(hash),
    retry: false,
  });

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="flex h-16 items-center justify-between border-b border-border/60 bg-surface-elevated px-6">
        <div className="flex items-center gap-3">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-teal text-teal-foreground shadow-soft">
            <GraduationCap className="h-4 w-4" />
          </span>
          <span className="text-display text-lg text-foreground">EduLife</span>
        </div>
        <Link
          to="/"
          className="inline-flex items-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-xs font-semibold text-foreground transition-colors hover:bg-accent"
        >
          <Home className="h-3.5 w-3.5" />
          Home
        </Link>
      </header>

      <main className="flex flex-1 flex-col items-center justify-center px-4 py-12">
        <div className="w-full max-w-lg space-y-6">
          <div className="text-center">
            <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
              Certificate verification
            </p>
            <h1 className="mt-2 text-display text-2xl text-foreground">
              Verify authenticity
            </h1>
          </div>

          {query.isLoading ? (
            <LoadingCard />
          ) : query.isError ? (
            <ErrorCard error={query.error} hash={hash} />
          ) : query.data ? (
            <ResultCard data={query.data} hash={hash} />
          ) : null}
        </div>
      </main>
    </div>
  );
}

function LoadingCard() {
  return (
    <div className="animate-pulse rounded-3xl border border-border bg-surface-elevated p-8 shadow-soft">
      <div className="h-5 w-40 rounded-lg bg-muted" />
      <div className="mt-4 h-8 w-64 rounded-lg bg-muted" />
      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-16 rounded-2xl bg-muted" />
        ))}
      </div>
    </div>
  );
}

function ErrorCard({ error, hash }: { error: Error; hash: string }) {
  const is404 = error instanceof ApiClientError && error.status === 404;

  return (
    <div className="rounded-3xl border border-destructive/30 bg-destructive/5 p-8 shadow-soft">
      <div className="flex items-center gap-3">
        <span className="grid h-10 w-10 place-items-center rounded-2xl bg-destructive/10 text-destructive">
          <XCircle className="h-5 w-5" />
        </span>
        <div>
          <p className="text-sm font-semibold text-foreground">
            {is404 ? "Certificate not found" : "Verification failed"}
          </p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {is404
              ? "No certificate matches this verification link."
              : error.message}
          </p>
        </div>
      </div>
      {is404 && (
        <p className="mt-4 rounded-xl bg-muted/60 px-4 py-3 font-mono text-[11px] text-muted-foreground break-all">
          {hash}
        </p>
      )}
    </div>
  );
}

function ResultCard({
  data,
  hash,
}: {
  data: { studentName: string; courseTitle: string; issuerName: string; issuedAt: string; certificateNumber: string; valid: boolean };
  hash: string;
}) {
  return (
    <article
      className={`overflow-hidden rounded-3xl border shadow-gold ${
        data.valid
          ? "border-gold/30 bg-gradient-gold"
          : "border-destructive/30 bg-destructive/5"
      }`}
    >
      <div className="p-8">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span
              className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-[11px] uppercase tracking-[0.16em] ${
                data.valid
                  ? "bg-gold-foreground/10 text-gold-foreground"
                  : "bg-destructive/10 text-destructive"
              }`}
            >
              {data.valid ? (
                <CheckCircle2 className="h-3.5 w-3.5" />
              ) : (
                <XCircle className="h-3.5 w-3.5" />
              )}
              {data.valid ? "Verified" : "Invalid certificate"}
            </span>
            <h2
              className={`mt-4 text-display text-2xl ${
                data.valid ? "text-gold-foreground" : "text-foreground"
              }`}
            >
              {data.courseTitle}
            </h2>
            <p
              className={`mt-1 text-sm ${
                data.valid ? "text-gold-foreground/85" : "text-muted-foreground"
              }`}
            >
              Awarded to {data.studentName}
            </p>
          </div>
          <div
            className={`grid h-14 w-14 shrink-0 place-items-center rounded-2xl ${
              data.valid ? "bg-gold-foreground/10 text-gold-foreground" : "bg-muted text-muted-foreground"
            }`}
          >
            <Award className="h-7 w-7" />
          </div>
        </div>

        <dl className="mt-6 grid gap-3 sm:grid-cols-2">
          <VerifyField
            icon={<User className="h-3.5 w-3.5" />}
            label="Issued by"
            value={data.issuerName}
            gold={data.valid}
          />
          <VerifyField
            icon={<CalendarDays className="h-3.5 w-3.5" />}
            label="Issue date"
            value={new Date(data.issuedAt).toLocaleDateString(undefined, {
              year: "numeric",
              month: "long",
              day: "numeric",
            })}
            gold={data.valid}
          />
          <VerifyField
            icon={<Hash className="h-3.5 w-3.5" />}
            label="Certificate number"
            value={data.certificateNumber}
            gold={data.valid}
          />
          <VerifyField
            icon={<CheckCircle2 className="h-3.5 w-3.5" />}
            label="Verification hash"
            value={hash}
            mono
            gold={data.valid}
          />
        </dl>
      </div>
    </article>
  );
}

function VerifyField({
  icon,
  label,
  value,
  mono,
  gold,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  mono?: boolean;
  gold?: boolean;
}) {
  const base = gold ? "bg-gold-foreground/8" : "bg-muted/60";
  const labelColor = gold ? "text-gold-foreground/75" : "text-muted-foreground";
  const valueColor = gold ? "text-gold-foreground" : "text-foreground";

  return (
    <div className={`rounded-2xl ${base} p-4`}>
      <dt className={`inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] ${labelColor}`}>
        {icon}
        {label}
      </dt>
      <dd className={`mt-2 break-all text-sm font-medium ${valueColor}${mono ? " font-mono text-xs" : ""}`}>
        {value}
      </dd>
    </div>
  );
}
