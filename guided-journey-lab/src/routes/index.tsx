import { createFileRoute } from "@tanstack/react-router";
import { PublicLandingPage } from "@/components/landing/PublicLandingPage";

export const Route = createFileRoute("/")({
  component: Index,
  head: () => ({
    meta: [
      { title: "EduLife | One path to master, validate, and graduate" },
      {
        name: "description",
        content:
          "EduLife gives Moroccan learners one structured path from guided lessons to verified certificates.",
      },
      { property: "og:title", content: "EduLife | Structured learning for Moroccan learners" },
      {
        property: "og:description",
        content:
          "Discover structured programs, server-validated exams, and verifiable certificates in Darija, French, and English.",
      },
      { property: "og:type", content: "website" },
    ],
  }),
});

function Index() {
  return <PublicLandingPage />;
}
