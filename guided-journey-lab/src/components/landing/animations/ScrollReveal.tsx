import { motion } from "framer-motion";
import type { ReactNode } from "react";

interface ScrollRevealProps {
  children: ReactNode;
  className?: string;
  delay?: number;
  direction?: "up" | "left" | "right" | "scale";
  duration?: number;
}

const directionMap = {
  up: { y: 60, x: 0, scale: 1, rotateX: 8 },
  left: { x: -80, y: 0, scale: 1, rotateX: 0 },
  right: { x: 80, y: 0, scale: 1, rotateX: 0 },
  scale: { scale: 0.9, y: 30, x: 0, rotateX: 0 },
};

export function ScrollReveal({
  children,
  className = "",
  delay = 0,
  direction = "up",
  duration = 0.9,
}: ScrollRevealProps) {
  const from = directionMap[direction];

  return (
    <motion.div
      className={className}
      initial={{
        opacity: 0,
        y: from.y,
        x: from.x,
        scale: from.scale,
        rotateX: from.rotateX,
        filter: "blur(8px)",
      }}
      whileInView={{
        opacity: 1,
        y: 0,
        x: 0,
        scale: 1,
        rotateX: 0,
        filter: "blur(0px)",
      }}
      viewport={{ once: true, margin: "-100px" }}
      transition={{
        duration,
        delay,
        ease: [0.16, 1, 0.3, 1],
      }}
      style={{ perspective: "1000px" }}
    >
      {children}
    </motion.div>
  );
}
