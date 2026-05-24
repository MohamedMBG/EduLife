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
  { value: 100, suffix: "+", label: "Lessons ready for guided learning" },
  { value: 3, suffix: "", label: "Learning languages supported" },
  { value: 6, suffix: "", label: "Steps from first lesson to certificate" },
  { value: 80, suffix: "%", label: "Pass score standard for certification" },
];

export function Stats() {
  return (
    <section className="relative py-28 lg:py-40 bg-surface">
      <div className="mx-auto max-w-6xl px-6 lg:px-10">
        <div className="max-w-3xl">
          <SectionLabel>The vision</SectionLabel>
          <SectionHeadline>Structured to scale meaningful learning.</SectionHeadline>
        </div>

        <div className="mt-16 grid grid-cols-2 lg:grid-cols-4 gap-6 lg:gap-4">
          {stats.map((s, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-60px" }}
              transition={{ duration: 0.6, delay: i * 0.08 }}
              className="rounded-3xl border border-border bg-surface-elevated p-8 lg:p-10"
            >
              <div className="text-display text-5xl lg:text-6xl bg-gradient-to-br from-primary to-primary-glow bg-clip-text text-transparent">
                <Counter to={s.value} suffix={s.suffix} />
              </div>
              <p className="mt-4 text-sm text-muted-foreground leading-relaxed">{s.label}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
