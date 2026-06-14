import { useState, useEffect, useMemo } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import {
  CalendarDays,
  Plus,
  Minus,
  Trash2,
  Clock,
  RotateCcw,
  BookOpen,
  CheckCircle2,
  AlertCircle,
  Sparkles,
} from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import { listMyEnrollments } from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

export const Route = createFileRoute("/planner")({
  component: PlannerRoute,
  head: () => ({ meta: [{ title: "Study Planner - EduLife" }] }),
});

interface PlannerTask {
  id: string;
  title: string;
  completed: boolean;
}

const DAYS = [
  { key: "Monday", label: "M" },
  { key: "Tuesday", label: "T" },
  { key: "Wednesday", label: "W" },
  { key: "Thursday", label: "T" },
  { key: "Friday", label: "F" },
  { key: "Saturday", label: "S" },
  { key: "Sunday", label: "S" },
];

function PlannerRoute() {
  return (
    <RequireAuth>
      <PlannerPage />
    </RequireAuth>
  );
}

function PlannerPage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const isClient = typeof window !== "undefined";

  // ── Local Storage State Init ──

  const [goal, setGoal] = useState(() => (isClient ? localStorage.getItem("edulife_planner_goal") || "" : ""));
  const [targetHours, setTargetHours] = useState(() => {
    if (!isClient) return 10;
    const val = localStorage.getItem("edulife_planner_target_hours");
    return val ? parseInt(val, 10) : 10;
  });
  const [completedHours, setCompletedHours] = useState(() => {
    if (!isClient) return 0;
    const val = localStorage.getItem("edulife_planner_completed_hours");
    return val ? parseFloat(val) : 0;
  });
  const [studyDays, setStudyDays] = useState<string[]>(() => {
    if (!isClient) return ["Monday", "Wednesday", "Friday"];
    const val = localStorage.getItem("edulife_planner_study_days");
    return val ? JSON.parse(val) : ["Monday", "Wednesday", "Friday"];
  });
  const [focusCourses, setFocusCourses] = useState<string[]>(() => {
    if (!isClient) return [];
    const val = localStorage.getItem("edulife_planner_focus_courses");
    return val ? JSON.parse(val) : [];
  });
  const [tasks, setTasks] = useState<PlannerTask[]>(() => {
    if (!isClient) return [];
    const val = localStorage.getItem("edulife_planner_tasks");
    return val ? JSON.parse(val) : [];
  });

  const [newTaskText, setNewTaskText] = useState("");
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  // ── Local Storage Autosave Effect ──

  useEffect(() => {
    if (isClient) localStorage.setItem("edulife_planner_goal", goal);
  }, [goal, isClient]);

  useEffect(() => {
    if (isClient) localStorage.setItem("edulife_planner_target_hours", String(targetHours));
  }, [targetHours, isClient]);

  useEffect(() => {
    if (isClient) localStorage.setItem("edulife_planner_completed_hours", String(completedHours));
  }, [completedHours, isClient]);

  useEffect(() => {
    if (isClient) localStorage.setItem("edulife_planner_study_days", JSON.stringify(studyDays));
  }, [studyDays, isClient]);

  useEffect(() => {
    if (isClient) localStorage.setItem("edulife_planner_focus_courses", JSON.stringify(focusCourses));
  }, [focusCourses, isClient]);

  useEffect(() => {
    if (isClient) localStorage.setItem("edulife_planner_tasks", JSON.stringify(tasks));
  }, [tasks, isClient]);

  // ── Backend Enrolled Courses Query ──

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const enrollments = enrollmentsQuery.data ?? [];

  // ── Operations ──

  const progressPercent = useMemo(() => {
    if (targetHours === 0) return 0;
    return Math.min(Math.round((completedHours / targetHours) * 100), 100);
  }, [completedHours, targetHours]);

  function handleLogHours(amount: number) {
    setCompletedHours((prev) => {
      const next = prev + amount;
      return Math.min(Math.max(0, next), targetHours * 2);
    });
  }

  function handleToggleDay(day: string) {
    setStudyDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    );
  }

  function handleToggleCourse(courseId: string) {
    setFocusCourses((prev) =>
      prev.includes(courseId) ? prev.filter((id) => id !== courseId) : [...prev, courseId]
    );
  }

  function handleAddTask(e?: React.FormEvent) {
    if (e) e.preventDefault();
    const trimmed = newTaskText.trim();
    if (!trimmed) return;

    if (tasks.length >= 10) {
      alert("Checklist limited to 10 active tasks to keep your study plan focused.");
      return;
    }

    const newTask: PlannerTask = {
      id: crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2),
      title: trimmed,
      completed: false,
    };

    setTasks((prev) => [...prev, newTask]);
    setNewTaskText("");
  }

  function handleToggleTask(taskId: string) {
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId ? { ...t, completed: !t.completed } : t))
    );
  }

  function handleDeleteTask(taskId: string) {
    setTasks((prev) => prev.filter((t) => t.id !== taskId));
  }

  function handleResetWeek() {
    setCompletedHours(0);
    // Keep uncompleted tasks, clear completed ones
    setTasks((prev) => prev.filter((t) => !t.completed));
    setShowResetConfirm(false);
  }

  return (
    <AppShell
      active="planner"
      user={{
        displayName: auth.session?.displayName ?? "EduLife learner",
        email: auth.session?.email ?? "",
      }}
      onLogout={auth.logout}
      header={
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">Study Planner</p>
          <p className="text-xs text-muted-foreground">
            Plan your weekly study routine and track your daily efforts.
          </p>
        </div>
      }
    >
      <div className="mx-auto max-w-5xl px-2 py-2 space-y-6">
        {/* Header Hero Section */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-3xl bg-gradient-to-br from-primary to-primary-glow px-6 py-7 text-primary-foreground shadow-elevated flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
        >
          <div>
            <p className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs uppercase tracking-[0.16em]">
              <CalendarDays className="h-3.5 w-3.5" />
              Weekly Study Cycle
            </p>
            <h1 className="mt-4 text-display text-3xl font-bold">Study Planner</h1>
            <p className="mt-1.5 max-w-xl text-xs text-primary-foreground/75 leading-relaxed">
              Design a minimalist study targets system. Set your target hours, configure your focus days, and check off learning items.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setShowResetConfirm(true)}
            className="inline-flex shrink-0 items-center justify-center gap-2 rounded-full bg-white px-5 py-2.5 text-xs font-semibold text-primary shadow-sm hover:scale-[1.02] active:scale-[0.98] transition-transform cursor-pointer"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            Start New Week
          </button>
        </motion.div>

        <div className="grid gap-6 lg:grid-cols-[1.3fr_0.9fr]">
          {/* LEFT COLUMN: Controls and Goal Settings */}
          <div className="space-y-6">
            {/* Card 1: Time Logging & Progress */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              className="rounded-3xl border border-border/80 bg-surface-elevated p-6 shadow-soft glass relative overflow-hidden"
            >
              <div className="absolute top-[-10%] right-[-10%] h-36 w-36 rounded-full bg-primary/5 blur-3xl pointer-events-none" />

              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-2">
                <Clock className="h-4 w-4 text-primary" />
                Time Tracker progress
              </h2>

              <div className="mt-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <p className="text-2xl font-bold text-foreground">
                    {completedHours.toFixed(1)} / {targetHours} hours
                  </p>
                  <p className="text-xs text-muted-foreground mt-1">
                    Completed study hours target this week
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => handleLogHours(0.5)}
                    className="rounded-full border border-primary/20 bg-primary/5 hover:bg-primary/10 px-4 py-2 text-xs font-bold text-primary transition-colors cursor-pointer"
                  >
                    +30 mins
                  </button>
                  <button
                    type="button"
                    onClick={() => handleLogHours(1.0)}
                    className="rounded-full border border-primary/20 bg-primary/5 hover:bg-primary/10 px-4 py-2 text-xs font-bold text-primary transition-colors cursor-pointer"
                  >
                    +1 hour
                  </button>
                  {completedHours > 0 && (
                    <button
                      type="button"
                      onClick={() => setCompletedHours(0)}
                      className="rounded-full border border-border bg-background hover:bg-accent px-4 py-2 text-xs font-bold text-muted-foreground transition-colors cursor-pointer"
                    >
                      Clear
                    </button>
                  )}
                </div>
              </div>

              {/* Progress bar */}
              <div className="mt-5">
                <div className="flex items-center justify-between text-xs text-muted-foreground mb-2">
                  <span>Target Progress</span>
                  <span className="font-semibold text-foreground">{progressPercent}%</span>
                </div>
                <div className="h-3.5 w-full rounded-full bg-muted/60 dark:bg-muted/10 overflow-hidden border border-border/40">
                  <div
                    className="h-full rounded-full bg-gradient-primary transition-all duration-500"
                    style={{ width: `${progressPercent}%` }}
                  />
                </div>
              </div>
            </motion.div>

            {/* Card 2: Motivation Goal */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="rounded-3xl border border-border/80 bg-surface-elevated p-6 shadow-soft glass"
            >
              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-amber-500" />
                My Focus This Week
              </h2>
              <p className="text-xs text-muted-foreground mt-1">
                Define one main priority to guide your choices.
              </p>

              <input
                type="text"
                value={goal}
                onChange={(e) => setGoal(e.target.value)}
                placeholder="Example: Prepare for Baccalaureate English test, study React basics..."
                className="mt-4 w-full rounded-xl border border-border/80 bg-background px-4 py-3 text-sm leading-relaxed text-foreground outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/10"
              />
            </motion.div>

            {/* Card 3: Hour and Day Configurations */}
            <div className="grid gap-6 md:grid-cols-2">
              {/* Hour Target */}
              <motion.div
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="rounded-3xl border border-border/80 bg-surface-elevated p-6 shadow-soft glass flex flex-col justify-between"
              >
                <div>
                  <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                    Weekly target
                  </h3>
                  <p className="text-xs text-muted-foreground mt-1">
                    Select how many hours you plan to study.
                  </p>
                </div>

                <div className="mt-5 flex items-center justify-center gap-4">
                  <button
                    type="button"
                    onClick={() => setTargetHours((prev) => Math.max(1, prev - 1))}
                    className="grid h-10 w-10 place-items-center rounded-xl border border-border bg-background hover:bg-accent text-foreground transition-colors cursor-pointer"
                  >
                    <Minus className="h-4.5 w-4.5" />
                  </button>
                  <span className="text-xl font-bold text-foreground min-w-[80px] text-center">
                    {targetHours}h
                  </span>
                  <button
                    type="button"
                    onClick={() => setTargetHours((prev) => Math.min(40, prev + 1))}
                    className="grid h-10 w-10 place-items-center rounded-xl border border-border bg-background hover:bg-accent text-foreground transition-colors cursor-pointer"
                  >
                    <Plus className="h-4.5 w-4.5" />
                  </button>
                </div>
              </motion.div>

              {/* Study Days */}
              <motion.div
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.25 }}
                className="rounded-3xl border border-border/80 bg-surface-elevated p-6 shadow-soft glass"
              >
                <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                  Planned study days
                </h3>
                <p className="text-xs text-muted-foreground mt-1">
                  Select which days you want to active study.
                </p>

                <div className="mt-5 flex justify-between gap-1">
                  {DAYS.map(({ key, label }) => {
                    const isSelected = studyDays.includes(key);
                    return (
                      <button
                        key={key}
                        type="button"
                        onClick={() => handleToggleDay(key)}
                        className={`grid h-8 w-8 place-items-center rounded-full text-xs font-bold transition-all cursor-pointer ${
                          isSelected
                            ? "bg-primary text-primary-foreground shadow-sm scale-110"
                            : "border border-border bg-background text-muted-foreground hover:border-primary/40"
                        }`}
                      >
                        {label}
                      </button>
                    );
                  })}
                </div>
              </motion.div>
            </div>
          </div>

          {/* RIGHT COLUMN: Checklist and Focus Subjects */}
          <div className="space-y-6">
            {/* Card 4: Focus Courses checkboxes */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15 }}
              className="rounded-3xl border border-border/80 bg-surface-elevated p-6 shadow-soft glass"
            >
              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-primary" />
                Active Focus courses
              </h2>
              <p className="text-xs text-muted-foreground mt-1">
                Select your enrolled courses you want to study this week.
              </p>

              <div className="mt-4 space-y-2 max-h-[160px] overflow-y-auto pr-1">
                {enrollmentsQuery.isLoading ? (
                  <p className="text-xs text-muted-foreground py-2">Loading your enrollments...</p>
                ) : enrollments.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-border p-4 text-center">
                    <p className="text-xs text-muted-foreground">
                      No active enrollments found. Enroll in courses first to select focus items.
                    </p>
                  </div>
                ) : (
                  enrollments.map((course) => {
                    const isChecked = focusCourses.includes(course.courseId);
                    return (
                      <label
                        key={course.courseId}
                        className={`flex items-center gap-3 rounded-xl border p-3 text-sm cursor-pointer select-none transition-colors ${
                          isChecked
                            ? "border-primary/30 bg-primary/4 text-foreground font-semibold"
                            : "border-border/60 hover:bg-accent/40 text-muted-foreground"
                        }`}
                      >
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleToggleCourse(course.courseId)}
                          className="h-4 w-4 rounded border-border text-primary focus:ring-primary/20 accent-primary"
                        />
                        <span className="truncate">{course.title}</span>
                      </label>
                    );
                  })
                )}
              </div>
            </motion.div>

            {/* Card 5: Task Checklist */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="rounded-3xl border border-border/80 bg-surface-elevated p-6 shadow-soft glass flex flex-col min-h-[350px] justify-between"
            >
              <div>
                <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-teal-600" />
                  Study Tasks checklist
                </h2>
                <p className="text-xs text-muted-foreground mt-1">
                  Add specific items like "Read Chapter 1" or "Take mock exam".
                </p>

                {/* Task list container */}
                <div className="mt-4 space-y-2 max-h-[220px] overflow-y-auto pr-1">
                  <AnimatePresence initial={false}>
                    {tasks.length === 0 ? (
                      <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="text-center py-8"
                      >
                        <AlertCircle className="h-8 w-8 text-muted-foreground/35 mx-auto mb-2" />
                        <p className="text-xs text-muted-foreground">
                          No tasks planned yet. Add one below to start!
                        </p>
                      </motion.div>
                    ) : (
                      tasks.map((task) => (
                        <motion.div
                          key={task.id}
                          initial={{ opacity: 0, y: -8 }}
                          animate={{ opacity: 1, y: 0 }}
                          exit={{ opacity: 0, scale: 0.95 }}
                          transition={{ duration: 0.2 }}
                          className="flex items-center justify-between gap-3 rounded-xl border border-border/40 bg-background/50 p-3 hover:bg-background/95 transition-colors group"
                        >
                          <label className="flex items-center gap-3 cursor-pointer flex-1 min-w-0">
                            <input
                              type="checkbox"
                              checked={task.completed}
                              onChange={() => handleToggleTask(task.id)}
                              className="h-4 w-4 rounded border-border text-primary focus:ring-primary/20 accent-primary"
                            />
                            <span
                              className={`text-sm truncate leading-relaxed ${
                                task.completed
                                  ? "line-through text-muted-foreground opacity-60"
                                  : "text-foreground font-medium"
                              }`}
                            >
                              {task.title}
                            </span>
                          </label>
                          <button
                            type="button"
                            onClick={() => handleDeleteTask(task.id)}
                            className="text-muted-foreground hover:text-destructive p-1 rounded-lg hover:bg-destructive/10 transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100 cursor-pointer"
                            aria-label="Delete task"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </motion.div>
                      ))
                    )}
                  </AnimatePresence>
                </div>
              </div>

              {/* Add task form at bottom */}
              <form onSubmit={handleAddTask} className="mt-6 flex items-center gap-2">
                <input
                  type="text"
                  value={newTaskText}
                  onChange={(e) => setNewTaskText(e.target.value)}
                  placeholder="Finish section 1 outline..."
                  maxLength={60}
                  className="flex-1 rounded-xl border border-border/80 bg-background px-3 py-2 text-sm leading-relaxed text-foreground outline-none transition-all placeholder:text-muted-foreground/45 focus:border-primary"
                />
                <button
                  type="submit"
                  disabled={!newTaskText.trim()}
                  className="inline-flex shrink-0 items-center justify-center rounded-xl bg-foreground px-4 py-2.5 text-xs font-bold text-background disabled:opacity-30 disabled:pointer-events-none cursor-pointer"
                >
                  Add
                </button>
              </form>
            </motion.div>
          </div>
        </div>
      </div>

      {/* Reset Modal Overlay */}
      {showResetConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/40 backdrop-blur-sm">
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="w-full max-w-md rounded-3xl border border-border bg-surface-elevated p-6 shadow-luxury relative overflow-hidden"
          >
            <h3 className="text-lg font-bold text-foreground">Start New Week?</h3>
            <p className="mt-2 text-sm text-muted-foreground leading-relaxed">
              This will reset your logged completed hours to 0.0h and clear all completed tasks from your checklist. Uncompleted tasks and study days configuration will remain.
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setShowResetConfirm(false)}
                className="rounded-full border border-border bg-background hover:bg-accent px-5 py-2.5 text-xs font-semibold text-muted-foreground cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleResetWeek}
                className="rounded-full bg-primary px-5 py-2.5 text-xs font-semibold text-primary-foreground shadow-sm hover:scale-[1.02] cursor-pointer"
              >
                Start New Week
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AppShell>
  );
}
