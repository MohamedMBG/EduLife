import { createFileRoute, Link } from "@tanstack/react-router";
import { AnimatePresence, motion } from "framer-motion";
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  Award,
  CheckCircle,
  ChevronLeft,
  ChevronRight,
  Clock,
  RotateCcw,
  XCircle,
  Zap,
} from "lucide-react";
import { useState } from "react";
import { getLearnerCourseDetail } from "../lib/learner-flow-data";

export const Route = createFileRoute("/courses/$courseId/exam")({
  component: ExamPage,
  head: () => ({ meta: [{ title: "Exam - EduLife" }] }),
});

interface Question {
  id: string;
  text: string;
  options: { id: string; text: string }[];
  correct: string;
}

const EXAM_DATA: Record<string, { questions: Question[]; passMark: number; xpReward: number }> = {
  "1": {
    passMark: 80,
    xpReward: 150,
    questions: [
      {
        id: "q1",
        text: "Which HTML element is used to define the largest heading?",
        options: [
          { id: "a", text: "<h6>" },
          { id: "b", text: "<heading>" },
          { id: "c", text: "<h1>" },
          { id: "d", text: "<head>" },
        ],
        correct: "c",
      },
      {
        id: "q2",
        text: "Which CSS property controls the text size?",
        options: [
          { id: "a", text: "font-style" },
          { id: "b", text: "text-size" },
          { id: "c", text: "font-size" },
          { id: "d", text: "text-weight" },
        ],
        correct: "c",
      },
      {
        id: "q3",
        text: "What does CSS stand for?",
        options: [
          { id: "a", text: "Creative Style Sheets" },
          { id: "b", text: "Cascading Style Sheets" },
          { id: "c", text: "Computer Style Sheets" },
          { id: "d", text: "Colorful Style Sheets" },
        ],
        correct: "b",
      },
      {
        id: "q4",
        text: "Which Flexbox property aligns items along the main axis?",
        options: [
          { id: "a", text: "align-items" },
          { id: "b", text: "align-content" },
          { id: "c", text: "justify-self" },
          { id: "d", text: "justify-content" },
        ],
        correct: "d",
      },
      {
        id: "q5",
        text: "In JavaScript, which operator strictly compares value and type?",
        options: [
          { id: "a", text: "==" },
          { id: "b", text: "===" },
          { id: "c", text: "!=" },
          { id: "d", text: "=" },
        ],
        correct: "b",
      },
    ],
  },
};

type ExamPhase = "intro" | "questions" | "results";

function ExamPage() {
  const { courseId } = Route.useParams();
  const course = getLearnerCourseDetail(courseId);
  const exam = EXAM_DATA[courseId] ?? EXAM_DATA["1"];
  const questions = exam.questions;

  const [phase, setPhase] = useState<ExamPhase>("intro");
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [flagged, setFlagged] = useState<Set<string>>(new Set());

  const question = questions[current];
  const selected = answers[question?.id ?? ""];
  const answered = Object.keys(answers).length;

  function selectAnswer(optionId: string) {
    if (!question) return;
    setAnswers((prev) => ({ ...prev, [question.id]: optionId }));
  }

  function toggleFlag() {
    if (!question) return;
    setFlagged((prev) => {
      const next = new Set(prev);
      if (next.has(question.id)) {
        next.delete(question.id);
      } else {
        next.add(question.id);
      }
      return next;
    });
  }

  function retry() {
    setAnswers({});
    setFlagged(new Set());
    setCurrent(0);
    setPhase("intro");
  }

  const correctAnswers = questions.filter((item) => answers[item.id] === item.correct).length;
  const score = Math.round((correctAnswers / questions.length) * 100);
  const passed = score >= exam.passMark;

  if (phase === "intro") {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <header className="flex h-14 items-center gap-4 border-b border-border/60 bg-surface-elevated/90 px-6 backdrop-blur-md">
          <Link to="/courses/$courseId" params={{ courseId }} className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
            <ArrowLeft className="h-4 w-4" />
            Back to course
          </Link>
        </header>

        <main className="flex min-h-[calc(100vh-3.5rem)] items-center justify-center px-6 py-12">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55 }} className="w-full max-w-lg space-y-6 text-center">
            <div className="mx-auto grid h-20 w-20 place-items-center rounded-3xl bg-gradient-primary text-primary-foreground shadow-glow">
              <Award className="h-10 w-10" strokeWidth={1.5} />
            </div>
            <div>
              <h1 className="text-display text-2xl text-foreground">Final Exam</h1>
              <p className="mt-1 text-sm text-muted-foreground">{course.title}</p>
            </div>

            <div className="grid grid-cols-3 gap-3">
              {[
                { label: "Questions", value: String(questions.length) },
                { label: "Pass mark", value: `${exam.passMark}%` },
                { label: "XP reward", value: `+${exam.xpReward}` },
              ].map(({ label, value }) => (
                <div key={label} className="rounded-2xl border border-border/70 bg-surface-elevated p-4 text-center">
                  <p className="text-display text-xl font-bold tabular-nums text-foreground">{value}</p>
                  <p className="mt-0.5 text-[11px] text-muted-foreground">{label}</p>
                </div>
              ))}
            </div>

            <div className="space-y-2 rounded-2xl border border-gold/25 bg-gold/6 p-4 text-left">
              <div className="mb-1 flex items-center gap-2 text-xs font-semibold text-gold">
                <AlertCircle className="h-3.5 w-3.5" />
                Before you start
              </div>
              {[
                "No time limit in this frontend prototype.",
                "You can flag a question and return later.",
                "All questions must be answered before submission.",
                "A minimum score of 80% is required to unlock the certificate.",
              ].map((tip) => (
                <div key={tip} className="flex items-start gap-2 text-xs text-muted-foreground">
                  <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-gold/60" />
                  {tip}
                </div>
              ))}
            </div>

            <button onClick={() => setPhase("questions")} className="h-12 w-full rounded-2xl bg-gradient-primary text-sm font-semibold text-primary-foreground shadow-soft transition-all hover:opacity-90 active:scale-[0.99]">
              Start exam
            </button>
          </motion.div>
        </main>
      </div>
    );
  }

  if (phase === "results") {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <header className="flex h-14 items-center gap-4 border-b border-border/60 bg-surface-elevated/90 px-6 backdrop-blur-md">
          <Link to="/courses/$courseId" params={{ courseId }} className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
            <ArrowLeft className="h-4 w-4" />
            Back to course
          </Link>
        </header>

        <main className="flex min-h-[calc(100vh-3.5rem)] items-center justify-center px-6 py-12">
          <div className="w-full max-w-lg space-y-6">
            <motion.div
              initial={{ opacity: 0, scale: 0.92 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
              className={`relative overflow-hidden rounded-3xl p-8 text-center grain ${
                passed ? "bg-gradient-to-br from-primary to-primary-glow" : "bg-gradient-to-br from-[oklch(0.45_0.18_25)] to-[oklch(0.38_0.16_20)]"
              }`}
            >
              <div className="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-white/10 blur-3xl" />
              <div className="mx-auto mb-4 grid h-16 w-16 place-items-center rounded-2xl border border-white/20 bg-white/15">
                {passed ? <CheckCircle className="h-8 w-8 text-white" strokeWidth={1.75} /> : <XCircle className="h-8 w-8 text-white" strokeWidth={1.75} />}
              </div>
              <h2 className="text-display text-2xl text-white">{passed ? "Exam passed" : "Exam not passed"}</h2>
              <p className="mt-1 text-sm text-white/70">
                {passed ? "You cleared the final course gate and unlocked the certificate view." : `You need ${exam.passMark}% to pass this final exam.`}
              </p>
              <div className="mt-6 inline-flex items-baseline gap-1">
                <span className="text-display text-5xl font-bold tabular-nums text-white">{score}</span>
                <span className="text-xl text-white/70">%</span>
              </div>
              <p className="mt-1 text-sm text-white/60">
                {correctAnswers} / {questions.length} correct
              </p>
              {passed && (
                <div className="mt-4 flex items-center justify-center gap-1.5 text-sm text-white/80">
                  <Zap className="h-4 w-4" />
                  +{exam.xpReward} XP earned
                </div>
              )}
            </motion.div>

            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <span className="h-1 w-1 rounded-full bg-primary/60" />
                <h3 className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">Review</h3>
                <div className="h-px flex-1 bg-gradient-to-r from-border to-transparent" />
              </div>
              {questions.map((item, index) => {
                const userAnswer = answers[item.id];
                const isCorrect = userAnswer === item.correct;
                const correctOption = item.options.find((option) => option.id === item.correct);
                const selectedOption = item.options.find((option) => option.id === userAnswer);

                return (
                  <motion.div
                    key={item.id}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05 }}
                    className={`rounded-2xl border p-4 ${isCorrect ? "border-teal/25 bg-teal/4" : "border-destructive/20 bg-destructive/4"}`}
                  >
                    <div className="flex items-start gap-3">
                      <div className={`mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full ${isCorrect ? "bg-teal/15 text-teal" : "bg-destructive/15 text-destructive"}`}>
                        {isCorrect ? <CheckCircle className="h-4 w-4" strokeWidth={2} /> : <XCircle className="h-4 w-4" strokeWidth={2} />}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-foreground">
                          {index + 1}. {item.text}
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          Correct: <span className="font-medium text-foreground">{correctOption?.text}</span>
                        </p>
                        {!isCorrect && <p className="mt-0.5 text-xs text-destructive/80">Your answer: {selectedOption?.text ?? "-"}</p>}
                      </div>
                    </div>
                  </motion.div>
                );
              })}
            </div>

            <div className="flex gap-3 pb-6">
              <button onClick={retry} className="flex h-10 flex-1 items-center justify-center gap-2 rounded-2xl border border-border/80 text-sm font-medium text-muted-foreground transition-all hover:bg-accent hover:text-foreground">
                <RotateCcw className="h-4 w-4" />
                Retry
              </button>
              {passed ? (
                <Link to="/certificates" search={{ earned: course.id }} className="flex h-10 flex-1 items-center justify-center gap-2 rounded-2xl bg-primary text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90">
                  <Award className="h-4 w-4" />
                  View certificate
                </Link>
              ) : (
                <Link to="/courses/$courseId" params={{ courseId }} className="flex h-10 flex-1 items-center justify-center gap-2 rounded-2xl bg-primary text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90">
                  Back to course
                  <ArrowRight className="h-4 w-4" />
                </Link>
              )}
            </div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-20 flex h-14 items-center gap-4 border-b border-border/60 bg-surface-elevated/90 px-6 backdrop-blur-md">
        <button onClick={() => setPhase("intro")} className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
          <ArrowLeft className="h-4 w-4" />
          Exit
        </button>

        <div className="mx-auto max-w-xs flex-1 space-y-1">
          <div className="h-1.5 overflow-hidden rounded-full bg-border">
            <motion.div className="h-full rounded-full bg-primary" animate={{ width: `${((current + 1) / questions.length) * 100}%` }} transition={{ duration: 0.3 }} />
          </div>
          <p className="text-center text-[10px] tabular-nums text-muted-foreground">
            {current + 1} / {questions.length} - {answered} answered
          </p>
        </div>

        <button
          onClick={toggleFlag}
          className={`h-8 rounded-xl border px-3 text-xs font-medium transition-all ${
            flagged.has(question.id) ? "border-gold/40 bg-gold/8 text-gold" : "border-border/70 text-muted-foreground hover:text-foreground"
          }`}
        >
          {flagged.has(question.id) ? "Flagged" : "Flag"}
        </button>
      </header>

      <main className="flex min-h-[calc(100vh-3.5rem)] items-center justify-center px-6 py-10">
        <div className="w-full max-w-xl space-y-6">
          <AnimatePresence mode="wait">
            <motion.div key={question.id} initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} transition={{ duration: 0.25 }} className="space-y-6">
              <div className="rounded-2xl border border-border/70 bg-surface-elevated p-6" style={{ boxShadow: "var(--shadow-soft)" }}>
                <p className="mb-3 text-xs font-semibold uppercase tracking-[0.18em] text-primary">Question {current + 1}</p>
                <p className="text-base font-semibold leading-relaxed text-foreground">{question.text}</p>
              </div>

              <div className="space-y-2.5">
                {question.options.map((option) => (
                  <button
                    key={option.id}
                    onClick={() => selectAnswer(option.id)}
                    className={`flex w-full items-center gap-4 rounded-2xl border p-4 text-left transition-all duration-200 ${
                      selected === option.id ? "border-primary/50 bg-primary/8 shadow-soft" : "border-border/70 bg-surface-elevated hover:border-primary/30 hover:bg-primary/4"
                    }`}
                  >
                    <div className={`grid h-6 w-6 shrink-0 place-items-center rounded-full border text-xs font-bold transition-all ${selected === option.id ? "border-primary bg-primary text-primary-foreground" : "border-border/80 text-muted-foreground"}`}>
                      {option.id.toUpperCase()}
                    </div>
                    <span className={`text-sm font-medium ${selected === option.id ? "text-primary" : "text-foreground"}`}>{option.text}</span>
                  </button>
                ))}
              </div>
            </motion.div>
          </AnimatePresence>

          <div className="flex items-center justify-between pt-2">
            <button
              onClick={() => setCurrent((value) => Math.max(0, value - 1))}
              disabled={current === 0}
              className="flex h-10 items-center gap-2 rounded-2xl border border-border/80 px-5 text-sm font-medium text-muted-foreground transition-all hover:border-primary/30 hover:text-foreground disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </button>

            {current < questions.length - 1 ? (
              <button onClick={() => setCurrent((value) => Math.min(questions.length - 1, value + 1))} className="flex h-10 items-center gap-2 rounded-2xl bg-primary px-6 text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90">
                Next
                <ChevronRight className="h-4 w-4" />
              </button>
            ) : (
              <button
                onClick={() => setPhase("results")}
                disabled={answered < questions.length}
                className="flex h-10 items-center gap-2 rounded-2xl bg-primary px-6 text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {answered < questions.length ? `Answer all (${questions.length - answered} left)` : "Submit exam"}
              </button>
            )}
          </div>

          <div className="flex flex-wrap gap-2 pt-2">
            {questions.map((item, index) => (
              <button
                key={item.id}
                onClick={() => setCurrent(index)}
                className={`grid h-8 w-8 place-items-center rounded-xl border text-xs font-semibold transition-all ${
                  index === current
                    ? "border-primary bg-primary text-primary-foreground"
                    : answers[item.id]
                    ? "border-teal/40 bg-teal/10 text-teal"
                    : flagged.has(item.id)
                    ? "border-gold/40 bg-gold/10 text-gold"
                    : "border-border/70 text-muted-foreground hover:border-primary/30"
                }`}
              >
                {index + 1}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-4 pb-4 text-[10px] text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded bg-primary" />
              Current
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded bg-teal/60" />
              Answered
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded bg-gold/60" />
              Flagged
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded border border-border/70" />
              Unanswered
            </span>
          </div>
        </div>
      </main>

      <div className="fixed bottom-6 right-6 flex items-center gap-2 rounded-2xl border border-border/70 bg-surface-elevated px-4 py-2.5 text-xs font-medium text-muted-foreground shadow-elevated">
        <Clock className="h-3.5 w-3.5" />
        No time limit
      </div>
    </div>
  );
}
