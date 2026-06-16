import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowUpRight, CheckCircle2, Mail } from "lucide-react";

export function FinalCTA() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!email) return;
    setSubmitted(true);
  }

  return (
    <section id="cta" className="relative py-32 lg:py-44 overflow-hidden">
      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <motion.div
          initial={{ opacity: 0, y: 36, filter: "blur(6px)" }}
          whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 1, ease: [0.16, 1, 0.3, 1] }}
          className="relative overflow-hidden rounded-[2.5rem] bg-gradient-to-br from-primary via-primary to-primary-glow text-primary-foreground shadow-luxury"
        >
          <div className="absolute -top-40 -left-32 h-96 w-96 rounded-full bg-gold/30 blur-3xl" />
          <div className="absolute -bottom-40 -right-32 h-96 w-96 rounded-full bg-teal/30 blur-3xl" />
          <div
            className="absolute inset-0 opacity-[0.04] mix-blend-overlay"
            style={{
              backgroundImage:
                "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='160' height='160'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>\")",
            }}
          />

          <div className="relative grid lg:grid-cols-12 gap-10 lg:gap-16 p-10 sm:p-14 lg:p-20 items-center">
            <div className="lg:col-span-7">
              <span className="inline-flex items-center gap-2 rounded-full bg-primary-foreground/12 border border-primary-foreground/20 px-3 py-1 text-[10px] uppercase tracking-[0.2em] font-medium">
                <span className="h-1 w-1 rounded-full bg-gold" />
                Joining now · Pilot cohort
              </span>
              <h2 className="mt-7 text-display text-[clamp(2.25rem,5vw,4.5rem)] leading-[1.02] max-w-[16ch]">
                Start the smarter way to{" "}
                <span className="italic font-normal text-gold">learn</span>.
              </h2>
              <p className="mt-6 max-w-[44ch] text-lg leading-relaxed text-primary-foreground/80">
                One platform, one path — built for Moroccan learners. Join the waitlist and
                we'll send your access link before the public launch.
              </p>
            </div>

            <div className="lg:col-span-5">
              <AnimatePresence mode="wait">
                {submitted ? (
                  <motion.div
                    key="success"
                    initial={{ opacity: 0, scale: 0.96 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.96 }}
                    transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                    className="relative rounded-3xl bg-primary-foreground/10 border border-primary-foreground/20 backdrop-blur-sm p-8"
                  >
                    <span className="grid h-12 w-12 place-items-center rounded-2xl bg-gold text-gold-foreground shadow-bezel">
                      <CheckCircle2 className="h-5 w-5" strokeWidth={1.75} />
                    </span>
                    <p className="mt-5 text-display text-2xl">You're on the list.</p>
                    <p className="mt-2 text-sm text-primary-foreground/75">
                      We'll be in touch with your private access link before launch.
                    </p>
                  </motion.div>
                ) : (
                  <motion.form
                    key="form"
                    onSubmit={handleSubmit}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0 }}
                    className="relative rounded-3xl bg-primary-foreground/8 border border-primary-foreground/20 backdrop-blur-sm p-3"
                  >
                    <label
                      htmlFor="cta-email"
                      className="sr-only"
                    >
                      Email address
                    </label>
                    <div className="relative flex items-center">
                      <span className="absolute left-4 text-primary-foreground/60">
                        <Mail className="h-4 w-4" strokeWidth={1.5} />
                      </span>
                      <input
                        id="cta-email"
                        type="email"
                        required
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="your@email.com"
                        className="w-full h-14 rounded-2xl bg-primary-foreground/8 border border-primary-foreground/15 pl-11 pr-44 text-sm text-primary-foreground placeholder:text-primary-foreground/45 outline-none focus:border-gold focus:bg-primary-foreground/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
                      />
                      <button
                        type="submit"
                        className="group absolute right-1.5 inline-flex h-11 items-center gap-1 rounded-full bg-gold text-gold-foreground pl-5 pr-1 text-sm font-medium shadow-bezel transition-transform duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.97]"
                      >
                        <span>Join Waitlist</span>
                        <span className="grid h-9 w-9 place-items-center rounded-full bg-gold-foreground/12 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-x-0.5 group-hover:-translate-y-px group-hover:bg-gold-foreground/20">
                          <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
                        </span>
                      </button>
                    </div>
                    <p className="mt-3 px-2 text-[11px] text-primary-foreground/65 leading-relaxed">
                      No spam. Single access email when the pilot cohort opens.
                    </p>
                  </motion.form>
                )}
              </AnimatePresence>

              <a
                href="#journey"
                className="mt-6 inline-flex items-center gap-2 text-sm text-primary-foreground/80 hover:text-primary-foreground transition-colors group"
              >
                <span className="relative">
                  See how the journey works
                  <span className="absolute -bottom-0.5 left-0 h-px w-0 bg-primary-foreground transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:w-full" />
                </span>
                <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
              </a>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
