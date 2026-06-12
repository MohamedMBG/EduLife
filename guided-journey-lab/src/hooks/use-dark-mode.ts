import { useState, useEffect } from "react";

// Single source of truth for the dark-mode preference. The same key is read by the
// inline no-flash script in __root.tsx — keep them in sync.
export const DARK_MODE_STORAGE_KEY = "edulife-dark";

export function useDarkMode() {
  const [dark, setDark] = useState(false);

  useEffect(() => {
    // The <html> class is already set before hydration by the __root.tsx inline script,
    // so the DOM is the authority — not localStorage.
    setDark(document.documentElement.classList.contains("dark"));
  }, []);

  function toggle() {
    const next = !dark;
    document.documentElement.classList.toggle("dark", next);
    try {
      localStorage.setItem(DARK_MODE_STORAGE_KEY, String(next));
    } catch {
      // storage unavailable — silent
    }
    setDark(next);
  }

  return { dark, toggle };
}
