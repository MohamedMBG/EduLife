import { motion } from "framer-motion";
import { Sparkles, Award, CheckCircle } from "lucide-react";
import { LevelState } from "./level-types";

function formatRelative(date: Date) {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const days = Math.floor(diffMs / 86400000);
  if (days <= 0) return "today";
  if (days === 1) return "1 day ago";
  if (days < 7) return `${days} days ago`;
  if (days < 30) return `${Math.floor(days / 7)} wk ago`;
  return date.toLocaleDateString();
}

export function RecentActivityCard({ state }: { state: LevelState }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.4 }}
      className="rounded-3xl border border-border bg-surface-elevated shadow-soft overflow-hidden glass grain flex flex-col"
    >
      <div className="flex items-center justify-between px-5 py-4.5 border-b border-border bg-muted/20">
        <div className="flex items-center gap-2">
          <Sparkles className="h-4.5 w-4.5 text-amber-500 animate-spin" style={{ animationDuration: "6s" }} strokeWidth={2} />
          <p className="text-xs uppercase tracking-[0.2em] font-semibold text-muted-foreground">
            Activity Feed
          </p>
        </div>
        <span className="text-xs font-bold text-primary bg-primary/10 border border-primary/20 px-2.5 py-0.5 rounded-full">
          +{state.totalXp} Total XP
        </span>
      </div>

      <div className="flex-1 overflow-y-auto max-h-[178px] divide-y divide-border/60">
        {state.recentActivity.length === 0 ? (
          <div className="px-5 py-10 text-xs text-muted-foreground text-center">
            No activities registered yet. Start a lesson to gain experience!
          </div>
        ) : (
          state.recentActivity.map((item, i) => {
            const Icon = item.kind === "certificate" ? Award : CheckCircle;
            const accent =
              item.kind === "certificate" ? "text-amber-500 bg-amber-500/10" : "text-primary bg-primary/10";
            return (
              <motion.div
                key={`${item.kind}-${item.date.getTime()}-${i}`}
                initial={{ opacity: 0, x: 12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.35, delay: 0.3 + i * 0.05 }}
                className="flex items-center gap-3 px-5 py-3 hover:bg-muted/10 transition-colors"
              >
                <div
                  className={`grid h-8.5 w-8.5 shrink-0 place-items-center rounded-xl ${accent}`}
                >
                  <Icon className="h-4.5 w-4.5" strokeWidth={2} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-bold truncate text-foreground leading-tight">
                    {item.title}
                  </p>
                  <p className="text-[10px] text-muted-foreground mt-0.5 font-medium leading-none">
                    {item.subtitle} · {formatRelative(item.date)}
                  </p>
                </div>
                <p className={`text-xs font-black tabular-nums shrink-0 ${item.kind === "certificate" ? "text-amber-500" : "text-primary"}`}>
                  +{item.xp} XP
                </p>
              </motion.div>
            );
          })
        )}
      </div>
    </motion.div>
  );
}
