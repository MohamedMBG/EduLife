import { useMemo, useState, useRef, useEffect, type ReactNode } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { motion } from "framer-motion";
import {
  ArrowRight,
  BookOpen,
  Bot,
  BrainCircuit,
  CheckCircle2,
  Compass,
  RefreshCw,
  Sparkles,
  Send,
} from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import {
  enrollInCourse,
  listCourses,
  listMyEnrollments,
  requestAdvisorRecommendation,
} from "../lib/api/client";
import { analyzeCareerGoal, type AdvisorResult, type CourseRecommendation } from "../lib/career/advisor";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/advisor")({
  component: AdvisorRoute,
  head: () => ({ meta: [{ title: "Career Advisor - EduLife" }] }),
});

const EXAMPLES = [
  {
    label: "Software path",
    text: "I want to become a software developer and improve my English.",
  },
  {
    label: "Engineering prep",
    text: "I want to prepare for engineering studies after Bac.",
  },
  {
    label: "French writing",
    text: "I want better French writing for school and work.",
  },
];

interface ChatMessage {
  id: string;
  sender: "bot" | "user";
  text: string;
  recommendations?: CourseRecommendation[];
  isLoading?: boolean;
  isError?: boolean;
  retryGoal?: string;
}

function AdvisorRoute() {
  return (
    <RequireAuth>
      <AdvisorPage />
    </RequireAuth>
  );
}

function AdvisorPage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [goal, setGoal] = useState("");

  const coursesQuery = useQuery({
    queryKey: ["courses", "advisor"],
    queryFn: () => listCourses(auth.getAccessToken, { size: 100 }),
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

  // Fast lookup: courseId → CourseSummary for joining backend recommendation IDs with catalog
  const courseMap = useMemo(
    () => new Map((coursesQuery.data?.content ?? []).map((c) => [c.id, c])),
    [coursesQuery.data?.content],
  );

  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    {
      id: "welcome",
      sender: "bot",
      text: "Hello! I'm here to help you match your goals to the best course on EduLife.\n\nDescribe your target career, Baccalaureate needs, or languages you want to master in the chat bar below.",
    },
  ]);

  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  const enrolledCourseIds = new Set((enrollmentsQuery.data ?? []).map((item) => item.courseId));
  const trimmedGoal = goal.trim();
  const goalReady = trimmedGoal.length >= 4;
  const catalogCount = coursesQuery.data?.totalElements ?? coursesQuery.data?.content.length ?? 0;

  async function handleAnalyze(customGoal?: string) {
    const targetGoal = (customGoal ?? trimmedGoal).trim();
    if (targetGoal.length < 4) return;

    // 1. Append user bubble
    setMessages((prev) => [
      ...prev,
      { id: Date.now().toString(), sender: "user" as const, text: targetGoal },
    ]);
    setGoal("");

    // 2. Append typing indicator bubble
    const botLoadingId = (Date.now() + 1).toString();
    setMessages((prev) => [
      ...prev,
      { id: botLoadingId, sender: "bot" as const, text: "", isLoading: true },
    ]);

    if (appEnv.advisorAiEnabled) {
      // 3a. AI path: call backend, join courseIds with catalog, fall back on failure
      try {
        const apiResult = await requestAdvisorRecommendation(auth.getAccessToken, targetGoal);

        // Only keep IDs that exist in the locally-fetched catalog (second-layer guard)
        const recommendations = apiResult.recommendations
          .filter((r) => courseMap.has(r.courseId))
          .map((r) => ({
            course: courseMap.get(r.courseId)!,
            reason: r.reason,
            score: r.score,
          }));

        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === botLoadingId
              ? {
                  id: botLoadingId,
                  sender: "bot" as const,
                  text: apiResult.message,
                  recommendations,
                  isLoading: false,
                }
              : msg,
          ),
        );
      } catch {
        // Fallback: use rule-based advisor when catalog is available
        const catalog = coursesQuery.data?.content ?? [];
        if (catalog.length > 0) {
          const result = analyzeCareerGoal(targetGoal, catalog);
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === botLoadingId
                ? {
                    id: botLoadingId,
                    sender: "bot" as const,
                    text: result.message,
                    recommendations: result.recommendations,
                    isLoading: false,
                  }
                : msg,
            ),
          );
        } else {
          // Catalog empty + API failed: show error state with retry
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === botLoadingId
                ? {
                    id: botLoadingId,
                    sender: "bot" as const,
                    text: "Unable to reach the advisor right now. Please check your connection and try again.",
                    isLoading: false,
                    isError: true,
                    retryGoal: targetGoal,
                  }
                : msg,
            ),
          );
        }
      }
    } else {
      // 3b. Feature flag off: use rule-based with simulated lag (keep existing UX)
      setTimeout(() => {
        const result = analyzeCareerGoal(targetGoal, coursesQuery.data?.content ?? []);
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === botLoadingId
              ? {
                  id: botLoadingId,
                  sender: "bot" as const,
                  text: result.message,
                  recommendations: result.recommendations,
                  isLoading: false,
                }
              : msg,
          ),
        );
      }, 650);
    }
  }

  function handleQuickStart(text: string) {
    handleAnalyze(text);
  }

  const userInitials = auth.session?.displayName
    ? auth.session.displayName.split(" ").slice(0, 2).map(n => n[0].toUpperCase()).join("")
    : "ME";

  return (
    <AppShell
      active="advisor"
      user={{
        displayName: auth.session?.displayName ?? "EduLife learner",
        email: auth.session?.email ?? "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">Career Goal Advisor</p>
          <p className="text-xs text-muted-foreground">
            Get an instant course path designed for your goal.
          </p>
        </div>
      }
    >
      <div className="mx-auto max-w-5xl px-2 py-2 space-y-6">
        <div className="grid gap-6 lg:grid-cols-[1.35fr_0.65fr]">
          {/* Main Chat Workspace */}
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="flex flex-col h-[650px] rounded-3xl border border-border/80 bg-surface-elevated shadow-luxury glass grain overflow-hidden relative"
          >
            {/* Ambient Aurora Glow Mesh Blobs */}
            <div className="absolute top-[-10%] left-[-10%] h-64 w-64 rounded-full bg-primary/10 blur-[100px] pointer-events-none animate-glow" />
            <div className="absolute bottom-[-10%] right-[-10%] h-64 w-64 rounded-full bg-teal/10 blur-[100px] pointer-events-none animate-glow" style={{ animationDelay: "3s" }} />

            {/* Chat Header */}
            <div className="flex items-center gap-3 border-b border-border/60 bg-muted/20 px-6 py-4 z-10">
              <span className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 border border-primary/20 text-primary">
                <Bot className="h-5.5 w-5.5" />
                <span className="absolute bottom-[-1px] right-[-1px] h-3 w-3 rounded-full border-2 border-surface-elevated bg-teal" />
              </span>
              <div>
                <p className="text-sm font-bold text-foreground">EduLife AI Advisor</p>
                <p className="text-[10px] text-teal font-semibold uppercase tracking-wider">Online</p>
              </div>
            </div>

            {/* Chat Message Stream */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-thin z-10">
              {messages.map((msg) => {
                if (msg.sender === "user") {
                  return (
                    <motion.div
                      key={msg.id}
                      initial={{ scale: 0.92, opacity: 0, x: 10 }}
                      animate={{ scale: 1, opacity: 1, x: 0 }}
                      transition={{ type: "spring", stiffness: 220, damping: 20 }}
                      className="flex items-start justify-end gap-3.5 pl-12"
                    >
                      <div className="rounded-2xl rounded-tr-none bg-gradient-to-r from-emerald-600 to-teal-500 p-4 text-sm leading-relaxed text-white font-semibold shadow-sm max-w-[85%]">
                        {msg.text}
                      </div>
                      <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-gradient-to-r from-emerald-600 to-teal-500 text-xs font-bold text-white shadow-sm">
                        {userInitials}
                      </span>
                    </motion.div>
                  );
                } else {
                  return (
                    <div key={msg.id} className="flex flex-col gap-5">
                      <motion.div
                        initial={{ scale: 0.92, opacity: 0, x: -10 }}
                        animate={{ scale: 1, opacity: 1, x: 0 }}
                        transition={{ type: "spring", stiffness: 220, damping: 20 }}
                        className="flex items-start gap-3.5 max-w-[85%]"
                      >
                        <span className="relative grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-primary/10 border border-primary/20 text-primary shadow-sm">
                          <Bot className="h-4.5 w-4.5" />
                          <span className="absolute bottom-[-1px] right-[-1px] h-2.5 w-2.5 rounded-full border-2 border-surface bg-teal" />
                        </span>
                        <div className="rounded-2xl rounded-tl-none bg-primary/5 dark:bg-primary/10 border border-primary/20 shadow-luxury p-5 text-sm leading-relaxed text-foreground font-medium flex-1">
                          <div className="flex items-center gap-1.5 mb-2.5">
                            <Sparkles className="h-3.5 w-3.5 text-primary animate-pulse" />
                            <span className="text-[10px] font-extrabold uppercase tracking-wider text-primary">AI Advisor</span>
                          </div>
                          {msg.isLoading ? (
                            <div className="flex items-center gap-1.5 py-1">
                              <span className="text-xs text-muted-foreground font-semibold">Analyzing catalog</span>
                              <span className="h-1.5 w-1.5 rounded-full bg-primary animate-bounce" style={{ animationDelay: "0ms" }} />
                              <span className="h-1.5 w-1.5 rounded-full bg-primary animate-bounce" style={{ animationDelay: "150ms" }} />
                              <span className="h-1.5 w-1.5 rounded-full bg-primary animate-bounce" style={{ animationDelay: "300ms" }} />
                            </div>
                          ) : (
                            <>
                              <p className="whitespace-pre-line">{msg.text}</p>
                              {/* Retry button — only when error state */}
                              {msg.isError && msg.retryGoal && (
                                <motion.button
                                  type="button"
                                  whileTap={{ scale: 0.96 }}
                                  onClick={() => handleAnalyze(msg.retryGoal)}
                                  className="mt-3 inline-flex items-center gap-1.5 rounded-xl border border-primary/25 bg-primary/5 hover:bg-primary/10 px-3 py-1.5 text-xs font-bold text-primary transition-all cursor-pointer"
                                >
                                  <RefreshCw className="h-3.5 w-3.5" />
                                  Retry
                                </motion.button>
                              )}
                            </>
                          )}
                        </div>
                      </motion.div>

                      {/* Inline Course Recommendations */}
                      {!msg.isLoading && !msg.isError && Array.isArray(msg.recommendations) && msg.recommendations.length > 0 && (
                        <div className="pl-11 pr-4 space-y-4">
                          <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground flex items-center gap-1.5">
                            <Sparkles className="h-3.5 w-3.5 text-primary animate-pulse" />
                            Matched Course Options
                          </p>
                          <div className="grid gap-4">
                            {msg.recommendations.map((recommendation, index) => {
                              const enrolled = enrolledCourseIds.has(recommendation.course.id);
                              return (
                                <RecommendationCard
                                  key={recommendation.course.id}
                                  recommendation={recommendation}
                                  rank={index}
                                  enrolled={enrolled}
                                  enrolling={enrollMutation.isPending}
                                  onEnroll={() => enrollMutation.mutate(recommendation.course.id)}
                                />
                              );
                            })}
                          </div>
                        </div>
                      )}

                      {/* Empty state — API succeeded but no matching courses */}
                      {!msg.isLoading && !msg.isError && Array.isArray(msg.recommendations) && msg.recommendations.length === 0 && (
                        <div className="pl-11 pr-4">
                          <p className="text-xs text-muted-foreground font-medium">
                            No match — try a clearer goal.
                          </p>
                        </div>
                      )}
                    </div>
                  );
                }
              })}
              {/* Anchor element to automatically scroll to the bottom of the container */}
              <div ref={scrollRef} />
            </div>

            {/* Bottom Input Area */}
            <div className="border-t border-border/60 bg-muted/10 p-4 space-y-4 z-10">
              {/* Quick starts suggest pills */}
              <div className="flex flex-wrap gap-2 items-center">
                <span className="text-[9px] uppercase tracking-wider font-extrabold text-muted-foreground mr-1">
                  Try:
                </span>
                {EXAMPLES.map((example) => (
                  <motion.button
                    key={example.label}
                    type="button"
                    whileHover={{ scale: 1.05, y: -2 }}
                    whileTap={{ scale: 0.97 }}
                    transition={{ type: "spring", stiffness: 350, damping: 15 }}
                    onClick={() => handleQuickStart(example.text)}
                    className="rounded-full border border-border bg-background hover:border-primary/45 hover:bg-primary/5 px-3 py-1 text-xs font-bold text-muted-foreground hover:text-primary transition-all cursor-pointer shadow-sm"
                  >
                    {example.label}
                  </motion.button>
                ))}
              </div>

              {/* Message bar input */}
              <div className="relative flex items-center">
                <input
                  type="text"
                  value={goal}
                  onChange={(event) => setGoal(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      handleAnalyze();
                    }
                  }}
                  placeholder="Ask advisor: Describe what you want to achieve..."
                  className="w-full rounded-2xl border border-border/85 bg-background px-4 py-3.5 pr-12 text-sm leading-relaxed text-foreground outline-none transition-all placeholder:text-muted-foreground/50 focus:border-primary focus:ring-2 focus:ring-primary/10 shadow-sm"
                />
                <motion.button
                  type="button"
                  whileHover={{ scale: 1.08, rotate: -3 }}
                  whileTap={{ scale: 0.92, rotate: 3 }}
                  transition={{ type: "spring", stiffness: 400, damping: 15 }}
                  onClick={() => handleAnalyze()}
                  disabled={!goalReady || coursesQuery.isLoading}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 grid h-9 w-9 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-sm hover:shadow-[0_0_12px_oklch(var(--primary)/0.35)] cursor-pointer disabled:opacity-30 disabled:pointer-events-none"
                  aria-label="Send message"
                >
                  <Send className="h-4.5 w-4.5" />
                </motion.button>
              </div>
            </div>
          </motion.div>

          {/* Right Info Panel */}
          <div className="space-y-4">
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5, delay: 0.2 }}
              className="rounded-3xl border border-border/80 bg-surface-elevated p-5 shadow-soft glass grain"
            >
              <p className="text-xs uppercase tracking-[0.2em] font-bold text-muted-foreground mb-4">
                Advisor Stats
              </p>
              <div className="grid gap-3.5">
                <InfoMetric label="Catalog Checked" value={coursesQuery.isLoading ? "..." : catalogCount} />
                <InfoMetric label="Enrolled courses" value={`${enrolledCourseIds.size} active`} />
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5, delay: 0.3 }}
              className="rounded-3xl border border-border/80 bg-surface-elevated p-5 shadow-soft glass"
            >
              <p className="text-xs uppercase tracking-[0.2em] font-bold text-muted-foreground mb-4">
                Chat Tips
              </p>
              <div className="space-y-4">
                <TipRow label="Specify your Career" detail="Mention developer, designer, business manager." />
                <TipRow label="Mention your Level" detail="Indicate beginner, Bac, or expert status." />
                <TipRow label="Choose language" detail="State preferences for English, French, or Darija." />
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function InfoMetric({ label, value }: { label: string; value: number | string }) {
  return (
    <motion.div
      whileHover={{ y: -2, scale: 1.02 }}
      className="rounded-2xl border border-border/80 bg-background/95 dark:bg-background/40 px-4 py-3.5 shadow-sm flex flex-col justify-center"
    >
      <p className="text-[10px] uppercase tracking-wider font-semibold text-muted-foreground">{label}</p>
      <p className="mt-1 text-display text-2xl font-black text-gradient-primary leading-none py-1">
        {typeof value === "number" ? value.toLocaleString() : value}
      </p>
    </motion.div>
  );
}

function TipRow({ label, detail }: { label: string; detail: string }) {
  return (
    <div className="flex gap-3">
      <span className="mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full bg-primary/10 text-primary">
        <CheckCircle2 className="h-3.5 w-3.5" />
      </span>
      <div>
        <p className="text-sm font-medium text-foreground">{label}</p>
        <p className="text-xs text-muted-foreground">{detail}</p>
      </div>
    </div>
  );
}

function RecommendationCard({
  recommendation,
  rank,
  enrolled,
  enrolling,
  onEnroll,
}: {
  recommendation: CourseRecommendation;
  rank: number;
  enrolled: boolean;
  enrolling: boolean;
  onEnroll: () => void;
}) {
  const course = recommendation.course;
  const isBest = rank === 0;

  return (
    <motion.article
      initial={{ opacity: 0, y: 20, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ type: "spring", stiffness: 200, damping: 20, delay: 0.1 * (rank + 1) }}
      whileHover={{ y: -4, scale: 1.01, boxShadow: "var(--shadow-elevated)" }}
      className={`overflow-hidden rounded-3xl border transition-all duration-300 shadow-soft glass grain ${
        isBest
          ? "border-primary/35 bg-gradient-to-br from-primary/8 via-card to-teal/5 shadow-[0_4px_20px_oklch(var(--primary)/0.08)]"
          : "border-border bg-gradient-to-br from-card to-background"
      }`}
    >
      <div className="grid gap-0 md:grid-cols-[240px_1fr]">
        <div className="relative min-h-44 overflow-hidden bg-muted/30">
          {course.imageUrl ? (
            <motion.img
              whileHover={{ scale: 1.05 }}
              transition={{ duration: 0.4 }}
              src={course.imageUrl}
              alt={course.title}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="grid h-full min-h-44 place-items-center bg-gradient-to-br from-primary/10 to-primary-glow/10 text-primary">
              <BookOpen className="h-10 w-10 opacity-70" />
            </div>
          )}
          <span
            className={`absolute left-4 top-4 rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-wider shadow-sm border ${
              isBest
                ? "bg-gradient-gold text-gold-foreground border-gold/30 shadow-[0_0_8px_rgba(245,158,11,0.25)]"
                : "bg-background/90 text-foreground border-border"
            }`}
          >
            {isBest ? "Best match" : "Second option"}
          </span>
        </div>

        <div className="p-6 flex flex-col justify-between">
          <div>
            <div className="flex flex-wrap gap-2 text-[10px] uppercase tracking-wider font-bold">
              <span className="rounded-full bg-primary/10 border border-primary/20 px-3 py-1 text-primary">
                {course.level.replace("_", " ")}
              </span>
              <span className="rounded-full bg-muted/80 dark:bg-muted/10 border border-border px-3 py-1 text-muted-foreground">
                {formatLanguage(course.languageCode)}
              </span>
            </div>

            <h2 className="mt-4 text-2xl font-bold text-foreground leading-tight">{course.title}</h2>

            {/* Shaded advisor reasoning card block */}
            <div className="mt-4 bg-primary/5 dark:bg-primary/10 border border-primary/10 rounded-2xl p-4 flex gap-3 items-start shadow-sm">
              <Sparkles className="h-4 w-4 text-primary shrink-0 mt-0.5 animate-pulse" />
              <div>
                <p className="text-[10px] font-extrabold uppercase tracking-wider text-primary">Advisor Match Reasoning</p>
                <p className="mt-1 text-sm leading-relaxed text-foreground/80 font-medium">
                  {recommendation.reason}
                </p>
              </div>
            </div>

            {/* Next action guide card row */}
            <div className="mt-4 border-t border-border/60 pt-4 flex gap-3 items-start">
              <Compass className="h-4 w-4 text-muted-foreground shrink-0 mt-0.5" />
              <div>
                <p className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                  Advisor Next Step
                </p>
                <p className="mt-1 text-xs text-muted-foreground font-medium leading-relaxed">
                  Open the course outline, review the lessons structure, then enroll if it aligns with your timeline.
                </p>
              </div>
            </div>
          </div>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <Link
              to="/courses/$courseId"
              params={{ courseId: course.id }}
              className="inline-flex items-center gap-2 rounded-xl border border-primary/20 bg-primary/5 hover:bg-primary/10 hover:shadow-[0_0_12px_oklch(var(--primary)/0.15)] px-4 py-2.5 text-xs font-bold text-primary transition-all duration-200"
            >
              Open outline
              <ArrowRight className="h-4 w-4" />
            </Link>
            {enrolled ? (
              <Link
                to="/courses"
                className="inline-flex items-center gap-2 rounded-xl border border-teal-500/20 bg-teal-500/10 px-4 py-2.5 text-xs font-bold text-teal-650 dark:text-teal-450 shadow-sm"
              >
                Already enrolled
                <CheckCircle2 className="h-4 w-4" />
              </Link>
            ) : (
              <motion.button
                type="button"
                whileTap={{ scale: 0.96 }}
                onClick={onEnroll}
                disabled={enrolling}
                className="inline-flex items-center gap-2 rounded-xl bg-gradient-primary text-primary-foreground hover:shadow-glow hover:scale-[1.02] px-5 py-2.5 text-xs font-bold transition-all cursor-pointer disabled:opacity-60 disabled:pointer-events-none"
              >
                {enrolling ? "Enrolling..." : "Enroll in Course"}
              </motion.button>
            )}
          </div>
        </div>
      </div>
    </motion.article>
  );
}

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
