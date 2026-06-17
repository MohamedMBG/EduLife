import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { AnimatePresence, motion } from "framer-motion";
import {
  AlertCircle,
  CalendarDays,
  Check,
  ChevronRight,
  Loader2,
  Minus,
  MoreHorizontal,
  Plus,
  Trash2,
} from "lucide-react";

import { AppLayout } from "../components/app/AppLayout";
import { listMyEnrollments } from "../lib/api/client";
import type { EnrolledCourse } from "../lib/api/types";
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
  { key: "Monday", label: "M", name: "Monday" },
  { key: "Tuesday", label: "T", name: "Tuesday" },
  { key: "Wednesday", label: "W", name: "Wednesday" },
  { key: "Thursday", label: "T", name: "Thursday" },
  { key: "Friday", label: "F", name: "Friday" },
  { key: "Saturday", label: "S", name: "Saturday" },
  { key: "Sunday", label: "S", name: "Sunday" },
];

const DEFAULT_STUDY_DAYS = ["Monday", "Wednesday", "Thursday"];
const CARD_CLASS =
  "rounded-[8px] border border-[#edf1f5] bg-white shadow-[0_32px_64px_-42px_rgba(9,20,38,0.28)]";
const CONTROL_TRANSITION =
  "transition-all duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#091426]";

function PlannerRoute() {
  return (
    <RequireAuth>
      <PlannerPage />
    </RequireAuth>
  );
}

function readLocalString(key: string, fallback: string) {
  if (typeof window === "undefined") return fallback;
  return localStorage.getItem(key) ?? fallback;
}

function readLocalNumber(key: string, fallback: number) {
  if (typeof window === "undefined") return fallback;
  const value = Number(localStorage.getItem(key));
  return Number.isFinite(value) ? value : fallback;
}

function readLocalJson<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;

  const value = localStorage.getItem(key);
  if (!value) return fallback;

  try {
    return JSON.parse(value) as T;
  } catch {
    // Planner settings are local-only, so a corrupted browser value should not crash the route.
    return fallback;
  }
}

function PlannerPage() {
  const auth = useAuth();
  const isClient = typeof window !== "undefined";

  const [goal, setGoal] = useState(() => readLocalString("edulife_planner_goal", ""));
  const [targetHours, setTargetHours] = useState(() =>
    readLocalNumber("edulife_planner_target_hours", 11),
  );
  const [completedHours, setCompletedHours] = useState(() =>
    readLocalNumber("edulife_planner_completed_hours", 0),
  );
  const [studyDays, setStudyDays] = useState<string[]>(() =>
    readLocalJson("edulife_planner_study_days", DEFAULT_STUDY_DAYS),
  );
  const [focusCourses, setFocusCourses] = useState<string[]>(() =>
    readLocalJson("edulife_planner_focus_courses", []),
  );
  const [tasks, setTasks] = useState<PlannerTask[]>(() =>
    readLocalJson("edulife_planner_tasks", []),
  );
  const [newTaskText, setNewTaskText] = useState("");
  const [taskError, setTaskError] = useState<string | null>(null);
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  // The planner currently has no backend model, so localStorage is the source of truth for the
  // learner's weekly preferences and lightweight task checklist.
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
    if (isClient)
      localStorage.setItem("edulife_planner_focus_courses", JSON.stringify(focusCourses));
  }, [focusCourses, isClient]);

  useEffect(() => {
    if (isClient) localStorage.setItem("edulife_planner_tasks", JSON.stringify(tasks));
  }, [tasks, isClient]);

  const enrollmentsQuery = useQuery({
    queryKey: ["enrollments"],
    queryFn: () => listMyEnrollments(auth.getAccessToken),
  });

  const enrollments = enrollmentsQuery.data ?? [];
  const activeFocusCourses = enrollments.filter((course) => focusCourses.includes(course.courseId));
  const milestoneCourse = activeFocusCourses[0] ?? enrollments[0] ?? null;

  const progressPercent = useMemo(() => {
    if (targetHours <= 0) return 0;
    return Math.min(Math.round((completedHours / targetHours) * 100), 100);
  }, [completedHours, targetHours]);

  const remainingHours = Math.max(targetHours - completedHours, 0);

  function handleLogHours(amount: number) {
    // Keep accidental repeated taps from creating impossible weekly totals.
    setCompletedHours((prev) => Math.min(Math.max(0, prev + amount), targetHours * 2));
  }

  function handleToggleDay(day: string) {
    setStudyDays((prev) => (prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]));
  }

  function handleToggleCourse(courseId: string) {
    setFocusCourses((prev) =>
      prev.includes(courseId) ? prev.filter((id) => id !== courseId) : [...prev, courseId],
    );
  }

  function handleAddTask(e?: FormEvent) {
    if (e) e.preventDefault();
    const trimmed = newTaskText.trim();

    if (!trimmed) return;

    if (tasks.length >= 10) {
      // Inline feedback avoids a blocking alert while preserving the focused 10-task rule.
      setTaskError("Checklist limited to 10 active tasks to keep your study plan focused.");
      return;
    }

    const newTask: PlannerTask = {
      id: crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2),
      title: trimmed,
      completed: false,
    };

    setTasks((prev) => [...prev, newTask]);
    setNewTaskText("");
    setTaskError(null);
  }

  function handleToggleTask(taskId: string) {
    setTasks((prev) => prev.map((t) => (t.id === taskId ? { ...t, completed: !t.completed } : t)));
  }

  function handleDeleteTask(taskId: string) {
    setTasks((prev) => prev.filter((t) => t.id !== taskId));
  }

  function handleResetWeek() {
    setCompletedHours(0);
    // Completed tasks are removed because a new week should keep only unfinished work in view.
    setTasks((prev) => prev.filter((t) => !t.completed));
    setShowResetConfirm(false);
  }

  return (
    <AppLayout>
      <div>
        <PlannerHeader onStartNewWeek={() => setShowResetConfirm(true)} />

        <div className="mt-16 grid grid-cols-1 gap-6 lg:grid-cols-12">
          <div className="space-y-8 lg:col-span-8">
            <TimeTrackerCard
              completedHours={completedHours}
              onClear={() => setCompletedHours(0)}
              onLogHours={handleLogHours}
              onTargetChange={setTargetHours}
              progressPercent={progressPercent}
              remainingHours={remainingHours}
              targetHours={targetHours}
            />

            <StudyTasksChecklist
              newTaskText={newTaskText}
              onAddTask={handleAddTask}
              onDeleteTask={handleDeleteTask}
              onNewTaskTextChange={(value) => {
                setNewTaskText(value);
                if (taskError) setTaskError(null);
              }}
              onToggleTask={handleToggleTask}
              taskError={taskError}
              tasks={tasks}
            />
          </div>

          <aside className="space-y-8 lg:col-span-4">
            <StudyDaysCard selectedDays={studyDays} onToggleDay={handleToggleDay} />

            <ActiveFocusCard
              courses={enrollments}
              error={enrollmentsQuery.error}
              focusCourseIds={focusCourses}
              isLoading={enrollmentsQuery.isLoading}
              onRetry={() => void enrollmentsQuery.refetch()}
              onToggleCourse={handleToggleCourse}
            />

            <UpcomingMilestoneCard course={milestoneCourse} />
          </aside>
        </div>

        <WeeklyFocusNote goal={goal} onGoalChange={setGoal} />
        <ResetWeekDialog
          open={showResetConfirm}
          onCancel={() => setShowResetConfirm(false)}
          onConfirm={handleResetWeek}
        />
      </div>
    </AppLayout>
  );
}

function PlannerHeader({ onStartNewWeek }: { onStartNewWeek: () => void }) {
  return (
    <section className="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <nav
          className="flex items-center gap-2 text-[12px] font-semibold uppercase tracking-[0.18em] text-[#31435e]"
          aria-label="Breadcrumb"
        >
          <Link to="/dashboard" className={`${CONTROL_TRANSITION} hover:text-[#091426]`}>
            Dashboard
          </Link>
          <ChevronRight className="h-3.5 w-3.5" strokeWidth={1.7} aria-hidden />
          <span className="text-[#091426]">Study Planner</span>
        </nav>
        <h1 className="mt-6 text-[42px] font-light leading-none tracking-[0] text-[#050b14] sm:text-[56px]">
          Study Planner
        </h1>
        <p className="mt-5 max-w-[68ch] text-[18px] font-light leading-8 tracking-[0.03em] text-[#505f76] sm:text-[20px]">
          Organize your academic journey with precision and calm.
        </p>
      </div>

      <button
        type="button"
        onClick={onStartNewWeek}
        className={`inline-flex h-[52px] w-full items-center justify-center gap-3 rounded-[4px] border border-[#75777d] bg-transparent px-7 text-[12px] font-bold uppercase tracking-[0.2em] text-[#091426] hover:-translate-y-0.5 hover:bg-white sm:w-auto ${CONTROL_TRANSITION}`}
      >
        <CalendarDays className="h-5 w-5" strokeWidth={1.8} aria-hidden />
        Start New Week
      </button>
    </section>
  );
}

function TimeTrackerCard({
  completedHours,
  onClear,
  onLogHours,
  onTargetChange,
  progressPercent,
  remainingHours,
  targetHours,
}: {
  completedHours: number;
  onClear: () => void;
  onLogHours: (amount: number) => void;
  onTargetChange: (updater: (prev: number) => number) => void;
  progressPercent: number;
  remainingHours: number;
  targetHours: number;
}) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.55, ease: [0.32, 0.72, 0, 1] }}
      className={`${CARD_CLASS} p-6 sm:p-8`}
      aria-labelledby="time-tracker-title"
    >
      <div className="grid gap-8 xl:grid-cols-[156px_minmax(0,1fr)_1px_168px] xl:items-center">
        <ProgressRing percentage={progressPercent} />

        <div>
          <h2
            id="time-tracker-title"
            className="text-[30px] font-normal tracking-[0] text-[#050b14]"
          >
            Time Tracker
          </h2>
          <p className="mt-2 text-[14px] font-light leading-6 tracking-[0.03em] text-[#31435e]">
            You have completed {formatHoursShort(completedHours)} of {targetHours} targeted study
            hours.
          </p>

          <div className="mt-5 flex flex-wrap gap-4">
            <StatBlock label="Today" value={formatDuration(completedHours)} />
            <StatBlock label="Remaining" value={formatDuration(remainingHours)} />
          </div>

          <div className="mt-5 flex flex-wrap gap-2">
            <SmallActionButton onClick={() => onLogHours(0.5)}>Log 30m</SmallActionButton>
            <SmallActionButton onClick={() => onLogHours(1)}>Log 1h</SmallActionButton>
            <SmallActionButton disabled={completedHours <= 0} onClick={onClear}>
              Clear
            </SmallActionButton>
          </div>
        </div>

        <div className="hidden h-28 bg-[#dfe3e7] xl:block" aria-hidden />

        <div className="xl:text-right">
          <p className="text-[12px] font-light uppercase tracking-[0.14em] text-[#505f76]">
            Weekly Target
          </p>
          <div className="mt-4 flex items-baseline gap-2 xl:justify-end">
            <span className="text-[38px] font-bold leading-none text-[#091426]">{targetHours}</span>
            <span className="text-[28px] font-light text-[#505f76]">h</span>
          </div>
          <div className="mt-4 flex gap-2 xl:justify-end">
            <button
              type="button"
              onClick={() => onTargetChange((prev) => Math.max(1, prev - 1))}
              className={`grid h-9 w-9 place-items-center rounded-[4px] border border-[#dfe3e7] text-[#091426] hover:bg-[#f0f4f8] ${CONTROL_TRANSITION}`}
              aria-label="Decrease weekly target"
            >
              <Minus className="h-4 w-4" aria-hidden />
            </button>
            <button
              type="button"
              onClick={() => onTargetChange((prev) => Math.min(40, prev + 1))}
              className={`grid h-9 w-9 place-items-center rounded-[4px] border border-[#dfe3e7] text-[#091426] hover:bg-[#f0f4f8] ${CONTROL_TRANSITION}`}
              aria-label="Increase weekly target"
            >
              <Plus className="h-4 w-4" aria-hidden />
            </button>
          </div>
        </div>
      </div>
    </motion.section>
  );
}

function ProgressRing({ percentage }: { percentage: number }) {
  const radius = 56;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (percentage / 100) * circumference;

  return (
    <div
      className="relative mx-auto grid h-[136px] w-[136px] place-items-center sm:mx-0"
      role="img"
      aria-label={`Weekly study progress ${percentage} percent`}
    >
      <svg className="h-full w-full -rotate-90" viewBox="0 0 136 136" aria-hidden>
        <circle cx="68" cy="68" fill="none" r={radius} stroke="#dfe3e7" strokeWidth="7" />
        <circle
          cx="68"
          cy="68"
          fill="none"
          r={radius}
          stroke="#091426"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="butt"
          strokeWidth="7"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-[28px] font-bold leading-none text-[#091426]">{percentage}%</span>
        <span className="mt-2 text-[10px] font-semibold uppercase tracking-[0.12em] text-[#8a96aa]">
          Weekly
        </span>
      </div>
    </div>
  );
}

function StatBlock({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-[106px] rounded-[4px] bg-[#eaeef2] px-4 py-3">
      <span className="block text-[12px] font-light uppercase tracking-[0.1em] text-[#505f76]">
        {label}
      </span>
      <span className="mt-1 block text-[16px] font-bold leading-none text-[#091426]">{value}</span>
    </div>
  );
}

function SmallActionButton({
  children,
  disabled,
  onClick,
}: {
  children: string;
  disabled?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={`rounded-[4px] border border-[#dfe3e7] px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-[#31435e] hover:bg-[#f6fafe] disabled:cursor-not-allowed disabled:opacity-40 ${CONTROL_TRANSITION}`}
    >
      {children}
    </button>
  );
}

function StudyTasksChecklist({
  newTaskText,
  onAddTask,
  onDeleteTask,
  onNewTaskTextChange,
  onToggleTask,
  taskError,
  tasks,
}: {
  newTaskText: string;
  onAddTask: (event: FormEvent) => void;
  onDeleteTask: (taskId: string) => void;
  onNewTaskTextChange: (value: string) => void;
  onToggleTask: (taskId: string) => void;
  taskError: string | null;
  tasks: PlannerTask[];
}) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.06, duration: 0.55, ease: [0.32, 0.72, 0, 1] }}
      className={`${CARD_CLASS} p-6 sm:p-8`}
      aria-labelledby="study-tasks-title"
    >
      <div className="flex items-start justify-between gap-4">
        <h2 id="study-tasks-title" className="text-[30px] font-normal tracking-[0] text-[#050b14]">
          Study Tasks Checklist
        </h2>
        <button
          type="button"
          className={`grid h-9 w-9 place-items-center rounded-[4px] text-[#505f76] hover:bg-[#f6fafe] hover:text-[#091426] ${CONTROL_TRANSITION}`}
          aria-label="More checklist actions"
        >
          <MoreHorizontal className="h-5 w-5" aria-hidden />
        </button>
      </div>

      <div className="mt-10 divide-y divide-[#edf1f5]">
        <AnimatePresence initial={false}>
          {tasks.length === 0 ? (
            <motion.div
              key="empty"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="rounded-[8px] border border-dashed border-[#dfe3e7] bg-[#f8fbfe] px-5 py-8 text-center"
            >
              <AlertCircle className="mx-auto h-6 w-6 text-[#8a96aa]" strokeWidth={1.7} />
              <p className="mt-3 text-[14px] font-semibold text-[#091426]">No study tasks yet</p>
              <p className="mx-auto mt-2 max-w-md text-[13px] leading-6 text-[#505f76]">
                Add one focused item for this week. The checklist stays on this device until a
                planner backend is introduced.
              </p>
            </motion.div>
          ) : (
            tasks.map((task, index) => (
              <StudyTaskRow
                key={task.id}
                index={index}
                task={task}
                onDelete={() => onDeleteTask(task.id)}
                onToggle={() => onToggleTask(task.id)}
              />
            ))
          )}
        </AnimatePresence>
      </div>

      <form onSubmit={onAddTask} className="mt-7">
        <label className="sr-only" htmlFor="planner-new-task">
          Add new study task
        </label>
        <div className="flex flex-col gap-3 rounded-[4px] border-2 border-dashed border-[#e6ebf1] p-3 sm:flex-row">
          <input
            id="planner-new-task"
            type="text"
            value={newTaskText}
            onChange={(event) => onNewTaskTextChange(event.target.value)}
            placeholder="Add New Task"
            maxLength={60}
            className={`h-11 min-w-0 flex-1 rounded-[4px] border border-transparent bg-transparent px-3 text-[13px] font-medium text-[#091426] outline-none placeholder:text-[#505f76] focus:border-[#dfe3e7] focus:bg-white ${CONTROL_TRANSITION}`}
          />
          <button
            type="submit"
            disabled={!newTaskText.trim()}
            className={`inline-flex h-11 items-center justify-center gap-2 rounded-[4px] px-5 text-[12px] font-bold uppercase tracking-[0.16em] text-[#31435e] hover:bg-white disabled:cursor-not-allowed disabled:opacity-45 ${CONTROL_TRANSITION}`}
          >
            <Plus className="h-4 w-4" aria-hidden />
            Add New Task
          </button>
        </div>
        {taskError ? (
          <p className="mt-3 text-[12px] font-medium text-[#ba1a1a]">{taskError}</p>
        ) : null}
      </form>
    </motion.section>
  );
}

function StudyTaskRow({
  index,
  onDelete,
  onToggle,
  task,
}: {
  index: number;
  onDelete: () => void;
  onToggle: () => void;
  task: PlannerTask;
}) {
  const tags = task.completed
    ? ["COMPLETED"]
    : index === 0
      ? ["URGENT", "DUE TODAY"]
      : index === 1
        ? ["DUE TOMORROW"]
        : ["DUE THIS WEEK"];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.98 }}
      className="group flex items-start gap-4 py-6"
    >
      <button
        type="button"
        onClick={onToggle}
        className={`mt-1 grid h-6 w-6 shrink-0 place-items-center rounded-[2px] border-2 ${CONTROL_TRANSITION} ${
          task.completed
            ? "border-[#091426] bg-[#091426] text-white"
            : "border-[#75777d] text-transparent hover:border-[#091426]"
        }`}
        aria-pressed={task.completed}
        aria-label={
          task.completed ? `Mark ${task.title} incomplete` : `Mark ${task.title} complete`
        }
      >
        <Check className="h-4 w-4" strokeWidth={2.2} aria-hidden />
      </button>

      <div className="min-w-0 flex-1">
        <p
          className={`text-[15px] leading-6 tracking-[0] ${
            task.completed ? "text-[#66728a] line-through" : "text-[#091426]"
          }`}
        >
          {task.title}
        </p>
        <div className="mt-2 flex flex-wrap gap-3">
          {tags.map((tag) => (
            <span
              key={tag}
              className={`rounded-[4px] px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.08em] ${
                tag === "URGENT" ? "bg-[#d0e1fb] text-[#38485d]" : "bg-transparent text-[#8a96aa]"
              }`}
            >
              {tag}
            </span>
          ))}
        </div>
      </div>

      <button
        type="button"
        onClick={onDelete}
        className={`grid h-9 w-9 shrink-0 place-items-center rounded-[4px] text-[#8a96aa] opacity-100 hover:bg-[#fff1f0] hover:text-[#ba1a1a] sm:opacity-0 sm:group-hover:opacity-100 ${CONTROL_TRANSITION}`}
        aria-label={`Delete ${task.title}`}
      >
        <Trash2 className="h-4 w-4" aria-hidden />
      </button>
    </motion.div>
  );
}

function StudyDaysCard({
  onToggleDay,
  selectedDays,
}: {
  onToggleDay: (day: string) => void;
  selectedDays: string[];
}) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1, duration: 0.55, ease: [0.32, 0.72, 0, 1] }}
      className={`${CARD_CLASS} p-6 sm:p-8`}
      aria-labelledby="study-days-title"
    >
      <h2 id="study-days-title" className="text-[30px] font-normal tracking-[0] text-[#050b14]">
        Study Days
      </h2>
      <div className="mt-7 flex justify-between gap-2">
        {DAYS.map((day) => {
          const selected = selectedDays.includes(day.key);
          return (
            <button
              key={day.key}
              type="button"
              onClick={() => onToggleDay(day.key)}
              className={`flex flex-col items-center gap-3 ${CONTROL_TRANSITION}`}
              aria-pressed={selected}
              aria-label={`${selected ? "Remove" : "Add"} ${day.name} as a study day`}
            >
              <span
                className={`grid h-10 w-10 place-items-center rounded-[8px] border text-[14px] font-bold ${
                  selected
                    ? "border-[#091426] bg-[#091426] text-white"
                    : "border-[#c5c6cd] bg-white text-[#a7afbd]"
                }`}
              >
                {day.label}
              </span>
              <span
                className={`h-1.5 w-1.5 rounded-full ${
                  selected ? "bg-[#091426]" : "bg-transparent"
                }`}
                aria-hidden
              />
            </button>
          );
        })}
      </div>
    </motion.section>
  );
}

function ActiveFocusCard({
  courses,
  error,
  focusCourseIds,
  isLoading,
  onRetry,
  onToggleCourse,
}: {
  courses: EnrolledCourse[];
  error: unknown;
  focusCourseIds: string[];
  isLoading: boolean;
  onRetry: () => void;
  onToggleCourse: (courseId: string) => void;
}) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.16, duration: 0.55, ease: [0.32, 0.72, 0, 1] }}
      className={`${CARD_CLASS} p-6 sm:p-8`}
      aria-labelledby="active-focus-title"
    >
      <h2 id="active-focus-title" className="text-[30px] font-normal tracking-[0] text-[#050b14]">
        Active Focus
      </h2>

      <div className="mt-7 space-y-4">
        {isLoading ? (
          <FocusSkeleton />
        ) : error ? (
          <ErrorPanel
            detail="We could not load your enrolled courses for focus selection."
            onRetry={onRetry}
          />
        ) : courses.length === 0 ? (
          <EmptyPanel
            title="No focus courses"
            detail="Enroll in a course first, then return here to choose weekly focus areas."
          />
        ) : (
          courses.map((course) => (
            <ActiveFocusRow
              key={course.courseId}
              course={course}
              selected={focusCourseIds.includes(course.courseId)}
              onToggle={() => onToggleCourse(course.courseId)}
            />
          ))
        )}
      </div>
    </motion.section>
  );
}

function ActiveFocusRow({
  course,
  onToggle,
  selected,
}: {
  course: EnrolledCourse;
  onToggle: () => void;
  selected: boolean;
}) {
  return (
    <article className="flex items-center justify-between gap-4 rounded-[4px] border border-[#dfe3e7] px-4 py-4">
      <div className="flex min-w-0 items-center gap-4">
        <CourseThumb course={course} />
        <div className="min-w-0">
          <h3 className="truncate text-[15px] font-bold leading-5 text-[#091426]">
            {course.title}
          </h3>
          <p className="mt-2 text-[10px] font-semibold uppercase tracking-[0.08em] text-[#8a96aa]">
            Active enrollment
          </p>
        </div>
      </div>
      <button
        type="button"
        onClick={onToggle}
        className={`relative h-[22px] w-[44px] shrink-0 rounded-full ${CONTROL_TRANSITION} ${
          selected ? "bg-[#091426]" : "bg-[#e7ebef]"
        }`}
        role="switch"
        aria-checked={selected}
        aria-label={`${selected ? "Disable" : "Enable"} ${course.title} as active focus`}
      >
        <span
          className={`absolute top-1/2 h-3.5 w-3.5 -translate-y-1/2 rounded-full bg-white shadow-[0_2px_8px_rgba(9,20,38,0.18)] transition-transform duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] ${
            selected ? "translate-x-[25px]" : "translate-x-[5px]"
          }`}
          aria-hidden
        />
      </button>
    </article>
  );
}

function CourseThumb({ course }: { course: EnrolledCourse }) {
  if (course.imageUrl) {
    return (
      <div className="relative h-12 w-12 shrink-0">
        <img
          src={course.imageUrl}
          alt={`${course.title} course thumbnail`}
          className="h-12 w-12 shrink-0 rounded-[2px] object-cover grayscale"
          onError={(e) => {
            e.currentTarget.style.display = "none";
            e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
          }}
        />
        <div
          className="grid h-12 w-12 shrink-0 place-items-center rounded-[2px] bg-[#111516] text-[14px] font-bold uppercase text-white"
          data-fallback=""
          hidden
        >
          {course.title.slice(0, 1)}
        </div>
      </div>
    );
  }

  return (
    <div
      className="grid h-12 w-12 shrink-0 place-items-center rounded-[2px] bg-[#111516] text-[14px] font-bold uppercase text-white"
      aria-label={`${course.title} course thumbnail placeholder`}
    >
      {course.title.slice(0, 1)}
    </div>
  );
}

function FocusSkeleton() {
  return (
    <div className="space-y-4" aria-label="Loading focus courses">
      {[0, 1, 2].map((item) => (
        <div
          key={item}
          className="flex items-center gap-4 rounded-[4px] border border-[#edf1f5] px-4 py-4"
        >
          <div className="h-12 w-12 animate-pulse rounded-[2px] bg-[#eaeef2]" />
          <div className="min-w-0 flex-1 space-y-2">
            <div className="h-4 w-2/3 animate-pulse rounded-[2px] bg-[#eaeef2]" />
            <div className="h-3 w-1/3 animate-pulse rounded-[2px] bg-[#eaeef2]" />
          </div>
          <Loader2 className="h-4 w-4 animate-spin text-[#8a96aa]" aria-hidden />
        </div>
      ))}
    </div>
  );
}

function UpcomingMilestoneCard({ course }: { course: EnrolledCourse | null }) {
  const title = course ? `${course.title} review checkpoint` : "End of Semester Exam Review";

  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.22, duration: 0.55, ease: [0.32, 0.72, 0, 1] }}
      className="relative min-h-[190px] overflow-hidden rounded-[8px] bg-[#091426] shadow-[0_32px_64px_-42px_rgba(9,20,38,0.5)]"
      aria-labelledby="milestone-title"
    >
      {course?.imageUrl ? (
        <img
          src={course.imageUrl}
          alt={`${course.title} milestone background`}
          className="absolute inset-0 h-full w-full object-cover grayscale"
          onError={(e) => {
            e.currentTarget.style.display = "none";
            e.currentTarget.parentElement?.querySelector("[data-fallback]")?.removeAttribute("hidden");
          }}
        />
      ) : null}
      <div className="absolute inset-0 bg-[linear-gradient(135deg,#c9d2de,#4b5565_48%,#091426)]" data-fallback="" hidden={!!course?.imageUrl} />
      <div className="absolute inset-0 bg-gradient-to-t from-[#091426]/88 via-[#091426]/38 to-transparent" />
      <div className="relative flex min-h-[190px] flex-col justify-end p-5">
        <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-white/68">
          Upcoming Milestone
        </p>
        <h2 id="milestone-title" className="mt-4 text-[20px] font-bold leading-7 text-white">
          {title}
        </h2>
      </div>
    </motion.section>
  );
}

function WeeklyFocusNote({
  goal,
  onGoalChange,
}: {
  goal: string;
  onGoalChange: (goal: string) => void;
}) {
  return (
    <section className="mt-8 rounded-[8px] border border-[#dfe3e7] bg-white/62 p-4 sm:p-5">
      <label
        htmlFor="weekly-focus"
        className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#31435e]"
      >
        Weekly focus note
      </label>
      <input
        id="weekly-focus"
        type="text"
        value={goal}
        onChange={(event) => onGoalChange(event.target.value)}
        placeholder="Example: prepare the final exam review outline"
        className={`mt-3 h-11 w-full rounded-[4px] border border-[#dfe3e7] bg-white px-3 text-[14px] text-[#091426] outline-none placeholder:text-[#8a96aa] focus:border-[#091426] ${CONTROL_TRANSITION}`}
      />
    </section>
  );
}

function ResetWeekDialog({
  onCancel,
  onConfirm,
  open,
}: {
  onCancel: () => void;
  onConfirm: () => void;
  open: boolean;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#091426]/28 px-4 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, y: 16, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        className="w-full max-w-md rounded-[8px] border border-[#dfe3e7] bg-white p-6 shadow-[0_30px_80px_-38px_rgba(9,20,38,0.65)]"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reset-week-title"
      >
        <h2 id="reset-week-title" className="text-[22px] font-normal text-[#091426]">
          Start New Week?
        </h2>
        <p className="mt-3 text-[14px] leading-6 text-[#505f76]">
          This resets logged study hours to 0 and removes completed checklist items. Unfinished
          tasks, study days, and focus courses stay in place.
        </p>
        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancel}
            className={`h-11 rounded-[4px] border border-[#dfe3e7] px-5 text-[12px] font-bold uppercase tracking-[0.14em] text-[#31435e] hover:bg-[#f6fafe] ${CONTROL_TRANSITION}`}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className={`h-11 rounded-[4px] bg-[#091426] px-5 text-[12px] font-bold uppercase tracking-[0.14em] text-white hover:bg-[#1e293b] ${CONTROL_TRANSITION}`}
          >
            Start New Week
          </button>
        </div>
      </motion.div>
    </div>
  );
}

function EmptyPanel({ detail, title }: { detail: string; title: string }) {
  return (
    <div className="rounded-[4px] border border-dashed border-[#dfe3e7] bg-[#f8fbfe] px-4 py-6 text-center">
      <p className="text-[14px] font-semibold text-[#091426]">{title}</p>
      <p className="mt-2 text-[13px] leading-6 text-[#505f76]">{detail}</p>
    </div>
  );
}

function ErrorPanel({ detail, onRetry }: { detail: string; onRetry: () => void }) {
  return (
    <div className="rounded-[4px] border border-[#ffd6d2] bg-[#fff7f6] px-4 py-5">
      <p className="text-[13px] font-semibold text-[#93000a]">{detail}</p>
      <button
        type="button"
        onClick={onRetry}
        className={`mt-4 rounded-[4px] border border-[#ffd6d2] px-4 py-2 text-[11px] font-bold uppercase tracking-[0.14em] text-[#93000a] hover:bg-white ${CONTROL_TRANSITION}`}
      >
        Retry
      </button>
    </div>
  );
}

function formatDuration(hours: number) {
  const safeHours = Math.max(0, hours);
  const wholeHours = Math.floor(safeHours);
  const minutes = Math.round((safeHours - wholeHours) * 60);

  if (wholeHours === 0 && minutes === 0) return "0h";
  if (wholeHours === 0) return `${minutes}m`;
  if (minutes === 0) return `${wholeHours}h`;
  return `${wholeHours}h ${minutes}m`;
}

function formatHoursShort(hours: number) {
  return Number.isInteger(hours) ? String(hours) : hours.toFixed(1);
}
