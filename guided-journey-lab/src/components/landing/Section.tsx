import { motion } from "framer-motion";
import type { ReactNode } from "react";

export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <motion.p
      initial={{ opacity: 0, y: 10 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.5 }}
      className="text-xs uppercase tracking-[0.24em] text-primary/80 font-medium"
    >
      {children}
    </motion.p>
  );
}

export function SectionHeadline({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <motion.h2
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ duration: 0.7 }}
      className={`text-display mt-4 text-4xl sm:text-5xl lg:text-6xl leading-[1.05] text-foreground ${className}`}
    >
      {children}
    </motion.h2>
  );
}
