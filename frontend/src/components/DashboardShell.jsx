import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { StaggeredMenu } from "./StaggeredMenu";

const NAV_ITEMS = [
  { label: "Dashboard", link: "/" },
  { label: "Portfolio", link: "/#portfolio", ariaLabel: "Go to portfolio on dashboard" },
  { label: "Market Data", link: "/market-data" },
  { label: "Add Transaction", link: "/transactions" },
];

// Shared background, theme, spacing, and navigation for dashboard pages.
export default function DashboardShell({ children }) {
  const location = useLocation();

  useEffect(() => {
    if (location.pathname !== "/" || location.hash !== "#portfolio") return undefined;

    const frame = window.requestAnimationFrame(() => {
      document.getElementById("portfolio")?.scrollIntoView({
        behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
          ? "auto"
          : "smooth",
        block: "start",
      });
    });

    return () => window.cancelAnimationFrame(frame);
  }, [location.pathname, location.hash]);

  return (
    <>
      <StaggeredMenu
           className="dark"
           position="right"
           colors={["rgba(20, 28, 40, 0.56)", "rgba(32, 42, 56, 0.42)"]}
           items={NAV_ITEMS}
           logoUrl="/poca-logo.png"
           menuButtonColor="#f8fafc"
           openMenuButtonColor="#f8fafc"
           accentColor="#b8dc89"
           displaySocials={false}
           isFixed
      />
      <div className="dark relative isolate min-h-svh w-full overflow-hidden text-foreground [color-scheme:dark]">
        <div aria-hidden="true" className="pointer-events-none fixed inset-0 -z-10 bg-[url('/images/DashboardBG.png')] bg-cover bg-center">
          <div className="absolute inset-0 bg-black/20" />
        </div>
        <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 pb-8 pt-24 md:px-8 md:pb-12 md:pt-28">
          <div className="space-y-6 p-2 sm:p-6">{children}</div>
        </div>
      </div>
    </>
  );
}
