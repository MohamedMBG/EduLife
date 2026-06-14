import { motion } from "framer-motion";
import { CheckCircle, Swords, Scroll, Sparkles } from "lucide-react";
import { LevelState, BADGE_DEFS } from "./level-types";

export function QuestsCard({ state }: { state: LevelState }) {
  const quests = [
    {
      icon: Swords,
      title: "Daily Warrior",
      desc: "Complete 3 lessons today",
      progress: state.questDailyDone,
      total: 3,
      xp: 50,
      color: "text-primary",
      barColor: "bg-primary shadow-[0_0_8px_oklch(var(--primary)/0.35)]",
      bg: "bg-primary/4 dark:bg-primary/10 border-primary/15",
    },
    {
      icon: Scroll,
      title: "Knowledge Seeker",
      desc: "Earn any course certificate",
      progress: state.questCertEarned ? 1 : 0,
      total: 1,
      xp: 75,
      color: "text-amber-500",
      barColor: "bg-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.35)]",
      bg: "bg-amber-500/4 dark:bg-amber-500/10 border-amber-500/15",
    },
    {
      icon: Sparkles,
      title: "Streak Keeper",
      desc: "Study 7 days in a row",
      progress: Math.min(state.streak, 7),
      total: 7,
      xp: 120,
      color: "text-teal",
      barColor: "bg-teal shadow-[0_0_8px_oklch(var(--teal)/0.35)]",
      bg: "bg-teal/4 dark:bg-teal/10 border-teal/15",
    },
  ];

  const doneCount = quests.filter((q) => q.progress >= q.total).length;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.25 }}
      className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft glass grain relative"
    >
      <div className="flex items-center gap-2 mb-5">
        <Swords className="h-4.5 w-4.5 text-primary" strokeWidth={2} />
        <p className="text-xs uppercase tracking-[0.25em] font-semibold text-muted-foreground">
          Quests & Challenges
        </p>
        <span className="ml-auto text-xs font-bold text-primary bg-primary/10 border border-primary/20 rounded-full px-2.5 py-0.5">
          {doneCount}/{quests.length} Done
        </span>
      </div>
      <div className="space-y-3.5">
        {quests.map((q, i) => {
          const Icon = q.icon;
          const pct = Math.round((q.progress / q.total) * 100);
          const done = q.progress >= q.total;
          return (
            <motion.div
              key={q.title}
              initial={{ opacity: 0, x: -12 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.3 + i * 0.08 }}
              whileHover={{ x: 3, scale: 1.01 }}
              className={`flex items-center gap-3.5 rounded-2xl border p-3.5 transition-all ${
                done ? "opacity-75 border-border bg-muted/20" : q.bg
              }`}
            >
              <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-background/95 dark:bg-background/80 shadow-sm">
                <Icon className={`h-5 w-5 ${done ? "text-muted-foreground" : q.color}`} strokeWidth={2} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1.5">
                  <p className={`text-sm font-bold leading-none ${done ? "text-muted-foreground line-through" : "text-foreground"}`}>
                    {q.title}
                  </p>
                  {done && <CheckCircle className="h-3.5 w-3.5 text-teal shrink-0" />}
                </div>
                <div className="h-2 rounded-full bg-background/60 dark:bg-background/30 overflow-hidden">
                  <motion.div
                    className={`h-full rounded-full ${done ? "bg-muted-foreground/40" : q.barColor}`}
                    initial={{ width: 0 }}
                    animate={{ width: `${pct}%` }}
                    transition={{ duration: 0.9, delay: 0.4 + i * 0.1 }}
                  />
                </div>
                <p className="text-[10px] text-muted-foreground/80 mt-1.5 font-medium">
                  {q.desc} · {q.progress}/{q.total}
                </p>
              </div>
              <div className="shrink-0 text-right">
                <span className={`text-sm font-black ${done ? "text-muted-foreground" : q.color}`}>
                  +{q.xp}
                </span>
                <p className="text-[9px] uppercase tracking-wider text-muted-foreground font-semibold leading-none">XP</p>
              </div>
            </motion.div>
          );
        })}
      </div>
    </motion.div>
  );
}
