import { motion } from "framer-motion";
import { Link } from "@tanstack/react-router";
import { Shield, ArrowLeft } from "lucide-react";
import { LESSON_XP, CERT_XP } from "./level-types";

export function LevelSkeleton() {
  return (
    <div className="space-y-6">
      <div className="rounded-3xl border border-border bg-surface-elevated/40 h-48 animate-pulse glass" />
      <div className="grid md:grid-cols-2 gap-4">
        <div className="rounded-3xl border border-border bg-surface-elevated/40 h-44 animate-pulse glass" />
        <div className="rounded-3xl border border-border bg-surface-elevated/40 h-44 animate-pulse glass" />
      </div>
      <div className="rounded-3xl border border-border bg-surface-elevated/40 h-56 animate-pulse glass" />
      <div className="grid lg:grid-cols-[1fr_360px] gap-4">
        <div className="rounded-3xl border border-border bg-surface-elevated/40 h-56 animate-pulse glass" />
        <div className="rounded-3xl border border-border bg-surface-elevated/40 h-56 animate-pulse glass" />
      </div>
    </div>
  );
}

export function EmptyState() {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="rounded-3xl border border-border bg-surface-elevated p-10 text-center glass shadow-luxury grain py-16"
    >
      <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-primary/10 text-primary mb-6 shadow-sm">
        <Shield className="h-7 w-7 animate-pulse" strokeWidth={1.5} />
      </div>
      <h2 className="text-display text-3xl text-foreground mb-3 font-bold">
        Start Earning XP
      </h2>
      <p className="text-sm text-muted-foreground max-w-md mx-auto mb-8 leading-relaxed font-medium">
        Finish your first lesson to unlock levels, streaks, and achievements.
        Every completed lesson is worth <span className="text-primary font-bold">{LESSON_XP} XP</span> and each certificate adds{" "}
        <span className="text-amber-500 font-bold">{CERT_XP} XP</span>.
      </p>
      <Link
        to="/explore"
        className="inline-flex items-center gap-2 rounded-2xl bg-gradient-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-glow hover:scale-[1.03] transition-all duration-300"
      >
        Explore Courses
        <ArrowLeft className="h-4 w-4 rotate-180" />
      </Link>
    </motion.div>
  );
}
