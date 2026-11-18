// useEffect loads Portfolio data when the page opens.
import { useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { buy, createPortfolio, getPortfolios, getTrades, getValuation, sell } from "../api/portfolio";
import DashboardShell from "../components/DashboardShell";
import { getQuote, getPriceHistory, getTopStocks, getStockMovers } from "../api/marketData";
import SpotlightCard from "../components/SpotlightCard";
import StrokeText from "../components/StrokeText";
import CountUp from "../components/CountUp";
import PriceHistoryChart from "../components/PriceHistoryChart";
// The form handles purchase inputs and validation.
import RecordPurchaseForm from "../components/RecordPurchaseForm";
import SellSharesForm from "../components/SellSharesForm";
import TradeHistory from "../components/TradeHistory";
// Use the shared layout for the market ranking cards.
import MarketRankingCard from "../components/MarketRankingCard";
import useAuth from "../hooks/useAuth";
import { formatMoney, valuationReason } from "../lib/valuation.js";

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
const moverPercentage = new Intl.NumberFormat("en-IE", {
  style: "percent",
  signDisplay: "exceptZero",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const moverQuoteTime = new Intl.DateTimeFormat("en-IE", {
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "UTC",
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
  const { status: authStatus, session } = useAuth();
  const userId = authStatus === "ready" ? session?.user.id : undefined;
  const location = useLocation();
  const [tickerInput, setTickerInput] = useState("");

  const [quote, setQuote] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const [error, setError] = useState("");

  const [history, setHistory] = useState([]);
  const [historyError, setHistoryError] = useState("");
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const lookupGridRef = useRef(null);
  const [loadingGridHeight, setLoadingGridHeight] = useState(null);

  const [topStocks, setTopStocks] = useState({ status: "loading" });
  const [movers, setMovers] = useState({ status: "loading" });

  useEffect(() => {
    const controller = new AbortController();

    // A failed ranking request should leave holdings and stock lookup usable.
    getStockMovers(controller.signal).then(
      (ranking) => {
        if (!controller.signal.aborted) setMovers({ status: "ready", ranking });
      },
      () => {
        if (!controller.signal.aborted) setMovers({ status: "unavailable" });
      },
    );

    return () => controller.abort();
  }, []);

  const moversRanking = movers.ranking;
  const moverRows = moversRanking?.rows.map(({ ticker, company, percentChange, asOf, stale }) => ({
    ticker,
    company,
    // The quote already expresses the change in percentage points, e.g. 8 for 8%.
    metric: moverPercentage.format(percentChange / 100),
    metricClassName: percentChange > 0 ? "text-green-300" : percentChange < 0 ? "text-red-300" : "",
    asOf,
    asOfLabel: `${moverQuoteTime.format(new Date(asOf))} UTC`,
    stale,
  })) ?? [];

  let moversFooter = "Stock movers are temporarily unavailable.";
  if (movers.status === "loading") moversFooter = "Loading movers…";
  if (moversRanking) {
    moversFooter = moverRows.length > 0
      ? `Daily change`
      : "No tracked stocks have daily change data.";
  }

  const hasLookupChart = Boolean(quote && !isHistoryLoading && !historyError && history.length > 1);
  const alignRankingCards = hasLookupChart || loadingGridHeight !== null;

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
    rankingFooter = `Captured ${rankingCaptureDate.format(new Date(snapshot.capturedOn))}`;
  }

  // Keep the request status and its data together.
  const [portfolioResult, setPortfolioResult] = useState(null);
  const portfolioData = userId && portfolioResult?.userId === userId
    ? portfolioResult
    : { status: "loading", portfolio: null, holdings: [], error: "" };

  // Share only the selected ticker with the purchase form; preserve its other inputs.
  const [purchaseTicker, setPurchaseTicker] = useState("");

  const [purchaseOpen, setPurchaseOpen] = useState(false);

  // The last looked-up quote, shown in the buy form as a price estimate.
  const [quoteEstimate, setQuoteEstimate] = useState(null);

  const purchaseQuantityRef = useRef(null);

  // The holding whose sell form is open, if any.
  const [sellingTicker, setSellingTicker] = useState(null);
  const sellQuantityRef = useRef(null);

  // A short confirmation after a buy or sale, announced to screen readers.
  const [tradeNotice, setTradeNotice] = useState("");

  const [newPortfolioName, setNewPortfolioName] = useState("My portfolio");
  const [createState, setCreateState] = useState({ saving: false, error: "" });

  const [previousUserId, setPreviousUserId] = useState(userId);
  if (previousUserId !== userId) {
    setPreviousUserId(userId);
    setPortfolioResult(null);
    setPurchaseOpen(false);
    setPurchaseTicker("");
    setQuoteEstimate(null);
    setSellingTicker(null);
    setTradeNotice("");
  }

  useEffect(() => {
    if (purchaseOpen) purchaseQuantityRef.current?.focus();
  }, [purchaseOpen]);

  useEffect(() => {
    if (sellingTicker) sellQuantityRef.current?.focus();
  }, [sellingTicker]);

  // Changing this number requests the data again (Retry, or after a trade).
  const [portfolioReload, setPortfolioReload] = useState(0);

  useEffect(() => {
    if (!userId) return;

    const controller = new AbortController();

    async function loadPortfolio() {
      // After a trade, keep showing the current table while it refreshes
      // instead of flashing back to "Loading your holdings…".
      setPortfolioResult((previous) =>
        previous?.userId === userId && previous.status === "ready"
          ? previous
          : {
            userId,
            status: "loading",
            portfolio: null,
            holdings: [],
            trades: [],
            error: "",
          }
      );

      try {
        const portfolios = await getPortfolios(controller.signal);
        if (controller.signal.aborted) return;

        // Initially display the first available portfolio.
        // Its name will be visible so the user knows which one is selected.
        const selectedPortfolio = portfolios[0] ?? null;

        // Trade history is secondary: if it fails, still show the valuation.
        const [valuation, tradesResult] = selectedPortfolio
          ? await Promise.all([
            getValuation(selectedPortfolio.id, controller.signal),
            getTrades(selectedPortfolio.id, controller.signal).then(
              (trades) => ({ trades, error: "" }),
              (error) => {
                if (error.name === "AbortError") throw error;
                return { trades: [], error: error.message };
              },
            ),
          ])
          : [null, { trades: [], error: "" }];
        // Fully sold holdings stay in the database with zero shares; hide them.
        const holdings = (valuation?.holdings ?? []).filter((holding) => Number(holding.quantity) > 0);

        // Ignore results if the page was left while requests were running.
        if (controller.signal.aborted) return;

        setPortfolioResult({
          userId,
          status: "ready",
          portfolio: selectedPortfolio,
          holdings,
          valuation,
          trades: tradesResult.trades,
          tradesError: tradesResult.error,
          error: "",
        });
      } catch (error) {
        if (controller.signal.aborted) return;

        setPortfolioResult({
          userId,
          status: "error",
          portfolio: null,
          holdings: [],
          trades: [],
          error: error.message,
        });
      }
    }

    loadPortfolio();

    // Cancel unfinished requests when leaving the page or retrying.
    return () => controller.abort();
  }, [userId, portfolioReload]);

  const portfolioId = portfolioData.portfolio?.id;

  async function handleBuy({ ticker, quantity, price, executedAt }) {
    setTradeNotice("");
    await buy(portfolioId, ticker, quantity, { price, executedAt });
    setTradeNotice(
      executedAt
        ? `Recorded ${quantity} ${ticker} bought on ${new Date(executedAt).toLocaleString("en-IE")}.`
        : `Bought ${quantity} ${ticker}.`
    );
    setPortfolioReload((current) => current + 1);
  }

  async function handleSell({ ticker, quantity }) {
    setTradeNotice("");
    await sell(portfolioId, ticker, quantity);
    setSellingTicker(null);
    setTradeNotice(`Sold ${quantity} ${ticker}.`);
    setPortfolioReload((current) => current + 1);
  }

  async function handleCreatePortfolio(event) {
    event.preventDefault();
    const name = newPortfolioName.trim();
    if (!name) {
      setCreateState({ saving: false, error: "Enter a name for your portfolio." });
      return;
    }

    setCreateState({ saving: true, error: "" });
    try {
      await createPortfolio(name);
      setCreateState({ saving: false, error: "" });
      setPortfolioReload((current) => current + 1);
    } catch (error) {
      setCreateState({ saving: false, error: error.message });
    }
  }


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

    // Hold the desktop grid at its current height while replacing the quote and chart.
    if (hasLookupChart && window.matchMedia("(min-width: 64rem)").matches) {
      setLoadingGridHeight(lookupGridRef.current?.getBoundingClientRect().height ?? null);
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
      setLoadingGridHeight(null);
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
        <header className="flex flex-col items-start gap-2 text-left">

          <h1 className="w-full">
            <StrokeText
              align="left"
              className="max-w-[520px]"
              text="Portfolio"
              fontSize={64}
              letterSpacing={-2}
              strokeColor="#f1cbfc"
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
            {/* Totals use the backend calculations, including its completeness policy. */}
            <dl className="grid gap-y-4 sm:grid-cols-3">
              <div>
                <dt className="text-sm text-white/80">Current value</dt>
                <dd className="mt-1 text-2xl font-semibold">{formatMoney(portfolioData.valuation?.currentValue, portfolioData.valuation?.currency)}</dd>
              </div>

              <div>
                <dt className="text-sm text-white/80">Cost of current holdings</dt>
                <dd className="mt-1 text-2xl font-semibold">{formatMoney(portfolioData.valuation?.costBasis, portfolioData.valuation?.currency)}</dd>
              </div>

              <div>
                <dt className="text-sm text-white/80">Unrealised gain / loss</dt>
                <dd className="mt-1 text-2xl font-semibold">{formatMoney(portfolioData.valuation?.unrealisedGainLoss, portfolioData.valuation?.currency)}</dd>
              </div>
            </dl>

            {portfolioData.valuation && !portfolioData.valuation.complete && (
              <p className="text-sm text-amber-400" role="status">
                Valuation incomplete: {portfolioData.valuation.unavailableHoldings} holding(s) unavailable. Portfolio totals are unavailable.
              </p>
            )}
            {portfolioData.valuation?.stale && (
              <p className="text-sm text-amber-400">Valuation includes cached prices. Check each quote’s timestamp.</p>
            )}

            {/* Announce loading and results without interrupting the user. */}
            <div aria-live="polite" className="border-t border-white/10 pt-5">
              {authStatus === "loading" && (
                <p className="text-sm text-white/60">Checking your session…</p>
              )}

              {authStatus === "ready" && !userId && (
                <Link
                  to="/login"
                  state={{ from: location.pathname + location.search }}
                  className="text-sm text-[#b8dc89] underline"
                >
                  Sign in to view your holdings
                </Link>
              )}

              {userId && portfolioData.status === "loading" && (
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
                  {!portfolioData.portfolio && (
                    <form onSubmit={handleCreatePortfolio} className="space-y-3">
                      <p>You have no portfolios yet. Create one to start buying shares.</p>
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                        <label className="min-w-0 flex-1 text-sm text-white/85">
                          Portfolio name
                          <input
                            type="text"
                            value={newPortfolioName}
                            onChange={(event) => setNewPortfolioName(event.target.value)}
                            maxLength={255}
                            required
                            disabled={createState.saving}
                            className="mt-1.5 h-11 w-full rounded-lg border border-white/15 bg-black/20 px-3 text-white"
                          />
                        </label>
                        <button
                          type="submit"
                          disabled={createState.saving}
                          className="min-h-11 rounded-lg bg-[#b8dc89] px-4 py-2 text-sm font-semibold text-black disabled:opacity-50"
                        >
                          {createState.saving ? "Creating…" : "Create portfolio"}
                        </button>
                      </div>
                      {createState.error && (
                        <p role="alert" className="text-sm text-amber-400">{createState.error}</p>
                      )}
                    </form>
                  )}

                  {portfolioData.portfolio && portfolioData.holdings.length === 0 && (
                    <p>You have no holdings in this portfolio yet.</p>
                  )}

                  {/* Contain table scrolling so the whole page stays within the screen. */}
                  {portfolioData.holdings.length > 0 && (
                    <div className="overflow-x-auto">
                      {/* Keep quote timestamps and unavailable reasons beside each holding. */}
                      <table className="w-full min-w-[760px] text-left text-sm [&_td]:pr-4 [&_th]:pr-4">
                        <thead className="border-b border-white/10 text-white/80">
                          <tr>
                            <th scope="col" className="pb-3 font-medium">Stock</th>
                            <th scope="col" className="pb-3 text-left font-medium">
                              Shares
                            </th>
                            <th scope="col" className="pb-3 text-left font-medium">
                              Average price paid
                            </th>
                            <th scope="col" className="pb-3 font-medium">Current price / quote time</th>
                            <th scope="col" className="pb-3 font-medium">Current value</th>
                            <th scope="col" className="pb-3 font-medium">Unrealised gain / loss</th>
                            <th scope="col" className="pb-3 font-medium">
                              <span className="sr-only">Actions</span>
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
                                {formatPrice(holding.avgPurchasePrice)} {holding.currency ?? "(currency unknown)"}
                              </td>
                              <td className="py-4 pr-3">
                                {formatMoney(holding.quote?.price, holding.quote?.currency)}
                                {holding.quote?.asOf && <span className="block text-xs text-white/75">As of {new Date(holding.quote.asOf).toLocaleString("en-IE")}</span>}
                                {holding.quote?.stale && <span className="block text-xs text-amber-400">Cached price — live quote unavailable</span>}
                                {!holding.valuationAvailable && <span className="block text-xs text-amber-400">{valuationReason(holding.unavailableReason)}</span>}
                              </td>
                              <td className="py-4">{formatMoney(holding.currentValue, holding.currency)}</td>
                              <td className="py-4">{formatMoney(holding.unrealisedGainLoss, holding.currency)}</td>
                              <td className="py-4 text-right">
                                <button
                                  type="button"
                                  aria-expanded={sellingTicker === holding.ticker}
                                  aria-controls="sell-panel"
                                  onClick={() => {
                                    setTradeNotice("");
                                    setSellingTicker((current) =>
                                      current === holding.ticker ? null : holding.ticker
                                    );
                                  }}
                                  className="rounded-lg border border-red-300/40 px-3 py-1.5 text-sm text-red-300"
                                >
                                  Sell<span className="sr-only"> {holding.ticker}</span>
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  {(() => {
                    const sellingHolding = portfolioData.holdings.find(
                      (holding) => holding.ticker === sellingTicker
                    );
                    return sellingHolding && (
                      <div id="sell-panel">
                        <SellSharesForm
                          key={sellingHolding.ticker}
                          holding={sellingHolding}
                          quantityInputRef={sellQuantityRef}
                          onSell={handleSell}
                          onCancel={() => setSellingTicker(null)}
                        />
                      </div>
                    );
                  })()}

                  {tradeNotice && (
                    <p role="status" className="text-sm text-green-300">{tradeNotice}</p>
                  )}
                </div>
              )}
            </div>

            {/* Buying needs a portfolio to put the shares in. */}
            {userId && portfolioId != null && (
              <>
                <button
                  type="button"
                  aria-expanded={purchaseOpen}
                  aria-controls="purchase-panel"
                  onClick={() => setPurchaseOpen((open) => !open)}
                  className="rounded-lg border border-[#b8dc89]/40 px-4 py-2 text-sm text-[#b8dc89]"
                >
                  {purchaseOpen ? "Close form" : "Add shares"}
                </button>

                <div id="purchase-panel" hidden={!purchaseOpen}>
                  <RecordPurchaseForm
                    key={userId}
                    ticker={purchaseTicker}
                    onTickerChange={setPurchaseTicker}
                    quantityInputRef={purchaseQuantityRef}
                    quoteEstimate={quoteEstimate}
                    onSave={handleBuy}
                  />
                </div>
              </>
            )}

          </section>
        </SpotlightCard>

        {portfolioData.status === "ready" && portfolioData.portfolio && (
          <SpotlightCard>
            <section aria-labelledby="trades-heading" className="space-y-5">
              <h2 id="trades-heading" className="text-2xl font-bold">
                Trade history
              </h2>
              <TradeHistory
                trades={portfolioData.trades ?? []}
                error={portfolioData.tradesError}
              />
            </section>
          </SpotlightCard>
        )}

        {/* Stack on smaller screens; use a wider lookup column on desktop. */}
        <div
          ref={lookupGridRef}
          style={loadingGridHeight === null ? undefined : {
            "--lookup-loading-height": `${loadingGridHeight}px`,
          }}
          className={`grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)] ${
            alignRankingCards ? "lg:items-stretch lg:min-h-[var(--lookup-loading-height,0px)]" : ""
          }`}
        >
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

                    {/* Open the buy form for this stock, using the quote as an estimate. */}
                    {userId && portfolioId != null && (
                      <button
                        type="button"
                        onClick={() => {
                          setPurchaseTicker(quote.symbol);
                          setQuoteEstimate({
                            symbol: quote.symbol,
                            price: quote.price,
                            currency: details?.currency,
                            asOf: quote.asOf,
                            stale: quote.stale,
                          });
                          setPurchaseOpen(true);
                          // The form may already be open; focus the shares field either way.
                          purchaseQuantityRef.current?.focus();
                          document.getElementById("portfolio-heading")?.scrollIntoView({ behavior: "smooth" });
                        }}
                        className="rounded-lg border border-[#b8dc89]/40 px-4 py-2 text-sm text-[#b8dc89]"
                      >
                        Buy this stock
                      </button>
                    )}
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
          <aside
            aria-label="Market overview"
            className={`grid min-w-0 gap-6 ${alignRankingCards ? "lg:auto-rows-fr" : ""}`}
          >
            <MarketRankingCard
              title="Top 5 stocks"
              subtitle="By market capitalisation"
              rows={topStockRows}
              footer={rankingFooter}
              onSelect={handleRankingSelect}
              disabled={isLoading}
            />

            <MarketRankingCard
              title="Top 5 movers"
              subtitle="Largest daily % moves"
              rows={moverRows}
              footer={moversFooter}
              onSelect={handleRankingSelect}
              disabled={isLoading}
            />
          </aside>
        </div>
      </main>
    </DashboardShell>
  );
}
