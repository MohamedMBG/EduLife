import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";
import { Compass, Target, Globe2 } from "lucide-react";

const panels = [
  {
    Icon: Compass,
    accent: "01 · Direction",
    headline: "Guidance, not a content firehose.",
    body: "Every course follows a structured path. No rabbit holes, no decision fatigue — just a clear next step from first lesson to final certificate.",
    metric: { value: "6", label: "ordered steps per course" },
  },
  {
    Icon: Target,
    accent: "02 · Completion",
    headline: "Built around finishing, not browsing.",
    body: "Progress tracking, exam gates at 80%, and earned certificates create real accountability. Start something. Finish it. Prove it.",
    metric: { value: "80%", label: "pass threshold, server-enforced" },
  },
  {
    Icon: Globe2,
    accent: "03 · Outcomes",
    headline: "Designed for access, trust, and real results.",
    body: "Multilingual, mobile-first, and shaped around the realities of Moroccan learners — not copied from a Western LMS template.",
    metric: { value: "3", label: "first-class languages" },
  },
];

export function WhyEduLife() {
  return (
    <section className="relative py-32 lg:py-44 bg-surface overflow-hidden">
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-border to-transparent" />
      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <div className="max-w-3xl">
          <SectionLabel>Why EduLife</SectionLabel>
          <SectionHeadline>
            Built for clarity,{" "}
            <span className="italic font-normal text-muted-foreground">not content overload.</span>
          </SectionHeadline>
        </div>

        <div className="mt-20 grid lg:grid-cols-12 gap-6 lg:gap-5">
          {panels.map((p, i) => (
            <motion.article
              key={p.accent}
              initial={{ opacity: 0, y: 24, filter: "blur(4px)" }}
              whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              viewport={{ once: true, margin: "-60px" }}
              transition={{ duration: 0.8, delay: i * 0.12, ease: [0.16, 1, 0.3, 1] }}
              className={`group relative overflow-hidden rounded-[1.75rem] hairline bg-surface-elevated p-8 lg:p-10 flex flex-col ${
                i === 0
                  ? "lg:col-span-5 lg:row-span-2 lg:min-h-[480px]"
                  : i === 1
                  ? "lg:col-span-7 lg:col-start-6"
                  : "lg:col-span-7 lg:col-start-6"
              }`}
            >
              {/* tinted hairline top */}
              <div className="absolute inset-x-8 top-0 h-px bg-gradient-to-r from-transparent via-primary/40 to-transparent" />
              {/* corner glyph */}
              <div className="absolute -bottom-10 -right-10 h-44 w-44 rounded-full bg-primary/5 blur-2xl" />

              <div className="flex items-center justify-between">
                <span className="text-[10px] uppercase tracking-[0.2em] text-primary font-mono font-semibold">
                  {p.accent}
                </span>
                <span className="bezel">
                  <span className="bezel-inner grid h-11 w-11 place-items-center bg-primary/10 text-primary group-hover:bg-gradient-primary group-hover:text-primary-foreground transition-colors duration-500">
                    <p.Icon className="h-5 w-5" strokeWidth={1.5} />
                  </span>
                </span>
              </div>

              <h3
                className={`mt-8 text-display leading-snug text-foreground ${
                  i === 0 ? "text-3xl lg:text-[2.2rem] lg:max-w-[14ch] mt-auto pt-12" : "text-2xl"
                }`}
              >
                {p.headline}
              </h3>
              <p className="mt-4 text-base leading-relaxed text-muted-foreground max-w-[46ch]">
                {p.body}
              </p>

              <div className="mt-8 pt-6 border-t border-border/60 flex items-baseline gap-3">
                <span className="text-display text-3xl text-foreground">{p.metric.value}</span>
                <span className="text-xs text-muted-foreground tracking-wide">{p.metric.label}</span>
              </div>
            </motion.article>
          ))}
        </div>
      </div>
    </section>
  );
}
