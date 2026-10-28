import { useState } from "react";
import DashboardShell from "../components/DashboardShell";
import { getQuote } from "../api/marketData"

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
      <main className="space-y-6 text-left">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold">Market Data</h1>
          <p className="text-sm text-muted-foreground">
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
          <section aria-live="polite" className="rounded-lg border border-border p-4">
            <h2 className="text-xl font-semibold">{quote.symbol}</h2>
            <p>Price per share: {quote.price}</p>
            <p>As of: {new Date(quote.asOf).toLocaleString("en-IE")}</p>

            {quote.stale && (
              <p className="text-amber-400">
                Cached price — live market data is temporarily unavailable.
              </p>
            )}
          </section>
        )}
      </main>
    </DashboardShell>
  );
}
