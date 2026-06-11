import { createFileRoute, Outlet } from "@tanstack/react-router";
import { RequireAdmin } from "../lib/auth/auth-context";

export const Route = createFileRoute("/admin")({
  component: AdminLayout,
});

function AdminLayout() {
  return (
    <RequireAdmin>
      <Outlet />
    </RequireAdmin>
  );
}
