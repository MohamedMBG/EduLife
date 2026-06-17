import { Link } from "@tanstack/react-router";
import { ArrowUpRight } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth/auth-context";

export function PublicWaitlistCTA() {
  const [email, setEmail] = useState("");
  const auth = useAuth();
  const primaryCtaTo = auth.status === "authenticated" ? "/dashboard" : "/register";

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    // The web app does not expose a waitlist contract yet, so the homepage keeps this form
    // presentational instead of inventing a backend endpoint or changing API behavior.
    if (!email.trim()) {
      toast.error("Enter an email address to join the pilot list.");
      return;
    }

    toast.success("Pilot invitations are opening soon.", {
      description: "EduLife will connect this form once the waitlist endpoint exists.",
    });
    setEmail("");
  }

  return (
    <section id="admissions" className="px-5 py-20 sm:px-6 lg:px-8 lg:py-28">
      <div className="mx-auto max-w-[1280px]">
        <div className="overflow-hidden rounded-[2.4rem] bg-[#2a3448] px-6 py-12 text-center text-white shadow-[0_34px_86px_-46px_rgba(9,20,38,0.54)] sm:px-10 lg:px-16 lg:py-16">
          <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-white/62">
            Now accepting pilot members
          </p>
          <h2 className="mx-auto mt-6 max-w-[10ch] pb-2 text-[clamp(2.2rem,4.5vw,4rem)] font-light leading-[1.08] tracking-[-0.05em] text-white">
            Start the smarter way <span className="italic text-white/68">to learn today.</span>
          </h2>
          <p className="mx-auto mt-6 max-w-[40ch] text-base leading-8 text-white/68">
            Join the waitlist to receive the first guided cohorts, structured for Moroccan learners
            who want one system from course discovery to certification.
          </p>

          <form
            onSubmit={handleSubmit}
            className="mx-auto mt-10 flex max-w-[560px] flex-col gap-3 sm:flex-row"
          >
            <label htmlFor="waitlist-email" className="sr-only">
              Email address
            </label>
            <input
              id="waitlist-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="Enter your email"
              aria-label="Email address"
              className="h-14 flex-1 rounded-full border border-white/12 bg-white/8 px-5 text-base text-white outline-none transition-colors placeholder:text-white/35 focus:border-white/28"
            />
            <button
              type="submit"
              className="inline-flex h-14 items-center justify-center gap-3 rounded-full bg-white px-6 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary transition-transform duration-300 hover:-translate-y-0.5 active:translate-y-0 active:scale-[0.98]"
            >
              Join Waitlist
              <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-[#eef3f8]">
                <ArrowUpRight className="h-3.5 w-3.5" />
              </span>
            </button>
          </form>

          <p className="mt-4 text-[11px] uppercase tracking-[0.16em] text-white/42">
            Invitations are sent in batches while the pilot admissions flow is being prepared.
          </p>

          <div className="mt-8">
            <Link
              to={primaryCtaTo}
              className="inline-flex items-center gap-2 text-sm text-white/78 transition-colors hover:text-white"
            >
              Prefer to explore the platform directly?
              <span className="font-medium underline underline-offset-4">Continue to EduLife</span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
