import { motion } from "framer-motion";
import { Flame, Trophy } from "lucide-react";
import { LevelState } from "./level-types";

export function StreakCard({ state }: { state: LevelState }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.2 }}
      className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft glass grain relative overflow-hidden"
    >
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-2">
          <Flame className="h-4.5 w-4.5 text-amber-500 animate-pulse" strokeWidth={2} />
          <p className="text-xs uppercase tracking-[0.25em] font-semibold text-muted-foreground">
            This Week's Run
          </p>
        </div>
        <span className="text-xs font-bold text-amber-500 bg-amber-500/10 border border-amber-500/25 px-2.5 py-0.5 rounded-full">
          🔥 {state.streak} Day streak
        </span>
      </div>
      <div className="flex items-center justify-between gap-1.5 py-1">
        {state.streakDays.map((d, i) => (
          <div key={i} className="flex flex-col items-center gap-2">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2 + i * 0.05, type: "spring", bounce: 0.4 }}
              whileHover={{ scale: 1.1 }}
              className={`w-10 h-10 rounded-2xl flex items-center justify-center text-lg transition-all border-2 ${
                d.active
                  ? "bg-amber-500/10 border-amber-400 text-amber-500 shadow-[0_0_12px_rgba(245,158,11,0.25)]"
                  : "bg-muted/40 dark:bg-muted/10 border-border text-muted-foreground/30"
              } ${d.today ? "ring-2 ring-primary ring-offset-2 ring-offset-background" : ""}`}
            >
              {d.active ? (
                <motion.span
                  animate={{ y: [0, -2, 0] }}
                  transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut", delay: i * 0.15 }}
                >
                  🔥
                </motion.span>
              ) : (
                "·"
              )}
            </motion.div>
            <span className={`text-[10px] font-bold ${d.active ? "text-amber-500 font-extrabold" : "text-muted-foreground/50"}`}>
              {d.label}
            </span>
          </div>
        ))}
      </div>
      <div className="mt-5 pt-4 border-t border-border flex items-center justify-between text-xs text-muted-foreground">
        <span>Longest Streak Record</span>
        <span className="font-bold text-foreground flex items-center gap-1">
          {state.longestStreak} days <Trophy className="h-3.5 w-3.5 text-amber-500" />
        </span>
      </div>
    </motion.div>
  );
}
