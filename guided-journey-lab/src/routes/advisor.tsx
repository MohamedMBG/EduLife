import { useEffect, useMemo, useRef, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AnimatePresence, motion } from "framer-motion";
import { toast } from "sonner";
import {
  ArrowRight,
  BookOpen,
  Check,
  CheckCircle2,
  ChevronRight,
  Compass,
  GraduationCap,
  Loader2,
  Medal,
  RefreshCw,
  Sparkles,
  Target,
  User,
  WandSparkles,
  X,
  Zap,
} from "lucide-react";

import { AppLayout } from "../components/app/AppLayout";
import {
  enrollInCourse,
  listCourses,
  listMyEnrollments,
  requestAdvisorRecommendation,
} from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";
import { analyzeCareerGoal, type CourseRecommendation } from "../lib/career/advisor";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/advisor")({
  component: AdvisorRoute,
  head: () => ({ meta: [{ title: "AI Career Advisor - EduLife" }] }),
});

const EXAMPLE_GOAL =
  "I want to transition from marketing to data analysis, looking for a beginner-friendly foundation that fits my busy schedule.";

const CONTEXT_CHIPS = [
  { label: "Bac Student", text: "I am a Bac student" },
  { label: "Career Switcher", text: "I am switching careers", active: true },
  { label: "Software", text: "and want to learn software development" },
  { label: "English", text: "with English support" },
  { label: "Darija", text: "explained in Moroccan Darija" },
  { label: "French", text: "taught in French" },
  { label: "Business", text: "for a practical business goal" },
];

interface Brief {
  id: string;
  goal: string;
  message: string;
  recommendations: CourseRecommendation[];
  isLoading?: boolean;
  isError?: boolean;
}

function AdvisorRoute() {
  return (
    <RequireAuth>
      <AdvisorPage />
    </RequireAuth>
  );
}

/* ─── Midnight Minimalist tokens ─── */
const MM = {
  bg: "#f6fafe",
  primary: "#091426",
  primaryContainer: "#1e293b",
  onPrimary: "#ffffff",
  secondary: "#505f76",
  surface: "#f0f4f8",
  surfaceContainer: "#eaeef2",
  surfaceHigh: "#e4e9ed",
  outlineVariant: "#c5c6cd",
  outline: "#75777d",
  onSurface: "#171c1f",
  onSurfaceVariant: "#45474c",
  error: "#ba1a1a",
  errorContainer: "#ffdad6",
} as const;

const EASE_OUT = [0.16, 1, 0.3, 1] as const;

function AdvisorPage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [goal, setGoal] = useState("");
  const [historyOpen, setHistoryOpen] = useState(false);
  const resultRef = useRef<HTMLDivElement>(null);

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
      toast.success("Course enrollment updated.");
    },
    onError: () => {
      toast.error("Enrollment failed. Please try again.");
    },
  });

  const courseMap = useMemo(
    () => new Map((coursesQuery.data?.content ?? []).map((course) => [course.id, course])),
    [coursesQuery.data?.content],
  );

  const [briefs, setBriefs] = useState<Brief[]>([]);
  const [activeBriefId, setActiveBriefId] = useState<string | null>(null);

  useEffect(() => {
    try {
      const stored = localStorage.getItem("edulife_advisor_briefs");
      if (!stored) return;
      const parsed = JSON.parse(stored) as Brief[];
      if (parsed.length > 0) {
        setBriefs(parsed);
        setActiveBriefId(parsed[0].id);
      }
    } catch {
      localStorage.removeItem("edulife_advisor_briefs");
    }
  }, []);

  const currentBrief = useMemo(() => {
    if (activeBriefId) {
      return briefs.find((brief) => brief.id === activeBriefId) ?? briefs[0];
    }
    return briefs[0];
  }, [activeBriefId, briefs]);

  const enrolledCourseIds = useMemo(
    () => new Set((enrollmentsQuery.data ?? []).map((item) => item.courseId)),
    [enrollmentsQuery.data],
  );

  const trimmedGoal = goal.trim();
  const goalReady = trimmedGoal.length >= 4;
  const catalogCount = coursesQuery.data?.totalElements ?? coursesQuery.data?.content.length ?? 0;

  const currentBriefId = currentBrief?.id;
  const currentBriefLoading = currentBrief?.isLoading;

  useEffect(() => {
    if (currentBriefId && resultRef.current) {
      resultRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, [currentBriefId, currentBriefLoading]);

  function saveBriefsToStorage(updatedList: Brief[]) {
    try {
      const completedBriefs = updatedList.filter((brief) => !brief.isLoading && !brief.isError);
      localStorage.setItem("edulife_advisor_briefs", JSON.stringify(completedBriefs));
    } catch {
      toast.error("Could not save advisor history on this device.");
    }
  }

  function handleContextChip(text: string) {
    setGoal((current) => {
      const clean = current.trim();
      if (!clean) return `${text}.`;
      if (clean.toLowerCase().includes(text.toLowerCase())) return current;
      return `${clean.replace(/[.]$/, "")} ${text}.`;
    });
  }

  function handleDeleteBrief(id: string, event: React.MouseEvent) {
    event.stopPropagation();
    setBriefs((previous) => {
      const updated = previous.filter((brief) => brief.id !== id);
      localStorage.setItem("edulife_advisor_briefs", JSON.stringify(updated));
      return updated;
    });
    if (activeBriefId === id) {
      setActiveBriefId(null);
    }
  }

  function handleClearHistory() {
    setBriefs([]);
    setActiveBriefId(null);
    localStorage.removeItem("edulife_advisor_briefs");
  }

  async function handleAnalyze(customGoal?: string) {
    const targetGoal = (customGoal ?? trimmedGoal).trim();
    if (targetGoal.length < 4 || coursesQuery.isLoading) return;

    const briefId = crypto.randomUUID();
    const loadingBrief: Brief = {
      id: briefId,
      goal: targetGoal,
      message: "",
      recommendations: [],
      isLoading: true,
    };

    setBriefs((previous) => [loadingBrief, ...previous]);
    setActiveBriefId(briefId);
    setGoal("");

    if (appEnv.advisorAiEnabled) {
      try {
        const apiResult = await requestAdvisorRecommendation(auth.getAccessToken, targetGoal);
        const recommendations = apiResult.recommendations
          .filter((recommendation) => courseMap.has(recommendation.courseId))
          .map((recommendation) => ({
            course: courseMap.get(recommendation.courseId)!,
            reason: recommendation.reason,
            score: recommendation.score,
          }));

        setBriefs((previous) => {
          const updated = previous.map((brief) =>
            brief.id === briefId
              ? { ...brief, message: apiResult.message, recommendations, isLoading: false }
              : brief,
          );
          saveBriefsToStorage(updated);
          return updated;
        });
        return;
      } catch {
        // Fallback to deterministic catalog ranking
      }
    }

    const catalog = coursesQuery.data?.content ?? [];
    if (catalog.length > 0) {
      const result = analyzeCareerGoal(targetGoal, catalog);
      setBriefs((previous) => {
        const updated = previous.map((brief) =>
          brief.id === briefId
            ? {
                ...brief,
                message: result.message,
                recommendations: result.recommendations,
                isLoading: false,
              }
            : brief,
        );
        saveBriefsToStorage(updated);
        return updated;
      });
      return;
    }

    setBriefs((previous) =>
      previous.map((brief) =>
        brief.id === briefId
          ? {
              ...brief,
              message:
                "Unable to reach the advisor right now. Check your connection and try again.",
              isLoading: false,
              isError: true,
            }
          : brief,
      ),
    );
  }

  return (
    <AppLayout>
      <div
        className="min-h-screen font-sans antialiased"
        style={{ background: MM.bg, color: MM.onSurface }}
      >
        <AdvisorHero
          goal={goal}
          setGoal={setGoal}
          goalReady={goalReady}
          isCatalogLoading={coursesQuery.isLoading}
          catalogCount={catalogCount}
          briefs={briefs}
          historyOpen={historyOpen}
          setHistoryOpen={setHistoryOpen}
          activeBriefId={activeBriefId}
          setActiveBriefId={setActiveBriefId}
          onAnalyze={handleAnalyze}
          onUseExample={() => setGoal(EXAMPLE_GOAL)}
          onContextChip={handleContextChip}
          onDeleteBrief={handleDeleteBrief}
          onClearHistory={handleClearHistory}
        />

        <div ref={resultRef}>
          <AnimatePresence mode="wait">
            {currentBrief ? (
              <AdvisorResult
                key={currentBrief.id}
                brief={currentBrief}
                enrolledCourseIds={enrolledCourseIds}
                enrolling={enrollMutation.isPending}
                onEnroll={(courseId) => enrollMutation.mutate(courseId)}
                onRetry={() => handleAnalyze(currentBrief.goal)}
              />
            ) : (
              <AdvisorEmpty catalogCount={catalogCount} />
            )}
          </AnimatePresence>
        </div>

        <AdvisorFooter />
      </div>
    </AppLayout>
  );
}

/* ═══════════════════════════════════════════
   Section 01 — Hero / User Input
   ═══════════════════════════════════════════ */

interface HeroProps {
  goal: string;
  setGoal: (goal: string) => void;
  goalReady: boolean;
  isCatalogLoading: boolean;
  catalogCount: number;
  briefs: Brief[];
  historyOpen: boolean;
  setHistoryOpen: (open: boolean) => void;
  activeBriefId: string | null;
  setActiveBriefId: (id: string | null) => void;
  onAnalyze: (goal?: string) => void;
  onUseExample: () => void;
  onContextChip: (text: string) => void;
  onDeleteBrief: (id: string, event: React.MouseEvent) => void;
  onClearHistory: () => void;
}

function AdvisorHero({
  goal,
  setGoal,
  goalReady,
  isCatalogLoading,
  catalogCount,
  briefs,
  historyOpen,
  setHistoryOpen,
  activeBriefId,
  setActiveBriefId,
  onAnalyze,
  onUseExample,
  onContextChip,
  onDeleteBrief,
  onClearHistory,
}: HeroProps) {
  return (
    <section className="relative pb-16 pt-8 lg:pb-24 lg:pt-12">
      <div className="mx-auto grid max-w-[1280px] gap-16 px-5 sm:px-8 lg:grid-cols-12 lg:gap-16 lg:px-16">
        {/* Left — editorial intro */}
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, ease: EASE_OUT }}
          className="lg:col-span-7"
        >
          <div className="flex items-center gap-2 mb-6">
            <User className="h-4 w-4" style={{ color: MM.primary }} strokeWidth={1.5} />
            <span
              className="text-[10px] font-bold uppercase tracking-[0.2em]"
              style={{ color: MM.primary }}
            >
              User Input
            </span>
          </div>

          <div className="relative">
            <span
              className="pointer-events-none absolute -left-10 -top-5 select-none text-[80px] font-extralight leading-none"
              style={{ color: `${MM.primary}0D` }}
            >
              01
            </span>
            <div className="relative z-10">
              <span
                className="inline-block rounded-full px-4 py-1.5 text-[10px] font-bold uppercase tracking-[0.2em]"
                style={{ background: `${MM.primary}0D`, color: MM.primary }}
              >
                AI Personalized Discovery
              </span>

              <h1
                className="mt-8 text-[clamp(2.5rem,5.5vw,3.75rem)] font-black leading-[1.1] tracking-tight"
                style={{ color: MM.primary }}
              >
                Precision learning
                <br />
                for your{" "}
                <span
                  className="bg-clip-text text-transparent"
                  style={{
                    backgroundImage: `linear-gradient(135deg, ${MM.primary}, ${MM.secondary})`,
                  }}
                >
                  next chapter.
                </span>
              </h1>

              <p
                className="mt-7 max-w-xl text-lg leading-relaxed"
                style={{ color: MM.onSurfaceVariant }}
              >
                Skip the endless browsing. Share your career aspirations and our live intelligence
                engine will surface the exact match from{" "}
                {catalogCount > 0 ? `${catalogCount}+` : ""} active courses.
              </p>

              <div className="mt-9 flex flex-col gap-4">
                {["Real-time catalog synchronization", "Context-aware reasoning & fit scoring"].map(
                  (text) => (
                    <div key={text} className="flex items-center gap-4">
                      <div
                        className="flex h-6 w-6 items-center justify-center rounded-full"
                        style={{ background: `${MM.primary}14` }}
                      >
                        <Check
                          className="h-3 w-3"
                          style={{ color: MM.primary }}
                          strokeWidth={2.5}
                        />
                      </div>
                      <span className="text-base" style={{ color: `${MM.onSurface}CC` }}>
                        {text}
                      </span>
                    </div>
                  ),
                )}
              </div>
            </div>
          </div>
        </motion.div>

        {/* Right — prompt card */}
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.1, ease: EASE_OUT }}
          className="lg:col-span-5"
        >
          <div
            className="rounded-[2rem] p-8 sm:p-10"
            style={{
              background: MM.surface,
              border: `1px solid ${MM.outlineVariant}4D`,
              boxShadow: `0 4px 6px -1px rgba(0,0,0,0.05), 0 20px 40px -10px ${MM.primary}0D`,
            }}
          >
            <div className="mb-8 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span
                  className="rounded-full px-3 py-1 text-[9px] font-bold uppercase tracking-widest"
                  style={{ background: `${MM.secondary}1A`, color: MM.secondary }}
                >
                  User Input
                </span>
              </div>
              <h2 className="text-lg font-bold tracking-tight" style={{ color: MM.primary }}>
                What&apos;s your next career move?
              </h2>
              <WandSparkles className="h-5 w-5" style={{ color: MM.outline }} strokeWidth={1.4} />
            </div>

            <div className="mb-8">
              <label
                htmlFor="advisor-goal"
                className="mb-4 block text-[11px] font-bold uppercase tracking-widest"
                style={{ color: MM.secondary }}
              >
                Describe your dream role or transition
              </label>
              <textarea
                id="advisor-goal"
                value={goal}
                onChange={(e) => setGoal(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    onAnalyze();
                  }
                }}
                className="h-48 w-full resize-none rounded-2xl p-6 text-base transition-all"
                style={{
                  background: `${MM.surface}80`,
                  border: `1px solid ${MM.outlineVariant}80`,
                  color: MM.onSurface,
                  outline: "none",
                }}
                placeholder="e.g., Transition from marketing to data analysis, focused on Python..."
              />
            </div>

            <div className="mb-10">
              <p
                className="mb-4 text-[10px] uppercase tracking-[0.15em]"
                style={{ color: MM.outline }}
              >
                Focus Areas
              </p>
              <div className="flex flex-wrap gap-2">
                {CONTEXT_CHIPS.map((chip) => (
                  <button
                    key={chip.label}
                    type="button"
                    onClick={() => onContextChip(chip.text)}
                    className="rounded-full px-4 py-2 text-[11px] font-medium transition-all"
                    style={
                      goal.toLowerCase().includes(chip.text.toLowerCase())
                        ? {
                            background: MM.primary,
                            color: MM.onPrimary,
                            boxShadow: `0 4px 12px -4px ${MM.primary}40`,
                          }
                        : {
                            border: `1px solid ${MM.outlineVariant}66`,
                            color: MM.secondary,
                          }
                    }
                  >
                    {chip.label}
                  </button>
                ))}
              </div>
            </div>

            <button
              type="button"
              onClick={() => onAnalyze()}
              disabled={!goalReady || isCatalogLoading}
              className="flex w-full items-center justify-center gap-3 rounded-2xl py-5 text-[11px] font-semibold uppercase tracking-[0.2em] transition-all duration-300 disabled:opacity-40 disabled:cursor-not-allowed"
              style={{
                background: MM.primary,
                color: MM.onPrimary,
                boxShadow: `0 10px 30px -5px ${MM.primary}33`,
              }}
            >
              {isCatalogLoading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Checking catalog...
                </>
              ) : (
                <>
                  Generate My Pathway
                  <Zap className="h-4 w-4" strokeWidth={1.75} />
                </>
              )}
            </button>

            <div className="mt-6 flex items-center justify-between">
              <p className="text-center text-[10px] tracking-wider" style={{ color: MM.outline }}>
                SECURE AI PROCESSING &bull; {catalogCount} COURSES INDEXED
              </p>
              {briefs.length > 0 && (
                <button
                  type="button"
                  onClick={() => setHistoryOpen(!historyOpen)}
                  className="text-[10px] font-semibold tracking-wider"
                  style={{ color: MM.primary }}
                >
                  {briefs.length} BRIEF{briefs.length === 1 ? "" : "S"}
                </button>
              )}
            </div>

            {/* Example goal helper */}
            <button
              type="button"
              onClick={onUseExample}
              className="mt-4 w-full rounded-xl py-3 text-xs font-medium transition-all"
              style={{
                border: `1px solid ${MM.outlineVariant}`,
                color: MM.secondary,
              }}
            >
              Use example goal
            </button>
          </div>

          <BriefHistory
            briefs={briefs}
            open={historyOpen}
            activeBriefId={activeBriefId}
            onSelect={setActiveBriefId}
            onDelete={onDeleteBrief}
            onClear={onClearHistory}
          />
        </motion.div>
      </div>
    </section>
  );
}

function BriefHistory({
  briefs,
  open,
  activeBriefId,
  onSelect,
  onDelete,
  onClear,
}: {
  briefs: Brief[];
  open: boolean;
  activeBriefId: string | null;
  onSelect: (id: string | null) => void;
  onDelete: (id: string, event: React.MouseEvent) => void;
  onClear: () => void;
}) {
  if (!open || briefs.length === 0) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      className="mt-4 rounded-2xl p-3"
      style={{
        border: `1px solid ${MM.outlineVariant}`,
        background: "#ffffff",
        boxShadow: `0 20px 60px -45px ${MM.primary}33`,
      }}
    >
      <div className="mb-2 flex items-center justify-between px-2">
        <p className="text-[10px] font-semibold tracking-[0.2em]" style={{ color: MM.outline }}>
          RECENT BRIEFS
        </p>
        <button
          type="button"
          onClick={onClear}
          className="text-xs font-medium"
          style={{ color: MM.error }}
        >
          Clear
        </button>
      </div>
      <div className="max-h-52 space-y-1 overflow-y-auto">
        {briefs.slice(0, 6).map((brief) => (
          <button
            key={brief.id}
            type="button"
            onClick={() => onSelect(brief.id)}
            className="group flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-xs transition-colors duration-300"
            style={{
              background: activeBriefId === brief.id ? MM.surfaceHigh : "transparent",
              color: activeBriefId === brief.id ? MM.onSurface : MM.outline,
            }}
          >
            <span className="min-w-0 flex-1 truncate">{brief.goal}</span>
            <span
              role="button"
              tabIndex={0}
              onClick={(event) => onDelete(brief.id, event)}
              onKeyDown={(event) => {
                if (event.key === "Enter") onDelete(brief.id, event as unknown as React.MouseEvent);
              }}
              className="rounded-full p-1 opacity-40 transition-opacity group-hover:opacity-100"
            >
              <X className="h-3 w-3" />
            </span>
          </button>
        ))}
      </div>
    </motion.div>
  );
}

/* ═══════════════════════════════════════════
   Result Router
   ═══════════════════════════════════════════ */

function AdvisorResult({
  brief,
  enrolledCourseIds,
  enrolling,
  onEnroll,
  onRetry,
}: {
  brief: Brief;
  enrolledCourseIds: Set<string>;
  enrolling: boolean;
  onEnroll: (courseId: string) => void;
  onRetry: () => void;
}) {
  if (brief.isLoading) return <AdvisorLoading goal={brief.goal} />;
  if (brief.isError) return <AdvisorError brief={brief} onRetry={onRetry} />;
  if (brief.recommendations.length === 0) return <AdvisorNoMatch brief={brief} />;

  const best = brief.recommendations[0];
  const alternatives = brief.recommendations.slice(1);

  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -16 }}
      transition={{ duration: 0.7, ease: EASE_OUT }}
    >
      {/* Section 02 — Analysis */}
      <AnalysisSection brief={brief} recommendation={best} />

      {/* Section 03 — Selected Path */}
      <SelectedPathSection
        recommendation={best}
        enrolled={enrolledCourseIds.has(best.course.id)}
        enrolling={enrolling}
        onEnroll={() => onEnroll(best.course.id)}
      />

      {/* Alternatives */}
      {alternatives.length > 0 && (
        <section className="mx-auto max-w-[1280px] px-5 pb-16 sm:px-8 lg:px-16">
          <div className="flex items-center gap-6 mb-8">
            <span
              className="pointer-events-none select-none text-[60px] font-extralight leading-none"
              style={{ color: `${MM.primary}0D` }}
            >
              04
            </span>
            <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
            <span
              className="text-[10px] font-bold uppercase tracking-[0.3em]"
              style={{ color: MM.secondary }}
            >
              Other Paths
            </span>
          </div>
          <div className="space-y-4">
            {alternatives.map((recommendation) => (
              <AlternativePathCard
                key={recommendation.course.id}
                recommendation={recommendation}
                enrolled={enrolledCourseIds.has(recommendation.course.id)}
                enrolling={enrolling}
                onEnroll={() => onEnroll(recommendation.course.id)}
              />
            ))}
          </div>
        </section>
      )}
    </motion.div>
  );
}

/* ═══════════════════════════════════════════
   Section 02 — AI Analysis
   ═══════════════════════════════════════════ */

function AnalysisSection({
  brief,
  recommendation,
}: {
  brief: Brief;
  recommendation: CourseRecommendation;
}) {
  const fitScore = getFitScore(recommendation.score);
  const reasoningBullets = extractReasoningBullets(brief.message || recommendation.reason);

  return (
    <section className="mx-auto max-w-[1280px] px-5 pb-16 sm:px-8 lg:px-16 lg:pb-24">
      {/* Section header */}
      <div className="relative mb-12 flex items-center gap-6">
        <span
          className="pointer-events-none select-none text-[60px] font-extralight leading-none lg:text-[80px]"
          style={{ color: `${MM.primary}0D` }}
        >
          02
        </span>
        <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
        <span
          className="flex items-center gap-3 rounded-full px-6 py-1 text-[10px] font-bold uppercase tracking-[0.3em]"
          style={{ background: "#ffffff", color: MM.secondary }}
        >
          <span className="h-1.5 w-1.5 rounded-full" style={{ background: MM.primary }} />
          Synthesizing Insights
        </span>
        <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
      </div>

      {/* Goal quote */}
      <div className="mx-auto mb-16 max-w-4xl text-center">
        <p
          className="text-xl font-light italic leading-relaxed sm:text-2xl"
          style={{ color: `${MM.primary}B3` }}
        >
          &ldquo;{brief.goal}&rdquo;
        </p>
      </div>

      {/* Analysis grid */}
      <div className="mx-auto grid max-w-4xl gap-8 md:grid-cols-12">
        {/* Strategic reasoning card */}
        <div
          className="rounded-[2rem] p-8 sm:p-10 md:col-span-8"
          style={{
            background: MM.surface,
            border: `1px solid ${MM.outlineVariant}4D`,
          }}
        >
          <div className="mb-8 flex items-center gap-4">
            <div
              className="flex h-12 w-12 items-center justify-center rounded-full"
              style={{ background: MM.primary, color: MM.onPrimary }}
            >
              <Sparkles className="h-5 w-5" strokeWidth={1.5} />
            </div>
            <div>
              <span
                className="mb-2 inline-block rounded-full px-3 py-1 text-[9px] font-bold uppercase tracking-widest"
                style={{ background: MM.primary, color: MM.onPrimary }}
              >
                AI Analysis
              </span>
              <h3 className="text-lg font-bold" style={{ color: MM.primary }}>
                Strategic Reasoning
              </h3>
              <p className="text-[11px] uppercase tracking-wider" style={{ color: MM.outline }}>
                AI Advisor Logic
              </p>
            </div>
          </div>

          <ul className="mb-8 space-y-4">
            {reasoningBullets.map((bullet, i) => (
              <li key={i} className="flex items-start gap-3">
                <CheckCircle2
                  className="mt-0.5 h-5 w-5 shrink-0"
                  style={{ color: MM.primary }}
                  strokeWidth={1.5}
                />
                <span
                  className="text-base leading-relaxed"
                  style={{ color: MM.onSurfaceVariant }}
                  dangerouslySetInnerHTML={{ __html: highlightKeyTerms(bullet) }}
                />
              </li>
            ))}
          </ul>

          <div
            className="flex items-center gap-3 border-t py-4"
            style={{ borderColor: `${MM.outlineVariant}33` }}
          >
            <Sparkles className="h-4 w-4" style={{ color: MM.primary }} strokeWidth={1.5} />
            <p className="text-sm font-medium" style={{ color: MM.primary }}>
              Prioritizing practical productivity over raw syntax.
            </p>
          </div>
        </div>

        {/* Confidence score card */}
        <div className="flex flex-col gap-8 md:col-span-4">
          <div
            className="flex flex-grow flex-col items-center justify-center rounded-[2rem] p-10 text-center"
            style={{ background: MM.primary, color: MM.onPrimary }}
          >
            <p className="text-[10px] uppercase tracking-[0.2em] opacity-60">Confidence Fit</p>
            <div className="mt-2">
              <span className="text-5xl font-black">{fitScore}</span>
              <span className="text-2xl font-light">%</span>
            </div>
            <p
              className="mt-2 text-[11px] font-medium uppercase tracking-widest opacity-80"
              aria-label={`Fit score: ${fitScore} percent`}
            >
              Optimal Alignment
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ═══════════════════════════════════════════
   Section 03 — Selected Path / Recommendation
   ═══════════════════════════════════════════ */

function SelectedPathSection({
  recommendation,
  enrolled,
  enrolling,
  onEnroll,
}: {
  recommendation: CourseRecommendation;
  enrolled: boolean;
  enrolling: boolean;
  onEnroll: () => void;
}) {
  const course = recommendation.course;
  const fitScore = getFitScore(recommendation.score);
  const gains = getGainBullets(recommendation);

  return (
    <section className="relative mx-auto max-w-[1280px] px-5 pb-16 sm:px-8 lg:px-16 lg:pb-24">
      {/* Section header */}
      <div className="relative mb-16 flex items-center gap-6">
        <span
          className="pointer-events-none select-none text-[60px] font-extralight leading-none lg:text-[80px]"
          style={{ color: `${MM.primary}0D` }}
        >
          03
        </span>
        <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
        <span
          className="text-[10px] font-bold uppercase tracking-[0.3em]"
          style={{ color: MM.secondary }}
        >
          Selected Path
        </span>
      </div>

      {/* Large recommendation card */}
      <article
        className="overflow-hidden rounded-[2rem] lg:rounded-[3rem]"
        style={{
          background: "#ffffff",
          border: `1px solid ${MM.outlineVariant}33`,
          boxShadow: `0 4px 6px -1px rgba(0,0,0,0.05), 0 20px 40px -10px ${MM.primary}0D`,
        }}
      >
        <div className="flex flex-col lg:flex-row lg:min-h-[600px]">
          {/* Left — image */}
          <div className="relative overflow-hidden lg:w-2/5">
            {course.imageUrl ? (
              <img
                src={course.imageUrl}
                alt={`Cover image for ${course.title}`}
                className="h-full min-h-[300px] w-full object-cover transition-transform duration-1000 hover:scale-110"
                onError={(e) => {
                  e.currentTarget.style.display = "none";
                  e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
                }}
              />
            ) : null}
            <div
              className="flex h-full min-h-[300px] items-center justify-center"
              style={{
                background: `linear-gradient(135deg, ${MM.primaryContainer}, ${MM.primary})`,
              }}
              data-fallback=""
              hidden={!!course.imageUrl}
            >
              <BookOpen
                className="h-16 w-16 opacity-30"
                style={{ color: MM.onPrimary }}
                strokeWidth={1.2}
              />
            </div>
            <div
              className="absolute inset-0"
              style={{
                background: `linear-gradient(to top, ${MM.primary}E6, ${MM.primary}33 50%, transparent)`,
              }}
            />

            {/* Recommended badge */}
            <div className="absolute left-8 top-8">
              <div
                className="flex items-center gap-2 rounded-full px-4 py-2"
                style={{
                  background: "rgba(255,255,255,0.95)",
                  boxShadow: `0 8px 24px -8px ${MM.primary}40`,
                }}
              >
                <span
                  className="h-2 w-2 animate-pulse rounded-full"
                  style={{ background: "#22c55e" }}
                />
                <span
                  className="text-[10px] font-black uppercase tracking-widest"
                  style={{ color: MM.primary }}
                >
                  Recommended
                </span>
              </div>
            </div>

            {/* Bottom glass score panel */}
            <div className="absolute bottom-10 left-10 right-10">
              <div
                className="rounded-[2rem] p-8"
                style={{
                  background: "rgba(255,255,255,0.1)",
                  backdropFilter: "blur(16px)",
                  WebkitBackdropFilter: "blur(16px)",
                  border: "1px solid rgba(255,255,255,0.2)",
                }}
              >
                <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-white/60">
                  Curated Score
                </p>
                <div className="mt-2 flex items-center justify-between">
                  <span className="text-5xl font-black text-white">
                    {fitScore}% <span className="text-lg font-light opacity-50">match</span>
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Right — content */}
          <div
            className="flex flex-col justify-between p-8 sm:p-12 lg:w-3/5 lg:p-16"
            style={{ background: `${MM.surface}80` }}
          >
            <div>
              <div className="mb-6 flex items-center gap-2">
                <Sparkles className="h-4 w-4" style={{ color: MM.primary }} strokeWidth={1.5} />
                <span
                  className="text-[10px] font-bold uppercase tracking-[0.2em]"
                  style={{ color: MM.primary }}
                >
                  AI Advisor Response
                </span>
              </div>

              <div className="mb-10 flex gap-3">
                <span
                  className="rounded-full px-4 py-1.5 text-[10px] font-bold uppercase tracking-wider"
                  style={{ background: MM.surfaceContainer, color: MM.primary }}
                >
                  {formatLevel(course.level)}
                </span>
                <span
                  className="rounded-full px-4 py-1.5 text-[10px] font-bold uppercase tracking-wider"
                  style={{ background: MM.surfaceContainer, color: MM.primary }}
                >
                  {formatLanguage(course.languageCode)}
                </span>
              </div>

              <h2
                className="text-3xl font-black leading-tight tracking-tight sm:text-4xl lg:text-5xl"
                style={{ color: MM.primary }}
              >
                {course.title}
              </h2>

              <div className="mt-12 space-y-12">
                <div>
                  <div className="mb-4 flex items-center gap-3">
                    <Target className="h-5 w-5" style={{ color: MM.primary }} strokeWidth={1.5} />
                    <h4
                      className="text-[11px] font-bold uppercase tracking-[0.2em]"
                      style={{ color: MM.primary }}
                    >
                      Why this fits your goal
                    </h4>
                  </div>
                  <p className="text-lg leading-relaxed" style={{ color: MM.onSurfaceVariant }}>
                    {recommendation.reason}
                  </p>
                </div>

                <div>
                  <div className="mb-6 flex items-center gap-3">
                    <Medal className="h-5 w-5" style={{ color: MM.primary }} strokeWidth={1.5} />
                    <h4
                      className="text-[11px] font-bold uppercase tracking-[0.2em]"
                      style={{ color: MM.primary }}
                    >
                      What you will gain
                    </h4>
                  </div>
                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    {gains.map((gain, i) => (
                      <div
                        key={i}
                        className="flex items-center gap-4 rounded-2xl p-5"
                        style={{
                          background: MM.surface,
                          border: `1px solid ${MM.outlineVariant}4D`,
                        }}
                      >
                        <Check
                          className="h-4 w-4 shrink-0"
                          style={{ color: MM.primary }}
                          strokeWidth={2}
                        />
                        <span className="text-sm font-semibold" style={{ color: MM.onSurface }}>
                          {gain}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* CTAs */}
            <div
              className="mt-12 flex flex-col gap-4 border-t pt-10 sm:flex-row sm:items-center sm:gap-6"
              style={{ borderColor: `${MM.outlineVariant}33` }}
            >
              {enrolled ? (
                <Link
                  to="/courses/$courseId"
                  params={{ courseId: course.id }}
                  className="flex flex-1 items-center justify-center gap-3 rounded-2xl py-6 text-[11px] font-bold uppercase tracking-[0.25em]"
                  style={{
                    background: `${MM.primary}14`,
                    color: MM.primary,
                  }}
                >
                  <CheckCircle2 className="h-4 w-4" />
                  Already Enrolled — View Course
                </Link>
              ) : (
                <button
                  type="button"
                  onClick={onEnroll}
                  disabled={enrolling}
                  className="flex flex-1 items-center justify-center gap-3 rounded-2xl py-6 text-[11px] font-bold uppercase tracking-[0.25em] transition-all duration-300 disabled:opacity-50"
                  style={{
                    background: MM.primary,
                    color: MM.onPrimary,
                    boxShadow: `0 10px 30px -5px ${MM.primary}33`,
                  }}
                >
                  {enrolling ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Enrolling...
                    </>
                  ) : (
                    <>
                      Enroll in This Course
                      <Zap className="h-4 w-4" strokeWidth={1.75} />
                    </>
                  )}
                </button>
              )}
              <Link
                to="/courses/$courseId"
                params={{ courseId: course.id }}
                className="w-full rounded-2xl px-12 py-6 text-center text-[11px] font-bold uppercase tracking-[0.25em] transition-all sm:w-auto"
                style={{
                  border: `1px solid ${MM.outlineVariant}`,
                  color: MM.secondary,
                }}
              >
                Review Syllabus
              </Link>
            </div>
          </div>
        </div>
      </article>
    </section>
  );
}

function AlternativePathCard({
  recommendation,
  enrolled,
  enrolling,
  onEnroll,
}: {
  recommendation: CourseRecommendation;
  enrolled: boolean;
  enrolling: boolean;
  onEnroll: () => void;
}) {
  const course = recommendation.course;

  return (
    <article
      className="group rounded-2xl p-6 transition-all duration-300"
      style={{
        background: MM.surface,
        border: `1px solid ${MM.outlineVariant}4D`,
      }}
    >
      <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 gap-4">
          <Compass
            className="mt-1 h-5 w-5 shrink-0"
            style={{ color: MM.outline }}
            strokeWidth={1.5}
          />
          <div className="min-w-0">
            <p
              className="text-[10px] font-semibold tracking-[0.22em]"
              style={{ color: MM.onSurfaceVariant }}
            >
              ALTERNATIVE PATH
            </p>
            <h3 className="mt-2 text-lg font-semibold" style={{ color: MM.primary }}>
              {course.title}
            </h3>
            <p
              className="mt-2 max-w-[72ch] text-sm leading-6"
              style={{ color: MM.onSurfaceVariant }}
            >
              {recommendation.reason}
            </p>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {!enrolled && (
            <button
              type="button"
              onClick={onEnroll}
              disabled={enrolling}
              className="rounded-lg px-3 py-2 text-xs font-semibold transition-colors duration-300 disabled:opacity-50"
              style={{
                border: `1px solid ${MM.outlineVariant}`,
                background: "#ffffff",
                color: MM.primary,
              }}
            >
              Enroll
            </button>
          )}
          <Link
            to="/courses/$courseId"
            params={{ courseId: course.id }}
            aria-label={`View outline for ${course.title}`}
            className="grid h-10 w-10 place-items-center rounded-full transition-colors duration-300"
            style={{ color: MM.outline }}
          >
            <ChevronRight className="h-5 w-5" strokeWidth={1.75} />
          </Link>
        </div>
      </div>
    </article>
  );
}

/* ═══════════════════════════════════════════
   Loading / Error / Empty States
   ═══════════════════════════════════════════ */

function AdvisorLoading({ goal }: { goal: string }) {
  return (
    <motion.section
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="mx-auto max-w-[1280px] px-5 pb-20 sm:px-8 lg:px-16"
    >
      {/* Section header */}
      <div className="relative mb-12 flex items-center gap-6">
        <span
          className="pointer-events-none select-none text-[60px] font-extralight leading-none"
          style={{ color: `${MM.primary}0D` }}
        >
          02
        </span>
        <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
        <span
          className="flex items-center gap-3 rounded-full px-6 py-1 text-[10px] font-bold uppercase tracking-[0.3em]"
          style={{ background: "#ffffff", color: MM.secondary }}
        >
          <Loader2 className="h-3 w-3 animate-spin" />
          Synthesizing Insights
        </span>
        <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
      </div>

      <div className="mx-auto max-w-4xl text-center">
        <p
          className="mb-8 text-xl font-light italic leading-relaxed sm:text-2xl"
          style={{ color: `${MM.primary}B3` }}
        >
          &ldquo;{goal}&rdquo;
        </p>
      </div>

      <div
        className="mx-auto max-w-4xl rounded-[2rem] p-10"
        style={{
          background: "#ffffff",
          border: `1px solid ${MM.outlineVariant}33`,
        }}
      >
        <div className="space-y-4">
          <div
            className="h-4 w-3/4 animate-pulse rounded-full"
            style={{ background: MM.surfaceContainer }}
          />
          <div
            className="h-4 w-5/6 animate-pulse rounded-full"
            style={{ background: MM.surfaceContainer }}
          />
          <div
            className="h-4 w-1/2 animate-pulse rounded-full"
            style={{ background: MM.surfaceContainer }}
          />
          <div
            className="h-4 w-2/3 animate-pulse rounded-full"
            style={{ background: MM.surfaceContainer }}
          />
        </div>
      </div>
    </motion.section>
  );
}

function AdvisorError({ brief, onRetry }: { brief: Brief; onRetry: () => void }) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -16 }}
      className="mx-auto max-w-[1280px] px-5 pb-20 sm:px-8 lg:px-16"
    >
      <div
        className="mx-auto max-w-3xl rounded-[2rem] p-10 text-center"
        style={{
          background: "#ffffff",
          border: `1px solid ${MM.errorContainer}`,
        }}
      >
        <p className="text-[10px] font-semibold tracking-[0.22em]" style={{ color: MM.error }}>
          ADVISOR ERROR
        </p>
        <h2 className="mt-4 text-3xl font-bold" style={{ color: MM.primary }}>
          The catalog did not answer.
        </h2>
        <p className="mt-4 text-sm italic" style={{ color: MM.outline }}>
          &ldquo;{brief.goal}&rdquo;
        </p>
        <p
          className="mx-auto mt-5 max-w-[58ch] text-sm leading-7"
          style={{ color: MM.onSurfaceVariant }}
        >
          {brief.message}
        </p>
        <button
          type="button"
          onClick={onRetry}
          className="mt-7 inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl px-6 py-3 text-sm font-semibold transition-all"
          style={{ background: MM.primary, color: MM.onPrimary }}
        >
          Try again
          <RefreshCw className="h-4 w-4" />
        </button>
      </div>
    </motion.section>
  );
}

function AdvisorNoMatch({ brief }: { brief: Brief }) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -16 }}
      className="mx-auto max-w-[1280px] px-5 pb-20 sm:px-8 lg:px-16"
    >
      <div
        className="mx-auto max-w-3xl rounded-[2rem] p-10 text-center"
        style={{
          background: "#ffffff",
          border: `1px solid ${MM.outlineVariant}4D`,
        }}
      >
        <p className="text-[10px] font-semibold tracking-[0.22em]" style={{ color: MM.outline }}>
          NO CLEAR MATCH
        </p>
        <h2 className="mt-4 text-3xl font-bold" style={{ color: MM.primary }}>
          Try a sharper career brief.
        </h2>
        <p
          className="mx-auto mt-5 max-w-[58ch] text-sm leading-7"
          style={{ color: MM.onSurfaceVariant }}
        >
          {brief.message ||
            "Name the destination, current level, and preferred language so EduLife can compare the goal against the live course catalog."}
        </p>
      </div>
    </motion.section>
  );
}

function AdvisorEmpty({ catalogCount }: { catalogCount: number }) {
  return (
    <motion.section
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="mx-auto max-w-[1280px] px-5 pb-20 sm:px-8 lg:px-16"
    >
      <div className="mx-auto max-w-4xl opacity-75">
        <div className="relative mb-12 flex items-center gap-6">
          <span
            className="pointer-events-none select-none text-[60px] font-extralight leading-none"
            style={{ color: `${MM.primary}0D` }}
          >
            02
          </span>
          <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
          <span
            className="rounded-full px-6 py-1 text-[10px] font-bold uppercase tracking-[0.3em]"
            style={{ background: MM.surfaceContainer, color: MM.outline }}
          >
            Waiting for brief
          </span>
          <div className="h-px flex-grow" style={{ background: `${MM.outlineVariant}4D` }} />
        </div>

        <div
          className="rounded-[2rem] p-10"
          style={{
            background: "rgba(255,255,255,0.55)",
            border: `2px dashed ${MM.outlineVariant}`,
          }}
        >
          <p className="text-2xl font-semibold" style={{ color: `${MM.primary}66` }}>
            Submit a goal to receive one focused course recommendation.
          </p>
          <p className="mt-4 text-sm leading-7" style={{ color: MM.outline }}>
            The advisor has {catalogCount} live course{catalogCount === 1 ? "" : "s"} available for
            matching.
          </p>
        </div>
      </div>
    </motion.section>
  );
}

/* ═══════════════════════════════════════════
   Footer
   ═══════════════════════════════════════════ */

function AdvisorFooter() {
  return (
    <footer style={{ background: "#ffffff", borderTop: `1px solid ${MM.outlineVariant}4D` }}>
      <div className="mx-auto grid max-w-[1280px] grid-cols-1 gap-16 px-5 py-24 sm:px-8 md:grid-cols-12 lg:px-16">
        <div className="md:col-span-5">
          <Link
            to="/dashboard"
            className="flex items-center gap-2 text-2xl font-black tracking-tight"
            style={{ color: MM.primary }}
          >
            <div
              className="flex h-8 w-8 items-center justify-center rounded-lg"
              style={{ background: MM.primary }}
            >
              <GraduationCap className="h-4 w-4 text-white" strokeWidth={1.75} />
            </div>
            EduLife
          </Link>
          <p
            className="mb-10 mt-8 max-w-sm text-base leading-relaxed"
            style={{ color: MM.onSurfaceVariant }}
          >
            Empowering the next generation of Moroccan professionals through high-fidelity,
            AI-curated learning pathways.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-12 md:col-span-7 lg:grid-cols-3">
          <div>
            <h4
              className="mb-8 text-[11px] font-bold uppercase tracking-[0.2em]"
              style={{ color: MM.primary }}
            >
              Platform
            </h4>
            <ul className="space-y-4">
              <li>
                <Link
                  to="/explore"
                  className="text-sm transition-colors"
                  style={{ color: MM.onSurfaceVariant }}
                >
                  Live Catalog
                </Link>
              </li>
              <li>
                <Link
                  to="/advisor"
                  className="text-sm transition-colors"
                  style={{ color: MM.onSurfaceVariant }}
                >
                  Career Advisor
                </Link>
              </li>
              <li>
                <Link
                  to="/dashboard"
                  className="text-sm transition-colors"
                  style={{ color: MM.onSurfaceVariant }}
                >
                  Dashboard
                </Link>
              </li>
            </ul>
          </div>
          <div>
            <h4
              className="mb-8 text-[11px] font-bold uppercase tracking-[0.2em]"
              style={{ color: MM.primary }}
            >
              Governance
            </h4>
            <ul className="space-y-4">
              <li>
                <span className="text-sm" style={{ color: MM.onSurfaceVariant }}>
                  Privacy Policy
                </span>
              </li>
              <li>
                <span className="text-sm" style={{ color: MM.onSurfaceVariant }}>
                  Terms of Use
                </span>
              </li>
              <li>
                <Link
                  to="/certificates"
                  className="text-sm transition-colors"
                  style={{ color: MM.onSurfaceVariant }}
                >
                  Trust Center
                </Link>
              </li>
            </ul>
          </div>
          <div className="hidden lg:block">
            <h4
              className="mb-8 text-[11px] font-bold uppercase tracking-[0.2em]"
              style={{ color: MM.primary }}
            >
              Social
            </h4>
            <ul className="space-y-4">
              <li>
                <span className="text-sm" style={{ color: MM.onSurfaceVariant }}>
                  LinkedIn
                </span>
              </li>
              <li>
                <span className="text-sm" style={{ color: MM.onSurfaceVariant }}>
                  Instagram
                </span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div
        className="mx-auto flex max-w-[1280px] flex-col items-center justify-between gap-6 px-5 py-12 sm:px-8 md:flex-row lg:px-16"
        style={{ borderTop: `1px solid ${MM.outlineVariant}4D` }}
      >
        <p className="text-[11px] font-medium tracking-widest" style={{ color: MM.outline }}>
          &copy; {new Date().getFullYear()} EDULIFE MOROCCO. THE FUTURE OF WORK IS NOW.
        </p>
        <div className="flex gap-8">
          <span
            className="text-[11px] font-bold uppercase tracking-widest"
            style={{ color: MM.outline }}
          >
            Support
          </span>
          <span
            className="text-[11px] font-bold uppercase tracking-widest"
            style={{ color: MM.outline }}
          >
            English (INTL)
          </span>
        </div>
      </div>
    </footer>
  );
}

/* ═══════════════════════════════════════════
   Utility Functions (preserved from original)
   ═══════════════════════════════════════════ */

function getFitScore(score: number) {
  if (!Number.isFinite(score)) return 88;
  if (score <= 1) return Math.max(1, Math.min(99, Math.round(score * 100)));
  if (score <= 40) return Math.max(56, Math.min(99, Math.round(55 + (score / 40) * 43)));
  return Math.max(1, Math.min(99, Math.round(score)));
}

function getGainBullets(recommendation: CourseRecommendation) {
  const course = recommendation.course;
  const candidates = `${course.shortDescription ?? ""}. ${recommendation.reason}`
    .split(/[.;:]/)
    .map((part) => part.trim())
    .filter((part) => part.length > 18)
    .slice(0, 4)
    .map((part) => sentenceToBullet(part));

  const fallback = [
    "Structured course outline",
    "Practical next step",
    `${formatLanguage(course.languageCode)} learning support`,
    `${formatLevel(course.level)} pacing`,
  ];

  return [...candidates, ...fallback].slice(0, 4);
}

function sentenceToBullet(value: string) {
  const clean = value
    .replace(/^I picked it because your goal connects with\s*/i, "")
    .replace(/^The course is\s*/i, "")
    .replace(/^It\s+/i, "")
    .replace(/\.$/, "");
  return clean.charAt(0).toUpperCase() + clean.slice(1);
}

function formatLevel(level: string) {
  return (level || "Beginner")
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatLanguage(languageCode: string) {
  switch ((languageCode || "en").toLowerCase()) {
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

function extractReasoningBullets(text: string): string[] {
  const sentences = text
    .split(/[.!?\n]+/)
    .map((s) => s.trim())
    .filter((s) => s.length > 15);

  if (sentences.length === 0) {
    return [
      "Focusing on digital workflow efficiency over theory.",
      "Building technical stamina for a smooth career switch.",
      "Prioritizing immediate professional value.",
    ];
  }

  return sentences.slice(0, 4);
}

function highlightKeyTerms(text: string): string {
  const keywords = [
    "digital workflow efficiency",
    "career switch",
    "professional value",
    "productivity",
    "technical",
    "foundational",
    "practical",
    "structured",
  ];

  let result = text;
  for (const keyword of keywords) {
    const regex = new RegExp(`(${keyword})`, "gi");
    result = result.replace(regex, `<strong style="color: ${MM.primary}">$1</strong>`);
  }
  return result;
}
