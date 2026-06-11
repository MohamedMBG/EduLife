import { useMemo, useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { AlertTriangle, ArrowLeft, CheckCircle2, Clock3, ShieldAlert } from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import { getCourseDetail, getExam, getExamStatus, submitExam } from "../lib/api/client";
import { ApiClientError } from "../lib/api/client";
import type { ExamAnswer } from "../lib/api/types";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/courses/$courseId/exam")({
  component: ExamRoute,
  head: () => ({ meta: [{ title: "Exam - EduLife" }] }),
});

function ExamRoute() {
  return (
    <RequireAuth>
      <ExamPage />
    </RequireAuth>
  );
}

function ExamPage() {
  const { courseId } = Route.useParams();
  const auth = useAuth();
  const navigate = useNavigate();
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);

  const courseQuery = useQuery({
    queryKey: ["course", courseId],
    queryFn: () => getCourseDetail(auth.getAccessToken, courseId),
  });

  const statusQuery = useQuery({
    queryKey: ["exam-status", courseId],
    queryFn: () => getExamStatus(auth.getAccessToken, courseId),
    retry: false,
  });

  const canTake =
    statusQuery.data && !statusQuery.data.passed && !statusQuery.data.inCooldown;

  const examQuery = useQuery({
    queryKey: ["exam", courseId],
    queryFn: () => getExam(auth.getAccessToken, courseId),
    enabled: Boolean(canTake),
  });

  const submitMutation = useMutation({
    mutationFn: (payload: ExamAnswer[]) =>
      submitExam(auth.getAccessToken, courseId, { answers: payload }),
    onSuccess: (result) => {
      navigate({
        to: "/courses/$courseId/exam/result",
        params: { courseId },
        search: {
          score: result.score,
          passScore: result.passScore,
          passed: result.passed,
          certificateNumber: result.certificateNumber ?? undefined,
          attemptsUsed: result.attemptsUsed,
          cooldownEndsAt: result.cooldownEndsAt ?? undefined,
        },
      });
    },
    onError: (err) => {
      if (err instanceof ApiClientError) {
        setSubmitError(err.message);
        if (err.status === 409 || err.status === 429) {
          statusQuery.refetch();
        }
      } else {
        setSubmitError(err instanceof Error ? err.message : "Submit failed.");
      }
    },
  });

  const courseTitle = courseQuery.data?.title || "Course";

  const sortedQuestions = useMemo(() => {
    if (!examQuery.data) return [];
    return [...examQuery.data.questions].sort((a, b) => a.orderIndex - b.orderIndex);
  }, [examQuery.data]);

  const allAnswered =
    sortedQuestions.length > 0 &&
    sortedQuestions.every((q) => Boolean(answers[q.questionId]));

  function handleSelect(questionId: string, choiceId: string) {
    setAnswers((prev) => ({ ...prev, [questionId]: choiceId }));
  }

  function handleSubmit() {
    if (!allAnswered) return;
    setSubmitError(null);
    submitMutation.mutate(
      sortedQuestions.map((q) => ({ questionId: q.questionId, choiceId: answers[q.questionId] })),
    );
  }

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
            <p className="text-sm font-semibold text-foreground">{courseTitle}</p>
            <p className="text-xs text-muted-foreground">Final exam — backend scored.</p>
          </div>
        </div>
      }
    >
      {statusQuery.isLoading ? (
        <StateCard title="Loading exam..." detail="Checking attempt status." />
      ) : statusQuery.isError ? (
        <StateCard
          title="Exam unavailable"
          detail={
            statusQuery.error instanceof Error
              ? statusQuery.error.message
              : "Could not load exam status."
          }
        />
      ) : statusQuery.data?.passed ? (
        <AlreadyPassedCard courseId={courseId} />
      ) : statusQuery.data?.inCooldown ? (
        <CooldownCard
          cooldownEndsAt={statusQuery.data.cooldownEndsAt}
          failedAttempts={statusQuery.data.failedAttempts}
        />
      ) : examQuery.isLoading ? (
        <StateCard title="Loading questions..." detail="Fetching the exam." />
      ) : examQuery.isError ? (
        <StateCard
          title="Could not load exam"
          detail={
            examQuery.error instanceof Error
              ? examQuery.error.message
              : "Unknown error."
          }
        />
      ) : !examQuery.data || sortedQuestions.length === 0 ? (
        <StateCard
          title="No questions yet"
          detail="The exam for this course has not been published. Check back soon."
        />
      ) : (
        <div className="space-y-6">
          <section className="rounded-3xl bg-gradient-to-br from-primary to-primary-glow px-6 py-8 text-primary-foreground shadow-elevated">
            <p className="text-xs uppercase tracking-[0.16em] text-primary-foreground/70">
              {examQuery.data.title}
            </p>
            <h1 className="mt-3 text-display text-4xl">Final exam</h1>
            <div className="mt-4 flex flex-wrap items-center gap-2 text-xs font-semibold">
              <span className="rounded-full bg-white/10 px-3 py-1 uppercase tracking-[0.14em]">
                Pass {examQuery.data.passScore}%
              </span>
              {examQuery.data.timeLimitMinutes ? (
                <span className="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-1">
                  <Clock3 className="h-3.5 w-3.5" />
                  {examQuery.data.timeLimitMinutes} min
                </span>
              ) : null}
              <span className="rounded-full bg-white/10 px-3 py-1">
                {sortedQuestions.length} questions
              </span>
              {statusQuery.data ? (
                <span className="rounded-full bg-white/10 px-3 py-1">
                  Failed attempts: {statusQuery.data.failedAttempts} /{" "}
                  {statusQuery.data.maxAttemptsBeforeCooldown}
                </span>
              ) : null}
            </div>
          </section>

          <ol className="space-y-4">
            {sortedQuestions.map((question, idx) => (
              <li
                key={question.questionId}
                className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft"
              >
                <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                  Question {idx + 1}
                </p>
                <p className="mt-2 text-base font-semibold text-foreground">
                  {question.questionText}
                </p>
                <div className="mt-4 space-y-2">
                  {question.choices.map((choice) => {
                    const selected = answers[question.questionId] === choice.choiceId;
                    return (
                      <label
                        key={choice.choiceId}
                        className={`flex cursor-pointer items-center gap-3 rounded-2xl border px-4 py-3 text-sm transition ${
                          selected
                            ? "border-primary bg-primary/8 text-foreground"
                            : "border-border bg-background text-foreground hover:border-primary/40"
                        }`}
                      >
                        <input
                          type="radio"
                          name={question.questionId}
                          value={choice.choiceId}
                          checked={selected}
                          onChange={() => handleSelect(question.questionId, choice.choiceId)}
                          className="h-4 w-4 accent-primary"
                        />
                        <span>{choice.choiceText}</span>
                      </label>
                    );
                  })}
                </div>
              </li>
            ))}
          </ol>

          {submitError ? (
            <div className="flex items-start gap-3 rounded-2xl border border-destructive/30 bg-destructive/8 p-4 text-sm text-destructive">
              <AlertTriangle className="mt-0.5 h-4 w-4" />
              <p>{submitError}</p>
            </div>
          ) : null}

          <div className="flex items-center justify-between rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
            <div className="text-xs text-muted-foreground">
              {Object.keys(answers).length} / {sortedQuestions.length} answered
            </div>
            <button
              type="button"
              onClick={handleSubmit}
              disabled={!allAnswered || submitMutation.isPending}
              className="inline-flex items-center gap-2 rounded-full bg-foreground px-5 py-2.5 text-xs font-semibold text-background disabled:opacity-50"
            >
              {submitMutation.isPending ? "Submitting..." : "Submit exam"}
            </button>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function AlreadyPassedCard({ courseId }: { courseId: string }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated p-8 text-center shadow-soft">
      <CheckCircle2 className="mx-auto h-10 w-10 text-primary" />
      <h2 className="mt-3 text-display text-2xl text-foreground">Already passed</h2>
      <p className="mt-2 text-sm text-muted-foreground">
        You have already passed this exam. Your certificate is available.
      </p>
      <div className="mt-5 flex justify-center gap-3">
        <Link
          to="/certificates"
          className="inline-flex items-center gap-2 rounded-full bg-foreground px-5 py-2.5 text-xs font-semibold text-background"
        >
          View certificates
        </Link>
        <Link
          to="/courses/$courseId"
          params={{ courseId }}
          className="inline-flex items-center gap-2 rounded-full border border-border bg-background px-5 py-2.5 text-xs font-semibold text-foreground"
        >
          Back to course
        </Link>
      </div>
    </div>
  );
}

function CooldownCard({
  cooldownEndsAt,
  failedAttempts,
}: {
  cooldownEndsAt: string | null;
  failedAttempts: number;
}) {
  const ends = cooldownEndsAt ? new Date(cooldownEndsAt) : null;
  return (
    <div className="rounded-3xl border border-destructive/30 bg-destructive/8 p-8 text-center shadow-soft">
      <ShieldAlert className="mx-auto h-10 w-10 text-destructive" />
      <h2 className="mt-3 text-display text-2xl text-foreground">Exam locked</h2>
      <p className="mt-2 text-sm text-muted-foreground">
        You used {failedAttempts} failed attempt(s). A 72-hour cooldown is active.
      </p>
      {ends ? (
        <p className="mt-3 text-sm font-semibold text-foreground">
          Available again {ends.toLocaleString()}
        </p>
      ) : null}
    </div>
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
