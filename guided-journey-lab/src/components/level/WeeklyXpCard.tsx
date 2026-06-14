import { motion } from "framer-motion";
import { TrendingUp } from "lucide-react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
} from "recharts";
import { LevelState } from "./level-types";

export function WeeklyXpCard({ state }: { state: LevelState }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.35 }}
      className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft glass grain relative overflow-hidden"
    >
      <div className="flex items-start justify-between mb-6">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] font-semibold text-muted-foreground mb-1">
            XP Weekly Record
          </p>
          <p className="text-display text-4xl leading-none text-gradient-primary font-bold">
            +{state.xpWeek}
          </p>
        </div>
        <span className="flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold bg-teal/10 text-teal border border-teal/20">
          <TrendingUp className="h-3.5 w-3.5" />
          {state.xpToday > 0 ? `+${state.xpToday} Today` : "No activity today"}
        </span>
      </div>
      <div className="h-[150px] w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={state.weeklyXp} barSize={24} barCategoryGap="30%">
            <defs>
              <linearGradient id="barActive" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="oklch(var(--primary))" stopOpacity={1} />
                <stop offset="100%" stopColor="oklch(var(--teal))" stopOpacity={0.7} />
              </linearGradient>
              <linearGradient id="barInactive" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="oklch(var(--primary))" stopOpacity={0.2} />
                <stop offset="100%" stopColor="oklch(var(--primary))" stopOpacity={0.05} />
              </linearGradient>
            </defs>
            <XAxis
              dataKey="day"
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 11, fill: "oklch(var(--muted-foreground))", fontFamily: "Figtree", fontWeight: 600 }}
              dy={6}
            />
            <YAxis hide />
            <Tooltip
              cursor={{ fill: "oklch(var(--primary) / 0.04)", radius: 6 }}
              content={({ active, payload }) =>
                active && payload?.length ? (
                  <div className="rounded-xl px-3 py-2 text-xs font-bold bg-card border border-border text-foreground shadow-luxury glass">
                    +{payload[0].value} XP
                  </div>
                ) : null
              }
            />
            <Bar dataKey="xp" radius={[6, 6, 3, 3]}>
              {state.weeklyXp.map((entry, i) => (
                <Cell
                  key={i}
                  fill={
                    entry.xp > 0
                      ? "url(#barActive)"
                      : "url(#barInactive)"
                  }
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </motion.div>
  );
}
