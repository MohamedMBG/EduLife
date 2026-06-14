# Task Audit - Study Planner Feature (Android and Web)

## Date
2026-06-13

## Task Summary
Designed and implemented a premium Study Planner feature for both the Android and Web versions of the EduLife platform. The feature allows learners to plan weekly study targets (motivation focus, target hours, planned study days, course priorities, and a dynamic checklist of tasks) and log study time. Progress is tracked and shown dynamically on the Home dashboard cards.

## Files Created
- [PlannerTask.java](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/java/com/baghdad/edulife/features/courses/model/PlannerTask.java) (Data Model)
- [PlannerPreferences.java](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/java/com/baghdad/edulife/features/courses/data/PlannerPreferences.java) (Preferences Utility)
- [PlannerViewModel.java](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/PlannerViewModel.java) (State ViewModel)
- [PlannerFragment.java](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/java/com/baghdad/edulife/features/courses/ui/PlannerFragment.java) (Fragment controller)
- [PlannerTaskAdapter.java](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/java/com/baghdad/edulife/features/courses/ui/PlannerTaskAdapter.java) (Checklist Adapter)
- [ic_nav_planner.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/drawable/ic_nav_planner.xml) (Vector Icon)
- [fragment_planner.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/layout/fragment_planner.xml) (Aesthetic layout)
- [item_planner_task.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/layout/item_planner_task.xml) (Checklist Item layout)
- [planner.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/routes/planner.tsx) (Web Planner route)

## Files Modified
- [nav_graph.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/navigation/nav_graph.xml) (Android Navigation routes)
- [bottom_nav_menu.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/menu/bottom_nav_menu.xml) (Android Bottom nav menu)
- [MainActivity.java](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/java/com/baghdad/edulife/MainActivity.java) (Bottom nav tab routing controller)
- [fragment_home.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/layout/fragment_home.xml) (Android Dashboard UI)
- [HomeFragment.java](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java) (Android Dashboard card binding)
- [strings.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/values/strings.xml) (Localizable strings)
- [AppShell.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/components/app/AppShell.tsx) (Web Nav sidebar registration)
- [dashboard.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/routes/dashboard.tsx) (Web Dashboard card & local storage hook integration)

## What Was Done
1. **Local storage strategy**: Created a client-decoupled study planner model. Parameters are stored locally via `SharedPreferences` on Android and `localStorage` on Web. This allows the feature to work seamlessly without database schemas, migrations, or backend API dependencies.
2. **Android App Integration**:
   - Added a `Planner` bottom tab.
   - Built a premium planning layout (`fragment_planner.xml`) with a gradient glass progress tracker (`+30m`/`+1h` log shortcuts), a text input for the motivation priority, minus/plus widgets to adjust target hours, day-of-week checkboxes, focus-course lists, and a modular task checklist (restricted to a maximum of 10 tasks to maintain visual hygiene).
   - Designed a new Home dashboard card that updates dynamically as the student logs study time and redirects them directly to the planner.
3. **Web App Integration**:
   - Implemented a premium matching page (`planner.tsx`) built using Tailwind CSS and Framer Motion animations.
   - Registered the planner route in the sidebar navigation schema.
   - Restructured the dashboard's advisor section to a dual responsive grid: **Career Goal Advisor** + **Weekly Study Planner**.
   - Resolved icon/import compiler issues in `dashboard.tsx` by importing `useState`, `BrainCircuit`, `CalendarDays`, and hooking up local storage states.

## Architecture Compliance
- **Android**: Followed Pragmatic MVVM feature-first organization. UI files live in `features/courses/ui/`, state logic in `viewmodel/`, preferences in `data/`, and data models in `model/`.
- **Web**: Kept components modular under TanStack Router conventions. SSR compatibility was protected by wrapping all `localStorage` access checks inside `typeof window !== "undefined"` guards to prevent compiler crashes during worker-level hydration.

## Code Comments Added
- Added explanatory comments around local storage initialization, SSR guards, bottom nav tab index overrides, checklist item limits, and UI binding calculations.

## Validation / Testing
- Verified Android compiles cleanly:
  ```powershell
  .\gradlew.bat :app:compileDebugJavaWithJavac
  ```
  Result: **SUCCESSFUL**.
- Checked Web type safety and compiled production build:
  ```powershell
  npx tsc --noEmit
  npm run build
  ```
  Result: **SUCCESSFUL** (dist/client and dist/server bundles created successfully).

## Risks / Notes
- The planner stores state locally. If a student switches between devices (e.g. Android to Web), their planners will be individual and unsynced. Once the backend modular monolith establishes a planner model, these preferences can easily be mapped to a REST API.
