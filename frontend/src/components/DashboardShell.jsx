import { useAuth } from '../context/useAuth.js';
import { useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { StaggeredMenu } from "./StaggeredMenu";
import Grainient from "./Grainient";
import { Button } from "./ui/button";

const NAV_ITEMS = [
  { label: "Dashboard", link: "/" },
  // Portfolio combines holdings with the former standalone market-data lookup.
  { label: "Portfolio", link: "/portfolio"},
  { label: "Transactions", link: "/transactions" },
  { label: "Add Transaction", link: "/transactions/new" },
];

/**
 * Provides shared dashboard navigation, background and signed-in session controls.
 *
 * @param {object} props layout inputs.
 * @param {import("react").ReactNode} props.children page content rendered inside the shared shell.
 * @return {import("react").ReactElement} The dashboard layout.
 */
export default function DashboardShell({ children }) {
  const location = useLocation();
  const { user, logout } = useAuth();

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
    {/*VIDEO SCRIPT*/}
{/*           <Link */}
{/*             to="/" */}
{/*             aria-label="Go to Dashboard" */}
{/*             className="pointer-events-auto fixed left-4 top-4 z-[39] block size-16" */}
{/*           > */}
{/*             <video */}
{/*               autoPlay */}
{/*               loop */}
{/*               muted */}
{/*               playsInline */}
{/*               preload="metadata" */}
{/*               aria-hidden="true" */}
{/*               className="h-full w-full object-contain" */}
{/*             > */}
{/*               <source src="/logo-spin-2.mp4" type="video/mp4" /> */}
{/*             </video> */}
{/*           </Link> */}
      <StaggeredMenu
           className="dark"
           position="right"
           colors={["rgba(20, 28, 40, 0.56)", "rgba(32, 42, 56, 0.42)"]}
           items={NAV_ITEMS}
           logoVideoUrl="/SpinningPocaTransparent.webm"
           menuButtonColor="#f8fafc"
           openMenuButtonColor="#f8fafc"
           accentColor="#f1cbfc"
           displaySocials={false}
           isFixed
      />
      <div className="dark relative isolate min-h-svh w-full overflow-hidden text-foreground [color-scheme:dark]">
        <div
          aria-hidden="true"
          className="pointer-events-none fixed inset-0 -z-10"
          style={{ background: "linear-gradient(100deg, #05110f, #065f46, #34d399, #ecfccb)" }}
        >
          <Grainient
            colors={["#03041e", "#3b82f6", "#06b6d4", "#f1cbfc"]}
            angle={100}
            bands={3}
            silk={0.6}
            flow={1.1}
            className="h-full w-full"
          />
          <div className="absolute inset-0 bg-black/20" />
        </div>
        <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 pb-8 pt-24 md:px-8 md:pb-12 md:pt-28">
          <div className="space-y-6 p-2 sm:p-6">
            {user && <div className="flex flex-wrap items-center justify-end gap-3 text-sm">
              <span>{user.email}</span>
              <Button type="button" variant="outline" className="h-10" onClick={() => logout()}>Sign out</Button>
            </div>}
            {children}
          </div>
        </div>
      </div>
    </>
  );
}
