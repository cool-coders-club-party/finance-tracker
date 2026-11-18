import { useState } from "react";

const tradeTime = new Intl.DateTimeFormat("en-IE", {
  dateStyle: "medium",
  timeStyle: "short",
});

const selectClass =
  "mt-1.5 h-10 w-full rounded-lg border border-white/15 bg-black/20 px-3 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#b8dc89]/70";

function formatNumber(value) {
  if (value == null || !Number.isFinite(Number(value))) return "—";
  return Number(value).toLocaleString("en-IE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

// Lists every buy and sell, newest first, with optional stock and type filters.
export default function TradeHistory({ trades, error }) {
  const [tickerFilter, setTickerFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");

  if (error) {
    return <p className="text-sm text-amber-400">Trade history unavailable: {error}</p>;
  }

  if (trades.length === 0) {
    return <p className="text-sm text-white/70">No trades yet. Your buys and sales will appear here.</p>;
  }

  // Offer every stock that has ever been traded, including fully sold ones.
  const tickers = [...new Set(trades.map((trade) => trade.ticker))].sort();
  // If the selected stock disappears from the data, fall back to showing everything.
  const activeTicker = tickers.includes(tickerFilter) ? tickerFilter : "ALL";

  const visible = trades
    .filter((trade) => activeTicker === "ALL" || trade.ticker === activeTicker)
    .filter((trade) => typeFilter === "ALL" || trade.type === typeFilter)
    .sort((a, b) => Date.parse(b.executedAt) - Date.parse(a.executedAt));

  const isFiltered = activeTicker !== "ALL" || typeFilter !== "ALL";

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <label className="text-sm text-white/85 sm:w-44">
          Stock
          <select
            value={activeTicker}
            onChange={(event) => setTickerFilter(event.target.value)}
            className={selectClass}
          >
            <option className="bg-neutral-900" value="ALL">All stocks</option>
            {tickers.map((ticker) => (
              <option className="bg-neutral-900" key={ticker} value={ticker}>{ticker}</option>
            ))}
          </select>
        </label>

        <label className="text-sm text-white/85 sm:w-44">
          Type
          <select
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
            className={selectClass}
          >
            <option className="bg-neutral-900" value="ALL">Buys and sales</option>
            <option className="bg-neutral-900" value="BUY">Buys only</option>
            <option className="bg-neutral-900" value="SELL">Sales only</option>
          </select>
        </label>

        {isFiltered && (
          <button
            type="button"
            onClick={() => {
              setTickerFilter("ALL");
              setTypeFilter("ALL");
            }}
            className="h-10 rounded-lg px-3 text-sm text-white/80 underline"
          >
            Clear filters
          </button>
        )}

        {/* Announce the result count when filters change. */}
        <p aria-live="polite" className="text-sm text-white/70 sm:ml-auto sm:pb-2">
          {isFiltered
            ? `Showing ${visible.length} of ${trades.length} trades`
            : `${trades.length} trade${trades.length === 1 ? "" : "s"}`}
        </p>
      </div>

      {visible.length === 0 ? (
        <p className="text-sm text-white/70">No trades match these filters.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm [&_td]:pr-4 [&_th]:pr-4">
            <thead className="border-b border-white/10 text-white/80">
              <tr>
                <th scope="col" className="pb-3 font-medium">Date</th>
                <th scope="col" className="pb-3 font-medium">Type</th>
                <th scope="col" className="pb-3 font-medium">Stock</th>
                <th scope="col" className="pb-3 font-medium">Shares</th>
                <th scope="col" className="pb-3 font-medium">Price</th>
                <th scope="col" className="pb-3 font-medium">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/10">
              {visible.map((trade) => (
                <tr key={trade.id}>
                  <td className="py-3 text-white/85">{tradeTime.format(new Date(trade.executedAt))}</td>
                  <td className={`py-3 font-semibold ${trade.type === "BUY" ? "text-green-300" : "text-red-300"}`}>
                    {trade.type === "BUY" ? "Buy" : "Sell"}
                  </td>
                  <th scope="row" className="py-3 text-[#b8dc89]">{trade.ticker}</th>
                  <td className="py-3 tabular-nums">{trade.quantity}</td>
                  <td className="py-3 tabular-nums">{formatNumber(trade.price)}</td>
                  <td className="py-3 tabular-nums">{formatNumber(Number(trade.quantity) * Number(trade.price))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
