import { Outlet, createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/certificates")({
  component: CertificatesLayout,
});

function CertificatesLayout() {
  return <Outlet />;
}
