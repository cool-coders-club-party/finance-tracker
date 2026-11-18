const tradeTime = new Intl.DateTimeFormat("en-IE", {
  dateStyle: "medium",
  timeStyle: "short",
});

function formatNumber(value) {
  if (value == null || !Number.isFinite(Number(value))) return "—";
  return Number(value).toLocaleString("en-IE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

// Lists every buy and sell, newest first.
export default function TradeHistory({ trades, error }) {
  if (error) {
    return <p className="text-sm text-amber-400">Trade history unavailable: {error}</p>;
  }

  if (trades.length === 0) {
    return <p className="text-sm text-white/70">No trades yet. Your buys and sales will appear here.</p>;
  }

  const sorted = [...trades].sort(
    (a, b) => Date.parse(b.executedAt) - Date.parse(a.executedAt)
  );

  return (
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
          {sorted.map((trade) => (
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
  );
}
