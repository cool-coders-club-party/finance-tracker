import { useEffect, useMemo, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import StatCard from "@/components/StatCard";
import DashboardShell from "@/components/DashboardShell";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Wallet, Receipt, TrendingUp, Landmark } from "lucide-react";
import { Pie, PieChart, Cell, Label } from "recharts";
import { getTransactionHistory } from "../api/transactions";

const formatMoney = (amount) =>
  new Intl.NumberFormat("en-IE", { style: "currency", currency: "EUR" }).format(amount);

const DASHBOARD_USER_ID = 5;

const mockDashboardData = {
//   transactions: {
//     available: false,
//     totalIncome: 3200,
//     totalIncomeChange: 8.2,
//     totalIncomeCaption: "vs. last month",
//     totalIncomePositive: true,
//
//     totalExpenses: 1875,
//     totalExpensesChange: 3.4,
//     totalExpensesCaption: "vs. last month",
//     totalExpensesPositive: false,
//
//     cashBalance: 1325,
//     cashBalanceChange: -1.9,
//     cashBalanceCaption: "across 3 accounts",
//     cashBalancePositive: true,
//
//     expensesByCategory: [
//       { key: "housing", label: "Housing", amount: 900 },
//       { key: "groceries", label: "Groceries", amount: 350 },
//       { key: "transport", label: "Transport", amount: 175 },
//       { key: "entertainment", label: "Entertainment", amount: 225 },
//       { key: "other", label: "Other", amount: 225 },
//     ],
//   },
  portfolio: {
    available: true,
    investmentValue: 4820,
    investmentValueChange: 5.7,
    investmentValueCaption: "portfolio, 30 days",
    investmentValuePositive: true,
  },
};

 
function Dashboard() {
    const { portfolio } = mockDashboardData;

    // Get transaction expense history.
    const [transactionHistory, setTransactionHistory] = useState({
      status: "loading",
      records: [],
      error: "",
    });

    // New abort controller, if we leave dashboard early they don't load.
    useEffect(() => {
      const controller = new AbortController();

      async function loadExpenses() {
        try { // Load first 100 pages
          const firstPage = await getTransactionHistory(DASHBOARD_USER_ID, {
            page: 0,
            size: 100,
            type: null,
            signal: controller.signal,
          });

          // Load the all other pages
          const remainingPages = await Promise.all(
            Array.from(
              { length: Math.max(0, firstPage.totalPages - 1) },
              (_, index) =>
                getTransactionHistory(DASHBOARD_USER_ID, {
                  page: index + 1,
                  size: 100,
                  type: null,
                  signal: controller.signal,
                }),
            ),
          );

          if (controller.signal.aborted) return;

          // Put content into records array, use flatmap to flatten nested structure
          const records = [
            ...(firstPage.content ?? []),
            ...remainingPages.flatMap((page) => page.content ?? []),
          ];

          // If error return expense history with an error.
          setTransactionHistory({ status: "ready", records, error: "" });
        } catch (error) {
          if (controller.signal.aborted) return;
          setTransactionHistory({
            status: "error",
            records: [],
            error: error.message,
          });
        }
      }
      // loadExpenses and end useEffect call.
      loadExpenses();
      return () => controller.abort();
    }, []);

    const transactionTotals = useMemo(() => {
      let totalIncome = 0;
      let totalExpenses = 0;

      for (const record of transactionHistory.records) {
        const amount = Number(record.amount);

        if (!Number.isFinite(amount) || amount <= 0) continue;

        if (record.transactionType === "INCOME") {
          totalIncome += amount;
        } else if (record.transactionType === "EXPENSE") {
          totalExpenses += amount;
        }
      }

      return {
        totalIncome,
        totalExpenses,
        cashBalance: totalIncome - totalExpenses,
      };
    }, [transactionHistory.records]);

    // New empty map, find every transaction, skip if not expense.
    const expensesByCategory = useMemo(() => {
      const totalsByCategory = new Map();

      for (const record of transactionHistory.records) {
        if (record.transactionType !== "EXPENSE") continue;
        // Convert to number.
        const category = record.transactionCategory;
        const amount = Number(record.amount);
        // Skip invalid records
        if (!category || !Number.isFinite(amount) || amount <= 0) continue;
        // Make a key and add it to categories total, so we don't have e.g. multiple housing categories
        // Multiple housing transactions get added together under one housing category, etc.
        const key = category.toLowerCase();
        totalsByCategory.set(key, (totalsByCategory.get(key) ?? 0) + amount);
      }
      // Turn map int chart data objects we can use. Make a readable label.
      return Array.from(totalsByCategory, ([key, amount]) => ({
        key,
        label: key
          .replaceAll("_", " ")
          .replace(/\b\w/g, (letter) => letter.toUpperCase()),
        amount,
      })).sort((a, b) => b.amount - a.amount); // Sort from most spending to lowest.
    }, [transactionHistory.records]); // Tells react when to recalculate.

    const combinedTotal = transactionTotals.cashBalance + portfolio.investmentValue;

    const palette = [
      "var(--chart-1)",
      "var(--chart-2)",
      "var(--chart-3)",
      "var(--chart-4)",
      "var(--chart-5)",
    ];

    const categoryChartConfig = Object.fromEntries(
        expensesByCategory.map((item, index) => [
            item.key,
            { label: item.label, color: palette[index % palette.length] },
            ])
    );

    const totalSpent = expensesByCategory.reduce(
        (sum, item) => sum + item.amount,
        0
    );

  return (
      <DashboardShell>
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h1 className="text-3xl font-bold">Dashboard</h1>
        </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Income"
          icon={Wallet}
          available={transactionHistory.status === "ready"}
          value={transactionTotals.totalIncome}
          formatter={formatMoney}
        />
        <StatCard
          title="Total Expenses"
          icon={Receipt}
          available={transactionHistory.status === "ready"}
          value={transactionTotals.totalExpenses}
          formatter={formatMoney}
        />
        <StatCard
          title="Cash Balance"
          icon={Landmark}
          available={transactionHistory.status === "ready"}
          value={transactionTotals.cashBalance}
          formatter={formatMoney}
        />
        <StatCard
          title="Investment Value"
          icon={TrendingUp}
          available={portfolio.available}
          value={portfolio.investmentValue}
          formatter={formatMoney}
          change={portfolio.investmentValueChange}
          caption={portfolio.investmentValueCaption}
          positiveIsGood={portfolio.investmentValuePositive}
        />
      </div>

      <Card className="glass bg-card/10">
        <CardHeader>
          <CardTitle>Combined Total</CardTitle>
        </CardHeader>
        <CardContent className="text-4xl font-bold">
          {formatMoney(combinedTotal)}
        </CardContent>
      </Card>

      {/* Sample Chart */}
      {/*            <Card>
              <CardHeader>
                <CardTitle>Income vs Expenses</CardTitle>
              </CardHeader>
              <CardContent>
                <ChartContainer config={chartConfig}>
                  <BarChart data={chartData}>
                    <CartesianGrid vertical={false} />
                    <XAxis dataKey="category" />
                    <YAxis />
                    <ChartTooltip content={<ChartTooltipContent />} />
                    <Bar dataKey="amount" fill="var(--color-amount)" radius={4} />
                  </BarChart>
                </ChartContainer>
              </CardContent>
            </Card>*/}

            <Card className="glass bg-card/10">
              <CardHeader>
                <CardTitle>Spending by Category</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-6 md:grid-cols-[260px_1fr] md:items-center">
                <ChartContainer config={categoryChartConfig} className="mx-auto h-[260px] w-[260px]">
                  <PieChart>
                    <ChartTooltip content={<ChartTooltipContent hideLabel />} />
                    <Pie
                      data={expensesByCategory}
                      dataKey="amount"
                      nameKey="key"
                      innerRadius="62%"
                      outerRadius="92%"
                      paddingAngle={2}
                      cornerRadius={4}
                    >
                      {expensesByCategory.map((item) => (
                        <Cell key={item.key} fill={`var(--color-${item.key})`} />
                      ))}
                      <Label
                        content={({ viewBox }) => {
                          if (!viewBox || !("cx" in viewBox)) return null;
                          const { cx, cy } = viewBox;
                          return (
                            <text x={cx} y={cy} textAnchor="middle" dominantBaseline="middle">
                              <tspan x={cx} y={cy - 10} className="fill-foreground text-2xl font-semibold">
                                {formatMoney(totalSpent)}
                              </tspan>
                              <tspan x={cx} y={cy + 14} className="fill-muted-foreground text-xs">
                                Total spent
                              </tspan>
                            </text>
                          );
                        }}
                      />
                    </Pie>
                  </PieChart>
                </ChartContainer>

                <ul className="flex flex-col gap-3">
                    {expensesByCategory.map((item) => {
                        const share = (item.amount / totalSpent) * 100;
                        return (
                            <li key={item.key} className="flex flex-col gap-2">
                                <div className="flex items-center justify-between gap-3 text-sm">
                                    <span className="flex items-center gap-2">
                                        <span
                                            className="inline-block size-2.5 rounded-full"
                                            style={{ backgroundColor: categoryChartConfig[item.key].color }}
                                        />
                                        {item.label}
                                    </span>
                                    <span className="flex items-baseline gap-2">
                                        <span className="font-medium">{formatMoney(item.amount)}</span>
                                        <span className="text-xs text-muted-foreground">{share.toFixed(1)}%</span>
                                    </span>
                                </div>
                                <div className="h-1.5 w-full overflow-hidden rounded-full bg-foreground/10">
                                    <div
                                        className="h-full rounded-full"
                                        style={{
                                            width: `${share}%`,
                                            backgroundColor: categoryChartConfig[item.key].color,
                                        }}
                                    />
                                </div>
                            </li>
                            );
                        })}
                </ul>
              </CardContent>
            </Card>
            
      </DashboardShell>
    );
}

export default Dashboard
