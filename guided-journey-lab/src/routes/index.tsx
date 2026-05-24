import { createFileRoute } from "@tanstack/react-router";
import { Nav } from "@/components/landing/Nav";
import { Hero } from "@/components/landing/Hero";
import { Problem } from "@/components/landing/Problem";
import { Journey } from "@/components/landing/Journey";
import { Features } from "@/components/landing/Features";
import { WhyEduLife } from "@/components/landing/WhyEduLife";
import { Certificate } from "@/components/landing/Certificate";
import { Morocco } from "@/components/landing/Morocco";
import { Stats } from "@/components/landing/Stats";
import { FinalCTA } from "@/components/landing/FinalCTA";
import { Footer } from "@/components/landing/Footer";

export const Route = createFileRoute("/")({
  component: Index,
  head: () => ({
    meta: [
      { title: "EduLife — One clear path to learn, pass, and grow" },
      {
        name: "description",
        content:
          "EduLife is a mobile-first learning platform for Moroccan learners. Discover courses, track progress, pass exams, and earn verified certificates.",
      },
      { property: "og:title", content: "EduLife — Structured learning for real progress" },
      {
        property: "og:description",
        content:
          "One guided path from course to certificate. Built for Moroccan learners in Darija, French, and English.",
      },
      { property: "og:type", content: "website" },
    ],
  }),
});

function Index() {
  return (
    <div className="relative min-h-screen bg-background text-foreground">
      <Nav />
      <main>
        <Hero />
        <Problem />
        <Journey />
        <Features />
        <WhyEduLife />
        <Certificate />
        <Morocco />
        <Stats />
        <FinalCTA />
      </main>
      <Footer />
    </div>
  );
}
