import { useMemo, useState, useRef, useEffect } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";

import {
  ArrowUpRight,
  ArrowDown,
  BookOpen,
  CheckCircle2,
  RefreshCw,
  Sparkles,
  Send,
  Layers,
  Globe2,
  History,
  X,
  CornerDownLeft,
  Copy,
  Printer,
} from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import {
  enrollInCourse,
  listCourses,
  listMyEnrollments,
  requestAdvisorRecommendation,
} from "../lib/api/client";
import {
  analyzeCareerGoal,
  type CourseRecommendation,
} from "../lib/career/advisor";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";
import { appEnv } from "../lib/env";

export const Route = createFileRoute("/advisor")({
  component: AdvisorRoute,
  head: () => ({ meta: [{ title: "Career Advisor - EduLife" }] }),
});

const EXAMPLES = [
  { label: "Software path", text: "I want to become a software developer and improve my English." },
  { label: "Engineering prep", text: "I want to prepare for engineering studies after Bac." },
  { label: "French writing", text: "I want better French writing for school and work." },
  { label: "Design pivot", text: "I'm switching careers to design and want a structured intro." },
  { label: "Bac science", text: "Help me revise science topics for Bac with clear examples." },
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

function AdvisorPage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [goal, setGoal] = useState("");
  const [historyOpen, setHistoryOpen] = useState(false);

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

  const courseMap = useMemo(
    () => new Map((coursesQuery.data?.content ?? []).map((c) => [c.id, c])),
    [coursesQuery.data?.content],
  );

  const [briefs, setBriefs] = useState<Brief[]>([]);
  const [activeBriefId, setActiveBriefId] = useState<string | null>(null);
  const briefRef = useRef<HTMLDivElement>(null);

  // Load briefs from localStorage on mount
  useEffect(() => {
    try {
      const stored = localStorage.getItem("edulife_advisor_briefs");
      if (stored) {
        const parsed = JSON.parse(stored) as Brief[];
        if (parsed.length > 0) {
          setBriefs(parsed);
          setActiveBriefId(parsed[0].id);
        }
      }
    } catch (e) {
      console.error("Failed to load briefs from localStorage", e);
    }
  }, []);

  const saveBriefsToStorage = (updatedList: Brief[]) => {
    try {
      const toSave = updatedList.filter((b) => !b.isLoading && !b.isError);
      localStorage.setItem("edulife_advisor_briefs", JSON.stringify(toSave));
    } catch (e) {
      console.error("Failed to save briefs", e);
    }
  };

  const currentBrief = useMemo(() => {
    if (activeBriefId) {
      return briefs.find((b) => b.id === activeBriefId) ?? briefs[0];
    }
    return briefs[0];
  }, [briefs, activeBriefId]);

  useEffect(() => {
    if (currentBrief && briefRef.current) {
      briefRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, [currentBrief?.id, currentBrief?.isLoading]);

  const handleClearHistory = () => {
    if (window.confirm("Are you sure you want to clear all your brief history?")) {
      setBriefs([]);
      setActiveBriefId(null);
      localStorage.removeItem("edulife_advisor_briefs");
      toast.success("History cleared successfully");
    }
  };

  const handleDeleteBrief = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setBriefs((prev) => {
      const updated = prev.filter((b) => b.id !== id);
      localStorage.setItem("edulife_advisor_briefs", JSON.stringify(updated));
      return updated;
    });
    if (activeBriefId === id) {
      setActiveBriefId(null);
    }
    toast.success("Brief deleted from history");
  };

  const enrolledCourseIds = new Set((enrollmentsQuery.data ?? []).map((item) => item.courseId));
  const trimmedGoal = goal.trim();
  const goalReady = trimmedGoal.length >= 4;
  const catalogCount = coursesQuery.data?.totalElements ?? coursesQuery.data?.content.length ?? 0;

  async function handleAnalyze(customGoal?: string) {
    const targetGoal = (customGoal ?? trimmedGoal).trim();
    if (targetGoal.length < 4) return;

    const briefId = Date.now().toString();
    setBriefs((prev) => [
      {
        id: briefId,
        goal: targetGoal,
        message: "",
        recommendations: [],
        isLoading: true,
      },
      ...prev,
    ]);
    setActiveBriefId(briefId);
    setGoal("");

    if (appEnv.advisorAiEnabled) {
      try {
        const apiResult = await requestAdvisorRecommendation(auth.getAccessToken, targetGoal);
        const recommendations = apiResult.recommendations
          .filter((r) => courseMap.has(r.courseId))
          .map((r) => ({
            course: courseMap.get(r.courseId)!,
            reason: r.reason,
            score: r.score,
          }));

        setBriefs((prev) => {
          const updated = prev.map((b) =>
            b.id === briefId
              ? { ...b, message: apiResult.message, recommendations, isLoading: false }
              : b,
          );
          saveBriefsToStorage(updated);
          return updated;
        });
      } catch {
        const catalog = coursesQuery.data?.content ?? [];
        if (catalog.length > 0) {
          const result = analyzeCareerGoal(targetGoal, catalog);
          setBriefs((prev) => {
            const updated = prev.map((b) =>
              b.id === briefId
                ? {
                    ...b,
                    message: result.message,
                    recommendations: result.recommendations,
                    isLoading: false,
                  }
                : b,
            );
            saveBriefsToStorage(updated);
            return updated;
          });
        } else {
          setBriefs((prev) => {
            const updated = prev.map((b) =>
              b.id === briefId
                ? {
                    ...b,
                    message:
                      "Unable to reach the advisor right now. Check your connection and try again.",
                    isLoading: false,
                    isError: true,
                  }
                : b,
            );
            return updated;
          });
        }
      }
    } else {
      setTimeout(() => {
        const result = analyzeCareerGoal(targetGoal, coursesQuery.data?.content ?? []);
        setBriefs((prev) => {
          const updated = prev.map((b) =>
            b.id === briefId
              ? {
                  ...b,
                  message: result.message,
                  recommendations: result.recommendations,
                  isLoading: false,
                }
              : b,
          );
          saveBriefsToStorage(updated);
          return updated;
        });
      }, 1200);
    }
  }

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
            One brief, grounded on the live course catalog.
          </p>
        </div>
      }
    >
      <div className="-mx-4 sm:-mx-6 lg:-mx-8 -my-8">
        <AdvisorStage
          goal={goal}
          setGoal={setGoal}
          onAnalyze={handleAnalyze}
          goalReady={goalReady}
          isCatalogLoading={coursesQuery.isLoading}
          catalogCount={catalogCount}
          enrolledCount={enrolledCourseIds.size}
          briefs={briefs}
          examples={EXAMPLES}
          historyOpen={historyOpen}
          setHistoryOpen={setHistoryOpen}
          currentBrief={currentBrief}
          activeBriefId={activeBriefId}
          setActiveBriefId={setActiveBriefId}
          onDeleteBrief={handleDeleteBrief}
          onClearHistory={handleClearHistory}
        />

        <div ref={briefRef}>
          <AnimatePresence mode="wait">
            {currentBrief && (
              <BriefSpread
                key={currentBrief.id}
                brief={currentBrief}
                enrolledCourseIds={enrolledCourseIds}
                enrolling={enrollMutation.isPending}
                onEnroll={(courseId) => enrollMutation.mutate(courseId)}
                onRetry={() => handleAnalyze(currentBrief.goal)}
              />
            )}
          </AnimatePresence>

          {!currentBrief && <EmptyAdvisorState catalogCount={catalogCount} />}
        </div>
      </div>
    </AppShell>
  );
}

interface StageProps {
  goal: string;
  setGoal: (v: string) => void;
  onAnalyze: (g?: string) => void;
  goalReady: boolean;
  isCatalogLoading: boolean;
  catalogCount: number;
  enrolledCount: number;
  briefs: Brief[];
  examples: typeof EXAMPLES;
  historyOpen: boolean;
  setHistoryOpen: (v: boolean) => void;
  currentBrief?: Brief;
  activeBriefId: string | null;
  setActiveBriefId: (id: string | null) => void;
  onDeleteBrief: (id: string, e: React.MouseEvent) => void;
  onClearHistory: () => void;
}

function AdvisorStage({
  goal,
  setGoal,
  onAnalyze,
  goalReady,
  isCatalogLoading,
  catalogCount,
  briefs,
  examples,
  historyOpen,
  setHistoryOpen,
  activeBriefId,
  setActiveBriefId,
  onDeleteBrief,
  onClearHistory,
}: StageProps) {
  const [bgSel, setBgSel] = useState("");
  const [interestSel, setInterestSel] = useState("");
  const [langSel, setLangSel] = useState("");

  const handleBuilderSelect = (type: "bg" | "interest" | "lang", value: string) => {
    let nextBg = bgSel;
    let nextInterest = interestSel;
    let nextLang = langSel;

    if (type === "bg") {
      nextBg = bgSel === value ? "" : value;
      setBgSel(nextBg);
    } else if (type === "interest") {
      nextInterest = interestSel === value ? "" : value;
      setInterestSel(nextInterest);
    } else if (type === "lang") {
      nextLang = langSel === value ? "" : value;
      setLangSel(nextLang);
    }

    let parts: string[] = [];
    if (nextBg) {
      if (nextBg === "Career Switcher") parts.push("I am switching my career");
      else if (nextBg === "Univ Student") parts.push("I am a university student");
      else parts.push(`I am a ${nextBg.toLowerCase()}`);
    }
    if (nextInterest) {
      let intStr = nextInterest.toLowerCase();
      if (intStr === "software") intStr = "software development";
      if (intStr === "languages") intStr = "languages and communication";
      if (intStr === "business") intStr = "business planning";
      if (intStr === "engineering") intStr = "engineering prep subjects";
      parts.push(nextBg ? `looking to learn ${intStr}` : `I want to learn ${intStr}`);
    }
    if (nextLang) {
      if (nextLang === "Darija") parts.push("explained in Moroccan Darija");
      else if (nextLang === "French") parts.push("taught in French");
      else parts.push("taught in English");
    }

    if (parts.length > 0) {
      let result = parts[0];
      if (parts.length > 1) {
        result += nextBg && nextInterest ? " " + parts[1] : " and " + parts[1].replace("I want to ", "");
      }
      if (parts.length > 2) result += " " + parts[2];
      setGoal(result + ".");
    } else {
      setGoal("");
    }
  };

  const handleResetBuilder = () => {
    setBgSel("");
    setInterestSel("");
    setLangSel("");
    setGoal("");
  };

  const hasBuilderSelection = bgSel || interestSel || langSel;

  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-zinc-950 via-slate-950 to-neutral-900 text-primary-foreground">
      {/* Ambient orbs */}
      <div className="pointer-events-none absolute -top-40 -left-20 h-[440px] w-[640px] rounded-full bg-primary-glow/20 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-40 -right-32 h-[400px] w-[560px] rounded-full bg-gold/10 blur-3xl" />
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(70%_45%_at_50%_0%,oklch(0.40_0.19_152/0.22),transparent_70%)]" />
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.04]"
        style={{
          backgroundImage:
            "linear-gradient(oklch(1 0 0 / 0.5) 1px, transparent 1px), linear-gradient(90deg, oklch(1 0 0 / 0.5) 1px, transparent 1px)",
          backgroundSize: "60px 60px",
        }}
      />

      <div className="relative mx-auto max-w-6xl px-6 lg:px-10 pt-12 pb-14">
        {/* Minimal top strip */}
        <div className="flex items-center justify-between mb-10">
          <div className="flex items-center gap-2.5 text-[10px] font-mono uppercase tracking-[0.2em] text-primary-foreground/45">
            <span className="h-1.5 w-1.5 rounded-full bg-teal animate-pulse" />
            <span>Advisor</span>
            <span className="opacity-30">·</span>
            <span className="hidden sm:inline">
              {isCatalogLoading ? "Indexing catalog…" : `${catalogCount} courses live`}
            </span>
          </div>
          {briefs.length > 0 && (
            <button
              type="button"
              onClick={() => setHistoryOpen(!historyOpen)}
              className="inline-flex items-center gap-1.5 rounded-full border border-primary-foreground/12 px-3 py-1 text-[10px] font-mono uppercase tracking-[0.18em] text-primary-foreground/55 hover:text-primary-foreground hover:border-primary-foreground/25 transition-all duration-300"
            >
              <History className="h-3 w-3" strokeWidth={1.75} />
              {briefs.length} brief{briefs.length === 1 ? "" : "s"}
            </button>
          )}
        </div>

        <div className="grid lg:grid-cols-2 gap-12 lg:gap-16 items-start">
          {/* Left: headline + feature list */}
          <motion.div
            initial={{ opacity: 0, y: 20, filter: "blur(6px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            transition={{ duration: 0.9, ease: [0.16, 1, 0.3, 1] }}
            className="flex flex-col"
          >
            <span
              className="eyebrow eyebrow-dot self-start"
              style={{ background: "oklch(1 0 0 / 0.06)", color: "oklch(1 0 0 / 0.65)", border: "1px solid oklch(1 0 0 / 0.10)" }}
            >
              One question · One course brief
            </span>
            <h1 className="mt-6 text-display leading-[0.92] tracking-tighter text-[clamp(2.8rem,5.5vw,4.75rem)]">
              What do you want
              <br />
              to{" "}
              <span className="italic font-normal text-gold">build toward</span>
              <span className="text-primary-foreground/50">?</span>
            </h1>
            <p className="mt-5 text-sm lg:text-base text-primary-foreground/55 leading-relaxed max-w-[42ch]">
              Describe a career, a Bac track, a language, or a project. The advisor reads the
              live catalog and returns one editorial brief — not a list of links.
            </p>

            {/* Feature list — replaces the AI persona card */}
            <ul className="mt-10 flex flex-col gap-3.5">
              {[
                ["Names one course", "Best-fit pick from the live catalog, never generic suggestions."],
                ["Explains the why", "Editorial reasoning grounded in your stated goal."],
                ["Shows the full picture", "Level, language, and fit score on every recommendation."],
              ].map(([heading, detail]) => (
                <li key={heading} className="flex items-start gap-3.5">
                  <span className="mt-[5px] h-1.5 w-1.5 shrink-0 rounded-full bg-primary-glow/70" />
                  <div>
                    <span className="text-sm font-medium text-primary-foreground/80">{heading}</span>
                    <span className="text-sm text-primary-foreground/40"> — {detail}</span>
                  </div>
                </li>
              ))}
            </ul>
          </motion.div>

          {/* Right: composer + prompt builder + starters */}
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.12, ease: [0.16, 1, 0.3, 1] }}
            className="flex flex-col gap-5"
          >
            {/* Textarea card — clean, no inner sections */}
            <div className="rounded-2xl border border-primary-foreground/10 bg-primary-foreground/[0.03] backdrop-blur-sm shadow-[0_20px_50px_-16px_oklch(0_0_0/0.55)]">
              <textarea
                value={goal}
                onChange={(e) => setGoal(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    onAnalyze();
                  }
                }}
                rows={3}
                placeholder="e.g. I want to become a backend developer fluent in English…"
                className="w-full resize-none bg-transparent px-5 pt-5 pb-3 text-display text-xl lg:text-2xl leading-snug tracking-tight text-primary-foreground placeholder:text-primary-foreground/20 outline-none border-none focus:ring-0 focus:outline-none"
              />
              <div className="flex items-center justify-between gap-4 px-4 pb-4">
                <span className="inline-flex items-center gap-1.5 text-[10px] font-mono uppercase tracking-[0.18em] text-primary-foreground/35">
                  <CornerDownLeft className="h-3 w-3" strokeWidth={1.75} />
                  Enter to send
                </span>
                <button
                  type="button"
                  onClick={() => onAnalyze()}
                  disabled={!goalReady || isCatalogLoading}
                  className="group inline-flex h-9 items-center gap-1 rounded-full bg-gold text-gold-foreground pl-4 pr-1 text-xs font-semibold shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.03] active:scale-[0.97] disabled:opacity-25 disabled:pointer-events-none"
                >
                  <span>Draft brief</span>
                  <span className="grid h-7 w-7 place-items-center rounded-full bg-gold-foreground/15 transition-all duration-300 group-hover:translate-x-0.5 group-hover:-translate-y-px group-hover:bg-gold-foreground/25">
                    <Send className="h-3 w-3" strokeWidth={1.75} />
                  </span>
                </button>
              </div>
            </div>

            {/* Prompt builder — outside card, breathing room */}
            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-[9px] font-mono uppercase tracking-[0.2em] text-primary-foreground/35">
                  Build your prompt
                </span>
                {hasBuilderSelection && (
                  <button
                    type="button"
                    onClick={handleResetBuilder}
                    className="text-[9px] font-mono uppercase tracking-[0.18em] text-primary-foreground/40 hover:text-primary-foreground/75 transition-colors"
                  >
                    Reset
                  </button>
                )}
              </div>

              {[
                { label: "Background", type: "bg" as const, items: ["Bac Student", "Univ Student", "Career Switcher", "Hobbyist"], sel: bgSel },
                { label: "Interest", type: "interest" as const, items: ["Software", "Languages", "Business", "Engineering"], sel: interestSel },
                { label: "Language", type: "lang" as const, items: ["Darija", "French", "English"], sel: langSel },
              ].map(({ label, type, items, sel }) => (
                <div key={label} className="flex items-center gap-2 flex-wrap">
                  <span className="shrink-0 text-[9px] font-mono uppercase tracking-[0.14em] text-primary-foreground/30 w-16">
                    {label}
                  </span>
                  {items.map((item) => {
                    const isSelected = sel === item;
                    return (
                      <button
                        key={item}
                        type="button"
                        onClick={() => handleBuilderSelect(type, item)}
                        className={`text-[10px] px-2.5 py-1 rounded-full border transition-all duration-250 cursor-pointer ${
                          isSelected
                            ? "bg-gold text-gold-foreground border-gold shadow-bezel font-medium"
                            : "border-primary-foreground/10 text-primary-foreground/60 hover:border-primary-foreground/22 hover:text-primary-foreground/90"
                        }`}
                      >
                        {item}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>

            {/* Quick starters */}
            <div className="flex items-center gap-2.5 flex-wrap pt-1">
              <span className="shrink-0 text-[9px] font-mono uppercase tracking-[0.2em] text-primary-foreground/30">
                Starters
              </span>
              {examples.map((ex) => (
                <button
                  key={ex.label}
                  type="button"
                  onClick={() => onAnalyze(ex.text)}
                  className="group inline-flex items-center gap-1 rounded-full border border-primary-foreground/10 px-3 py-1 text-[10px] text-primary-foreground/60 transition-all duration-400 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-primary-foreground/6 hover:text-primary-foreground/90 hover:border-primary-foreground/22"
                >
                  {ex.label}
                  <ArrowUpRight className="h-2.5 w-2.5 opacity-40 group-hover:opacity-80 transition-opacity" strokeWidth={2} />
                </button>
              ))}
            </div>
          </motion.div>
        </div>

        {/* History drawer — full-width below grid */}
        <AnimatePresence>
          {historyOpen && briefs.length > 0 && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
              className="overflow-hidden mt-8"
            >
              <div className="rounded-2xl border border-primary-foreground/10 bg-primary-foreground/[0.03] backdrop-blur-sm p-5">
                <div className="flex items-center justify-between border-b border-primary-foreground/8 pb-3 mb-4">
                  <span className="text-[10px] font-mono uppercase tracking-[0.22em] text-primary-foreground/45">
                    Recent briefs
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={onClearHistory}
                      className="text-[9px] font-mono uppercase tracking-[0.14em] text-primary-foreground/40 hover:text-red-400 border border-primary-foreground/10 rounded px-2 py-0.5 transition-colors cursor-pointer"
                    >
                      Clear all
                    </button>
                    <button
                      type="button"
                      onClick={() => setHistoryOpen(false)}
                      className="grid h-6 w-6 place-items-center rounded-full text-primary-foreground/40 hover:text-primary-foreground hover:bg-primary-foreground/8 transition-colors cursor-pointer"
                      aria-label="Close history"
                    >
                      <X className="h-3 w-3" strokeWidth={1.75} />
                    </button>
                  </div>
                </div>
                <ul className="flex flex-col gap-1 max-h-[180px] overflow-y-auto">
                  {briefs.slice(0, 6).map((b, i) => {
                    const isActive = b.id === activeBriefId;
                    return (
                      <li
                        key={b.id}
                        onClick={() => setActiveBriefId(b.id)}
                        className={`group flex items-center justify-between gap-3 rounded-xl border px-3 py-2 transition-all duration-250 cursor-pointer ${
                          isActive
                            ? "border-gold/40 bg-gold/5"
                            : "border-transparent hover:border-primary-foreground/10 hover:bg-primary-foreground/[0.03]"
                        }`}
                      >
                        <div className="flex items-center gap-3 flex-1 min-w-0">
                          <span className="shrink-0 text-[9px] font-mono text-primary-foreground/30">
                            {String(briefs.length - i).padStart(2, "0")}
                          </span>
                          <p className={`text-xs leading-normal truncate flex-1 ${isActive ? "text-gold" : "text-primary-foreground/70"}`}>
                            {b.goal}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={(e) => onDeleteBrief(b.id, e)}
                          className="opacity-0 group-hover:opacity-100 text-primary-foreground/35 hover:text-red-400 p-1 transition-all cursor-pointer"
                        >
                          <X className="h-3 w-3" strokeWidth={2} />
                        </button>
                      </li>
                    );
                  })}
                </ul>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {briefs.length > 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4, duration: 0.5 }}
            className="mt-10 flex justify-center"
          >
            <span className="inline-flex items-center gap-2 text-[10px] font-mono uppercase tracking-[0.22em] text-primary-foreground/35">
              Brief below
              <ArrowDown className="h-3 w-3 animate-bounce" strokeWidth={1.75} />
            </span>
          </motion.div>
        )}
      </div>
    </section>
  );
}

function StreamingText({ text }: { text: string }) {
  const words = useMemo(() => text.split(" "), [text]);

  const container = {
    hidden: { opacity: 0 },
    visible: (i = 1) => ({
      opacity: 1,
      transition: { staggerChildren: 0.02, delayChildren: 0.05 * i },
    }),
  };

  const child = {
    visible: {
      opacity: 1,
      y: 0,
      filter: "blur(0px)",
      transition: {
        type: "spring",
        damping: 20,
        stiffness: 100,
      },
    },
    hidden: {
      opacity: 0,
      y: 6,
      filter: "blur(2px)",
    },
  };

  return (
    <motion.span
      variants={container}
      initial="hidden"
      animate="visible"
      className="inline-flex flex-wrap gap-x-1 gap-y-0.5"
    >
      {words.map((word, idx) => (
        <motion.span key={idx} variants={child} className="inline-block">
          {word}
        </motion.span>
      ))}
    </motion.span>
  );
}


function ComparisonCard({
  recommendation,
  isBest,
  enrolled,
  enrolling,
  onEnroll,
}: {
  recommendation: CourseRecommendation;
  isBest: boolean;
  enrolled: boolean;
  enrolling: boolean;
  onEnroll: () => void;
}) {
  const c = recommendation.course;
  const matchScore = recommendation.score > 1 
    ? Math.min(99, Math.round(55 + (recommendation.score / 40) * 43))
    : Math.round((recommendation.score ?? 0.92) * 100);

  return (
    <div className={`rounded-3xl p-6 lg:p-8 flex flex-col justify-between border relative overflow-hidden transition-all duration-300 hover:shadow-elevated ${
      isBest 
        ? "border-primary/25 bg-primary/4" 
        : "border-border bg-surface-elevated"
    }`}>
      {isBest && (
        <div className="absolute top-0 right-0 w-32 h-32 rounded-full bg-primary/5 blur-2xl pointer-events-none" />
      )}
      <div>
        <div className="flex items-center justify-between gap-3 mb-6">
          <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-[10px] font-mono uppercase tracking-wider font-semibold ${
            isBest ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
          }`}>
            {isBest ? "Best Match" : "Alternative Path"}
          </span>
          <div className="text-right">
            <span className="text-[9px] font-mono uppercase text-muted-foreground tracking-wider block">Fit score</span>
            <span className="text-xl font-bold text-foreground">{matchScore}%</span>
          </div>
        </div>

        <div className="aspect-video w-full rounded-xl overflow-hidden bg-muted mb-6 hairline">
          {c.imageUrl ? (
            <img src={c.imageUrl} alt={c.title} className="h-full w-full object-cover" />
          ) : (
            <div className="grid h-full place-items-center bg-gradient-to-br from-primary/15 to-primary-glow/15 text-primary">
              <BookOpen className="h-10 w-10 opacity-40" strokeWidth={1.5} />
            </div>
          )}
        </div>

        <h3 className="text-display text-2xl text-foreground font-semibold leading-tight mb-4">
          {c.title}
        </h3>

        <div className="space-y-4 mb-6">
          {/* Reason */}
          <div className="rounded-xl bg-surface/50 p-4 border border-border/40">
            <span className="text-[9px] font-mono uppercase text-muted-foreground tracking-wider block mb-1">
              Why recommend?
            </span>
            <p className="text-sm leading-relaxed text-foreground/85">
              {recommendation.reason}
            </p>
          </div>

          {/* Level / Language */}
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-xl border border-border/50 p-3 bg-surface/30">
              <span className="text-[9px] font-mono uppercase text-muted-foreground tracking-wider block">Level</span>
              <span className="text-sm font-medium text-foreground">{c.level.replace("_", " ")}</span>
            </div>
            <div className="rounded-xl border border-border/50 p-3 bg-surface/30">
              <span className="text-[9px] font-mono uppercase text-muted-foreground tracking-wider block">Language</span>
              <span className="text-sm font-medium text-foreground">{formatLanguage(c.languageCode)}</span>
            </div>
          </div>

          {/* Description */}
          {c.shortDescription && (
            <div className="pt-2 border-t border-border/40">
              <span className="text-[9px] font-mono uppercase text-muted-foreground tracking-wider block mb-1">Description</span>
              <p className="text-xs leading-relaxed text-muted-foreground">{c.shortDescription}</p>
            </div>
          )}
        </div>
      </div>

      <div className="mt-6 pt-6 border-t border-border/40 flex items-center justify-between gap-3">
        <Link
          to="/courses/$courseId"
          params={{ courseId: c.id }}
          className="text-xs font-semibold text-primary hover:underline flex items-center gap-1 cursor-pointer"
        >
          View Course Outline
          <ArrowUpRight className="h-3.5 w-3.5" />
        </Link>

        {enrolled ? (
          <span className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full bg-teal/15 text-teal text-xs font-semibold">
            <CheckCircle2 className="h-3.5 w-3.5" />
            Enrolled
          </span>
        ) : (
          <button
            type="button"
            onClick={onEnroll}
            disabled={enrolling}
            className="inline-flex h-9 items-center gap-1 rounded-full bg-foreground text-background px-4 text-xs font-semibold shadow transition-all duration-300 hover:scale-[1.02] cursor-pointer"
          >
            {enrolling ? "Enrolling..." : "Enroll"}
          </button>
        )}
      </div>
    </div>
  );
}

function BriefSpread({
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
  const [compareOpen, setCompareOpen] = useState(false);

  const handleCopyBrief = () => {
    try {
      const recommendationsText = brief.recommendations
        .map(
          (r, i) =>
            `${i + 1}. ${r.course.title} (${r.course.level}, ${formatLanguage(
              r.course.languageCode,
            )}) - Fit Score: ${Math.round(r.score > 1 ? 55 + (r.score / 40) * 43 : r.score * 100)}%\nReason: ${
              r.reason
            }`,
        )
        .join("\n\n");

      const clipboardText = `EduLife AI Career Advisor Brief\nGoal: "${brief.goal}"\nSummary: ${brief.message}\n\nRecommendations:\n${recommendationsText}`;

      navigator.clipboard.writeText(clipboardText);
      toast.success("Brief copied to clipboard!");
    } catch (e) {
      toast.error("Failed to copy brief.");
    }
  };

  const handlePrintBrief = () => {
    const styleEl = document.createElement("style");
    styleEl.innerHTML = `
      @media print {
        body * {
          visibility: hidden;
        }
        #print-area, #print-area * {
          visibility: visible;
        }
        #print-area {
          position: absolute;
          left: 0;
          top: 0;
          width: 100%;
          background: white !important;
          color: black !important;
        }
        .eyebrow, button {
          display: none !important;
        }
      }
    `;
    document.head.appendChild(styleEl);
    window.print();
    document.head.removeChild(styleEl);
  };

  if (brief.isLoading) {
    return <BriefSkeleton goal={brief.goal} />;
  }
  if (brief.isError) {
    return <BriefError goal={brief.goal} message={brief.message} onRetry={onRetry} />;
  }
  if (brief.recommendations.length === 0) {
    return <BriefEmpty goal={brief.goal} message={brief.message} />;
  }

  const best = brief.recommendations[0];
  const alts = brief.recommendations.slice(1);

  return (
    <motion.section
      id="print-area"
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.9, ease: [0.16, 1, 0.3, 1] }}
      className="relative bg-background py-16 lg:py-24"
    >
      <div className="mx-auto max-w-6xl px-6 lg:px-10">
        {/* Brief header strip */}
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between mb-12 lg:mb-16">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="eyebrow eyebrow-dot">
                Brief · {new Date().toLocaleDateString("en-US", { day: "2-digit", month: "short" })}
              </span>
              {alts.length > 0 && (
                <button
                  type="button"
                  onClick={() => setCompareOpen(!compareOpen)}
                  className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[10px] font-mono uppercase tracking-wider font-semibold border transition-all duration-300 cursor-pointer ${
                    compareOpen 
                      ? "bg-gold text-gold-foreground border-gold" 
                      : "border-border hover:border-foreground/30 hover:bg-surface text-muted-foreground hover:text-foreground"
                  }`}
                >
                  <Layers className="h-3 w-3" />
                  {compareOpen ? "Editorial View" : "Compare Side-by-Side"}
                </button>
              )}
              <button
                type="button"
                onClick={handleCopyBrief}
                className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[10px] font-mono uppercase tracking-wider font-semibold border border-border hover:border-foreground/30 hover:bg-surface text-muted-foreground hover:text-foreground transition-all duration-300 cursor-pointer"
              >
                <Copy className="h-3 w-3" />
                Copy text
              </button>
              <button
                type="button"
                onClick={handlePrintBrief}
                className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[10px] font-mono uppercase tracking-wider font-semibold border border-border hover:border-foreground/30 hover:bg-surface text-muted-foreground hover:text-foreground transition-all duration-300 cursor-pointer"
              >
                <Printer className="h-3 w-3" />
                Print / PDF
              </button>
            </div>
            <p className="mt-4 text-[10px] font-mono uppercase tracking-[0.22em] text-muted-foreground">
              You asked
            </p>
            <p className="mt-2 text-display text-2xl lg:text-3xl leading-snug text-foreground max-w-[34ch] italic">
              &ldquo;{brief.goal}&rdquo;
            </p>
          </div>
          <div className="lg:max-w-[36ch] lg:text-right">
            <p className="text-[10px] font-mono uppercase tracking-[0.22em] text-muted-foreground">
              Advisor summary
            </p>
            <p className="mt-2 text-sm leading-relaxed text-foreground/80">
              <StreamingText text={brief.message} />
            </p>
          </div>
        </div>

        <AnimatePresence mode="wait">
          {compareOpen ? (
            <motion.div
              key="comparison"
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -15 }}
              transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
              className="grid md:grid-cols-2 gap-8 items-stretch"
            >
              <ComparisonCard
                recommendation={best}
                isBest
                enrolled={enrolledCourseIds.has(best.course.id)}
                enrolling={enrolling}
                onEnroll={() => onEnroll(best.course.id)}
              />
              <ComparisonCard
                recommendation={alts[0]}
                isBest={false}
                enrolled={enrolledCourseIds.has(alts[0].course.id)}
                enrolling={enrolling}
                onEnroll={() => onEnroll(alts[0].course.id)}
              />
            </motion.div>
          ) : (
            <motion.div
              key="editorial"
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -15 }}
              transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
              className="space-y-20"
            >
              <BestMatchSpread
                recommendation={best}
                enrolled={enrolledCourseIds.has(best.course.id)}
                enrolling={enrolling}
                onEnroll={() => onEnroll(best.course.id)}
              />

              {alts.length > 0 && (
                <div className="mt-20 lg:mt-28">
                  <div className="flex items-end justify-between mb-8">
                    <div>
                      <span className="text-[10px] font-mono uppercase tracking-[0.22em] text-muted-foreground">
                        /02 · Alternative path
                      </span>
                      <h3 className="mt-3 text-display text-3xl lg:text-4xl text-foreground leading-[1.05] max-w-[20ch]">
                        If the first brief doesn&apos;t fit.
                      </h3>
                    </div>
                  </div>
                  <div className="grid gap-5 md:grid-cols-2">
                    {alts.map((rec, i) => (
                      <AltMatchCard
                        key={rec.course.id}
                        recommendation={rec}
                        rank={i + 1}
                        enrolled={enrolledCourseIds.has(rec.course.id)}
                        enrolling={enrolling}
                        onEnroll={() => onEnroll(rec.course.id)}
                      />
                    ))}
                  </div>
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.section>
  );
}

function BestMatchSpread({
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
  const c = recommendation.course;
  const matchScore = recommendation.score > 1 
    ? Math.min(99, Math.round(55 + (recommendation.score / 40) * 43))
    : Math.round((recommendation.score ?? 0.92) * 100);

  return (
    <article className="relative">
      <div className="grid lg:grid-cols-12 gap-8 lg:gap-10">
        {/* Left: Course image as editorial hero */}
        <div className="lg:col-span-5">
          <div className="bezel sticky top-24">
            <div className="bezel-inner overflow-hidden">
              <div className="relative aspect-[4/5] bg-muted">
                {c.imageUrl ? (
                  <img
                    src={c.imageUrl}
                    alt={c.title}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="grid h-full place-items-center bg-gradient-to-br from-primary via-primary to-primary-glow text-primary-foreground">
                    <BookOpen className="h-16 w-16 opacity-50" strokeWidth={1.25} />
                  </div>
                )}
                {/* Overlay stamp */}
                <div className="absolute top-5 left-5 right-5 flex items-start justify-between">
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-gold text-gold-foreground px-3 py-1.5 text-[10px] font-mono uppercase tracking-[0.22em] shadow-bezel">
                    <Sparkles className="h-3 w-3" strokeWidth={1.75} />
                    Best match
                  </span>
                  <div className="text-right">
                    <p className="text-[9px] font-mono uppercase tracking-[0.22em] text-white/70 drop-shadow">
                      Fit
                    </p>
                    <p className="text-display text-3xl text-white leading-none drop-shadow">
                      {matchScore}
                      <span className="text-base ml-0.5">%</span>
                    </p>
                  </div>
                </div>
                {/* Bottom gradient */}
                <div className="absolute inset-x-0 bottom-0 h-32 bg-gradient-to-t from-black/60 to-transparent" />
                <div className="absolute bottom-5 left-5 right-5 flex items-center gap-2 text-[10px] font-mono uppercase tracking-[0.22em] text-white/85">
                  <span className="rounded-full bg-white/15 backdrop-blur-sm border border-white/20 px-2.5 py-1">
                    {c.level.replace("_", " ")}
                  </span>
                  <span className="rounded-full bg-white/15 backdrop-blur-sm border border-white/20 px-2.5 py-1">
                    {formatLanguage(c.languageCode)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right: Editorial body */}
        <div className="lg:col-span-7 flex flex-col">
          <span className="text-[10px] font-mono uppercase tracking-[0.22em] text-primary">
            /01 · Recommended course
          </span>

          <h2 className="mt-4 text-display text-[clamp(2.25rem,5vw,4rem)] leading-[0.95] tracking-tight text-foreground">
            {c.title}
          </h2>

          {/* Drop-cap intro */}
          <div className="mt-8 relative">
            <p className="text-[10px] font-mono uppercase tracking-[0.22em] text-muted-foreground">
              Match reasoning
            </p>
            <div className="mt-3 rounded-[1.25rem] hairline bg-surface p-6 lg:p-7 relative overflow-hidden">
              <div className="pointer-events-none absolute -top-12 -right-8 h-32 w-32 rounded-full bg-primary/8 blur-2xl" />
              <p className="relative text-lg lg:text-xl leading-relaxed text-foreground/90">
                <span className="float-left text-display text-6xl lg:text-7xl leading-[0.85] mr-3 mt-1 text-gradient-primary">
                  {recommendation.reason.charAt(0)}
                </span>
                <StreamingText text={recommendation.reason.slice(1)} />
              </p>
            </div>
          </div>

          {/* Course meta grid */}
          <div className="mt-8 grid grid-cols-3 gap-3">
            <MetaTile label="Level" value={c.level.replace("_", " ")} Icon={Layers} />
            <MetaTile label="Language" value={formatLanguage(c.languageCode)} Icon={Globe2} />
            <MetaTile
              label="Fit score"
              value={`${matchScore}%`}
              Icon={Sparkles}
              accent
            />
          </div>

          {/* Description if exists */}
          {c.shortDescription && (
            <div className="mt-8 pt-8 border-t border-border/60">
              <p className="text-[10px] font-mono uppercase tracking-[0.22em] text-muted-foreground">
                What it covers
              </p>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground max-w-[60ch]">
                {c.shortDescription}
              </p>
            </div>
          )}

          {/* CTA row */}
          <div className="mt-10 flex flex-wrap items-center gap-3">
            {enrolled ? (
              <Link
                to="/courses"
                className="group inline-flex h-12 items-center gap-1.5 rounded-full bg-teal/15 hairline border-teal/30 pl-5 pr-1 text-sm font-medium text-foreground"
              >
                <span>Already enrolled · open</span>
                <span className="grid h-10 w-10 place-items-center rounded-full bg-foreground/8 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-x-0.5 group-hover:-translate-y-px">
                  <CheckCircle2 className="h-3.5 w-3.5" strokeWidth={1.75} />
                </span>
              </Link>
            ) : (
              <button
                type="button"
                onClick={onEnroll}
                disabled={enrolling}
                className="group relative inline-flex h-12 items-center gap-1.5 rounded-full bg-foreground text-background pl-6 pr-1.5 text-sm font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98] disabled:opacity-60 disabled:pointer-events-none"
              >
                <span>{enrolling ? "Enrolling…" : "Enroll in this course"}</span>
                <span className="grid h-9 w-9 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-x-0.5 group-hover:-translate-y-px group-hover:bg-background/25">
                  <ArrowUpRight className="h-4 w-4" strokeWidth={1.75} />
                </span>
              </button>
            )}
            <Link
              to="/courses/$courseId"
              params={{ courseId: c.id }}
              className="group inline-flex h-12 items-center gap-1.5 rounded-full px-5 text-sm font-medium text-foreground/85 hover:text-foreground transition-colors"
            >
              <span className="relative">
                Open course outline
                <span className="absolute -bottom-0.5 left-0 h-px w-0 bg-foreground transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:w-full" />
              </span>
              <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
            </Link>
          </div>
        </div>
      </div>
    </article>
  );
}

function MetaTile({
  label,
  value,
  Icon,
  accent,
}: {
  label: string;
  value: string;
  Icon: typeof Layers;
  accent?: boolean;
}) {
  return (
    <div
      className={`relative overflow-hidden rounded-2xl p-4 ${
        accent
          ? "bg-gradient-to-br from-primary via-primary to-primary-glow text-primary-foreground shadow-bezel"
          : "hairline bg-surface"
      }`}
    >
      <div className="flex items-center justify-between">
        <span
          className={`text-[10px] font-mono uppercase tracking-[0.2em] ${
            accent ? "text-primary-foreground/65" : "text-muted-foreground"
          }`}
        >
          {label}
        </span>
        <Icon
          className={`h-3.5 w-3.5 ${accent ? "text-primary-foreground/75" : "text-primary/70"}`}
          strokeWidth={1.5}
        />
      </div>
      <p className={`mt-3 text-display text-xl leading-none ${accent ? "" : "text-foreground"}`}>
        {value}
      </p>
    </div>
  );
}

function AltMatchCard({
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
  const c = recommendation.course;
  return (
    <motion.article
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.1 * rank, ease: [0.16, 1, 0.3, 1] }}
      className="group relative flex flex-col overflow-hidden rounded-[1.5rem] hairline bg-surface-elevated transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:shadow-elevated"
    >
      <div className="relative aspect-[16/9] overflow-hidden bg-muted">
        {c.imageUrl ? (
          <img
            src={c.imageUrl}
            alt={c.title}
            className="h-full w-full object-cover transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.04]"
          />
        ) : (
          <div className="grid h-full place-items-center bg-gradient-to-br from-primary/10 to-primary-glow/10 text-primary">
            <BookOpen className="h-9 w-9 opacity-60" strokeWidth={1.25} />
          </div>
        )}
        <span className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-background/85 hairline px-2.5 py-1 text-[10px] font-mono uppercase tracking-[0.18em] text-foreground backdrop-blur-sm">
          Alt {String(rank).padStart(2, "0")}
        </span>
      </div>

      <div className="flex flex-col flex-1 p-6">
        <div className="flex flex-wrap gap-2 text-[10px] font-mono uppercase tracking-[0.18em]">
          <span className="rounded-full bg-primary/8 hairline px-2.5 py-0.5 text-primary">
            {c.level.replace("_", " ")}
          </span>
          <span className="rounded-full bg-surface hairline px-2.5 py-0.5 text-muted-foreground">
            {formatLanguage(c.languageCode)}
          </span>
        </div>

        <h4 className="mt-3 text-display text-xl leading-snug text-foreground">{c.title}</h4>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground line-clamp-3 max-w-[44ch]">
          {recommendation.reason}
        </p>

        <div className="mt-auto pt-6 flex flex-wrap items-center gap-2">
          <Link
            to="/courses/$courseId"
            params={{ courseId: c.id }}
            className="group/btn inline-flex items-center gap-1.5 rounded-full hairline bg-surface px-3.5 py-2 text-xs font-medium text-foreground/85 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:text-foreground hover:-translate-y-0.5"
          >
            Outline
            <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
          </Link>
          {enrolled ? (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-teal/15 hairline border-teal/30 px-3.5 py-2 text-xs font-medium text-foreground/85">
              <CheckCircle2 className="h-3 w-3" strokeWidth={1.75} />
              Enrolled
            </span>
          ) : (
            <button
              type="button"
              onClick={onEnroll}
              disabled={enrolling}
              className="group/btn relative inline-flex items-center gap-1 rounded-full bg-foreground text-background pl-3.5 pr-1 py-1 h-9 text-xs font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98] disabled:opacity-60 disabled:pointer-events-none"
            >
              <span>Enroll</span>
              <span className="grid h-7 w-7 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover/btn:translate-x-0.5 group-hover/btn:-translate-y-px group-hover/btn:bg-background/25">
                <ArrowUpRight className="h-3 w-3" strokeWidth={1.75} />
              </span>
            </button>
          )}
        </div>
      </div>
    </motion.article>
  );
}

function BriefSkeleton({ goal }: { goal: string }) {
  return (
    <section className="bg-background py-16 lg:py-24">
      <div className="mx-auto max-w-6xl px-6 lg:px-10">
        <div className="flex items-center gap-3 mb-10">
          <span className="text-[10px] font-mono uppercase tracking-[0.22em] text-primary">
            Drafting brief
          </span>
          <span className="flex gap-1">
            <span className="h-1.5 w-1.5 rounded-full bg-primary animate-bounce" style={{ animationDelay: "0ms" }} />
            <span className="h-1.5 w-1.5 rounded-full bg-primary animate-bounce" style={{ animationDelay: "150ms" }} />
            <span className="h-1.5 w-1.5 rounded-full bg-primary animate-bounce" style={{ animationDelay: "300ms" }} />
          </span>
        </div>
        <p className="text-[10px] font-mono uppercase tracking-[0.22em] text-muted-foreground">
          For
        </p>
        <p className="mt-2 text-display text-2xl lg:text-3xl text-foreground/60 italic max-w-[40ch]">
          &ldquo;{goal}&rdquo;
        </p>

        <div className="mt-12 grid lg:grid-cols-12 gap-8">
          <div className="lg:col-span-5">
            <div className="bezel">
              <div className="bezel-inner aspect-[4/5] bg-muted/50 animate-pulse" />
            </div>
          </div>
          <div className="lg:col-span-7 space-y-5">
            <div className="h-4 w-32 rounded-full bg-muted animate-pulse" />
            <div className="h-14 w-full rounded-2xl bg-muted/60 animate-pulse" />
            <div className="h-14 w-3/4 rounded-2xl bg-muted/60 animate-pulse" />
            <div className="h-32 w-full rounded-2xl bg-muted/40 animate-pulse" />
            <div className="grid grid-cols-3 gap-3">
              <div className="h-20 rounded-2xl bg-muted/40 animate-pulse" />
              <div className="h-20 rounded-2xl bg-muted/40 animate-pulse" />
              <div className="h-20 rounded-2xl bg-muted/40 animate-pulse" />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function BriefError({
  goal,
  message,
  onRetry,
}: {
  goal: string;
  message: string;
  onRetry: () => void;
}) {
  return (
    <section className="bg-background py-16 lg:py-24">
      <div className="mx-auto max-w-3xl px-6 lg:px-10 text-center">
        <span className="eyebrow eyebrow-dot">Brief failed</span>
        <h2 className="mt-6 text-display text-3xl lg:text-4xl text-foreground leading-tight">
          The catalog didn&apos;t answer.
        </h2>
        <p className="mt-4 text-sm text-muted-foreground italic">&ldquo;{goal}&rdquo;</p>
        <p className="mt-6 text-base text-muted-foreground leading-relaxed">{message}</p>
        <button
          type="button"
          onClick={onRetry}
          className="group mt-8 inline-flex h-11 items-center gap-1.5 rounded-full bg-foreground text-background pl-5 pr-1 text-sm font-medium shadow-bezel transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98]"
        >
          <span>Try again</span>
          <span className="grid h-9 w-9 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:rotate-180">
            <RefreshCw className="h-3.5 w-3.5" strokeWidth={1.75} />
          </span>
        </button>
      </div>
    </section>
  );
}

function BriefEmpty({ goal, message }: { goal: string; message: string }) {
  return (
    <section className="bg-background py-16 lg:py-24">
      <div className="mx-auto max-w-3xl px-6 lg:px-10 text-center">
        <span className="eyebrow eyebrow-dot">No match</span>
        <h2 className="mt-6 text-display text-3xl lg:text-4xl text-foreground leading-tight">
          The catalog doesn&apos;t have a clear path for this yet.
        </h2>
        <p className="mt-4 text-sm text-muted-foreground italic">&ldquo;{goal}&rdquo;</p>
        <p className="mt-6 text-base text-muted-foreground leading-relaxed max-w-[50ch] mx-auto">
          {message || "Try a sharper goal — name the destination, your current level, and the language you want to study in."}
        </p>
      </div>
    </section>
  );
}

function EmptyAdvisorState({ catalogCount }: { catalogCount: number }) {
  return (
    <section className="relative bg-background py-20 lg:py-28">
      <div className="mx-auto max-w-5xl px-6 lg:px-10">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          <div>
            <span className="eyebrow eyebrow-dot">Brief preview</span>
            <h2 className="mt-6 text-display text-4xl lg:text-5xl leading-[1.02] text-foreground max-w-[14ch]">
              Every brief is{" "}
              <span className="italic font-normal text-muted-foreground">grounded.</span>
            </h2>
            <p className="mt-6 text-base text-muted-foreground leading-relaxed max-w-[48ch]">
              The advisor only recommends courses that exist in the live catalog —{" "}
              {catalogCount} indexed right now. No hallucinated learning paths, no
              fabricated certificates.
            </p>

            <ul className="mt-10 space-y-4">
              {[
                ["Says what to take", "Names one best-fit course, not a list of links."],
                ["Says why", "Editorial reasoning, grounded in your stated goal."],
                ["Shows the path", "Level, language, and fit score on every recommendation."],
              ].map(([k, v]) => (
                <li key={k} className="flex items-start gap-3">
                  <span className="mt-1 grid h-6 w-6 shrink-0 place-items-center rounded-full bg-primary/10 text-primary">
                    <CheckCircle2 className="h-3 w-3" strokeWidth={2} />
                  </span>
                  <div>
                    <p className="text-sm font-medium text-foreground">{k}</p>
                    <p className="mt-1 text-[13px] text-muted-foreground leading-relaxed">{v}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <div className="relative">
            <div className="bezel">
              <div className="bezel-inner p-8 lg:p-10">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-mono uppercase tracking-[0.22em] text-muted-foreground">
                    Example brief
                  </span>
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-gold text-gold-foreground px-2.5 py-1 text-[10px] font-mono uppercase tracking-[0.22em] shadow-bezel">
                    <Sparkles className="h-2.5 w-2.5" strokeWidth={1.75} />
                    96%
                  </span>
                </div>
                <h3 className="mt-6 text-display text-[clamp(1.5rem,3vw,2.25rem)] leading-[0.98] text-foreground">
                  Full-Stack Web Development Path
                </h3>
                <p className="mt-5 text-sm leading-relaxed text-muted-foreground">
                  <span className="text-display text-3xl leading-[0.85] mr-1 float-left mt-1 text-gradient-primary">
                    A
                  </span>
                  structured 12-week ramp from HTML/CSS to a deployed React + Node project.
                  Matches the &ldquo;software developer&rdquo; goal exactly.
                </p>
                <div className="mt-6 grid grid-cols-3 gap-2 text-[10px] font-mono uppercase tracking-[0.18em]">
                  <span className="rounded-full hairline bg-surface text-center text-muted-foreground px-2 py-1">
                    Intermediate
                  </span>
                  <span className="rounded-full hairline bg-surface text-center text-muted-foreground px-2 py-1">
                    English
                  </span>
                  <span className="rounded-full bg-primary/10 hairline text-center text-primary px-2 py-1">
                    Fit 96%
                  </span>
                </div>
              </div>
            </div>
            <div className="pointer-events-none absolute -bottom-6 -right-6 -z-10 h-32 w-32 rounded-full bg-gold/20 blur-3xl" />
            <div className="pointer-events-none absolute -top-6 -left-6 -z-10 h-32 w-32 rounded-full bg-primary/20 blur-3xl" />
          </div>
        </div>

        <div className="mt-16 lg:mt-20 flex justify-center">
          <Link
            to="/courses"
            className="group inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <span className="relative">
              Already enrolled? Open my courses
              <span className="absolute -bottom-0.5 left-0 h-px w-0 bg-foreground transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:w-full" />
            </span>
            <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
          </Link>
        </div>
      </div>
    </section>
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
