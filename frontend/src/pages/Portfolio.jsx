// useEffect loads Portfolio data when the page opens.
import { useEffect, useRef, useState } from "react";
import { getPortfolios, getHoldings } from "../api/portfolio";
import DashboardShell from "../components/DashboardShell";
import { getQuote, getPriceHistory, getTopStocks } from "../api/marketData";
import SpotlightCard from "../components/SpotlightCard";
import StrokeText from "../components/StrokeText";
import CountUp from "../components/CountUp";
import PriceHistoryChart from "../components/PriceHistoryChart";
// The form handles purchase inputs and validation.
import RecordPurchaseForm from "../components/RecordPurchaseForm";
// Use the shared layout for the market ranking cards.
import MarketRankingCard from "../components/MarketRankingCard";

const rankingCaptureDate = new Intl.DateTimeFormat("en-IE", {
  dateStyle: "medium",
  timeZone: "UTC",
});
const rankingMarketCap = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  notation: "compact",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

function formatPrice(value) {
  if (value == null) return "—";

  return Number(value).toLocaleString("en-IE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

//This page will hold both stock lookup and the user's holdings
export default function Portfolio() {
  const [tickerInput, setTickerInput] = useState("");

  const [quote, setQuote] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const [error, setError] = useState("");

  const [history, setHistory] = useState([]);
  const [historyError, setHistoryError] = useState("");
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);

  const [topStocks, setTopStocks] = useState({ status: "loading" });

  useEffect(() => {
    const controller = new AbortController();
    let expiryTimer;

    async function loadTopStocks() {
      try {
        const snapshot = await getTopStocks(controller.signal);
        if (controller.signal.aborted) return;

        const remainingMs = Date.parse(snapshot.expiresAt) - Date.now();
        if (remainingMs <= 0) {
          setTopStocks({ status: "unavailable" });
          return;
        }

        setTopStocks({ status: "ready", snapshot });
        // Clear a stale ranking even if the user leaves the page open.
        expiryTimer = setTimeout(() => setTopStocks({ status: "unavailable" }), remainingMs);
      } catch {
        if (!controller.signal.aborted) setTopStocks({ status: "unavailable" });
      }
    }

    loadTopStocks();
    return () => {
      controller.abort();
      clearTimeout(expiryTimer);
    };
  }, []);

  const snapshot = topStocks.snapshot;
  const topStockRows = snapshot?.rows.map(({ ticker, company, marketCapUsd }) => ({
    ticker,
    company,
    metric: rankingMarketCap.format(marketCapUsd),
  })) ?? [];

  // The date marks when we captured the source, which can differ from the trading date.
  let rankingFooter = "Fresh ranking unavailable";
  if (topStocks.status === "loading") rankingFooter = "Loading ranking…";
  if (snapshot) {
    rankingFooter = `Snapshot captured ${rankingCaptureDate.format(new Date(snapshot.capturedOn))} · Stock Analysis`;
  }

  // Keep the request status and its data together.
  const [portfolioData, setPortfolioData] = useState({
    status: "loading",
    portfolio: null,
    holdings: [],
    error: "",
  });

  // Share only the selected ticker with the purchase form; preserve its other inputs.
  const [purchaseTicker, setPurchaseTicker] = useState("");

  const [purchasePrice, setPurchasePrice] = useState("");

  const [purchaseOpen, setPurchaseOpen] = useState(false);

  const [purchaseVersion, setPurchaseVersion] = useState(0);
  const [quoteEstimate, setQuoteEstimate] = useState(null);
  const [prefillError, setPrefillError] = useState("");

  const purchaseQuantityRef = useRef(null);

  useEffect(() => {
    if (purchaseOpen) purchaseQuantityRef.current?.focus();
  }, [purchaseOpen]);

  // Changing this number lets the Retry button request the data again.
  const [portfolioReload, setPortfolioReload] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadPortfolio() {
      setPortfolioData({
        status: "loading",
        portfolio: null,
        holdings: [],
        error: "",
      });

      try {
        const portfolios = await getPortfolios(controller.signal);

        // Initially display the first available portfolio.
        // Its name will be visible so the user knows which one is selected.
        const selectedPortfolio = portfolios[0] ?? null;

        const holdings = selectedPortfolio
          ? await getHoldings(selectedPortfolio.id, controller.signal)
          : [];

        // Ignore results if the page was left while requests were running.
        if (controller.signal.aborted) return;

        setPortfolioData({
          status: "ready",
          portfolio: selectedPortfolio,
          holdings,
          error: "",
        });
      } catch (error) {
        if (controller.signal.aborted) return;

        setPortfolioData({
          status: "error",
          portfolio: null,
          holdings: [],
          error: error.message,
        });
      }
    }

    loadPortfolio();

    // Cancel unfinished requests when leaving the page or retrying.
    return () => controller.abort();
  }, [portfolioReload]);


  function handleSubmit(event) {
    event.preventDefault();
    lookupStock(tickerInput);
  }

  function handleRankingSelect(ticker) {
    setTickerInput(ticker);
    document.getElementById("lookup-heading")?.focus();
    lookupStock(ticker);
  }

  // Both entry points use the same quote and history request.
  async function lookupStock(rawTicker) {
    if (isLoading) return;

    const symbol = rawTicker.trim().toUpperCase();

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
      setQuote({ ...result, lookedUpAt: new Date().toISOString() });

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
      {/* Use the available width while preventing children from widening the page. */}
      <main className="mx-auto w-full min-w-0 max-w-6xl space-y-6 text-left">
        <header className="flex flex-col items-center gap-2 text-center">

          <h1 className="w-full">
            <StrokeText
              className="mx-auto max-w-[520px]"
              text="Portfolio"
              fontSize={64}
              letterSpacing={-2}
              strokeColor="#b8dc89"
              fillColor="currentColor"
              drawDuration={1.1}
              stagger={0.04}
              fillDelay={0.1}
            />
          </h1>
          <p className="text-sm text-white/90">
            Track your holdings. Explore the market.
          </p>
        </header>

        {/* Holdings occupy the full width because they are this page's primary task. */}
        <SpotlightCard>
          <section aria-labelledby="portfolio-heading" className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 id="portfolio-heading" className="text-2xl font-bold">
                Your portfolio
              </h2>
              {portfolioData.portfolio && (
                <span className="text-sm text-white/80">
                  {portfolioData.portfolio.name}
                </span>
              )}
            </div>

            {/* Missing data is shown as a dash, never as a misleading zero balance. */}
            {/* Match the table's equal thirds without horizontal gaps shifting the columns. */}
            <dl className="grid gap-y-4 sm:grid-cols-3">
              <div>
                <dt className="text-sm text-white/80">Current value</dt>
                <dd className="mt-1 text-2xl font-semibold">—</dd>
              </div>

              <div>
                <dt className="text-sm text-white/80">Invested</dt>
                <dd className="mt-1 text-2xl font-semibold">—</dd>
              </div>

              <div>
                <dt className="text-sm text-white/80">Unrealised gain / loss</dt>
                <dd className="mt-1 text-2xl font-semibold">—</dd>
              </div>
            </dl>

            {/* Announce loading and results without interrupting the user. */}
            <div aria-live="polite" className="border-t border-white/10 pt-5">
              {portfolioData.status === "loading" && (
                <p className="text-sm text-white/60">Loading your holdings…</p>
              )}

              {portfolioData.status === "error" && (
                <div className="space-y-3">
                  <p role="alert" className="text-sm text-amber-400">
                    {portfolioData.error}
                  </p>

                  <button
                    type="button"
                    onClick={() => setPortfolioReload((current) => current + 1)}
                    className="rounded-lg border border-white/20 px-4 py-2 text-sm"
                  >
                    Retry
                  </button>
                </div>
              )}

              {portfolioData.status === "ready" && (
                <div className="space-y-4">
                  {!portfolioData.portfolio && <p>You have no portfolios yet.</p>}

                  {portfolioData.portfolio && portfolioData.holdings.length === 0 && (
                    <p>You have no holdings in this portfolio yet.</p>
                  )}

                  {/* Contain table scrolling so the whole page stays within the screen. */}
                  {portfolioData.holdings.length > 0 && (
                    <div className="overflow-x-auto">
                      {/* Equal columns keep headings and values aligned as data changes. */}
                      <table className="w-full table-fixed text-left text-sm">
                        <colgroup>
                          <col className="w-1/3" />
                          <col className="w-1/3" />
                          <col className="w-1/3" />
                        </colgroup>
                        <thead className="border-b border-white/10 text-white/80">
                          <tr>
                            <th scope="col" className="pb-3 font-medium">Stock</th>
                            <th scope="col" className="pb-3 text-left font-medium">
                              Shares
                            </th>
                            <th scope="col" className="pb-3 text-left font-medium">
                              Average price paid
                            </th>
                          </tr>
                        </thead>

                        <tbody className="divide-y divide-white/10">
                          {portfolioData.holdings.map((holding) => (
                            <tr key={holding.id}>
                              <th scope="row" className="py-4 text-[#b8dc89]">
                                {holding.ticker}
                              </th>
                              <td className="py-4 text-left tabular-nums">
                                {holding.quantity}
                              </td>
                              <td className="py-4 text-left tabular-nums">
                                {formatPrice(holding.avgPurchasePrice)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                      <p className="mt-2 text-xs text-white/75">
                        Purchase-price currency unavailable.
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>

            <button
              type="button"
              aria-expanded={purchaseOpen}
              aria-controls="purchase-panel"
              onClick={() => setPurchaseOpen((open) => !open)}
            >
              {purchaseOpen ? "Close purchase form" : "Record purchase"}
            </button>

            <div id="purchase-panel" hidden={!purchaseOpen}>
              <RecordPurchaseForm
                key={purchaseVersion}
                ticker={purchaseTicker}
                onTickerChange={(ticker) => {
                  setPurchaseTicker(ticker);
                  if (quoteEstimate) {
                    setPurchasePrice("");
                    setQuoteEstimate(null);
                  }
                }}
                quantityInputRef={purchaseQuantityRef}
                priceInput={purchasePrice}
                onPriceChange={setPurchasePrice}
                quoteEstimate={quoteEstimate}
              />
            </div>

          </section>
        </SpotlightCard>

        {/* Stack on smaller screens; use a wider lookup column on desktop. */}
        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
          <SpotlightCard className="min-w-0">
            <section aria-labelledby="lookup-heading" className="min-w-0 space-y-5">
              <h2 id="lookup-heading" tabIndex={-1} className="scroll-mt-24 text-2xl font-bold">
                Stock lookup
              </h2>

              <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row">
                <label className="min-w-0 flex-1">
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
                  className="h-10 w-full self-end rounded-lg bg-primary px-4 text-primary-foreground disabled:opacity-50 sm:w-auto"
                >
                  {isHistoryLoading ? "Loading history..." : isLoading ? "Looking up..." : "Find price"}
                </button>
              </form>

              {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
              {quote && (
                <div className="min-w-0 border-t border-white/10 pt-5">
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

                    {/* Prefill only the ticker; a live quote is not the user's purchase price. */}
                    <button
                      type="button"
                      onClick={() => {
                        setPrefillError("");

                        const price = Number(quote.price);
                        if (quote.details?.currency !== "USD" || !Number.isFinite(price) || price <= 0) {
                          setPrefillError("This quote needs a USD price to fill the purchase form.");
                          return;
                        }

                        const form = purchaseQuantityRef.current?.form;
                        const hasDraft = form && Array.from(new FormData(form).values())
                          .some((value) => String(value).trim() !== "");

                        if (hasDraft && !window.confirm("Replace your current purchase draft?")) {
                          return;
                        }

                        setPurchaseTicker(quote.symbol);
                        setPurchasePrice(String(quote.price));
                        setQuoteEstimate({
                          asOf: quote.asOf,
                          stale: quote.stale,
                          lookedUpAt: quote.lookedUpAt,
                        });
                        setPurchaseVersion((version) => version + 1);
                        setPurchaseOpen(true);
                      }}
                      className="rounded-lg border border-[#b8dc89]/40 px-4 py-2 text-sm text-[#b8dc89]"
                    >
                      Use this stock
                    </button>

                    {prefillError && <p role="alert">{prefillError}</p>}

                    {/* Stack statistics on narrow screens so long ranges can fit. */}
                    {details && (
                      <dl className="grid grid-cols-1 gap-x-6 gap-y-5 border-t border-white/10 pt-5 sm:grid-cols-2">
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

                        <div className="sm:col-span-2">
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
                </div>
              )}
            </section>
          </SpotlightCard>

          {/* This follows lookup on small screens and sits beside it on desktop. */}
          <aside aria-label="Market overview" className="min-w-0 space-y-6">
            <MarketRankingCard
              title="Top 5 stocks"
              subtitle="By market capitalisation · US stocks"
              rows={topStockRows}
              footer={rankingFooter}
              onSelect={handleRankingSelect}
              disabled={isLoading}
            />

            <SpotlightCard>
              <section aria-labelledby="movers-heading" className="space-y-3">
                <header>
                  <h2 id="movers-heading" className="text-xl font-bold">
                    Top 5 movers
                  </h2>
                  <p className="mt-1 text-sm text-white/60">
                    Largest absolute daily change · US stocks
                  </p>
                </header>
                <p className="border-t border-white/10 pt-4 text-sm text-white/60">
                  No ranking loaded.
                </p>
              </section>
            </SpotlightCard>
          </aside>
        </div>
      </main>
    </DashboardShell>
  );
}
