import { motion } from "framer-motion";
import type { ReactNode } from "react";

interface SplitTextProps {
  children: string;
  className?: string;
  delay?: number;
  stagger?: number;
  as?: "h1" | "h2" | "h3" | "p" | "span";
}

const wordVariant = {
  hidden: { opacity: 0, y: 40, filter: "blur(12px)", rotateX: 45 },
  visible: { opacity: 1, y: 0, filter: "blur(0px)", rotateX: 0 },
};

export function SplitText({
  children,
  className = "",
  delay = 0,
  stagger = 0.06,
  as: Tag = "h1",
}: SplitTextProps) {
  const words = children.split(" ");

  return (
    <Tag className={className} style={{ perspective: "800px" }}>
      <motion.span
        initial="hidden"
        animate="visible"
        transition={{ staggerChildren: stagger, delayChildren: delay }}
        className="inline"
      >
        {words.map((word, i) => (
          <span key={`${word}-${i}`} className="inline-block overflow-hidden">
            <motion.span
              variants={wordVariant}
              transition={{
                duration: 0.8,
                ease: [0.16, 1, 0.3, 1],
              }}
              className="inline-block"
              style={{ transformOrigin: "center bottom" }}
            >
              {word}
              {i < words.length - 1 ? " " : ""}
            </motion.span>
          </span>
        ))}
      </motion.span>
    </Tag>
  );
}

interface SplitTextRevealProps {
  children: ReactNode;
  className?: string;
  delay?: number;
}

export function RevealBlock({
  children,
  className = "",
  delay = 0,
}: SplitTextRevealProps) {
  return (
    <div className={`overflow-hidden ${className}`}>
      <motion.div
        initial={{ y: "100%", opacity: 0 }}
        whileInView={{ y: "0%", opacity: 1 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{
          duration: 0.9,
          delay,
          ease: [0.16, 1, 0.3, 1],
        }}
      >
        {children}
      </motion.div>
    </div>
  );
}
