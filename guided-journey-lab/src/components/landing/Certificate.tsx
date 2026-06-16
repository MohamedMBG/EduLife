import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";
import { ShieldCheck, Fingerprint, FileCheck2 } from "lucide-react";
import certImg from "@/assets/certificate.png";

const proof = [
  { Icon: FileCheck2, label: "Standard", value: "Exam pass required" },
  { Icon: Fingerprint, label: "Identifier", value: "Unique per learner" },
  { Icon: ShieldCheck, label: "Verification", value: "Public, no login" },
];

export function Certificate() {
  return (
    <section id="certificate" className="relative py-32 lg:py-44 overflow-hidden">
      <div className="absolute left-1/2 top-1/2 -z-10 h-[520px] w-[760px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-gradient-gold blur-3xl opacity-[0.18]" />

      <div className="mx-auto max-w-7xl px-6 lg:px-10 grid lg:grid-cols-12 gap-12 lg:gap-16 items-center">
        <div className="lg:col-span-5">
          <SectionLabel>Proof of achievement</SectionLabel>
          <SectionHeadline>
            Progress should{" "}
            <span className="italic font-normal text-muted-foreground">lead somewhere real.</span>
          </SectionHeadline>
          <p className="mt-8 max-w-[48ch] text-lg leading-relaxed text-muted-foreground">
            EduLife certificates are only issued after passing the final exam. Each one has a
            unique identifier, can be verified by anyone, and carries the same weight whether
            the learner studied on Android or web.
          </p>

          <dl className="mt-12 grid sm:grid-cols-3 gap-4">
            {proof.map((p, i) => (
              <motion.div
                key={p.label}
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-40px" }}
                transition={{ duration: 0.6, delay: i * 0.1, ease: [0.16, 1, 0.3, 1] }}
                className="relative rounded-2xl hairline bg-surface-elevated p-5"
              >
                <div className="flex items-center justify-between">
                  <dt className="text-[10px] uppercase tracking-[0.2em] text-muted-foreground font-mono">
                    {p.label}
                  </dt>
                  <p.Icon className="h-3.5 w-3.5 text-primary/70" strokeWidth={1.5} />
                </div>
                <dd className="mt-3 text-sm text-foreground font-medium">{p.value}</dd>
              </motion.div>
            ))}
          </dl>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 40, rotate: -3 }}
          whileInView={{ opacity: 1, y: 0, rotate: -2 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 1, ease: [0.16, 1, 0.3, 1] }}
          className="lg:col-span-7 relative"
        >
          <div className="absolute -inset-10 -z-10 bg-gradient-aurora blur-3xl opacity-50" />

          {/* watermark stamp */}
          <motion.div
            initial={{ opacity: 0, scale: 0.8, rotate: 12 }}
            whileInView={{ opacity: 1, scale: 1, rotate: 8 }}
            viewport={{ once: true }}
            transition={{ duration: 0.9, delay: 0.4, ease: [0.16, 1, 0.3, 1] }}
            className="absolute -top-6 right-6 z-20 hidden sm:grid h-24 w-24 place-items-center rounded-full bg-gradient-gold text-gold-foreground shadow-gold ring-4 ring-background"
          >
            <div className="text-center leading-none">
              <p className="text-[9px] tracking-[0.18em] font-mono uppercase">Verified</p>
              <p className="mt-1.5 text-display text-2xl">80+</p>
              <p className="text-[8px] tracking-[0.18em] font-mono uppercase">Score</p>
            </div>
          </motion.div>

          <div className="bezel animate-float">
            <div className="bezel-inner overflow-hidden">
              <img
                src={certImg}
                alt="EduLife verified certificate of completion"
                width={1280}
                height={896}
                loading="lazy"
                className="w-full h-auto"
              />
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
