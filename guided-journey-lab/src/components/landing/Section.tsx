import { motion } from "framer-motion";
import type { ReactNode } from "react";

export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <motion.span
      initial={{ opacity: 0, y: 8 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      className="eyebrow eyebrow-dot"
    >
      {children}
    </motion.span>
  );
}

export function SectionHeadline({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <motion.h2
      initial={{ opacity: 0, y: 20, filter: "blur(6px)" }}
      whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ duration: 0.85, ease: [0.16, 1, 0.3, 1] }}
      className={`text-display mt-6 text-[clamp(2.25rem,4.6vw,4.25rem)] leading-[1.02] text-foreground ${className}`}
    >
      {children}
    </motion.h2>
  );
}

export function SectionKicker({ children }: { children: ReactNode }) {
  return (
    <p className="mt-6 max-w-[58ch] text-lg leading-relaxed text-muted-foreground">{children}</p>
  );
}
