import { Card, CardHeader, CardTitle, CardContent, CardAction } from "@/components/ui/card";
import StatCard from "@/components/StatCard";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, ResponsiveContainer } from "recharts";
import { Wallet, Receipt, TrendingUp, Landmark } from "lucide-react"
import { Pie, PieChart, Cell, Label } from "recharts";

const formatMoney = (amount) =>
    new Intl.NumberFormat("en-IE", { style: "currency", currency: "EUR" }).format(amount);

const mockDashboardData = {
  transactions: {
    available: true,
    totalIncome: 3200,
    totalIncomeChange: 8.2,
    totalIncomeCaption: "vs. last month",
    totalIncomePositive: true,

    totalExpenses: 1875,
    totalExpensesChange: 3.4,
    totalExpensesCaption: "vs. last month",
    totalExpensesPositive: false,

    cashBalance: 1325,
    cashBalanceChange: -1.9,
    cashBalanceCaption: "across 3 accounts",
    cashBalancePositive: true,

    expensesByCategory: [
        { key: "housing", label: "Housing", amount: 900 },
        { key: "groceries", label: "Groceries", amount: 350 },
        { key: "transport", label: "Transport", amount: 175 },
        { key: "entertainment", label: "Entertainment", amount: 225 },
        { key: "other", label: "Other", amount: 225 },
    ],
  },
  portfolio: {
    available: true,
    investmentValue: 4820,
    investmentValueChange: 5.7,
    investmentValueCaption: "portfolio, 30 days",
    investmentValuePositive: true,
  },
};

function Dashboard() {
    const { transactions, portfolio } = mockDashboardData;
    const combinedTotal = transactions.cashBalance + portfolio.investmentValue;

    const chartData = [
      { category: "Income", amount: transactions.totalIncome },
      { category: "Expenses", amount: transactions.totalExpenses },
    ];

    const chartConfig = {
      amount: {
        label: "Amount",
        color: "var(--chart-1)",
      },
    };

    const palette = [
      "var(--chart-1)",
      "var(--chart-2)",
      "var(--chart-3)",
      "var(--chart-4)",
      "var(--chart-5)",
    ];

    const categoryChartConfig = Object.fromEntries(
        transactions.expensesByCategory.map((item, index) => [
            item.key,
            { label: item.label, color: palette[index % palette.length] },
            ])
    );

    const totalSpent = transactions.expensesByCategory.reduce(
        (sum, item) => sum + item.amount,
        0
    );

    return (
        <div className="dark relative isolate min-h-svh overflow-hidden">
            <div
                aria-hidden="true"
                className="pointer-events-none fixed inset-0 -z-10 bg-[url('images/DashboardBG.png')] bg-cover bg-center"
            >
            <div className="absolute inset-0 bg-black/20" />
        </div>

        <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 md:px-8 md:py-12">
        <div className="p-6 space-y-6">
            <h1 className="text-3xl font-bold">Dashboard</h1>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    title="Total Income"
                    icon={Wallet}
                    available={transactions.available}
                    value={transactions.totalIncome}
                    formatter={formatMoney}
                    change={transactions.totalIncomeChange}
                    caption={transactions.totalIncomeCaption}
                    positiveIsGood={transactions.totalIncomePositive}
                />
                <StatCard
                    title="Total Expenses"
                    icon={Receipt}
                    available={transactions.available}
                    value={transactions.totalExpenses}
                    formatter={formatMoney}
                    change={transactions.totalExpensesChange}
                    caption={transactions.totalExpensesCaption}
                    positiveIsGood={transactions.totalExpensesPositive}
                />
                <StatCard
                    title="Cash Balance"
                    icon={Landmark}
                    available={transactions.available}
                    value={transactions.cashBalance}
                    formatter={formatMoney}
                    change={transactions.cashBalanceChange}
                    caption={transactions.cashBalanceCaption}
                    positiveIsGood={transactions.cashBalancePositive}
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
                      data={transactions.expensesByCategory}
                      dataKey="amount"
                      nameKey="key"
                      innerRadius="62%"
                      outerRadius="92%"
                      paddingAngle={2}
                      cornerRadius={4}
                    >
                      {transactions.expensesByCategory.map((item) => (
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
                    {transactions.expensesByCategory.map((item) => {
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
        </div>
      </div>
     </div>
    );
}

export default Dashboard