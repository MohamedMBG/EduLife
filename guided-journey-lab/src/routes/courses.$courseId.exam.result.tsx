import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Award, CheckCircle2, ShieldAlert } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

interface ExamResultSearch {
  score?: number;
  passScore?: number;
  passed?: boolean;
  certificateNumber?: string;
  attemptsUsed?: number;
  cooldownEndsAt?: string;
}

export const Route = createFileRoute("/courses/$courseId/exam/result")({
  component: ExamResultRoute,
  head: () => ({ meta: [{ title: "Exam Result - EduLife" }] }),
  validateSearch: (search: Record<string, unknown>): ExamResultSearch => ({
    score: typeof search.score === "number" ? search.score : undefined,
    passScore: typeof search.passScore === "number" ? search.passScore : undefined,
    passed: typeof search.passed === "boolean" ? search.passed : undefined,
    certificateNumber:
      typeof search.certificateNumber === "string" ? search.certificateNumber : undefined,
    attemptsUsed: typeof search.attemptsUsed === "number" ? search.attemptsUsed : undefined,
    cooldownEndsAt: typeof search.cooldownEndsAt === "string" ? search.cooldownEndsAt : undefined,
  }),
});

function ExamResultRoute() {
  return (
    <RequireAuth>
      <ExamResultPage />
    </RequireAuth>
  );
}

function ExamResultPage() {
  const { courseId } = Route.useParams();
  const search = Route.useSearch();
  const auth = useAuth();

  const hasResult = typeof search.score === "number" && typeof search.passed === "boolean";

  return (
    <AppLayout>
      {!hasResult ? (
        <div className="rounded-3xl border border-border bg-surface-elevated p-8 text-center shadow-soft">
          <p className="text-sm font-semibold text-foreground">No result to display</p>
          <p className="mt-2 text-sm text-muted-foreground">
            Submit the exam first to see your result here.
          </p>
          <Link
            to="/courses/$courseId/exam"
            params={{ courseId }}
            className="mt-5 inline-flex items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-semibold text-primary-foreground"
          >
            Go to exam
          </Link>
        </div>
      ) : search.passed ? (
        <PassCard
          score={search.score!}
          passScore={search.passScore ?? 80}
          certificateNumber={search.certificateNumber}
          courseId={courseId}
        />
      ) : (
        <FailCard
          score={search.score!}
          passScore={search.passScore ?? 80}
          attemptsUsed={search.attemptsUsed ?? 0}
          cooldownEndsAt={search.cooldownEndsAt}
          courseId={courseId}
        />
      )}
    </AppLayout>
  );
}

function PassCard({
  score,
  passScore,
  certificateNumber,
  courseId,
}: {
  score: number;
  passScore: number;
  certificateNumber: string | undefined;
  courseId: string;
}) {
  return (
    <div className="rounded-3xl border border-border bg-gradient-gold p-8 text-center shadow-gold">
      <Award className="mx-auto h-12 w-12 text-gold-foreground" />
      <h1 className="mt-3 text-display text-3xl text-gold-foreground">You passed</h1>
      <p className="mt-2 text-sm text-gold-foreground/85">
        Score {score}% — pass threshold {passScore}%
      </p>
      {certificateNumber ? (
        <p className="mt-4 inline-flex items-center gap-2 rounded-full bg-gold-foreground/10 px-4 py-2 text-xs font-semibold text-gold-foreground">
          <CheckCircle2 className="h-3.5 w-3.5" />
          Certificate {certificateNumber}
        </p>
      ) : null}
      <div className="mt-6 flex justify-center gap-3">
        <Link
          to="/certificates"
          className="inline-flex items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-semibold text-primary-foreground"
        >
          View certificates
        </Link>
        <Link
          to="/courses/$courseId"
          params={{ courseId }}
          className="inline-flex items-center gap-2 rounded-full border border-foreground/15 bg-background/40 px-5 py-2.5 text-xs font-semibold text-foreground"
        >
          Back to course
        </Link>
      </div>
    </div>
  );
}

function FailCard({
  score,
  passScore,
  attemptsUsed,
  cooldownEndsAt,
  courseId,
}: {
  score: number;
  passScore: number;
  attemptsUsed: number;
  cooldownEndsAt: string | undefined;
  courseId: string;
}) {
  const ends = cooldownEndsAt ? new Date(cooldownEndsAt) : null;
  const locked = Boolean(ends);

  return (
    <div className="rounded-3xl border border-destructive/30 bg-destructive/8 p-8 text-center shadow-soft">
      <ShieldAlert className="mx-auto h-12 w-12 text-destructive" />
      <h1 className="mt-3 text-display text-3xl text-foreground">Did not pass</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        Score {score}% — pass threshold {passScore}%
      </p>
      <p className="mt-3 text-sm font-semibold text-foreground">
        Failed attempts used: {attemptsUsed}
      </p>
      {locked && ends ? (
        <p className="mt-2 text-sm text-destructive">
          72-hour cooldown active. Try again {ends.toLocaleString()}
        </p>
      ) : null}
      <div className="mt-6 flex justify-center gap-3">
        {!locked ? (
          <Link
            to="/courses/$courseId/exam"
            params={{ courseId }}
            className="inline-flex items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-semibold text-primary-foreground"
          >
            Retry exam
          </Link>
        ) : null}
        <Link
          to="/courses/$courseId"
          params={{ courseId }}
          className="inline-flex items-center gap-2 rounded-full border border-border bg-background px-5 py-2.5 text-xs font-semibold text-foreground"
        >
          Review course
        </Link>
      </div>
    </div>
  );
}
