import { motion } from "framer-motion";
import { Trophy, CheckCircle, Lock } from "lucide-react";
import {
  LevelState,
  BADGE_DEFS,
  BadgeRarity,
  rarityConfig,
} from "./level-types";

export function AchievementsCard({ state }: { state: LevelState }) {
  const earnedCount = state.earnedBadges.size;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.45 }}
      className="space-y-4"
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <Trophy className="h-4.5 w-4.5 text-amber-500 animate-bounce" strokeWidth={2} />
          <p className="text-xs uppercase tracking-[0.28em] font-semibold text-muted-foreground">
            Achievements & Badges
          </p>
        </div>
        <div className="flex items-center gap-3">
          {(["common", "rare", "epic", "legendary"] as BadgeRarity[]).map((r) => (
            <span
              key={r}
              className={`hidden sm:inline text-[10px] font-bold uppercase tracking-wider ${rarityConfig[r].color}`}
            >
              {rarityConfig[r].label}
            </span>
          ))}
          <span className="text-xs font-bold text-muted-foreground bg-muted px-2.5 py-0.5 rounded-full border border-border">
            <span className="text-foreground">{earnedCount}</span>/{BADGE_DEFS.length}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
        {BADGE_DEFS.map((badge, i) => {
          const earned = state.earnedBadges.has(badge.key);
          const Icon = badge.icon;
          const r = rarityConfig[badge.rarity];

          // Generate a custom gradient background and glow based on badge rarity
          let badgeStyle = "bg-muted/30 border-border opacity-50 dark:bg-muted/5";
          if (earned) {
            switch (badge.rarity) {
              case "common":
                badgeStyle = "bg-slate-500/10 border-slate-500/20 text-slate-500 shadow-sm";
                break;
              case "rare":
                badgeStyle = "bg-primary/10 border-primary/20 text-primary shadow-glow shadow-[0_0_12px_rgba(20,120,60,0.15)]";
                break;
              case "epic":
                badgeStyle = "bg-violet-500/10 border-violet-500/20 text-violet-500 shadow-[0_0_12px_rgba(139,92,246,0.18)]";
                break;
              case "legendary":
                badgeStyle = "bg-amber-500/10 border-amber-500/25 text-amber-500 shadow-gold shadow-[0_0_16px_rgba(245,158,11,0.25)] shimmer animate-glow";
                break;
            }
          }

          return (
            <motion.div
              key={badge.key}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.4, delay: 0.05 + i * 0.03 }}
              whileHover={earned ? { scale: 1.05, rotate: 1, y: -4 } : {}}
              className={`relative flex flex-col gap-3.5 rounded-2xl border p-4.5 transition-all overflow-hidden ${badgeStyle}`}
            >
              {earned && badge.rarity === "legendary" && (
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full animate-shimmer pointer-events-none" />
              )}
              <div className="flex items-center justify-between">
                <div
                  className={`grid h-10 w-10 place-items-center rounded-xl ${
                    earned ? "bg-background/90 dark:bg-background/60 shadow-sm" : "bg-muted dark:bg-muted/20"
                  }`}
                >
                  <Icon
                    className={`h-5 w-5 ${earned ? r.color : "text-muted-foreground/30"}`}
                    strokeWidth={2}
                  />
                </div>
                <span
                  className={`text-[9px] uppercase tracking-wider font-extrabold ${
                    earned ? r.color : "text-muted-foreground/30"
                  }`}
                >
                  {badge.rarity}
                </span>
              </div>
              <div>
                <p
                  className={`text-xs font-black leading-tight ${
                    earned ? "text-foreground" : "text-muted-foreground/60"
                  }`}
                >
                  {badge.title}
                </p>
                <p className="text-[10px] mt-1 leading-tight text-muted-foreground/60 font-medium">
                  {badge.desc}
                </p>
              </div>
              {earned ? (
                <CheckCircle
                  className={`absolute top-2.5 right-2.5 h-3.5 w-3.5 ${r.color} opacity-70`}
                />
              ) : (
                <Lock className="absolute top-2.5 right-2.5 h-3.5 w-3.5 text-muted-foreground/30 dark:text-muted-foreground/20" />
              )}
            </motion.div>
          );
        })}
      </div>
    </motion.div>
  );
}
