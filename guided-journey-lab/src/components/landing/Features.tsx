import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline, SectionKicker } from "./Section";
import { Layers, TrendingUp, FileCheck, Medal, Smartphone, Languages } from "lucide-react";

export function Features() {
  return (
    <section id="features" className="relative py-32 lg:py-44">
      <div className="absolute inset-x-0 top-0 -z-10 h-72 bg-gradient-to-b from-surface/60 to-transparent" />

      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <div className="grid lg:grid-cols-12 gap-10 items-end">
          <div className="lg:col-span-7">
            <SectionLabel>What makes it different</SectionLabel>
            <SectionHeadline>
              Everything that matters.{" "}
              <span className="italic font-normal text-muted-foreground">Nothing fragmented.</span>
            </SectionHeadline>
          </div>
          <div className="lg:col-span-5">
            <SectionKicker>
              Six tightly-scoped pillars. No bolted-on dashboards, no busywork — every screen
              earns its place in the learning loop.
            </SectionKicker>
          </div>
        </div>

        <div className="mt-20 grid grid-cols-1 md:grid-cols-6 gap-4 md:gap-5 auto-rows-[minmax(220px,auto)]">
          {/* Lead card: Structured Courses */}
          <FeatureLead
            className="md:col-span-4 md:row-span-2"
            Icon={Layers}
            label="01 · Structure"
            title="Courses with a beginning, middle, and a final."
            body="Every course follows a known path: sections → lessons → exam → certificate. Designed so learners always know what's next, never what's optional."
            tone="primary"
          />

          <FeatureCard
            className="md:col-span-2"
            Icon={TrendingUp}
            label="02 · Progress"
            title="Lesson-by-lesson tracking."
            body="Marked complete on the server, not the client."
          />

          <FeatureCard
            className="md:col-span-2"
            Icon={FileCheck}
            label="03 · Exam"
            title="Backend-graded MCQ."
            body="Answers never leave the server. Two failures → 72-hour cooldown."
          />

          <FeatureCard
            className="md:col-span-3"
            Icon={Medal}
            label="04 · Certificate"
            title="Issued only after passing."
            body="A signed, verifiable certificate with a unique ID — not a participation badge."
            accent="gold"
          />

          <FeatureCard
            className="md:col-span-3"
            Icon={Smartphone}
            label="05 · Mobile"
            title="The way Morocco actually studies."
            body="A native Android client and a fast web app sharing the same backend contracts."
          />

          <FeatureCard
            className="md:col-span-6"
            Icon={Languages}
            label="06 · Language"
            title="Darija, Français, English — equally first-class."
            body="Course content, UI strings, and certificates all respect the learner's language. Multilingual is not an afterthought tab on a settings screen."
            wide
          />
        </div>
      </div>
    </section>
  );
}

function FeatureLead({
  className = "",
  Icon,
  label,
  title,
  body,
  tone = "primary",
}: {
  className?: string;
  Icon: typeof Layers;
  label: string;
  title: string;
  body: string;
  tone?: "primary" | "gold";
}) {
  return (
    <motion.article
      initial={{ opacity: 0, y: 26, filter: "blur(6px)" }}
      whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
      viewport={{ once: true, margin: "-60px" }}
      transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
      className={`group relative overflow-hidden rounded-[2rem] hairline bg-surface-elevated p-8 lg:p-10 ${className}`}
    >
      <div
        className={`pointer-events-none absolute -top-32 -right-24 h-[420px] w-[420px] rounded-full blur-3xl opacity-40 ${
          tone === "gold" ? "bg-gold/40" : "bg-primary/25"
        }`}
      />
      <div className="relative flex h-full flex-col">
        <div className="flex items-center justify-between">
          <span className="text-[10px] uppercase tracking-[0.2em] text-muted-foreground font-mono">
            {label}
          </span>
          <div className="bezel">
            <div className="bezel-inner grid h-12 w-12 place-items-center bg-gradient-primary text-primary-foreground">
              <Icon className="h-5 w-5" strokeWidth={1.5} />
            </div>
          </div>
        </div>

        <h3 className="mt-auto pt-16 text-display text-3xl sm:text-4xl leading-[1.05] text-foreground max-w-[18ch]">
          {title}
        </h3>
        <p className="mt-5 max-w-[42ch] text-base leading-relaxed text-muted-foreground">{body}</p>
      </div>
    </motion.article>
  );
}

function FeatureCard({
  className = "",
  Icon,
  label,
  title,
  body,
  accent,
  wide = false,
}: {
  className?: string;
  Icon: typeof Layers;
  label: string;
  title: string;
  body: string;
  accent?: "gold";
  wide?: boolean;
}) {
  return (
    <motion.article
      initial={{ opacity: 0, y: 22 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-50px" }}
      transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
      className={`group relative overflow-hidden rounded-[1.5rem] hairline bg-surface-elevated p-6 lg:p-7 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 hover:shadow-elevated ${className}`}
    >
      <div className="flex items-start gap-4">
        <span
          className={`grid h-10 w-10 shrink-0 place-items-center rounded-xl shadow-bezel ${
            accent === "gold"
              ? "bg-gradient-gold text-gold-foreground"
              : "bg-primary/10 text-primary group-hover:bg-gradient-primary group-hover:text-primary-foreground transition-colors duration-500"
          }`}
        >
          <Icon className="h-4 w-4" strokeWidth={1.5} />
        </span>
        <p className="text-[10px] uppercase tracking-[0.2em] text-muted-foreground font-mono pt-2.5">
          {label}
        </p>
      </div>
      <h3
        className={`mt-6 text-display leading-snug text-foreground ${
          wide ? "text-2xl lg:text-[1.7rem]" : "text-xl"
        }`}
      >
        {title}
      </h3>
      <p className="mt-3 text-sm leading-relaxed text-muted-foreground max-w-[48ch]">{body}</p>
    </motion.article>
  );
}
