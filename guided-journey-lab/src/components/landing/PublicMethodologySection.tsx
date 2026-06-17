import { BookOpen, Compass, GraduationCap, Shield } from "lucide-react";
import { useRef, useEffect } from "react";
import { motion } from "framer-motion";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { TextClipReveal } from "./animations";

gsap.registerPlugin(ScrollTrigger);

const METHODOLOGY_STEPS = [
  {
    number: "01",
    title: "Discover",
    description: "Browse structured paths built around practical outcomes instead of noise.",
    icon: Compass,
  },
  {
    number: "02",
    title: "Learn",
    description:
      "Follow lessons and resources in one sequence with progress written back instantly.",
    icon: BookOpen,
  },
  {
    number: "03",
    title: "Verify",
    description: "Submit the final MCQ exam and let the backend score the attempt securely.",
    icon: Shield,
  },
  {
    number: "04",
    title: "Graduate",
    description:
      "Receive a verifiable certificate that can be checked through a public proof link.",
    icon: GraduationCap,
    dark: true,
  },
];

export function PublicMethodologySection() {
  const sectionRef = useRef<HTMLElement>(null);
  const trackRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)"
    ).matches;
    if (prefersReducedMotion || !sectionRef.current || !trackRef.current) return;

    const track = trackRef.current;

    const ctx = gsap.context(() => {
      const cards = gsap.utils.toArray<HTMLElement>(".method-card");
      const totalWidth = track.scrollWidth;
      const viewportWidth = window.innerWidth;
      const distance = totalWidth - viewportWidth;

      if (distance <= 0) return;

      gsap.to(track, {
        x: -distance,
        ease: "none",
        scrollTrigger: {
          trigger: sectionRef.current,
          start: "top top",
          end: () => `+=${distance * 1.2}`,
          pin: true,
          scrub: 1,
          invalidateOnRefresh: true,
        },
      });

      cards.forEach((card, i) => {
        gsap.from(card, {
          opacity: 0.3,
          scale: 0.92,
          rotateY: -8,
          ease: "none",
          scrollTrigger: {
            trigger: card,
            containerAnimation: gsap.getById?.("horizontal") as gsap.core.Tween | undefined,
            start: "left 80%",
            end: "left 40%",
            scrub: true,
          },
        });
      });
    }, sectionRef);

    return () => ctx.revert();
  }, []);

  return (
    <section
      id="programs"
      ref={sectionRef}
      className="relative overflow-hidden"
    >
      {/* Header - shown above the horizontal scroll area */}
      <div className="px-5 pb-6 pt-20 sm:px-6 lg:px-8 lg:pt-28">
        <div className="mx-auto max-w-[1280px]">
          <TextClipReveal>
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#505f76]">
              Methodology
            </p>
            <h2 className="mt-5 pb-2 text-[clamp(2.4rem,5vw,4.2rem)] font-light leading-[1.06] tracking-[-0.05em] text-primary">
              Built to be <span className="italic text-[#505f76]">finished.</span>
            </h2>
          </TextClipReveal>
        </div>
      </div>

      {/* Horizontal scroll track */}
      <div
        ref={trackRef}
        className="flex items-center gap-6 px-5 pb-20 sm:px-6 lg:gap-8 lg:px-8"
        style={{ width: "fit-content" }}
      >
        {/* Spacer so first card doesn't start at edge */}
        <div className="w-[calc((100vw-1280px)/2)] shrink-0 max-lg:hidden" />

        {METHODOLOGY_STEPS.map((step, index) => {
          const Icon = step.icon;

          return (
            <motion.article
              key={step.number}
              className={`method-card relative w-[320px] shrink-0 overflow-hidden rounded-[2rem] border p-7 transition-transform duration-300 hover:-translate-y-1 sm:w-[380px] ${
                step.dark
                  ? "border-primary bg-primary text-white shadow-[0_30px_70px_-34px_rgba(9,20,38,0.52)]"
                  : "border-[#dfe3e7] bg-white text-primary shadow-[0_18px_48px_-40px_rgba(9,20,38,0.26)]"
              }`}
            >
              <span
                className={`absolute right-6 top-4 text-[4.6rem] font-semibold tracking-[-0.08em] ${
                  step.dark ? "text-white/10" : "text-primary/7"
                }`}
                aria-hidden
              >
                {step.number}
              </span>

              <span
                className={`inline-flex h-12 w-12 items-center justify-center rounded-[1.1rem] border ${
                  step.dark
                    ? "border-white/10 bg-white/8 text-white"
                    : "border-[#dfe3e7] bg-[#f0f4f8] text-primary"
                }`}
              >
                <Icon className="h-5 w-5" />
              </span>

              <h3 className="mt-16 text-[1.45rem] font-semibold tracking-[-0.04em]">
                {step.title}
              </h3>
              <p
                className={`mt-4 max-w-[25ch] text-sm leading-7 ${
                  step.dark ? "text-white/68" : "text-[#505f76]"
                }`}
              >
                {step.description}
              </p>
            </motion.article>
          );
        })}

        {/* End spacer */}
        <div className="w-16 shrink-0" />
      </div>
    </section>
  );
}
