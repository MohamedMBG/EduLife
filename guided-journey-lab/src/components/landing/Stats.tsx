import { motion, useInView, useMotionValue, useTransform, animate } from "framer-motion";
import { useEffect, useRef } from "react";
import { SectionLabel, SectionHeadline } from "./Section";

function Counter({ to, suffix = "" }: { to: number; suffix?: string }) {
  const ref = useRef<HTMLSpanElement>(null);
  const inView = useInView(ref, { once: true, margin: "-80px" });
  const value = useMotionValue(0);
  const rounded = useTransform(value, (v) => Math.round(v).toString() + suffix);

  useEffect(() => {
    if (inView) {
      const controls = animate(value, to, { duration: 1.8, ease: [0.22, 1, 0.36, 1] });
      return () => controls.stop();
    }
  }, [inView, to, value]);

  return <motion.span ref={ref}>{rounded}</motion.span>;
}

const stats = [
  {
    value: 100,
    suffix: "+",
    label: "Lessons authored for guided learning",
    note: "Across the live catalog",
  },
  {
    value: 3,
    suffix: "",
    label: "First-class languages",
    note: "Darija · Français · English",
    accent: "gold" as const,
  },
  {
    value: 6,
    suffix: "",
    label: "Steps from first lesson to certificate",
    note: "Discover → Enroll → Pass",
  },
  {
    value: 80,
    suffix: "%",
    label: "Pass standard for certification",
    note: "Server-enforced, no client trust",
  },
];

export function Stats() {
  return (
    <section className="relative py-32 lg:py-44 bg-surface overflow-hidden">
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-border to-transparent" />
      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <div className="grid lg:grid-cols-12 gap-10 items-end">
          <div className="lg:col-span-7">
            <SectionLabel>The vision</SectionLabel>
            <SectionHeadline>
              Structured to scale{" "}
              <span className="italic font-normal text-muted-foreground">
                meaningful learning.
              </span>
            </SectionHeadline>
          </div>
        </div>

        <div className="mt-20 grid grid-cols-2 lg:grid-cols-12 gap-4 lg:gap-5">
          {stats.map((s, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 24, filter: "blur(4px)" }}
              whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              viewport={{ once: true, margin: "-60px" }}
              transition={{ duration: 0.7, delay: i * 0.08, ease: [0.16, 1, 0.3, 1] }}
              className={`relative overflow-hidden rounded-[1.5rem] hairline bg-surface-elevated p-7 lg:p-9 ${
                i === 0
                  ? "lg:col-span-5 lg:row-span-2 min-h-[260px] lg:min-h-[340px]"
                  : "lg:col-span-7 grid grid-cols-3 items-end gap-5"
              }`}
            >
              {i === 0 ? (
                <>
                  <span className="text-[10px] uppercase tracking-[0.2em] font-mono text-muted-foreground">
                    /01
                  </span>
                  <div className="mt-4 lg:mt-12">
                    <div className="text-display text-6xl lg:text-7xl text-gradient-primary leading-none">
                      <Counter to={s.value} suffix={s.suffix} />
                    </div>
                    <p className="mt-6 text-base text-foreground/85 leading-snug max-w-[20ch]">
                      {s.label}
                    </p>
                    <p className="mt-2 text-xs text-muted-foreground">{s.note}</p>
                  </div>
                </>
              ) : (
                <>
                  <div className="col-span-1">
                    <span className="text-[10px] uppercase tracking-[0.2em] font-mono text-muted-foreground">
                      /{String(i + 1).padStart(2, "0")}
                    </span>
                    <div
                      className={`mt-4 text-display text-5xl lg:text-6xl leading-none ${
                        s.accent === "gold"
                          ? "text-transparent bg-gradient-gold bg-clip-text"
                          : "text-foreground"
                      }`}
                    >
                      <Counter to={s.value} suffix={s.suffix} />
                    </div>
                  </div>
                  <div className="col-span-2">
                    <p className="text-sm text-foreground/85 leading-snug">{s.label}</p>
                    <p className="mt-1.5 text-xs text-muted-foreground">{s.note}</p>
                  </div>
                </>
              )}
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
