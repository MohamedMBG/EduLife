import { useState } from "react";
import { motion } from "framer-motion";
import { Scroll, Lock, CheckCircle, Sparkles } from "lucide-react";
import {
  LevelState,
  LEVEL_PATH,
  LEVEL_METADATA,
  LEVEL_THRESHOLDS,
} from "./level-types";

export function SkillTreeCard({ state }: { state: LevelState }) {
  const [selectedLvNum, setSelectedLvNum] = useState<number>(state.level);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.3 }}
      className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft glass grain relative overflow-hidden"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Scroll className="h-4.5 w-4.5 text-primary" strokeWidth={2} />
          <p className="text-xs uppercase tracking-[0.28em] font-semibold text-muted-foreground">
            Learning Path & Ranks
          </p>
        </div>
        <div className="flex items-center gap-3 text-xs font-semibold text-muted-foreground">
          <div className="flex items-center gap-1.5">
            <div className="h-2.5 w-2.5 rounded-full bg-gradient-primary shadow-[0_0_6px_oklch(var(--primary)/0.4)]" />
            Mastered
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-2.5 w-2.5 rounded-full border-2 border-primary bg-primary/10" />
            Current
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-2.5 w-2.5 rounded-full bg-muted border border-border" />
            Locked
          </div>
        </div>
      </div>

      <div className="overflow-x-auto pb-4 scrollbar-thin scrollbar-thumb-muted-foreground/25">
        <div className="relative min-w-max h-[190px]">
          {/* Connection lines for the entire tree in a single SVG */}
          <svg className="absolute inset-0 w-full h-full pointer-events-none overflow-visible">
            {LEVEL_PATH.map((lv, i) => {
              if (i === LEVEL_PATH.length - 1) return null;
              const topCurrent = i % 2 === 0 ? 16 : 96;
              const topNext = (i + 1) % 2 === 0 ? 16 : 96;
              const xCurrent = i * 112 + 16 + 32; // Center X of node (x_start + width/2)
              const yCurrent = topCurrent + 28;  // Center Y of node (top + height/2)
              const xNext = (i + 1) * 112 + 16 + 32;
              const yNext = topNext + 28;

              const done = lv.n < state.level;
              const active = lv.n === state.level;

              // Bezier control points for smooth S-curves
              const cp1x = xCurrent + 56;
              const cp1y = yCurrent;
              const cp2x = xNext - 56;
              const cp2y = yNext;

              return (
                <g key={`line-${i}`}>
                  {/* Glowing accent backdrop for active path */}
                  {(done || active) && (
                    <path
                      d={`M ${xCurrent} ${yCurrent} C ${cp1x} ${cp1y} ${cp2x} ${cp2y} ${xNext} ${yNext}`}
                      fill="none"
                      stroke="oklch(var(--primary) / 0.18)"
                      strokeWidth="6"
                      className="blur-[3px] transition-all duration-500"
                    />
                  )}
                  <path
                    d={`M ${xCurrent} ${yCurrent} C ${cp1x} ${cp1y} ${cp2x} ${cp2y} ${xNext} ${yNext}`}
                    fill="none"
                    stroke={done ? "oklch(var(--primary))" : "oklch(var(--border))"}
                    strokeWidth="3.5"
                    strokeDasharray={done ? "none" : "6 5"}
                    className="transition-all duration-500"
                  />
                </g>
              );
            })}
          </svg>

          {LEVEL_PATH.map((lv, i) => {
            const done = lv.n < state.level;
            const current = lv.n === state.level;
            const locked = lv.n > state.level;
            const top = i % 2 === 0 ? 16 : 96;
            const x = i * 112 + 16;
            const isSelected = selectedLvNum === lv.n;

            return (
              <div key={lv.n}>
                <motion.div
                  initial={{ opacity: 0, scale: 0.6 }}
                  animate={{ opacity: 1, scale: current ? 1.08 : 1 }}
                  transition={{ duration: 0.5, delay: 0.1 + i * 0.05, ease: [0.22, 1, 0.36, 1] }}
                  className="absolute flex flex-col items-center gap-1.5 cursor-pointer select-none"
                  style={{ left: x, top, width: 64 }}
                  onClick={() => setSelectedLvNum(lv.n)}
                >
                  {done && (
                    <div className="text-[9px] font-bold text-primary bg-primary/10 rounded-full px-1.5 py-0.5 -mb-0.5 shadow-sm border border-primary/10">
                      ✓ Done
                    </div>
                  )}
                  {current && (
                    <div className="text-[9px] font-bold text-primary animate-pulse -mb-0.5 flex items-center gap-0.5">
                      <div className="h-1.5 w-1.5 rounded-full bg-primary" />
                      Current
                    </div>
                  )}
                  {locked && (
                    <div className="text-[9px] text-muted-foreground/40 -mb-0.5 invisible">·</div>
                  )}

                  <div className="relative">
                    {current && (
                      <>
                        <motion.div
                          className="absolute inset-[-8px] rounded-2xl border-2 border-primary/45"
                          animate={{ scale: [1, 1.14, 1], opacity: [0.75, 0.15, 0.75] }}
                          transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut" }}
                        />
                        <motion.div
                          className="absolute inset-[-14px] rounded-2xl border border-primary/20"
                          animate={{ scale: [1, 1.2, 1], opacity: [0.4, 0.05, 0.4] }}
                          transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut", delay: 0.4 }}
                        />
                      </>
                    )}
                    {isSelected && (
                      <motion.div
                        layoutId="selectedNodeRing"
                        className="absolute inset-[-6px] rounded-2xl border-2 border-dashed border-primary/60 dark:border-primary/80 z-10 pointer-events-none"
                        transition={{ type: "spring", stiffness: 300, damping: 20 }}
                      />
                    )}
                    <motion.div
                      whileHover={locked ? {} : { scale: 1.06, y: -2 }}
                      className={`relative grid h-14 w-14 rounded-2xl border-2 place-items-center text-2xl transition-all shadow-sm ${
                        done
                          ? `bg-gradient-to-br ${LEVEL_METADATA[lv.n].gradient} border-transparent text-white shadow-glow`
                          : current
                          ? `bg-gradient-to-br ${LEVEL_METADATA[lv.n].gradient} border-primary text-white`
                          : "bg-muted/30 dark:bg-muted/10 border-border/80 text-muted-foreground/30"
                      }`}
                    >
                      {locked ? (
                        <Lock
                          className="h-5 w-5 text-muted-foreground/30 dark:text-muted-foreground/20"
                          strokeWidth={1.75}
                        />
                      ) : (
                        (() => {
                          const IconComponent = LEVEL_METADATA[lv.n].icon;
                          return (
                            <IconComponent
                              className={`h-5 w-5 ${
                                done || current
                                  ? "text-white"
                                  : LEVEL_METADATA[lv.n].textColor
                              }`}
                              strokeWidth={2.2}
                            />
                          );
                        })()
                      )}
                      <div
                        className={`absolute -bottom-1.5 -right-1.5 grid h-5 w-5 place-items-center rounded-full text-[9px] font-black border ${
                          done || current
                            ? `bg-gradient-to-br ${LEVEL_METADATA[lv.n].gradient} text-white border-white/20`
                            : "bg-muted dark:bg-muted/20 text-muted-foreground/45 border-border"
                        }`}
                      >
                        {lv.n}
                      </div>
                    </motion.div>
                  </div>

                  <p
                    className={`text-[10px] font-bold leading-tight text-center ${
                      done
                        ? "text-primary"
                        : current
                        ? "text-foreground"
                        : "text-muted-foreground/40"
                    }`}
                  >
                    {lv.title}
                  </p>
                </motion.div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Dynamic Detail Card */}
      <motion.div
        key={selectedLvNum}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="mt-2 mb-4 rounded-2xl border border-border bg-muted/30 dark:bg-muted/5 p-4 flex flex-col md:flex-row items-start md:items-center gap-4 relative overflow-hidden"
      >
        <div className="absolute -right-10 -bottom-10 opacity-[0.03] dark:opacity-[0.06] pointer-events-none">
          {(() => {
            const PreviewIcon = LEVEL_METADATA[selectedLvNum].icon;
            return <PreviewIcon className="w-32 h-32" strokeWidth={0.5} />;
          })()}
        </div>

        <div className={`flex h-12 w-12 shrink-0 place-items-center justify-center rounded-xl bg-gradient-to-br ${LEVEL_METADATA[selectedLvNum].gradient} text-white shadow-md`}>
          {(() => {
            const PreviewIcon = LEVEL_METADATA[selectedLvNum].icon;
            return <PreviewIcon className="h-6 w-6" strokeWidth={2.2} />;
          })()}
        </div>

        <div className="flex-1 min-w-0 z-10">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <h4 className="text-sm font-black text-foreground leading-none">
              Level {selectedLvNum}: {LEVEL_PATH[selectedLvNum - 1].title}
            </h4>
            {selectedLvNum < state.level ? (
              <span className="text-[10px] font-bold text-teal bg-teal/10 border border-teal/20 px-2 py-0.5 rounded-full flex items-center gap-1">
                <CheckCircle className="h-3 w-3" /> Completed
              </span>
            ) : selectedLvNum === state.level ? (
              <span className="text-[10px] font-bold text-primary bg-primary/10 border border-primary/20 px-2 py-0.5 rounded-full flex items-center gap-1 animate-pulse">
                <Sparkles className="h-3 w-3 animate-spin" style={{ animationDuration: "3s" }} /> In Progress
              </span>
            ) : (
              <span className="text-[10px] font-bold text-muted-foreground bg-muted border border-border px-2 py-0.5 rounded-full flex items-center gap-1">
                <Lock className="h-3 w-3" /> Locked
              </span>
            )}
          </div>
          <p className="text-xs text-muted-foreground mb-2 leading-relaxed">
            {LEVEL_METADATA[selectedLvNum].description}
          </p>
          <div className="flex items-center gap-3 text-[11px] font-medium text-muted-foreground/80">
            <span>Requires: <strong className="text-foreground">{LEVEL_THRESHOLDS[selectedLvNum - 1].toLocaleString()} XP</strong></span>
            <span>·</span>
            <span>
              {selectedLvNum < state.level ? (
                <span className="text-teal font-semibold">Milestone Unlocked!</span>
              ) : selectedLvNum === state.level ? (
                <span>Remaining: <strong className="text-primary">{Math.max(0, state.xpRequired - state.xpInto).toLocaleString()} XP</strong></span>
              ) : (
                <span>Needs <strong className="text-foreground">{(LEVEL_THRESHOLDS[selectedLvNum - 1] - state.totalXp).toLocaleString()} more XP</strong> to reach</span>
              )}
            </span>
          </div>
        </div>
      </motion.div>

      <div className="mt-4 flex items-center gap-3">
        <div className="flex-1 h-2 rounded-full bg-muted/60 dark:bg-muted/10 overflow-hidden">
          <motion.div
            className="h-full rounded-full bg-gradient-primary shadow-[0_0_6px_oklch(var(--primary)/0.3)]"
            initial={{ width: 0 }}
            animate={{
              width: `${((state.level - 1) / LEVEL_PATH.length) * 100}%`,
            }}
            transition={{ duration: 1.2, delay: 0.8, ease: [0.22, 1, 0.36, 1] }}
          />
        </div>
        <span className="text-xs font-bold text-primary shrink-0">
          {state.level - 1}/{LEVEL_PATH.length} ranks achieved
        </span>
      </div>
    </motion.div>
  );
}
