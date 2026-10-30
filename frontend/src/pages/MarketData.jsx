import { useState } from "react";
import DashboardShell from "../components/DashboardShell";
import { getQuote } from "../api/marketData"
import SpotlightCard from "../components/SpotlightCard";
import StrokeText from "../components/StrokeText";
import CountUp from "../components/CountUp";

export default function MarketData() {
  const [tickerInput, setTickerInput] = useState("");

  const [quote, setQuote] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();

    const symbol = tickerInput.trim().toUpperCase();

    if (!symbol) {
      setError("Enter a ticker symbol.");
      setQuote(null);
      return;
    }

    setError("");
    setQuote(null);
    setIsLoading(true);

    try {
      // The backend supplies the price, timestamp, and stale flag.
      const result = await getQuote(symbol);
      setQuote(result);
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
            {isLoading ? "Looking up..." : "Find price"}
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
            </section>
          </SpotlightCard>
        )}
      </main>
    </DashboardShell>
  );
}
