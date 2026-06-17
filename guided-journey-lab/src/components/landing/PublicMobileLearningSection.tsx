import { Smartphone } from "lucide-react";
import { toast } from "sonner";
import { motion } from "framer-motion";

export function PublicMobileLearningSection() {
  function handleAndroidPilotClick() {
    toast.success("Android pilot access is coming soon.", {
      description: "The Play Store release will follow after the public web pilot.",
    });
  }

  return (
    <section className="px-5 py-20 sm:px-6 lg:px-8 lg:py-28">
      <div className="mx-auto grid max-w-[1280px] gap-12 lg:grid-cols-[minmax(0,0.78fr)_minmax(0,1fr)] lg:items-center lg:gap-16">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.72, ease: [0.16, 1, 0.3, 1] }}
          className="max-w-[500px]"
        >
          <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#505f76]">
            Mobile learning
          </p>
          <h2 className="mt-5 max-w-[10ch] pb-2 text-[clamp(2.3rem,4.7vw,3.9rem)] font-light leading-[1.06] tracking-[-0.05em] text-primary">
            Mastery on the move.
          </h2>
          <p className="mt-6 text-base leading-8 text-[#505f76] sm:text-[1.05rem]">
            The Android app keeps the same guided structure for lessons, progress tracking, exams,
            and certificates, so learners can continue without losing the thread.
          </p>

          <button
            type="button"
            onClick={handleAndroidPilotClick}
            className="mt-10 inline-flex items-center gap-4 rounded-full bg-primary px-5 py-3 text-left text-white shadow-[0_24px_52px_-30px_rgba(9,20,38,0.52)] transition-transform duration-300 hover:-translate-y-0.5 active:translate-y-0 active:scale-[0.98]"
          >
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-white/10">
              <Smartphone className="h-4.5 w-4.5" />
            </span>
            <span>
              <span className="block text-[10px] font-semibold uppercase tracking-[0.16em] text-white/58">
                Android app
              </span>
              <span className="block text-base font-semibold tracking-[-0.03em]">Google Play</span>
            </span>
          </button>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.78, delay: 0.08, ease: [0.16, 1, 0.3, 1] }}
          className="relative"
        >
          <div className="rounded-[2.4rem] border border-[#dfe3e7] bg-white p-5 shadow-[0_28px_72px_-44px_rgba(9,20,38,0.3)]">
            <div className="grid gap-5 lg:grid-cols-[1.1fr_0.8fr]">
              <div className="overflow-hidden rounded-[2rem] border border-[#dfe3e7] bg-[linear-gradient(180deg,#f8fbff_0%,#edf3f9_100%)] p-5">
                <div className="rounded-[1.5rem] border border-white bg-white p-5 shadow-[0_16px_44px_-34px_rgba(9,20,38,0.28)]">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#768397]">
                        EduLife app
                      </p>
                      <p className="mt-2 text-lg font-semibold tracking-[-0.03em] text-primary">
                        Lesson continuity
                      </p>
                    </div>
                    <span className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-[#eff4fb] text-primary">
                      <Smartphone className="h-5 w-5" />
                    </span>
                  </div>

                  <div className="mt-8 grid gap-3">
                    <div className="rounded-[1.2rem] border border-[#dfe3e7] bg-[#f6fafe] px-4 py-4">
                      <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#768397]">
                        Today
                      </p>
                      <p className="mt-2 text-sm leading-7 text-[#505f76]">
                        Resume the next lesson, review notes, and keep progress synced.
                      </p>
                    </div>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <div className="rounded-[1.2rem] border border-[#dfe3e7] bg-white px-4 py-4">
                        <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#768397]">
                          Offline-first
                        </p>
                        <p className="mt-2 text-sm leading-7 text-[#505f76]">
                          Continue focused study on the move.
                        </p>
                      </div>
                      <div className="rounded-[1.2rem] border border-[#dfe3e7] bg-white px-4 py-4">
                        <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#768397]">
                          Unified path
                        </p>
                        <p className="mt-2 text-sm leading-7 text-[#505f76]">
                          The same structure as the web experience.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-center">
                <div className="relative h-[360px] w-[200px] rounded-[2.6rem] border border-[#c5c6cd] bg-[#0a1423] p-3 shadow-[0_28px_72px_-36px_rgba(9,20,38,0.44)]">
                  <div
                    className="absolute left-1/2 top-3 h-1.5 w-14 -translate-x-1/2 rounded-full bg-white/14"
                    aria-hidden
                  />
                  <div className="flex h-full flex-col justify-between rounded-[2.1rem] bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.14),_transparent_42%),linear-gradient(180deg,#121d31_0%,#07101d_100%)] px-5 py-7">
                    <div>
                      <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-white/55">
                        Course path
                      </p>
                      <p className="mt-2 text-base font-semibold tracking-[-0.03em] text-white">
                        Data foundations
                      </p>
                    </div>
                    <div className="space-y-3">
                      <div className="rounded-[1.2rem] border border-white/10 bg-white/7 px-4 py-4">
                        <p className="text-[10px] uppercase tracking-[0.16em] text-white/48">
                          Lesson 4
                        </p>
                        <p className="mt-2 text-sm text-white/80">Validation checkpoints</p>
                      </div>
                      <div className="rounded-full bg-white/10 p-1">
                        <div className="h-2 w-[68%] rounded-full bg-white/75" />
                      </div>
                    </div>
                    <div className="rounded-[1.2rem] border border-white/10 bg-white/7 px-4 py-4">
                      <p className="text-[10px] uppercase tracking-[0.16em] text-white/48">
                        Credential status
                      </p>
                      <p className="mt-2 text-sm text-white/80">Exam unlocked after final lesson</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
