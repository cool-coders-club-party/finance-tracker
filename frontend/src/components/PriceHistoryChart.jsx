import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export default function PriceHistoryChart({ points, currency }) {
  // Recharts needs numbers; the API supplies dates and precise prices.
  const data = points.map((point) => ({
    date: point.date,
    close: Number(point.close),
  }));

  const prices = data.map((point) => point.close);
  const low = Math.min(...prices);
  const high = Math.max(...prices);
  const padding = Math.max((high - low) * 0.15, 1);

  return (
    <div
      className="h-52 w-full"
      role="img"
      aria-label="Daily closing prices for the past month"
    >
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
          <defs>
            <linearGradient id="price-history-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#60a5fa" stopOpacity={0.3} />
              <stop offset="100%" stopColor="#60a5fa" stopOpacity={0} />
            </linearGradient>
          </defs>

          <XAxis
            dataKey="date"
            tickFormatter={(date) => date.slice(5)}
            tick={{ fill: "#a3a3a3", fontSize: 12 }}
            tickLine={false}
            axisLine={false}
            minTickGap={35}
          />
          <YAxis hide domain={[low - padding, high + padding]} />

          <Tooltip
            labelFormatter={(date) =>
              new Date(`${date}T00:00:00`).toLocaleDateString("en-IE", {
                day: "numeric",
                month: "short",
                year: "numeric",
              })
            }
            formatter={(value) => [
              `${Number(value).toFixed(2)} ${currency || ""}`.trim(),
              "Close",
            ]}
            contentStyle={{
              backgroundColor: "#171717",
              border: "1px solid #404040",
              borderRadius: "12px",
              color: "#fff",
            }}
          />

          <Area
            type="monotone"
            dataKey="close"
            stroke="#60a5fa"
            strokeWidth={3}
            fill="url(#price-history-fill)"
            dot={false}
            activeDot={{ r: 5 }}
            animationDuration={700}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}