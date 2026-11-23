/**
 * A small red callout that hangs just below whatever it is placed in. It is
 * absolutely positioned, so it takes no space in the page: showing or hiding
 * it never moves anything. The parent must be `relative`.
 */
export default function ErrorTooltip({ id, children }) {
  return (
    <p
      id={id}
      role="alert"
      className="absolute left-0 top-full z-10 mt-1 max-w-full rounded-md border border-red-400/40 bg-red-950 px-2.5 py-1 text-xs text-red-50 shadow-lg"
    >
      {/* The little arrow pointing up at the field. */}
      <span
        aria-hidden="true"
        className="absolute -top-1 left-4 size-2 rotate-45 border-l border-t border-red-400/40 bg-red-950"
      />
      {children}
    </p>
  );
}