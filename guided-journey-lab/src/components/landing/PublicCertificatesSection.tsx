import { Award, FileText, ShieldCheck } from "lucide-react";
import { useRef, type ReactNode } from "react";
import { motion, useScroll, useTransform } from "framer-motion";
import { TextClipReveal, ScrollReveal } from "./animations";

const CERTIFICATE_STATS = [
  { value: "80%", label: "Min Passing Grade" },
  { value: "2FA", label: "Identity Verification" },
  { value: "Forever", label: "Permanent Validity" },
  { value: "JSON", label: "Metadata Exports" },
];

export function PublicCertificatesSection() {
  const sectionRef = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: sectionRef,
    offset: ["start end", "end start"],
  });

  const certRotate = useTransform(scrollYProgress, [0, 0.5, 1], [6, 0, -3]);
  const certY = useTransform(scrollYProgress, [0, 0.5, 1], [60, 0, -30]);
  const badgeY = useTransform(scrollYProgress, [0, 0.5, 1], [40, 0, -50]);

  return (
    <section
      ref={sectionRef}
      id="faculty"
      className="bg-primary px-5 py-20 text-white sm:px-6 lg:px-8 lg:py-32"
    >
      <div className="mx-auto grid max-w-[1280px] gap-12 lg:grid-cols-[minmax(0,0.88fr)_minmax(0,1.02fr)] lg:items-center lg:gap-16">
        <ScrollReveal>
          <div className="max-w-[540px]">
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-white/58">
              Credentialing
            </p>
            <TextClipReveal>
              <h2 className="mt-5 max-w-[10ch] pb-2 text-[clamp(2.4rem,5vw,4.2rem)] font-light leading-[1.06] tracking-[-0.05em] text-white">
                Certificates that actually <span className="italic text-white/68">carry weight.</span>
              </h2>
            </TextClipReveal>
            <p className="mt-7 max-w-[38ch] text-base leading-8 text-white/68 sm:text-[1.05rem]">
              EduLife credentials are tied to graded completion, issuer identity, and a public
              verification code so institutions and employers can trust the result.
            </p>

            <div className="mt-10 grid gap-6 sm:grid-cols-2">
              {CERTIFICATE_STATS.map((stat, i) => (
                <motion.div
                  key={stat.label}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, margin: "-60px" }}
                  transition={{
                    duration: 0.6,
                    delay: i * 0.08,
                    ease: [0.16, 1, 0.3, 1],
                  }}
                  className="border-t border-white/10 pt-5"
                >
                  <p className="text-[1.7rem] font-semibold tracking-[-0.04em] text-white">
                    {stat.value}
                  </p>
                  <p className="mt-1 text-[10px] font-semibold uppercase tracking-[0.2em] text-white/50">
                    {stat.label}
                  </p>
                </motion.div>
              ))}
            </div>
          </div>
        </ScrollReveal>

        <div className="relative" style={{ perspective: "1200px" }}>
          <motion.div
            animate={{
              scale: [1, 1.08, 1],
              opacity: [0.4, 0.6, 0.4],
            }}
            transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
            className="absolute inset-0 rounded-full bg-white/6 blur-3xl"
            aria-hidden
          />

          <motion.div
            style={{ rotateZ: certRotate, y: certY }}
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true, margin: "-80px" }}
            transition={{ duration: 1, ease: [0.16, 1, 0.3, 1] }}
            className="relative mx-auto max-w-[640px] rounded-[2rem] bg-[#e9edf3] p-4 shadow-[0_38px_92px_-36px_rgba(0,0,0,0.52)]"
          >
            <div className="relative overflow-hidden rounded-[1.5rem] bg-white p-8 sm:p-12">
              <div
                className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(9,20,38,0.05),transparent_52%)]"
                aria-hidden
              />
              <div className="relative z-10 flex flex-col items-center text-center">
                <span className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-[#dfe3e7] bg-[#f6fafe] text-primary">
                  <Award className="h-5 w-5" />
                </span>
                <p className="mt-6 text-[11px] font-semibold uppercase tracking-[0.24em] text-[#505f76]">
                  Certificate
                </p>
                <div className="mt-3 h-px w-[4.5rem] bg-[#dfe3e7]" />
                <p className="mt-4 text-[11px] uppercase tracking-[0.28em] text-[#505f76]">
                  Of Academic Mastery
                </p>

                <p className="mt-12 text-sm italic text-[#768397]">
                  This high-distinction award is presented to
                </p>
                <p className="mt-5 border-b border-[#dfe3e7] px-8 pb-4 text-[2rem] font-semibold tracking-[-0.05em] text-primary sm:text-[2.7rem]">
                  BAGHDAD Mohamed
                </p>
                <p className="mt-7 max-w-[34ch] text-[0.97rem] leading-7 text-[#505f76]">
                  For the rigorous completion of the Advanced Full-Stack Engineering program,
                  meeting EduLife standards for structured learning and final exam validation.
                </p>

                <div className="mt-12 grid w-full gap-4 sm:grid-cols-3">
                  <ProofChip
                    icon={<Award className="h-4 w-4" />}
                    label="Issuer"
                    value="Academic Board"
                  />
                  <ProofChip
                    icon={<ShieldCheck className="h-4 w-4" />}
                    label="Certificate"
                    value="#ED-992-QXA"
                  />
                  <ProofChip
                    icon={<FileText className="h-4 w-4" />}
                    label="Hash"
                    value="9f2a7c1e"
                  />
                </div>
              </div>
            </div>
          </motion.div>

          <motion.div
            style={{ y: badgeY }}
            initial={{ opacity: 0, scale: 0.85 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true, margin: "-60px" }}
            transition={{ duration: 0.8, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
            className="absolute right-0 top-6 rounded-[1.5rem] border border-white/12 bg-white p-5 text-primary shadow-[0_22px_56px_-30px_rgba(0,0,0,0.42)] sm:right-[-10px]"
          >
            <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#505f76]">
              Final score
            </p>
            <p className="mt-2 text-[2rem] font-semibold tracking-[-0.05em]">94%</p>
            <span className="mt-3 inline-flex rounded-full bg-[#eff4fb] px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.16em] text-primary">
              Exceptional
            </span>
          </motion.div>
        </div>
      </div>
    </section>
  );
}

function ProofChip({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-[1.3rem] border border-[#dfe3e7] bg-[#f6fafe] px-4 py-4 text-left">
      <p className="inline-flex items-center gap-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-[#768397]">
        {icon}
        {label}
      </p>
      <p className="mt-3 text-sm font-semibold tracking-[-0.02em] text-primary">{value}</p>
    </div>
  );
}
