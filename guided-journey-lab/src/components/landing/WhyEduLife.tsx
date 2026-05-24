import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";

const panels = [
  {
    accent: "Direction",
    headline: "A platform that guides learners forward, not sideways.",
    body: "Every course follows a structured path. No rabbit holes, no decision fatigue — just clear progress from first lesson to final certificate.",
  },
  {
    accent: "Completion",
    headline: "A system focused on completion, not passive browsing.",
    body: "Progress tracking, exam gates, and earned certificates create real accountability. Start something. Finish it. Prove it.",
  },
  {
    accent: "Outcomes",
    headline: "Designed for access, trust, and real outcomes.",
    body: "Multilingual, mobile-first, and built around the realities of Moroccan learners — not copied from a Western template.",
  },
];

export function WhyEduLife() {
  return (
    <section className="relative py-28 lg:py-40 bg-surface">
      <div className="mx-auto max-w-6xl px-6 lg:px-10">
        <div className="max-w-3xl">
          <SectionLabel>Why EduLife</SectionLabel>
          <SectionHeadline>Built for clarity, not content overload.</SectionHeadline>
        </div>

        <div className="mt-16 grid md:grid-cols-3 gap-5">
          {panels.map((p, i) => (
            <motion.div
              key={p.accent}
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-60px" }}
              transition={{ duration: 0.7, delay: i * 0.1 }}
              className="relative rounded-3xl border border-border bg-surface-elevated p-8 overflow-hidden"
            >
              <div className="absolute inset-x-8 top-0 h-px bg-gradient-to-r from-transparent via-primary/40 to-transparent" />
              <p className="text-xs uppercase tracking-[0.24em] text-primary/80">{p.accent}</p>
              <p className="mt-5 text-xl text-display leading-snug text-foreground">{p.headline}</p>
              <p className="mt-4 text-sm text-muted-foreground leading-relaxed">{p.body}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
