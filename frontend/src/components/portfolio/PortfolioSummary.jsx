import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Cell, Pie, PieChart } from "recharts";
import { Briefcase, PiggyBank, TrendingUp } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "../ui/chart";
import { Button } from "../ui/button";
import StatCard from "../StatCard";
import { getPerformance, getPortfolios, getValuation } from "../../api/portfolio";
import { summarisePortfolio } from "../../lib/portfolioSummary";

const palette = ["var(--chart-1)", "var(--chart-2)", "var(--chart-3)", "var(--chart-4)", "var(--chart-5)"];

/**
 * Loads the summary for the user's first portfolio (the one the Portfolio page shows).
 * Performance is optional: if it fails, only the realised-profit card is unavailable.
 *
 * @param {AbortSignal} signal cancels the requests when the dashboard is left
 * @return {Promise<{status: "empty"}|{status: "ready", portfolio: object, summary: object}>}
 */
async function loadSummary(signal) {
  const portfolios = await getPortfolios(signal);
  const portfolio = portfolios[0];
  if (!portfolio) return { status: "empty" };

  const [valuation, performance] = await Promise.all([
    getValuation(portfolio.id, signal),
    getPerformance(portfolio.id, signal).catch((error) => {
      if (error.name === "AbortError") throw error;
      return null;
    }),
  ]);
  return { status: "ready", portfolio, summary: summarisePortfolio(valuation, performance) };
}

/**
 * Pie chart and list of how the portfolio's value is split between stocks.
 * Mirrors the dashboard's category breakdowns, but keeps tickers in capitals.
 *
 * @param {object} props
 * @param {Array<{ticker: string, value: number, share: number}>} props.allocation holdings, largest first
 * @param {number} props.excluded holdings left out because they couldn't be priced
 * @param {function(number): string} props.format formats an amount in the portfolio currency
 * @return {import("react").ReactElement} The allocation card.
 */
function AllocationCard({ allocation, excluded, format }) {
  const items = allocation.map((item, index) => ({
    ...item,
    key: item.ticker.toLowerCase(),
    color: palette[index % palette.length],
  }));
  const config = Object.fromEntries(items.map((item) => [item.key, { label: item.ticker, color: item.color }]));

  return (
    <Card className="glass bg-card/40">
      <CardHeader><CardTitle>Allocation by stock</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        {items.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {excluded > 0 ? "None of your holdings could be priced right now." : "No holdings yet."}
          </p>
        ) : (
          <div className="grid gap-6 md:grid-cols-[260px_1fr] md:items-center">
            <ChartContainer config={config} className="mx-auto h-[260px] w-[260px]">
              <PieChart>
                <ChartTooltip content={<ChartTooltipContent hideLabel formatter={(_value, _name, item) => (
                  <div className="flex justify-between gap-4">
                    <span>{item.payload.ticker}</span>
                    <span className="font-mono">{format(item.payload.value)}</span>
                  </div>
                )} />} />
                <Pie data={items} dataKey="share" nameKey="key" innerRadius="62%" outerRadius="92%"
                  paddingAngle={2} cornerRadius={4}>
                  {items.map((item) => <Cell key={item.key} fill={`var(--color-${item.key})`} />)}
                </Pie>
              </PieChart>
            </ChartContainer>
            <ul className="flex flex-col gap-4">
              {items.map((item) => (
                <li key={item.key} className="space-y-2">
                  <div className="flex flex-wrap justify-between gap-2 text-sm">
                    <span className="font-semibold">{item.ticker}</span>
                    <span>{format(item.value)} <span className="text-muted-foreground">({item.share.toFixed(1)}%)</span></span>
                  </div>
                  <div aria-hidden="true" className="h-1.5 overflow-hidden rounded-full bg-foreground/10">
                    <div className="h-full rounded-full" style={{ width: `${item.share}%`, backgroundColor: item.color }} />
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}
        {items.length > 0 && excluded > 0 && (
          <p className="text-sm text-muted-foreground">
            {excluded} holding{excluded === 1 ? " isn't" : "s aren't"} shown because no current price is available.
          </p>
        )}
      </CardContent>
    </Card>
  );
}

/**
 * Dashboard section summarising the user's portfolio: value, unrealised and realised profit,
 * and allocation by stock. Loads independently, so a slow or failing portfolio or market-data
 * service never delays the transaction cards. Values are in the portfolio's currency (USD) and
 * are never combined with transaction totals, which use the user's chosen display currency.
 *
 * Render with a key of the user's id so a different user starts from a fresh state.
 *
 * @return {import("react").ReactElement} The portfolio summary section.
 */
export default function PortfolioSummary() {
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    loadSummary(controller.signal).then(
      (loaded) => {
        if (!controller.signal.aborted) setResult({ attempt, ...loaded });
      },
      (error) => {
        if (!controller.signal.aborted) setResult({ attempt, status: "error", error: error.message });
      },
    );
    return () => controller.abort();
  }, [attempt]);

  // A result from an earlier attempt counts as still loading, so Retry shows progress.
  const current = result?.attempt === attempt ? result : null;
  const summary = current?.summary;
  const formatter = new Intl.NumberFormat("en-IE", {
    style: "currency", currency: summary?.currency || "USD", currencyDisplay: "code",
  });
  const format = (value) => formatter.format(value);

  return (
    <>
      <Card className="glass bg-card/40">
        <CardHeader><CardTitle>Portfolio summary</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          {!current && <p role="status">Loading portfolio…</p>}

          {current?.status === "error" && (
            <div role="alert" className="space-y-2">
              <p>{current.error}</p>
              <Button type="button" variant="outline" className="h-10"
                onClick={() => setAttempt((value) => value + 1)}>Retry</Button>
            </div>
          )}

          {current?.status === "empty" && (
            <p>
              You don't have a portfolio yet.{" "}
              <Link to="/portfolio" className="underline">Create one on the Portfolio page</Link>.
            </p>
          )}

          {summary && (
            <>
              <p className="text-sm text-muted-foreground">
                {current.portfolio.name} · valued in {summary.currency} at current market prices.{" "}
                <Link to="/portfolio" className="underline">View portfolio</Link>
              </p>
              {!summary.complete && (
                <p className="text-sm text-amber-400" role="status">
                  {summary.unavailableHoldings} holding{summary.unavailableHoldings === 1 ? "" : "s"} couldn't
                  be priced, so portfolio totals are unavailable.
                </p>
              )}
              {summary.stale && (
                <p className="text-sm text-amber-400">Some prices are cached because live market data is unavailable.</p>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {summary && (
        <>
          <div className="grid gap-4 md:grid-cols-3">
            <StatCard title="Portfolio Value" icon={Briefcase}
              available={summary.currentValue != null} value={summary.currentValue} formatter={format} />
            <StatCard title="Unrealised Gain / Loss" icon={TrendingUp}
              available={summary.unrealised != null} value={summary.unrealised} formatter={format}
              change={summary.unrealisedPercent ?? undefined} caption="vs cost of holdings" positiveIsGood />
            <StatCard title="Realised Profit" icon={PiggyBank}
              available={summary.realised != null} value={summary.realised} formatter={format} />
          </div>
          <AllocationCard allocation={summary.allocation} excluded={summary.excludedFromAllocation} format={format} />
        </>
      )}
    </>
  );
}
