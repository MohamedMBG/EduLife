# Android Gamification UI Upgrade

## Goal

Redesign the gamification UI/UX of the Android learner app to feel modern, interactive, and motivational with clean cards, bold accent colors, podium leaderboard, profile gamification overview, and micro-interactions.

## What Changed

### 1. Gamification Dashboard Redesign (`fragment_gamification.xml` + `GamificationFragment.java`)

- **Hero level card**: Replaced dark green gradient header with a vibrant blue rounded card (24dp radius) containing level ring, title, progress bar, and motivational message
- **Stat cards row**: Three accent-colored cards (orange XP, red streak, purple rank) replace the old monochrome stats — each with icon, bold number, and label
- **Quick stats row**: Lessons/courses/certificates in a clean white card with green/blue/purple accent numbers
- **Quick actions**: Three action buttons (Continue Learning, View Badges, View Ranking) with icons and rounded borders
- **Streak section**: Moved into a clean white section card with subtle borders instead of orange-tinted card
- **Rank display**: Shows user's actual leaderboard rank fetched from backend; tapping opens leaderboard screen
- **Updated rarity colors**: Badge detail dialog now uses the new accent palette (green/blue/purple/orange) instead of hardcoded hex values

### 2. Leaderboard Screen (NEW)

- **`LeaderboardFragment.java`**: Full-featured fragment with loading/error/empty states
- **`LeaderboardAdapter.java`**: RecyclerView adapter for rank 4+ entries with staggered fade-in animation
- **`fragment_leaderboard.xml`**: Layout with trophy header, podium section (top 3), and scrollable list
- **`item_leaderboard_entry.xml`**: Row layout with rank number, avatar initials circle, name, level, "You" badge, and XP
- **Podium**: 1st place (gold, crown, larger avatar), 2nd place (silver), 3rd place (bronze)
- **Current user highlighting**: Blue border + background + "You" chip badge
- **Navigation**: Accessible from gamification dashboard (rank card + "View Ranking" action) and from profile

### 3. Profile Gamification Overview

- **New card** in `fragment_profile.xml` between stats row and identity card
- Shows XP (orange), Level (blue), Streak (red), Badges earned (purple) — all fetched from backend via `GamificationViewModel`
- **Two action links**: "View Achievements" navigates to gamification tab, "View Ranking" opens leaderboard
- **`ProfileFragment.java`**: Added `GamificationViewModel` observation, binds gamification state on load

### 4. Color Palette Expansion

Added 16 new accent colors to `colors.xml`:
- Blue: `#3B82F6` (actions, level hero)
- Purple: `#7C3AED` (badges, ranking)
- Orange/Amber: `#F59E0B` (XP, rewards)
- Green: `#10B981` (progress, success)
- Rose: `#EF4444` (streak)
- Leaderboard: gold/silver/bronze podium colors

### 5. New Drawable Resources

- `bg_gamification_level_hero.xml` — Blue rounded card for hero section
- `bg_gamification_xp_card.xml` — Orange-tinted stat card
- `bg_gamification_streak_card_v2.xml` — Red-tinted stat card
- `bg_gamification_rank_card.xml` — Purple-tinted stat card
- `bg_gamification_stat_card.xml` — White card with subtle shadow
- `bg_gamification_section_card.xml` — White section container
- `bg_quick_action_btn.xml` — Rounded ripple button
- `bg_xp_progress_bar_v2.xml` — White-on-translucent progress bar
- `bg_level_ring_v2.xml` — White oval ring
- `bg_leaderboard_podium_gold/silver/bronze.xml` — Podium avatars
- `bg_leaderboard_current_user.xml` — Blue highlighted row
- `bg_leaderboard_row.xml` — Default row background
- `bg_leaderboard_avatar.xml` — Blue circle avatar
- `bg_profile_gamification_card.xml` — Profile card background
- `ic_trophy_gold.xml` — Gold trophy vector
- `ic_crown.xml` — Crown vector for 1st place
- `ic_ranking.xml` — Bar chart vector for ranking

### 6. String Resources

Added 30+ new strings for:
- Gamification v2 labels (level, XP, streak, rank)
- Quick actions
- Leaderboard (title, subtitle, loading, error, empty, podium labels)
- Profile gamification card labels and actions

### 7. Micro-interactions

- **Stat card count-up animation**: XP, lessons, courses, certs animate from 0 on first render
- **Level ring scale animation**: Entrance with overshoot interpolator
- **Streak pop animation**: Bouncy entrance
- **Progress bar fill animation**: Smooth 1200ms decelerate
- **Badge staggered entrance**: 60ms delay per badge with zoom-in
- **Leaderboard fade-in**: 350ms per row with 50ms stagger + translateY
- **Quick action press state**: Ripple feedback via `bg_quick_action_btn.xml`

## Files Touched

### New Files
- `app/src/main/java/com/baghdad/edulife/features/gamification/ui/LeaderboardFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/ui/LeaderboardAdapter.java`
- `app/src/main/res/layout/fragment_leaderboard.xml`
- `app/src/main/res/layout/item_leaderboard_entry.xml`
- `app/src/main/res/drawable/bg_gamification_*.xml` (8 files)
- `app/src/main/res/drawable/bg_leaderboard_*.xml` (5 files)
- `app/src/main/res/drawable/bg_quick_action_btn.xml`
- `app/src/main/res/drawable/bg_xp_progress_bar_v2.xml`
- `app/src/main/res/drawable/bg_level_ring_v2.xml`
- `app/src/main/res/drawable/bg_profile_gamification_card.xml`
- `app/src/main/res/drawable/ic_trophy_gold.xml`
- `app/src/main/res/drawable/ic_crown.xml`
- `app/src/main/res/drawable/ic_ranking.xml`

### Modified Files
- `app/src/main/res/values/colors.xml` — Added gamification accent + leaderboard colors
- `app/src/main/res/values/strings.xml` — Added gamification v2 + leaderboard + profile strings
- `app/src/main/res/layout/fragment_gamification.xml` — Complete redesign
- `app/src/main/res/layout/fragment_profile.xml` — Added gamification overview card
- `app/src/main/res/navigation/nav_graph.xml` — Added leaderboard destination + actions
- `app/src/main/java/.../ui/GamificationFragment.java` — New views, rank display, quick actions, leaderboard nav
- `app/src/main/java/.../viewmodel/GamificationViewModel.java` — Added leaderboard LiveData
- `app/src/main/java/.../ui/ProfileFragment.java` — Added gamification observation + nav

## Backend Impact

None. All data fetched from existing endpoints:
- `GET /api/v1/gamification/me` — XP, level, streak, badges
- `GET /api/v1/gamification/leaderboard?limit=20` — Rankings

No new backend endpoints required.

## Android Impact

- All new screens follow MVVM architecture
- No local gamification computation — backend is source of truth
- Navigation Component used for all transitions
- Loading/error/empty states on all screens
- Accessibility: content descriptions on interactive elements

## Web Impact

None.

## Architecture Compliance

- Java only, XML layouts only
- MVVM with ViewModel + LiveData + Repository
- No API calls in Fragments
- No business logic in UI
- Backend-authoritative gamification state
- Retrofit via existing ApiService

## Tests / Verification

- [x] `compileDebugJavaWithJavac` — BUILD SUCCESSFUL
- [ ] Manual: Gamification dashboard loads with new card layout
- [ ] Manual: Stat cards show correct XP, streak, rank
- [ ] Manual: Quick actions navigate correctly
- [ ] Manual: Leaderboard screen shows podium + list
- [ ] Manual: Current user highlighted in leaderboard
- [ ] Manual: Profile shows gamification overview card
- [ ] Manual: Profile nav links work (achievements, ranking)
- [ ] Manual: Loading/error/empty states display correctly
- [ ] Manual: Animations play on first render
- [ ] Manual: No text overflow on small/large screens

## Risks / Notes

- Leaderboard rank on dashboard shows "—" if user not in top 20 (backend returns limited results)
- Old drawables (`bg_gamification_header.xml`, `bg_streak_card.xml`, etc.) kept for backward compatibility but no longer referenced from the redesigned layout
- Badge detail dialog emoji removed in favor of clean text ("Earned" / "Locked")
