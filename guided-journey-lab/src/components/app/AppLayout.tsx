import type { ReactNode } from "react";
import { AppTopNav } from "./AppTopNav";

interface AppLayoutProps {
  children: ReactNode;
  onSearch?: (query: string) => void;
  searchValue?: string;
  showSearch?: boolean;
}

export function AppLayout({ children, onSearch, searchValue, showSearch }: AppLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <AppTopNav onSearch={onSearch} searchValue={searchValue} showSearch={showSearch} />
      <main className="flex-1 px-4 py-7 sm:px-6 lg:px-8">
        <div className="mx-auto w-full max-w-7xl">{children}</div>
      </main>
    </div>
  );
}
