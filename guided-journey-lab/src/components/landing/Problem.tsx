import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";
import { Check, X } from "lucide-react";

const chaos = [
  "Random YouTube playlists",
  "Shared PDFs in chat groups",
  "Conflicting advice everywhere",
  "No idea what to do next",
  "Nothing to prove you finished",
];

const flow = [
  "One structured course path",
  "Clear lesson-by-lesson progress",
  "A final exam that validates skill",
  "A certificate you actually earned",
  "Built for mobile, in your language",
];

export function Problem() {
  return (
    <section className="relative py-28 lg:py-40">
      <div className="mx-auto max-w-6xl px-6 lg:px-10">
        <div className="max-w-3xl">
          <SectionLabel>The old way</SectionLabel>
          <SectionHeadline>Learning is everywhere. Direction is not.</SectionHeadline>
          <p className="mt-6 text-lg text-muted-foreground leading-relaxed">
            Too many learners bounce between random videos, shared PDFs, chat groups, and
            disconnected advice. EduLife turns that confusion into one focused path.
          </p>
        </div>

        <div className="mt-16 grid md:grid-cols-2 gap-6">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-80px" }}
            transition={{ duration: 0.7 }}
            className="relative rounded-3xl border border-border bg-surface p-8 lg:p-10 overflow-hidden"
          >
            <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">Before</p>
            <h3 className="mt-3 text-2xl text-display">Fragmented</h3>
            <ul className="mt-8 space-y-4">
              {chaos.map((item, i) => (
                <li key={i} className="flex items-start gap-3 text-muted-foreground">
                  <span className="mt-1 grid h-5 w-5 place-items-center rounded-full bg-muted">
                    <X className="h-3 w-3" />
                  </span>
                  <span className="text-base">{item}</span>
                </li>
              ))}
            </ul>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-80px" }}
            transition={{ duration: 0.7, delay: 0.15 }}
            className="relative rounded-3xl border border-primary/20 bg-gradient-to-br from-primary to-primary-glow text-primary-foreground p-8 lg:p-10 overflow-hidden shadow-glow"
          >
            <div className="absolute -top-20 -right-20 h-60 w-60 rounded-full bg-white/10 blur-3xl" />
            <p className="text-xs uppercase tracking-[0.2em] text-primary-foreground/70">With EduLife</p>
            <h3 className="mt-3 text-2xl text-display">Guided</h3>
            <ul className="mt-8 space-y-4 relative">
              {flow.map((item, i) => (
                <li key={i} className="flex items-start gap-3">
                  <span className="mt-1 grid h-5 w-5 place-items-center rounded-full bg-gold text-gold-foreground">
                    <Check className="h-3 w-3" strokeWidth={3} />
                  </span>
                  <span className="text-base text-primary-foreground/95">{item}</span>
                </li>
              ))}
            </ul>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
