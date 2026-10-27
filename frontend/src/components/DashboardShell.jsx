// Shared background, theme, and spacing for dashboard pages.
export default function DashboardShell({ children }) {
  return (
    <div className="dark relative isolate min-h-svh w-full overflow-hidden text-foreground [color-scheme:dark]">
      <div aria-hidden="true" className="pointer-events-none fixed inset-0 -z-10 bg-[url('/images/DashboardBG.png')] bg-cover bg-center">
        <div className="absolute inset-0 bg-black/20" />
      </div>
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-8 md:px-8 md:py-12">
        <div className="space-y-6 p-2 sm:p-6">{children}</div>
      </div>
    </div>
  );
}
