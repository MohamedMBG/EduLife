import { useRef, useEffect } from "react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import type { ReactNode } from "react";

gsap.registerPlugin(ScrollTrigger);

interface TextClipRevealProps {
  children: ReactNode;
  className?: string;
  direction?: "left" | "bottom";
}

export function TextClipReveal({
  children,
  className = "",
  direction = "left",
}: TextClipRevealProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)"
    ).matches;
    if (prefersReducedMotion || !ref.current) return;

    const el = ref.current;
    const clipFrom =
      direction === "left"
        ? "inset(0 100% 0 0)"
        : "inset(100% 0 0 0)";
    const clipTo = "inset(0 0% 0 0)";

    gsap.set(el, { clipPath: clipFrom, opacity: 1 });

    const ctx = gsap.context(() => {
      gsap.to(el, {
        clipPath: clipTo,
        duration: 1.2,
        ease: "power3.out",
        scrollTrigger: {
          trigger: el,
          start: "top 85%",
          end: "top 40%",
          scrub: 0.6,
        },
      });
    });

    return () => ctx.revert();
  }, [direction]);

  return (
    <div ref={ref} className={className} style={{ opacity: 0 }}>
      {children}
    </div>
  );
}
