import { BookOpen, Compass, GraduationCap, Shield } from "lucide-react";
import { motion } from "framer-motion";

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
  return (
    <section id="programs" className="px-5 py-20 sm:px-6 lg:px-8 lg:py-28">
      <div className="mx-auto max-w-[1280px]">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
          className="max-w-[560px]"
        >
          <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#505f76]">
            Methodology
          </p>
          <h2 className="mt-5 pb-2 text-[clamp(2.4rem,5vw,4.2rem)] font-light leading-[1.06] tracking-[-0.05em] text-primary">
            Built to be <span className="italic text-[#505f76]">finished.</span>
          </h2>
        </motion.div>

        <div className="mt-12 grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {METHODOLOGY_STEPS.map((step, index) => {
            const Icon = step.icon;

            return (
              <motion.article
                key={step.number}
                initial={{ opacity: 0, y: 24 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-100px" }}
                transition={{
                  duration: 0.64,
                  delay: index * 0.06,
                  ease: [0.16, 1, 0.3, 1],
                }}
                className={`relative overflow-hidden rounded-[2rem] border p-7 transition-transform duration-300 hover:-translate-y-1 ${
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
        </div>
      </div>
    </section>
  );
}
