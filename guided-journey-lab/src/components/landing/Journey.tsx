import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline, SectionKicker } from "./Section";
import { Compass, BookOpen, PlayCircle, ClipboardCheck, Sparkles, Award } from "lucide-react";

const steps = [
  {
    icon: Compass,
    title: "Discover",
    desc: "Browse the catalog with clear outcomes — no infinite content scroll.",
  },
  {
    icon: BookOpen,
    title: "Enroll",
    desc: "One transactional enrollment opens the full course and resets progress.",
  },
  {
    icon: PlayCircle,
    title: "Learn",
    desc: "Move lesson by lesson; progress is written back to the server, not the device.",
  },
  {
    icon: ClipboardCheck,
    title: "Take Exam",
    desc: "Backend-graded MCQ. Correct answers never leave the server.",
  },
  {
    icon: Sparkles,
    title: "Pass at 80%",
    desc: "Two failures triggers a 72-hour cooldown — designed to slow rushed retries.",
  },
  {
    icon: Award,
    title: "Earn Certificate",
    desc: "Issued only on pass. Public verification page anyone can hit.",
  },
];

export function Journey() {
  return (
    <section id="journey" className="relative py-32 lg:py-44 bg-surface overflow-hidden">
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-border to-transparent" />

      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <div className="grid lg:grid-cols-12 gap-10 items-end">
          <div className="lg:col-span-7">
            <SectionLabel>How it works</SectionLabel>
            <SectionHeadline>
              A learning journey{" "}
              <span className="italic font-normal text-muted-foreground">designed to finish.</span>
            </SectionHeadline>
          </div>
          <div className="lg:col-span-5">
            <SectionKicker>
              Six steps from first lesson to a verifiable credential. No optional detours, no
              AI-recommended rabbit holes.
            </SectionKicker>
          </div>
        </div>

        <div className="relative mt-20">
          {/* Track */}
          <div
            aria-hidden
            className="hidden lg:block absolute left-0 right-0 top-[60px] h-px"
            style={{
              background:
                "linear-gradient(90deg, transparent 0%, oklch(0.40 0.19 152 / 0.30) 12%, oklch(0.40 0.19 152 / 0.30) 88%, transparent 100%)",
            }}
          />
          <div
            aria-hidden
            className="hidden lg:block absolute left-0 top-[57px] h-1.5 w-3 rounded-full bg-primary/40"
          />
          <div
            aria-hidden
            className="hidden lg:block absolute right-0 top-[55px] grid h-3 w-3 place-items-center rounded-full bg-gold shadow-gold"
          />

          <ol className="grid lg:grid-cols-6 gap-y-12 lg:gap-x-3">
            {steps.map((step, i) => {
              const Icon = step.icon;
              const isCert = i === steps.length - 1;
              return (
                <motion.li
                  key={step.title}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, margin: "-60px" }}
                  transition={{ duration: 0.65, delay: i * 0.07, ease: [0.16, 1, 0.3, 1] }}
                  className="relative flex lg:flex-col items-start gap-5 lg:gap-5"
                >
                  <div className="relative shrink-0">
                    <div className="bezel">
                      <div
                        className={`bezel-inner grid h-[104px] w-[104px] place-items-center ${
                          isCert
                            ? "bg-gradient-gold text-gold-foreground"
                            : "bg-gradient-primary text-primary-foreground"
                        }`}
                      >
                        <Icon className="h-7 w-7" strokeWidth={1.4} />
                      </div>
                    </div>
                    <span className="absolute -top-2 -right-2 grid h-7 w-7 place-items-center rounded-full bg-primary text-primary-foreground text-[11px] font-medium font-mono shadow-bezel">
                      {String(i + 1).padStart(2, "0")}
                    </span>
                  </div>
                  <div className="lg:mt-2 max-w-[240px]">
                    <h3 className="text-display text-xl text-foreground">{step.title}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                      {step.desc}
                    </p>
                  </div>
                </motion.li>
              );
            })}
          </ol>
        </div>
      </div>
    </section>
  );
}
