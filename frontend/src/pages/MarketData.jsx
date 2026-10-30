import { useState } from "react";
import DashboardShell from "../components/DashboardShell";
import { getQuote, getPriceHistory } from "../api/marketData";
import SpotlightCard from "../components/SpotlightCard";
import StrokeText from "../components/StrokeText";
import CountUp from "../components/CountUp";
import PriceHistoryChart from "../components/PriceHistoryChart";

function formatPrice(value) {
  if (value == null) return "—";

  return Number(value).toLocaleString("en-IE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export default function MarketData() {
  const [tickerInput, setTickerInput] = useState("");

  const [quote, setQuote] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const [error, setError] = useState("");

  const [history, setHistory] = useState([]);
  const [historyError, setHistoryError] = useState("");
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();

    const symbol = tickerInput.trim().toUpperCase();

    if (!symbol) {
      setError("Enter a ticker symbol.");
      setQuote(null);
      setHistory([]);
      return;
    }

    // Clear the previous result before starting a new search.
    setError("");
    setQuote(null);
    setHistory([]);
    setHistoryError("");
    setIsLoading(true);

    try {
      const result = await getQuote(symbol);
      setQuote(result);

      // History is useful, but a failure must not hide the quote.
      setIsHistoryLoading(true);
      try {
        const points = await getPriceHistory(symbol);
        setHistory(points);
      } catch (historyRequestError) {
        setHistoryError(historyRequestError.message);
      } finally {
        setIsHistoryLoading(false);
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsLoading(false);
    }
  }

  const details = quote?.details;
  const dailyChange = details?.change == null ? null : Number(details.change);
  const percentChange =
    details?.percentChange == null ? null : Number(details.percentChange);

  return (
    <DashboardShell>
      <main className="mx-auto w-full max-w-3xl space-y-6 px-4 text-left">
        <header className="flex flex-col items-center gap-2 text-center">
          <h1 className="w-full">
            <StrokeText
              className="mx-auto max-w-[520px]"
              text="Market Data"
              fontSize={64}
              letterSpacing={-2}
              strokeColor="#60a5fa"
              fillColor="currentColor"
              drawDuration={1.1}
              stagger={0.04}
              fillDelay={0.1}
            />
          </h1>
          <p className="text-sm text-white/90">
            Look up the latest available stock price.
          </p>
        </header>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row">
          <label className="flex-1">
            <span className="mb-2 block text-sm">Ticker symbol</span>
            <input
              type="search"
              value={tickerInput}
              onChange={(event) => setTickerInput(event.target.value)}
              placeholder="e.g. AAPL"
              className="h-10 w-full rounded-lg border border-input bg-background/70 px-3"
            />
          </label>

          <button
            type="submit"
            disabled={isLoading}
            className="h-10 self-end rounded-lg bg-primary px-4 text-primary-foreground disabled:opacity-50"
          >
            {isHistoryLoading ? "Loading history..." : isLoading ? "Looking up..." : "Find price"}
          </button>
        </form>

        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {quote && (
          <SpotlightCard className="motion-safe:animate-in motion-safe:fade-in motion-safe:duration-500">
            <section aria-live="polite" className="space-y-3">
              <div className="flex flex-wrap items-end justify-between gap-5">
                <div>
                  <h2 className="text-2xl font-bold">
                    {details?.name || quote.symbol}
                  </h2>
                  {details?.name && (
                    <p className="text-sm text-white/60">{quote.symbol}</p>
                  )}

                  <p className="mt-5 text-sm text-white/60">Price per share</p>
                  <p className="text-4xl font-bold text-blue-400">
                    <CountUp
                      to={Number(quote.price)}
                      decimals={2}
                      separator=","
                      duration={1.2}
                      trigger="load"
                    />
                    {details?.currency && (
                      <span className="ml-2 text-base font-semibold">
                        {details.currency}
                      </span>
                    )}
                  </p>
                </div>

                {dailyChange !== null && percentChange !== null && (
                  <p
                    className={`pb-1 text-lg font-semibold ${dailyChange >= 0 ? "text-blue-400" : "text-red-400"
                      }`}
                  >
                    {dailyChange >= 0 ? "+" : ""}
                    {dailyChange.toFixed(2)}{" "}
                    {percentChange >= 0 ? "+" : ""}
                    {percentChange.toFixed(2)}%
                  </p>
                )}
              </div>

              {details && (
                <dl className="grid grid-cols-2 gap-x-6 gap-y-5 border-t border-white/10 pt-5">
                  <div>
                    <dt className="text-sm text-white/60">Open</dt>
                    <dd className="font-semibold">{formatPrice(details.open)}</dd>
                  </div>

                  <div>
                    <dt className="text-sm text-white/60">Previous close</dt>
                    <dd className="font-semibold">{formatPrice(details.previousClose)}</dd>
                  </div>

                  <div>
                    <dt className="text-sm text-white/60">Volume</dt>
                    <dd className="font-semibold">
                      {details.volume == null
                        ? "—"
                        : Number(details.volume).toLocaleString("en-IE")}
                    </dd>
                  </div>

                  <div>
                    <dt className="text-sm text-white/60">Day range</dt>
                    <dd className="font-semibold">
                      {details.low == null || details.high == null
                        ? "—"
                        : `${formatPrice(details.low)} – ${formatPrice(details.high)}`}
                    </dd>
                  </div>

                  <div className="col-span-2">
                    <dt className="text-sm text-white/60">52-week range</dt>
                    <dd className="font-semibold">
                      {details.fiftyTwoWeekLow == null || details.fiftyTwoWeekHigh == null
                        ? "—"
                        : `${formatPrice(details.fiftyTwoWeekLow)} – ${formatPrice(details.fiftyTwoWeekHigh)}`}
                    </dd>
                  </div>
                </dl>
              )}

              <p className="text-sm text-muted-foreground">
                As of: {new Date(quote.asOf).toLocaleString("en-IE")}
              </p>

              {quote.stale && (
                <p className="text-sm text-amber-400">
                  Cached price — live market data is temporarily unavailable.
                </p>
              )}

              <div className="border-t border-white/10 pt-5">
                <h3 className="mb-3 text-sm font-semibold text-white/80">
                  Past month
                </h3>

                {isHistoryLoading && (
                  <p className="text-sm text-white/60">Loading price history...</p>
                )}

                {historyError && (
                  <p className="text-sm text-amber-400">{historyError}</p>
                )}

                {!isHistoryLoading && !historyError && history.length > 1 && (
                  <PriceHistoryChart
                    points={history}
                    currency={details?.currency}
                  />
                )}

                {!isHistoryLoading && !historyError && history.length <= 1 && (
                  <p className="text-sm text-white/60">
                    Not enough price history to draw a graph.
                  </p>
                )}
              </div>


            </section>
          </SpotlightCard>
        )}
      </main>
    </DashboardShell>
  );
}
