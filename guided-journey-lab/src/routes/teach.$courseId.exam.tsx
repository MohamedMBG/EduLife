import { useCallback, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  CheckCircle2,
  CircleDot,
  GripVertical,
  Plus,
  ShieldAlert,
  Trash2,
} from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { createCmsExam, getCmsExam, listCmsCourses } from "../lib/api/client";
import { ApiClientError } from "../lib/api/client";
import { RequireTeacher, useAuth } from "../lib/auth/auth-context";
import type { CmsExamAdmin } from "../lib/api/types";

export const Route = createFileRoute("/teach/$courseId/exam")({
  component: ExamBuilderRoute,
  head: () => ({ meta: [{ title: "Final Exam Builder - EduLife" }] }),
});

function ExamBuilderRoute() {
  return (
    <RequireTeacher>
      <ExamBuilderPage />
    </RequireTeacher>
  );
}

// ── Form state types ─────────────────────────────────────────────────────────

interface ChoiceForm {
  key: number;
  choiceText: string;
  correct: boolean;
}

interface QuestionForm {
  key: number;
  questionText: string;
  choices: ChoiceForm[];
}

interface ExamForm {
  title: string;
  passScore: string;
  timeLimitMinutes: string;
  questions: QuestionForm[];
}

interface ValidationError {
  field: string;
  message: string;
}

let nextKey = 1;
function genKey() {
  return nextKey++;
}

function emptyChoice(correct = false): ChoiceForm {
  return { key: genKey(), choiceText: "", correct };
}

function emptyQuestion(): QuestionForm {
  return {
    key: genKey(),
    questionText: "",
    choices: [emptyChoice(true), emptyChoice()],
  };
}

function defaultForm(): ExamForm {
  return {
    title: "Final Exam",
    passScore: "80",
    timeLimitMinutes: "30",
    questions: [emptyQuestion()],
  };
}

// ── Validation ───────────────────────────────────────────────────────────────

function validate(form: ExamForm): ValidationError[] {
  const errors: ValidationError[] = [];

  if (!form.title.trim()) {
    errors.push({ field: "title", message: "Exam title is required." });
  }

  const passScore = Number(form.passScore);
  if (!form.passScore || Number.isNaN(passScore) || passScore < 1 || passScore > 100) {
    errors.push({ field: "passScore", message: "Pass score must be between 1 and 100." });
  }

  const timeLimit = Number(form.timeLimitMinutes);
  if (form.timeLimitMinutes && (Number.isNaN(timeLimit) || timeLimit < 1)) {
    errors.push({ field: "timeLimitMinutes", message: "Time limit must be at least 1 minute." });
  }

  if (form.questions.length === 0) {
    errors.push({ field: "questions", message: "At least one question is required." });
  }

  form.questions.forEach((q, qi) => {
    const qNum = qi + 1;
    if (!q.questionText.trim()) {
      errors.push({
        field: `q-${qi}-text`,
        message: `Question ${qNum}: question text is required.`,
      });
    }
    if (q.choices.length < 2) {
      errors.push({
        field: `q-${qi}-choices`,
        message: `Question ${qNum} must have at least 2 choices.`,
      });
    }
    q.choices.forEach((c, ci) => {
      if (!c.choiceText.trim()) {
        errors.push({
          field: `q-${qi}-c-${ci}`,
          message: `Question ${qNum}, choice ${ci + 1}: text is required.`,
        });
      }
    });
    const correctCount = q.choices.filter((c) => c.correct).length;
    if (correctCount !== 1) {
      errors.push({
        field: `q-${qi}-correct`,
        message: `Question ${qNum} must have exactly one correct answer.`,
      });
    }
  });

  return errors;
}

// ── Main page ────────────────────────────────────────────────────────────────

function ExamBuilderPage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const { courseId } = Route.useParams();

  const coursesQuery = useQuery({
    queryKey: ["cms", "courses"],
    queryFn: () => listCmsCourses(auth.getAccessToken),
  });
  const course = coursesQuery.data?.find((c) => c.id === courseId);

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

  const is404 = examQuery.error instanceof ApiClientError && examQuery.error.status === 404;
  const is403 = examQuery.error instanceof ApiClientError && examQuery.error.status === 403;
  const existingExam = examQuery.data ?? null;
  const showBuilder = is404 || (!examQuery.isLoading && !existingExam && !is403);

  return (
    <AppLayout>
      <div className="mx-auto max-w-4xl space-y-8">
        <Link
          to="/teach/$courseId"
          params={{ courseId }}
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to course content
        </Link>

        {coursesQuery.isLoading || examQuery.isLoading ? (
          <StateCard
            title="Loading exam data..."
            detail="Checking if this course has a final exam."
          />
        ) : is403 ? (
          <div className="rounded-3xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
            <ShieldAlert className="mx-auto h-8 w-8 text-destructive" />
            <p className="mt-3 text-sm font-medium text-destructive">Access denied</p>
            <p className="mt-1 text-xs text-muted-foreground">
              You do not have permission to manage this course's exam.
            </p>
          </div>
        ) : !course && !coursesQuery.isLoading ? (
          <StateCard
            title="Course not found"
            detail="This course does not exist or belongs to another teacher."
          />
        ) : existingExam ? (
          <ExistingExamView exam={existingExam} />
        ) : showBuilder ? (
          <ExamBuilderForm
            courseId={courseId}
            onCreated={() => {
              queryClient.invalidateQueries({ queryKey: ["cms", "exam", courseId] });
            }}
          />
        ) : examQuery.isError ? (
          <StateCard title="Error loading exam" detail={examQuery.error.message} />
        ) : null}
      </div>
    </AppLayout>
  );
}

// ── Existing exam (read-only) ────────────────────────────────────────────────

function ExistingExamView({ exam }: { exam: CmsExamAdmin }) {
  return (
    <div className="space-y-6">
      <section>
        <h1 className="text-display text-3xl text-foreground">{exam.title}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          This course already has a final exam. One exam per course is currently supported.
        </p>
      </section>

      <div className="grid gap-4 sm:grid-cols-3">
        <MetricCard label="Pass score" value={`${exam.passScore}%`} />
        <MetricCard
          label="Time limit"
          value={exam.timeLimitMinutes ? `${exam.timeLimitMinutes} min` : "None"}
        />
        <MetricCard label="Questions" value={String(exam.questions.length)} />
      </div>

      <div className="space-y-4">
        {exam.questions
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((q, qi) => (
            <div key={q.id} className="rounded-3xl border border-border bg-card p-6 shadow-soft">
              <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                Question {qi + 1}
              </p>
              <p className="mt-2 text-sm font-medium text-foreground">{q.questionText}</p>
              <div className="mt-4 space-y-2">
                {q.choices.map((c) => (
                  <div
                    key={c.id}
                    className={`flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm ${
                      c.correct
                        ? "border border-primary/30 bg-primary/5 text-primary font-medium"
                        : "border border-border/60 bg-surface text-foreground"
                    }`}
                  >
                    <CircleDot
                      className={`h-4 w-4 ${c.correct ? "text-primary" : "text-muted-foreground"}`}
                    />
                    {c.choiceText}
                    {c.correct && (
                      <span className="ml-auto text-[10px] font-bold uppercase tracking-wider text-primary">
                        Correct
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card px-5 py-4 shadow-soft">
      <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">{label}</p>
      <p className="mt-1 text-display text-2xl text-foreground">{value}</p>
    </div>
  );
}

// ── Exam builder form ────────────────────────────────────────────────────────

function ExamBuilderForm({ courseId, onCreated }: { courseId: string; onCreated: () => void }) {
  const auth = useAuth();
  const [form, setForm] = useState<ExamForm>(defaultForm);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [success, setSuccess] = useState(false);

  const createMutation = useMutation({
    mutationFn: () => {
      const payload = {
        title: form.title.trim(),
        passScore: Number(form.passScore),
        timeLimitMinutes: form.timeLimitMinutes ? Number(form.timeLimitMinutes) : undefined,
        questions: form.questions.map((q, i) => ({
          questionText: q.questionText.trim(),
          orderIndex: i + 1,
          choices: q.choices.map((c) => ({
            choiceText: c.choiceText.trim(),
            correct: c.correct,
          })),
        })),
      };
      return createCmsExam(auth.getAccessToken, courseId, payload);
    },
    onSuccess: () => {
      setSuccess(true);
      onCreated();
    },
  });

  const is409 =
    createMutation.error instanceof ApiClientError && createMutation.error.status === 409;

  const handleSubmit = useCallback(
    (event: React.FormEvent) => {
      event.preventDefault();
      const validationErrors = validate(form);
      setErrors(validationErrors);
      if (validationErrors.length > 0) return;
      createMutation.mutate();
    },
    [form, createMutation],
  );

  const updateQuestion = useCallback((qi: number, patch: Partial<QuestionForm>) => {
    setForm((prev) => ({
      ...prev,
      questions: prev.questions.map((q, i) => (i === qi ? { ...q, ...patch } : q)),
    }));
  }, []);

  const removeQuestion = useCallback((qi: number) => {
    setForm((prev) => ({
      ...prev,
      questions: prev.questions.filter((_, i) => i !== qi),
    }));
  }, []);

  const addQuestion = useCallback(() => {
    setForm((prev) => ({
      ...prev,
      questions: [...prev.questions, emptyQuestion()],
    }));
  }, []);

  if (success) {
    return (
      <div className="space-y-6">
        <div className="rounded-3xl border border-primary/30 bg-primary/5 px-6 py-10 text-center">
          <CheckCircle2 className="mx-auto h-10 w-10 text-primary" />
          <p className="mt-3 text-display text-xl text-foreground">
            Final exam created successfully
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Learners will see this exam after completing course requirements.
          </p>
          <Link
            to="/teach/$courseId"
            params={{ courseId }}
            className="mt-6 inline-flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90"
          >
            Back to course
          </Link>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-8">
      <section>
        <h1 className="text-display text-3xl text-foreground">Final Exam Builder</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Create one MCQ exam for this course. Learners will receive it after completing the course
          requirements.
        </p>
      </section>

      {/* Settings card */}
      <div className="rounded-3xl border border-border bg-card p-6 shadow-soft space-y-5">
        <h2 className="text-display text-lg text-foreground">Exam settings</h2>

        <Field label="Exam title" required>
          <input
            required
            maxLength={200}
            value={form.title}
            onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
            placeholder="e.g. Final Exam"
            className={fieldClass}
          />
          <FieldError errors={errors} field="title" />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Pass score (%)" required>
            <input
              type="number"
              required
              min={1}
              max={100}
              value={form.passScore}
              onChange={(e) => setForm((f) => ({ ...f, passScore: e.target.value }))}
              placeholder="80"
              className={fieldClass}
            />
            <p className="mt-1 text-[11px] text-muted-foreground">
              Learners must score at or above this percentage to pass.
            </p>
            <FieldError errors={errors} field="passScore" />
          </Field>
          <Field label="Time limit (minutes)">
            <input
              type="number"
              min={1}
              value={form.timeLimitMinutes}
              onChange={(e) => setForm((f) => ({ ...f, timeLimitMinutes: e.target.value }))}
              placeholder="30"
              className={fieldClass}
            />
            <p className="mt-1 text-[11px] text-muted-foreground">Leave empty for no time limit.</p>
            <FieldError errors={errors} field="timeLimitMinutes" />
          </Field>
        </div>
      </div>

      {/* Questions */}
      <section className="space-y-5">
        <div className="flex items-center justify-between">
          <h2 className="text-display text-lg text-foreground">Questions</h2>
          <button
            type="button"
            onClick={addQuestion}
            className="inline-flex h-9 items-center gap-1.5 rounded-full border border-border px-3 text-xs font-medium text-foreground transition-colors hover:bg-accent"
          >
            <Plus className="h-3.5 w-3.5" /> Add question
          </button>
        </div>
        <FieldError errors={errors} field="questions" />

        {form.questions.map((q, qi) => (
          <QuestionCard
            key={q.key}
            qi={qi}
            question={q}
            errors={errors}
            onUpdate={(patch) => updateQuestion(qi, patch)}
            onRemove={() => removeQuestion(qi)}
            canRemove={form.questions.length > 1}
          />
        ))}
      </section>

      {/* Errors */}
      {createMutation.isError && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {is409
            ? "This course already has a final exam. Refresh to view it."
            : createMutation.error.message}
        </div>
      )}

      {errors.length > 0 && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          Please fix {errors.length} validation {errors.length === 1 ? "error" : "errors"} above.
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={createMutation.isPending}
          className="inline-flex h-11 items-center gap-2 rounded-full bg-primary px-6 text-sm font-medium text-primary-foreground shadow-elevated transition-all hover:opacity-90 disabled:opacity-40"
        >
          <CheckCircle2 className="h-4 w-4" />
          {createMutation.isPending ? "Saving exam..." : "Save exam"}
        </button>
        <Link
          to="/teach/$courseId"
          params={{ courseId }}
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          Cancel
        </Link>
      </div>
    </form>
  );
}

// ── Question card ────────────────────────────────────────────────────────────

function QuestionCard({
  qi,
  question,
  errors,
  onUpdate,
  onRemove,
  canRemove,
}: {
  qi: number;
  question: QuestionForm;
  errors: ValidationError[];
  onUpdate: (patch: Partial<QuestionForm>) => void;
  onRemove: () => void;
  canRemove: boolean;
}) {
  const setChoiceText = (ci: number, text: string) => {
    onUpdate({
      choices: question.choices.map((c, i) => (i === ci ? { ...c, choiceText: text } : c)),
    });
  };

  const setCorrect = (ci: number) => {
    onUpdate({
      choices: question.choices.map((c, i) => ({ ...c, correct: i === ci })),
    });
  };

  const addChoice = () => {
    onUpdate({ choices: [...question.choices, emptyChoice()] });
  };

  const removeChoice = (ci: number) => {
    if (question.choices.length <= 2) return;
    onUpdate({ choices: question.choices.filter((_, i) => i !== ci) });
  };

  return (
    <div className="rounded-3xl border border-border bg-card p-6 shadow-soft space-y-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <GripVertical className="h-4 w-4 text-muted-foreground" />
          <span className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
            Question {qi + 1}
          </span>
        </div>
        {canRemove && (
          <button
            type="button"
            onClick={onRemove}
            className="grid h-8 w-8 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
            aria-label={`Delete question ${qi + 1}`}
          >
            <Trash2 className="h-4 w-4" />
          </button>
        )}
      </div>

      <textarea
        rows={2}
        value={question.questionText}
        onChange={(e) => onUpdate({ questionText: e.target.value })}
        placeholder="Enter your question..."
        className="w-full rounded-xl border border-input bg-surface px-4 py-3 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
      />
      <FieldError errors={errors} field={`q-${qi}-text`} />

      <div className="space-y-2">
        <p className="text-xs text-muted-foreground">
          Select the correct answer with the radio button.
        </p>
        {question.choices.map((c, ci) => (
          <div key={c.key} className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setCorrect(ci)}
              className={`grid h-8 w-8 shrink-0 place-items-center rounded-full border-2 transition-all ${
                c.correct
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border text-muted-foreground hover:border-primary/40"
              }`}
              aria-label={
                c.correct ? `Choice ${ci + 1} is correct` : `Mark choice ${ci + 1} as correct`
              }
            >
              {c.correct && <CheckCircle2 className="h-4 w-4" />}
            </button>
            <input
              value={c.choiceText}
              onChange={(e) => setChoiceText(ci, e.target.value)}
              placeholder={`Choice ${ci + 1}`}
              className="flex-1 h-10 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
            />
            {question.choices.length > 2 && (
              <button
                type="button"
                onClick={() => removeChoice(ci)}
                className="grid h-8 w-8 shrink-0 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
                aria-label={`Remove choice ${ci + 1}`}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        ))}
        <FieldError errors={errors} field={`q-${qi}-correct`} />
        <FieldError errors={errors} field={`q-${qi}-choices`} />
        {question.choices.map((_, ci) => (
          <FieldError key={ci} errors={errors} field={`q-${qi}-c-${ci}`} />
        ))}
      </div>

      <button
        type="button"
        onClick={addChoice}
        className="inline-flex items-center gap-1.5 text-xs font-medium text-primary transition-colors hover:text-primary/80"
      >
        <Plus className="h-3.5 w-3.5" /> Add choice
      </button>
    </div>
  );
}

// ── Shared components ────────────────────────────────────────────────────────

const fieldClass =
  "w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all";

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

function FieldError({ errors, field }: { errors: ValidationError[]; field: string }) {
  const match = errors.find((e) => e.field === field);
  if (!match) return null;
  return (
    <p role="alert" className="text-xs text-destructive mt-1">
      {match.message}
    </p>
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
