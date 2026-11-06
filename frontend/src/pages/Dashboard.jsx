import { useEffect, useMemo, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import StatCard from "@/components/StatCard";
import DashboardShell from "@/components/DashboardShell";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid } from "recharts";
import { Wallet, Receipt, TrendingUp, Landmark, Search, Plus, Trash2, RefreshCw, Loader2 } from "lucide-react"
import SpendingByCurrency from "../components/transactions/SpendingByCurrency";
import { isValidUserId, readTransactionUser, rememberTransactionUser } from "../lib/transactionUser";
import useTransactionMetadata from '../hooks/useTransactionMetadata';
import { currencyLabel } from '../lib/money';
import { getTransactionSummary } from "../api/transactions";

const formatMoney = (amount) =>
  new Intl.NumberFormat("en-IE", { style: "currency", currency: "EUR" }).format(amount);



const portfolio = {
    available: true,
    investmentValue: 4820,
    investmentValueChange: 5.7,
    investmentValueCaption: "portfolio, 30 days",
    investmentValuePositive: true,
};

// ====================== MARKET DATA API IMPLEMENTATION BELOW: FORMATTING, SEARCHING, UPDATING, ETC. ======================
const HOLDINGS_STORAGE_KEY = "finance-tracker:dashboard-holdings";
const MARKET_DATA_API_BASE_URL = (
  import.meta.env.VITE_MARKET_DATA_API_BASE_URL || "/market-data"
).replace(/\/$/, "");

const holdingsChartConfig = {
  amount: {
    label: "Amount invested",
    color: "var(--chart-2)",
  },
};

const formatCompactMoney = (amount) =>
  new Intl.NumberFormat("en-IE", {
    style: "currency",
    currency: "EUR",
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(amount);

const formatQuotePrice = (price) =>
  new Intl.NumberFormat("en-IE", { maximumFractionDigits: 4 }).format(price);

function readSavedHoldings() {
  try {
    const saved = JSON.parse(localStorage.getItem(HOLDINGS_STORAGE_KEY) || "[]");
    if (!Array.isArray(saved)) return [];

    return saved
      .filter((holding) => holding && typeof holding.symbol === "string")
      .map((holding) => ({
        ...holding,
        symbol: holding.symbol.trim().toUpperCase(),
        amount: String(holding.amount ?? ""),
      }))
      .filter((holding) => holding.symbol);
  } catch {
    return [];
  }
}

async function fetchQuote(symbol) {
  const response = await fetch(
    `${MARKET_DATA_API_BASE_URL}/api/quotes/${encodeURIComponent(symbol)}`
  );
  const quote = await response.json().catch(() => ({}));

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error(`No quote found for ${symbol}. Check the ticker and try again.`);
    }
    throw new Error(quote.detail || `Market data request failed (${response.status}).`);
  }

  const price = Number(quote.price);
  if (!quote.symbol || !Number.isFinite(price)) {
    throw new Error("The market-data service returned an invalid quote.");
  }

  return {
    symbol: quote.symbol.trim().toUpperCase(),
    price,
    asOf: quote.asOf,
    //true when the backend had to use an older cached price
    stale: quote.stale === true,
  };
}

function InvestmentPortfolioCard() {
  const [holdings, setHoldings] = useState(readSavedHoldings);
  const [symbolQuery, setSymbolQuery] = useState("");
  const [draftAmount, setDraftAmount] = useState("");
  const [quoteResult, setQuoteResult] = useState(null);
  const [searchError, setSearchError] = useState("");
  const [amountError, setAmountError] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [refreshMessage, setRefreshMessage] = useState("");

  useEffect(() => {
    try {
      localStorage.setItem(HOLDINGS_STORAGE_KEY, JSON.stringify(holdings));
    } catch {
      // The form remains usable if browser storage is unavailable.
    }
  }, [holdings]);

  const chartData = useMemo(
    () => holdings
      .map((holding) => ({
        ...holding,
        amountValue: Number(holding.amount),
      }))
      .filter((holding) => Number.isFinite(holding.amountValue) && holding.amountValue > 0),
    [holdings]
  );

  const totalInvested = chartData.reduce((sum, holding) => sum + holding.amountValue, 0);
  const chartWidth = Math.max(560, chartData.length * 118 + 100);

  async function handleTickerSearch(event) {
    event.preventDefault();
    const symbol = symbolQuery.trim().toUpperCase();

    if (!symbol) {
      setSearchError("Enter a ticker symbol to search.");
      return;
    }
    //     if (!/^[A-Z0-9.^=$-]{1,24}$/.test(symbol)) {
    //       setSearchError("Use a ticker symbol with letters, numbers, dots, or hyphens.");
    //       return;
    //     }

    setIsSearching(true);
    setSearchError("");
    setAmountError("");
    setRefreshMessage("");
    setQuoteResult(null);

    try {
      setQuoteResult(await fetchQuote(symbol));
    } catch (error) {
      setSearchError(error instanceof TypeError
        ? "Couldn't reach the market-data service. Check that it is running and try again."
        : error.message);
    } finally {
      setIsSearching(false);
    }
  }

  function addHolding(event) {
    event.preventDefault();
    if (!quoteResult) return;

    const amount = Number(draftAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setAmountError("Enter an amount greater than €0.");
      return;
    }

    setHoldings((current) => {
      const existingIndex = current.findIndex(
        (holding) => holding.symbol === quoteResult.symbol
      );
      const updatedHolding = {
        ...(existingIndex >= 0 ? current[existingIndex] : {}),
        ...quoteResult,
        amount: String(amount),
      };

      if (existingIndex < 0) return [...current, updatedHolding];
      return current.map((holding, index) => index === existingIndex ? updatedHolding : holding);
    });
    setSymbolQuery("");
    setDraftAmount("");
    setQuoteResult(null);
    setAmountError("");
    setRefreshMessage("");
  }

  function updateHoldingAmount(symbol, amount) {
    setHoldings((current) => current.map((holding) =>
      holding.symbol === symbol ? { ...holding, amount } : holding
    ));
  }

  async function refreshQuotes() {
    if (!holdings.length) return;

    setIsRefreshing(true);
    setRefreshMessage("");
    const results = await Promise.all(holdings.map(async (holding) => {
      try {
        const quote = await fetchQuote(holding.symbol);
        return { holding: { ...holding, ...quote }, succeeded: true };
      } catch {
        return { holding, succeeded: false };
      }
    }));

    setHoldings(results.map(({ holding }) => holding));
    const succeeded = results.filter((result) => result.succeeded).length;
    setRefreshMessage(succeeded === results.length
      ? `Updated ${succeeded} ${succeeded === 1 ? "quote" : "quotes"}.`
      : `Updated ${succeeded} of ${results.length} quotes. Some prices couldn't be refreshed.`);
    setIsRefreshing(false);
  }

  return (
    <Card id="portfolio" className="glass bg-card/10 scroll-mt-24">
      <CardHeader>
        <CardTitle>Investment by ticker</CardTitle>
        <p className="text-sm text-muted-foreground">
          Search for a ticker, check its latest quote, and enter how much you invested.
        </p>
      </CardHeader>
      <CardContent className="space-y-6">
        <form onSubmit={handleTickerSearch} className="flex flex-col gap-3 sm:flex-row">
          <label className="relative min-w-0 flex-1">
            <span className="sr-only">Search ticker symbols</span>
            <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input
              type="search"
              value={symbolQuery}
              onChange={(event) => setSymbolQuery(event.target.value)}
              placeholder="Search ticker symbols, e.g. AAPL"
              autoComplete="off"
              className="h-10 w-full rounded-lg border border-input bg-background/70 pl-9 pr-3 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </label>
          <button
            type="submit"
            disabled={isSearching}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSearching ? <Loader2 aria-hidden="true" className="size-4 animate-spin" /> : <Search aria-hidden="true" className="size-4" />}
            Find ticker
          </button>
        </form>

        {searchError && <p role="alert" className="text-sm text-destructive">{searchError}</p>}

        {quoteResult && (
          <form onSubmit={addHolding} className="grid gap-4 rounded-lg border border-border/70 bg-background/35 p-4 sm:grid-cols-[minmax(0,1fr)_200px_auto] sm:items-end">
            <div className="min-w-0">
              <p className="font-semibold text-foreground">{quoteResult.symbol}</p>
              <p className="text-sm text-muted-foreground">
                Latest quote: {formatQuotePrice(quoteResult.price)} per share
                {quoteResult.asOf && (
                  <span> · as of {new Date(quoteResult.asOf).toLocaleString("en-IE", { dateStyle: "medium", timeStyle: "short" })}</span>
                )}
              </p>

              {quoteResult.stale && (
                <p role="status" className="text-sm text-amber-400">
                  Cached price - live market data is temporarily unavailable
                </p>
              )}

            </div>
            <label className="grid gap-1.5 text-sm">
              <span>Amount invested (€)</span>
              <input
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                value={draftAmount}
                onChange={(event) => {
                  setDraftAmount(event.target.value);
                  setAmountError("");
                }}
                placeholder="0.00"
                className="h-10 rounded-lg border border-input bg-background/70 px-3 text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </label>
            <button
              type="submit"
              className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-secondary px-4 text-sm font-medium text-secondary-foreground transition-colors hover:bg-secondary/80"
            >
              <Plus aria-hidden="true" className="size-4" />
              {holdings.some((holding) => holding.symbol === quoteResult.symbol) ? "Update holding" : "Add holding"}
            </button>
            {amountError && <p role="alert" className="text-sm text-destructive sm:col-span-3">{amountError}</p>}
          </form>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/60 pt-5">
          <div>
            <h3 className="font-medium text-foreground">Your holdings</h3>
            <p className="text-sm text-muted-foreground">Invested total: {formatMoney(totalInvested)}</p>
          </div>
          {holdings.length > 0 && (
            <button
              type="button"
              onClick={refreshQuotes}
              disabled={isRefreshing}
              className="inline-flex h-8 items-center gap-2 rounded-md px-2.5 text-sm text-muted-foreground transition-colors hover:bg-foreground/5 hover:text-foreground disabled:opacity-60"
            >
              <RefreshCw aria-hidden="true" className={`size-3.5 ${isRefreshing ? "animate-spin" : ""}`} />
              Refresh quotes
            </button>
          )}
        </div>

        {refreshMessage && <p role="status" className="text-sm text-muted-foreground">{refreshMessage}</p>}

        {holdings.length > 0 ? (
          <ul className="divide-y divide-border/60 rounded-lg border border-border/60">
            {holdings.map((holding) => (
              <li key={holding.symbol} className="grid gap-3 p-3 sm:grid-cols-[minmax(0,1fr)_180px_auto] sm:items-center sm:p-4">
                <div className="min-w-0">
                  <p className="font-semibold text-foreground">{holding.symbol}</p>
                  <p className="text-sm text-muted-foreground">
                    {Number.isFinite(Number(holding.price))
                      ? `Latest quote: ${formatQuotePrice(Number(holding.price))} per share`
                      : "Quote unavailable"}
                    {holding.asOf && (
                      <span> · {new Date(holding.asOf).toLocaleString("en-IE", { dateStyle: "medium", timeStyle: "short" })}</span>
                    )}
                  </p>

                  {/* Label an older price returned from the backend cache. */}
                  {holding.stale && (
                    <p className="text-xs text-amber-400">
                      Cached price - live quote unavailable.
                    </p>
                  )}

                </div>
                <label className="grid gap-1 text-xs text-muted-foreground">
                  Amount invested (€)
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    inputMode="decimal"
                    value={holding.amount}
                    onChange={(event) => updateHoldingAmount(holding.symbol, event.target.value)}
                    className="h-9 rounded-md border border-input bg-background/70 px-2.5 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  />
                </label>
                <button
                  type="button"
                  onClick={() => setHoldings((current) => current.filter((item) => item.symbol !== holding.symbol))}
                  aria-label={`Remove ${holding.symbol} holding`}
                  className="inline-flex size-9 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <Trash2 aria-hidden="true" className="size-4" />
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <div className="rounded-lg border border-dashed border-border/70 px-4 py-8 text-center">
            <p className="font-medium text-foreground">No holdings added yet</p>
            <p className="mt-1 text-sm text-muted-foreground">Search a ticker above to add your first investment.</p>
          </div>
        )}

        <div>
          <div className="mb-2 flex items-center justify-between gap-3">
            <h3 className="font-medium text-foreground">Amount invested by ticker</h3>
            {chartData.length > 0 && <span className="text-xs text-muted-foreground">Scroll horizontally to see all tickers</span>}
          </div>
          {chartData.length > 0 ? (
            <div className="overflow-x-auto rounded-lg border border-border/60 bg-background/20" tabIndex={0} aria-label="Horizontally scrollable investment chart">
              <ChartContainer config={holdingsChartConfig} className="max-w-none shrink-0 !aspect-auto" style={{ width: chartWidth, height: 300 }}>
                <BarChart data={chartData} margin={{ top: 20, right: 24, left: 8, bottom: 8 }}>
                  <CartesianGrid vertical={false} strokeDasharray="3 3" />
                  <XAxis dataKey="symbol" tickLine={false} axisLine={false} interval={0} />
                  <YAxis width={74} tickLine={false} axisLine={false} tickFormatter={formatCompactMoney} />
                  <ChartTooltip
                    content={<ChartTooltipContent formatter={(value, name) => (
                      <div className="flex min-w-40 justify-between gap-4">
                        <span className="text-muted-foreground">{name}</span>
                        <span className="font-mono font-medium text-foreground">{formatMoney(Number(value))}</span>
                      </div>
                    )} />}
                  />
                  <Bar dataKey="amountValue" name="Amount invested" fill="var(--color-amount)" radius={[5, 5, 0, 0]} maxBarSize={52} />
                </BarChart>
              </ChartContainer>
            </div>
          ) : (
            <p className="rounded-lg border border-border/60 bg-background/20 px-4 py-6 text-center text-sm text-muted-foreground">
              Add a holding with an invested amount to see it here.
            </p>
          )}
        </div>

        <p className="text-xs text-muted-foreground">
          Holdings are saved in this browser. The quote API does not include quote currency, so prices are shown without a currency symbol.
        </p>
      </CardContent>
    </Card>
  );
}
// ====================== MARKET DATA API IMPLEMENTATION ABOVE: FORMATTING, SEARCHING, UPDATING, ETC. ======================
function TransactionOverview({ userId }) {
  const metadata = useTransactionMetadata();
  const preferenceKey = `finance-tracker:transaction-currency:${userId}`;
  const [preferred, setPreferred] = useState(() => {
    try { return localStorage.getItem(preferenceKey); } catch { return null; }
  });
  const currencies = metadata.data?.currencies ?? [];
  const currency = currencies.includes(preferred) ? preferred : currencies[0];
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (!currency) return;
    const controller = new AbortController();
    getTransactionSummary(userId, currency, controller.signal).then(
      (summary) => {
        if (!controller.signal.aborted) setResult({ currency, attempt, summary });
      },
      (error) => {
        if (!controller.signal.aborted) setResult({ currency, attempt, error: error.message });
      },
    );
    return () => controller.abort();
  }, [userId, currency, attempt]);

  function changeCurrency(event) {
    const value = event.target.value;
    setPreferred(value);
    try { localStorage.setItem(preferenceKey, value); } catch { /* Preference is optional. */ }
  }

  const current = result?.currency === currency && result?.attempt === attempt ? result : null;
  const error = metadata.error || current?.error;
  const summary = current?.summary;
  const formatter = new Intl.NumberFormat('en-IE', {
    style: 'currency', currency: currency || 'EUR', currencyDisplay: 'code',
  });
  return (
    <>
      <Card className="glass bg-card/10">
        <CardHeader><CardTitle>Transaction summary</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <label className="grid max-w-xs gap-2 text-sm">Transaction display currency
            <select value={currency || ''} onChange={changeCurrency} disabled={!currencies.length}
              className="min-h-11 rounded-lg border border-input bg-background/70 px-3 text-foreground">
              {currencies.map((code) => <option key={code} value={code}>{currencyLabel(code)}</option>)}
            </select>
          </label>
          <p className="text-sm text-muted-foreground">All-time totals use transaction-date exchange rates.</p>
          {error ? <div role="alert" className="space-y-2">
            <p>{error}</p>
            <button type="button" className="rounded-lg border border-input px-4 py-2"
              onClick={metadata.error ? metadata.retry : () => setAttempt((value) => value + 1)}>Retry</button>
          </div> : !summary && <p role="status">Loading transaction summary…</p>}
        </CardContent>
      </Card>
      {summary && <>
        <div className="grid gap-4 md:grid-cols-3">
          <StatCard title="Total Income" icon={Wallet} available value={summary.income} formatter={formatter.format} />
          <StatCard title="Total Expenses" icon={Receipt} available value={summary.expenses} formatter={formatter.format} />
          <StatCard title="Net Income" icon={Landmark} available value={summary.netIncome} formatter={formatter.format} />
        </div>
        <SpendingByCurrency summary={summary} />
      </>}
    </>
  );
}

function Dashboard() {
    const [selectedUserId, setSelectedUserId] = useState(readTransactionUser);
    const [userIdInput, setUserIdInput] = useState(selectedUserId);
    const [userIdError, setUserIdError] = useState('');


    function selectTransactionUser(event) {
      event.preventDefault();
      const userId = userIdInput.trim();
      if (!isValidUserId(userId)) {
        setUserIdError('Enter a positive whole-number user ID within the supported range.');
        return;
      }
      setUserIdError('');
      setUserIdInput(userId);
      rememberTransactionUser(userId);
      setSelectedUserId(userId);

    }

  return (
      <DashboardShell>
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h1 className="text-3xl font-bold">Dashboard</h1>
        </div>

            <Card className="glass bg-card/10">
              <CardHeader>
                <CardTitle>Transaction user</CardTitle>
                <p className="text-sm text-foreground/80">Until login is connected, enter the same user ID used when creating your transactions.</p>
              </CardHeader>
              <CardContent>
                <form onSubmit={selectTransactionUser} className="flex flex-wrap items-end gap-3">
                  <label className="grid gap-2 text-sm">
                    User ID
                    <input type="text" inputMode="numeric" required maxLength={19}
                      value={userIdInput} onChange={(event) => { setUserIdInput(event.target.value); setUserIdError(''); }}
                      aria-invalid={Boolean(userIdError)} aria-describedby={userIdError ? 'dashboard-user-error' : undefined}
                      className="min-h-11 rounded-lg border border-input bg-background/70 px-3 text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" />
                  </label>
                  <button type="submit" className="min-h-11 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground">Show summary</button>
                </form>
                {userIdError && <p id="dashboard-user-error" role="alert" className="mt-2 text-sm text-destructive">{userIdError}</p>}
                <p role="status" className="mt-3 text-sm text-foreground/80">
                  {selectedUserId ? `Showing transactions for user ${selectedUserId}.` : 'Select a user to load their summary.'}
                </p>
              </CardContent>
            </Card>
            {selectedUserId && <TransactionOverview key={selectedUserId} userId={selectedUserId} />}
        <StatCard
          title="Investment Value (EUR)"
          icon={TrendingUp}
          available={portfolio.available}
          value={portfolio.investmentValue}
          formatter={formatMoney}
          change={portfolio.investmentValueChange}
          caption={portfolio.investmentValueCaption}
          positiveIsGood={portfolio.investmentValuePositive}
        />
            <InvestmentPortfolioCard />
      </DashboardShell>
    );
}

export default Dashboard
