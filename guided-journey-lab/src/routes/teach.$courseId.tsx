import { Outlet, createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/teach/$courseId")({
  component: () => <Outlet />,
});
