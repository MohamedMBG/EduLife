import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline, SectionKicker } from "./Section";
import { ArrowDown, Check, Minus } from "lucide-react";

const chaos = [
  "Random YouTube playlists",
  "PDFs scattered across chat groups",
  "Conflicting advice everywhere",
  "No clear sense of what to do next",
  "Nothing to prove you finished",
];

const flow = [
  "One structured course path",
  "Lesson-by-lesson progress, written to the backend",
  "An exam that actually validates skill",
  "A signed, verifiable certificate",
  "Built for mobile, in your language",
];

export function Problem() {
  return (
    <section className="relative py-32 lg:py-44">
      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <div className="grid lg:grid-cols-12 gap-10 items-end">
          <div className="lg:col-span-7">
            <SectionLabel>The old way</SectionLabel>
            <SectionHeadline>
              Learning is everywhere.{" "}
              <span className="italic font-normal text-muted-foreground">Direction is not.</span>
            </SectionHeadline>
          </div>
          <div className="lg:col-span-5">
            <SectionKicker>
              Too many learners bounce between random videos, shared PDFs, chat groups, and
              disconnected advice. EduLife turns that confusion into one focused path.
            </SectionKicker>
          </div>
        </div>

        <div className="mt-20 grid lg:grid-cols-12 gap-6 items-stretch">
          <motion.article
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-80px" }}
            transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
            className="lg:col-span-5 relative overflow-hidden rounded-[2rem] hairline bg-surface p-8 lg:p-10"
          >
            <div className="flex items-center justify-between">
              <span className="eyebrow">Before</span>
              <span className="text-[10px] uppercase tracking-[0.18em] font-mono text-muted-foreground">
                Fragmented
              </span>
            </div>
            <h3 className="mt-8 text-display text-3xl lg:text-4xl text-foreground/70 line-through decoration-from-font decoration-muted-foreground/40">
              Scattered learning
            </h3>
            <ul className="mt-8 space-y-3.5">
              {chaos.map((item, i) => (
                <li
                  key={i}
                  className="flex items-start gap-3 text-muted-foreground text-[15px] leading-relaxed"
                >
                  <span className="mt-1 grid h-5 w-5 place-items-center rounded-full bg-muted">
                    <Minus className="h-3 w-3" strokeWidth={2} />
                  </span>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </motion.article>

          {/* connector */}
          <div className="hidden lg:flex lg:col-span-2 items-center justify-center">
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 0.7, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
              className="bezel"
            >
              <div className="bezel-inner grid h-14 w-14 place-items-center bg-gradient-primary text-primary-foreground">
                <ArrowDown className="h-5 w-5 -rotate-90" strokeWidth={1.5} />
              </div>
            </motion.div>
          </div>

          <motion.article
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-80px" }}
            transition={{ duration: 0.7, delay: 0.15, ease: [0.16, 1, 0.3, 1] }}
            className="lg:col-span-5 relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-primary via-primary to-primary-glow text-primary-foreground p-8 lg:p-10 shadow-glow"
          >
            <div className="absolute -top-24 -right-24 h-72 w-72 rounded-full bg-white/10 blur-3xl" />
            <div className="absolute -bottom-32 -left-20 h-64 w-64 rounded-full bg-gold/30 blur-3xl" />

            <div className="relative flex items-center justify-between">
              <span className="inline-flex items-center gap-2 rounded-full bg-primary-foreground/15 border border-primary-foreground/20 px-3 py-1 text-[10px] uppercase tracking-[0.18em] font-medium">
                <span className="h-1 w-1 rounded-full bg-gold" />
                With EduLife
              </span>
              <span className="text-[10px] uppercase tracking-[0.18em] font-mono text-primary-foreground/60">
                Guided
              </span>
            </div>
            <h3 className="relative mt-8 text-display text-3xl lg:text-4xl">
              One path, end to end.
            </h3>
            <ul className="relative mt-8 space-y-3.5">
              {flow.map((item, i) => (
                <li key={i} className="flex items-start gap-3 text-[15px] leading-relaxed">
                  <span className="mt-1 grid h-5 w-5 place-items-center rounded-full bg-gold text-gold-foreground shadow-bezel">
                    <Check className="h-3 w-3" strokeWidth={2.5} />
                  </span>
                  <span className="text-primary-foreground/95">{item}</span>
                </li>
              ))}
            </ul>
          </motion.article>
        </div>
      </div>
    </section>
  );
}
