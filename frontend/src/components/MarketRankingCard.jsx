import SpotlightCard from "./SpotlightCard";

export default function MarketRankingCard({
  title,
  subtitle,
  rows,
  footer,
  onSelect,
  disabled,
}) {
  return (
    <SpotlightCard className="min-w-0">
      <section aria-label={title} className="space-y-3">
        <header>
          <h2 className="text-xl font-bold">{title}</h2>
          <p className="mt-1 text-sm text-white/75">{subtitle}</p>
        </header>

        {/* Keep stock names and ranking values in consistent columns. */}
        <ul className="divide-y divide-white/10 border-y border-white/10">
          {rows.map(({ ticker, company, metric }) => (
            <li key={ticker}>
              {/* A native button supports mouse and keyboard selection. */}
              <button
                type="button"
                onClick={() => onSelect(ticker)}
                disabled={disabled}
                className="grid w-full grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-3 rounded-md py-2.5 text-left hover:bg-white/5 focus-visible:outline focus-visible:outline-[#b8dc89] disabled:opacity-50"
              >
                <span className="min-w-0">
                  <span className="block text-sm font-semibold">{ticker}</span>
                  <span className="block break-words text-sm text-white/75">
                    {company}
                  </span>
                </span>

                <span className="text-right text-sm font-medium tabular-nums">
                  {metric}
                </span>
                <span aria-hidden="true" className="text-white/75">→</span>
              </button>
            </li>
          ))}
        </ul>

        <p className="text-xs text-white/75">{footer}</p>
      </section>
    </SpotlightCard>
  );
}
