import SpotlightCard from "./SpotlightCard";

/**
 * Displays a market ranking using caller-formatted rows and accessible ticker-selection buttons.
 * Optional row timestamps and stale flags identify cached mover quotes.
 *
 * @param {object} props card content and interaction.
 * @param {string} props.title visible and accessible card title.
 * @param {string} props.subtitle explanation of the ranking.
 * @param {Array<{ticker: string, company: string, metric: string, metricClassName?: string, asOf?: string, asOfLabel?: string, stale?: boolean}>} props.rows already ordered and formatted ranking rows.
 * @param {string} props.footer loading, source or availability message.
 * @param {function(string): void} props.onSelect receives the selected ticker.
 * @param {boolean} [props.disabled] disables selection while lookup is busy.
 * @return {import("react").ReactElement} The ranking card.
 */
export default function MarketRankingCard({
  title,
  subtitle,
  rows,
  footer,
  onSelect,
  disabled,
}) {
  return (
    <SpotlightCard className="flex min-w-0 flex-col p-4! sm:p-5!">
      <section aria-label={title} className="flex flex-1 flex-col gap-2">
        <header>
          <h2 className="text-xl font-bold">{title}</h2>
          <p className="mt-1 text-sm text-white/75">{subtitle}</p>
        </header>

        {/* Share extra card height across the rows while keeping their natural minimum size. */}
        {rows.length > 0 && (
          <ul className="flex flex-1 flex-col divide-y divide-white/10 border-y border-white/10">
            {rows.map(({ ticker, company, metric, metricClassName, asOf, asOfLabel, stale }) => (
              <li key={ticker} className="flex flex-1">
                {/* A native button supports mouse and keyboard selection. */}
                <button
                  type="button"
                  onClick={() => onSelect(ticker)}
                  disabled={disabled}
                  className="grid min-h-9 w-full grid-cols-[minmax(0,1fr)_auto_auto] content-center items-center gap-x-2 rounded-md py-1 text-left hover:bg-white/5 focus-visible:outline focus-visible:outline-[#b8dc89] disabled:opacity-50"
                >
                  <span className="flex min-w-0 items-baseline gap-2">
                    <span className="shrink-0 text-sm font-semibold">{ticker}</span>
                    <span className="truncate text-sm text-white/75" title={company}>
                      {company}
                    </span>
                  </span>

                  <span className={`text-right text-sm font-medium tabular-nums ${metricClassName ?? ""}`}>
                    {metric}
                  </span>
                  <span aria-hidden="true" className="text-white/75">→</span>
                  {asOf && (
                    <span className="col-span-full text-xs text-white/70">
                      <time dateTime={asOf}>{asOfLabel}</time>
                      {stale && <span className="text-amber-300"> · Cached quote</span>}
                    </span>
                  )}
                </button>
              </li>
            ))}
          </ul>
        )}

        <p role="status" className="mt-auto text-xs text-white/75">{footer}</p>
      </section>
    </SpotlightCard>
  );
}
