import { useState } from "react";
import DashboardShell from "../components/DashboardShell";
import { getQuote, getPriceHistory } from "../api/marketData";
import SpotlightCard from "../components/SpotlightCard";
import StrokeText from "../components/StrokeText";
import CountUp from "../components/CountUp";

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
              <h2 className="text-xl font-semibold">{quote.symbol}</h2>

              <p className="text-3xl font-bold">
                <span className="block text-sm font-normal text-muted-foreground">
                  Price per share
                </span>
                <CountUp
                  to={Number(quote.price)}
                  decimals={2}
                  separator=","
                  duration={1.2}
                  trigger="load"
                />
              </p>

              <p className="text-sm text-muted-foreground">
                As of: {new Date(quote.asOf).toLocaleString("en-IE")}
              </p>

              {quote.stale && (
                <p className="text-sm text-amber-400">
                  Cached price — live market data is temporarily unavailable.
                </p>
              )}

              {isHistoryLoading && <p className="text-sm">Loading price history...</p>}
              {historyError && <p className="text-sm text-amber-400">{historyError}</p>}
              {history.length > 0 && (
                <p className="text-sm text-muted-foreground">
                  {history.length} daily prices loaded.
                </p>
              )}


            </section>
          </SpotlightCard>
        )}
      </main>
    </DashboardShell>
  );
}
