import { motion } from "framer-motion";
import { Shield, BookOpen, Zap } from "lucide-react";
import {
  LevelState,
  LEVEL_PATH,
  LEVEL_METADATA,
  BADGE_DEFS,
} from "./level-types";

interface RankCardProps {
  state: LevelState;
  displayName: string;
}

export function RankCard({ state, displayName }: RankCardProps) {
  const currentLv = LEVEL_PATH[state.level - 1];
  const nextLv = state.isMax ? null : LEVEL_PATH[state.level];
  const xpRemaining = Math.max(0, state.xpRequired - state.xpInto);
  const earnedCount = state.earnedBadges.size;

  const meta = LEVEL_METADATA[state.level] || LEVEL_METADATA[1];
  const nextMeta = nextLv ? LEVEL_METADATA[nextLv.n] : null;

  const CurrentLevelIcon = meta.icon;
  const NextLevelIcon = nextMeta ? nextMeta.icon : null;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      className="relative rounded-3xl border border-primary/20 bg-gradient-to-br from-primary/10 via-card/70 to-teal/5 dark:from-primary/15 dark:via-card/50 dark:to-teal/10 p-6 md:p-8 overflow-hidden glass shadow-luxury grain"
    >
      <div className="absolute -right-6 -top-6 opacity-[0.05] pointer-events-none dark:opacity-[0.08]">
        <Shield strokeWidth={0.5} className="w-56 h-56 text-primary" />
      </div>

      <div className="relative flex flex-col md:flex-row md:items-center gap-6 md:gap-8">
        <div className="relative shrink-0 mx-auto md:mx-0">
          <motion.div
            className="absolute inset-[-6px] rounded-full border-2 border-primary/20"
            animate={{ scale: [1, 1.05, 1], opacity: [0.5, 0.15, 0.5] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />
          <motion.div
            className="absolute inset-[-14px] rounded-full border border-primary/10"
            animate={{ scale: [1, 1.08, 1], opacity: [0.3, 0.05, 0.3] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
          />

          <div className="relative w-32 h-32">
            <svg
              viewBox="0 0 112 112"
              className="absolute inset-0 w-full h-full"
              style={{ transform: "rotate(-90deg)" }}
            >
              <defs>
                <linearGradient id={`rankCircleGradient-${state.level}`} x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor={meta.colorFrom} />
                  <stop offset="100%" stopColor={meta.colorTo} />
                </linearGradient>
              </defs>
              <circle
                cx="56"
                cy="56"
                r="48"
                fill="none"
                strokeWidth="6"
                className="stroke-muted/50 dark:stroke-muted/20"
              />
              <motion.circle
                cx="56"
                cy="56"
                r="48"
                fill="none"
                strokeWidth="6"
                strokeLinecap="round"
                stroke={`url(#rankCircleGradient-${state.level})`}
                strokeDasharray={`${2 * Math.PI * 48}`}
                initial={{ strokeDashoffset: 2 * Math.PI * 48 }}
                animate={{
                  strokeDashoffset:
                    2 * Math.PI * 48 * (1 - state.xpPct / 100),
                }}
                transition={{ duration: 2, delay: 0.5, ease: [0.22, 1, 0.36, 1] }}
                style={{ filter: `drop-shadow(0px 0px 5px ${meta.colorFrom}70)` }}
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-display text-4xl font-bold leading-none text-foreground">
                {state.level}
              </span>
              <span className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-0.5">
                Level
              </span>
            </div>
          </div>
        </div>

        <div className="flex-1 min-w-0 text-center md:text-left">
          <div className="flex items-center justify-center md:justify-start gap-2 mb-1.5">
            <span className={`inline-flex p-1 rounded-lg bg-gradient-to-br ${meta.gradient} text-white shadow-sm`}>
              <CurrentLevelIcon className="h-4 w-4" strokeWidth={2.2} />
            </span>
            <p className="text-[10px] uppercase tracking-[0.25em] font-semibold text-muted-foreground">
              {displayName.split(" ")[0]} · Current Rank
            </p>
          </div>
          <h1
            className={`text-display leading-none bg-gradient-to-br ${meta.gradient} bg-clip-text text-transparent font-bold mb-2 pb-1`}
            style={{ fontSize: "clamp(2rem,5vw,3rem)" }}
          >
            {currentLv.title}
          </h1>
          <p className="text-xs italic text-muted-foreground mb-4 max-w-md">
            "{meta.description}"
          </p>
          <p className="text-sm text-muted-foreground mb-4">
            <span className="font-semibold text-foreground">{state.totalXp.toLocaleString()}</span> total XP
            <span className="mx-2 text-border">·</span>
            <span
              className={
                state.streak > 0
                  ? "text-amber-500 font-bold"
                  : "text-muted-foreground"
              }
            >
              🔥 {state.streak}-day streak
            </span>
          </p>

          <div className="max-w-md mx-auto md:mx-0">
            <div className="flex justify-between text-xs text-muted-foreground mb-1.5">
              <span>
                <span className="font-semibold text-foreground">
                  {state.xpInto.toLocaleString()}
                </span>
                <span> / {state.xpRequired.toLocaleString()} XP</span>
              </span>
              <span className="font-semibold text-primary">
                {state.isMax ? "MAX" : `${state.xpPct}%`}
              </span>
            </div>
            <div className="relative h-2.5 rounded-full overflow-hidden bg-muted/60 dark:bg-muted/10">
              {[25, 50, 75].map((pct) => (
                <div
                  key={pct}
                  className="absolute top-0 bottom-0 w-px bg-background/40 z-10"
                  style={{ left: `${pct}%` }}
                />
              ))}
              <motion.div
                className={`absolute inset-y-0 left-0 rounded-full bg-gradient-to-r ${meta.gradient} ${meta.glow}`}
                initial={{ width: 0 }}
                animate={{ width: `${state.xpPct}%` }}
                transition={{ duration: 1.8, delay: 0.6, ease: [0.22, 1, 0.36, 1] }}
              />
            </div>
            <p className="mt-2.5 text-xs text-muted-foreground leading-none flex items-center justify-center md:justify-start gap-1 flex-wrap">
              {state.isMax ? (
                <>Max rank reached. Keep stacking XP!</>
              ) : (
                <>
                  <span>{xpRemaining.toLocaleString()} XP to</span>
                  {NextLevelIcon && (
                    <span className={`inline-flex items-center gap-1 font-bold ${nextMeta?.textColor}`}>
                      <NextLevelIcon className="h-3.5 w-3.5" />
                      {nextLv?.title}
                    </span>
                  )}
                </>
              )}
            </p>
          </div>
        </div>

        <div className="flex flex-row md:flex-col justify-center gap-3 shrink-0">
          {[
            {
              label: "Lessons",
              value: state.totalLessons.toString(),
              icon: BookOpen,
              color: "text-primary",
              bg: "bg-primary/5 dark:bg-primary/10",
              border: "border-primary/10",
            },
            {
              label: "This Week",
              value: `+${state.xpWeek}`,
              icon: Zap,
              color: "text-teal",
              bg: "bg-teal/5 dark:bg-teal/10",
              border: "border-teal/10",
            },
            {
              label: "Badges",
              value: `${state.earnedBadges.size}/${BADGE_DEFS.length}`,
              icon: Shield,
              color: "text-amber-500",
              bg: "bg-amber-500/5 dark:bg-amber-500/10",
              border: "border-amber-500/10",
            },
          ].map((s) => {
            const Icon = s.icon;
            return (
              <motion.div
                key={s.label}
                whileHover={{ scale: 1.03, y: -2 }}
                className={`flex items-center gap-2.5 rounded-2xl border ${s.border} ${s.bg} px-4 py-2.5 min-w-[115px] shadow-sm`}
              >
                <Icon
                  className={`h-4.5 w-4.5 shrink-0 ${s.color}`}
                  strokeWidth={2}
                />
                <div>
                  <p className="text-[9px] uppercase tracking-wider text-muted-foreground font-semibold leading-none mb-0.5">
                    {s.label}
                  </p>
                  <p className="text-sm font-black text-foreground leading-tight">
                    {s.value}
                  </p>
                </div>
              </motion.div>
            );
          })}
        </div>
      </div>
      <span className="sr-only">{earnedCount} badges earned</span>
    </motion.div>
  );
}
