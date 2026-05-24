import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";
import { Compass, BookOpen, PlayCircle, ClipboardCheck, Sparkles, Award } from "lucide-react";

const steps = [
  { icon: Compass, title: "Discover", desc: "Browse structured courses with clear outcomes." },
  { icon: BookOpen, title: "Enroll", desc: "Start with confidence in a guided path." },
  { icon: PlayCircle, title: "Learn", desc: "Move lesson by lesson with real progress tracking." },
  { icon: ClipboardCheck, title: "Take Exam", desc: "Validate understanding through a final MCQ exam." },
  { icon: Sparkles, title: "Pass", desc: "Meet the standard and unlock the next step." },
  { icon: Award, title: "Get Certificate", desc: "Receive a verified certificate you actually earned." },
];

export function Journey() {
  return (
    <section id="journey" className="relative py-28 lg:py-40 bg-surface">
      <div className="mx-auto max-w-6xl px-6 lg:px-10">
        <div className="max-w-3xl">
          <SectionLabel>How it works</SectionLabel>
          <SectionHeadline>A learning journey designed to finish.</SectionHeadline>
        </div>

        <div className="relative mt-20">
          {/* Vertical line on mobile, horizontal on desktop */}
          {/* top = half of icon box height (h-[88px] / 2) */}
          <div className="hidden lg:block absolute left-0 right-0 top-[calc(88px/2)] h-px bg-gradient-to-r from-transparent via-border to-transparent" />

          <div className="grid lg:grid-cols-6 gap-10 lg:gap-4">
            {steps.map((step, i) => {
              const Icon = step.icon;
              const isCert = i === steps.length - 1;
              return (
                <motion.div
                  key={step.title}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, margin: "-60px" }}
                  transition={{ duration: 0.6, delay: i * 0.08 }}
                  className="relative flex lg:flex-col items-start lg:items-center gap-5 lg:gap-4 text-left lg:text-center"
                >
                  <div className="relative z-10 grid h-[88px] w-[88px] shrink-0 place-items-center rounded-2xl border border-border bg-surface-elevated shadow-soft">
                    <div
                      className={`grid h-14 w-14 place-items-center rounded-xl ${
                        isCert
                          ? "bg-gradient-gold text-gold-foreground"
                          : "bg-gradient-to-br from-primary to-primary-glow text-primary-foreground"
                      }`}
                    >
                      <Icon className="h-6 w-6" strokeWidth={1.75} />
                    </div>
                    <span className="absolute -top-2 -right-2 grid h-6 w-6 place-items-center rounded-full bg-foreground text-background text-[10px] font-medium">
                      {i + 1}
                    </span>
                  </div>
                  <div>
                    <h3 className="text-lg text-display text-foreground">{step.title}</h3>
                    <p className="mt-2 text-sm text-muted-foreground leading-relaxed max-w-[180px]">
                      {step.desc}
                    </p>
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
