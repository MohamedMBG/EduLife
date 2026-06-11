import { Outlet, createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/courses/$courseId")({
  component: CourseIdLayout,
});

function CourseIdLayout() {
  return <Outlet />;
}
