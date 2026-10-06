import { Card, CardHeader, CardTitle, CardContent, CardAction } from "@/components/ui/card";
import StatCard from "@/components/StatCard";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, ResponsiveContainer } from "recharts";
import { Wallet, Receipt, TrendingUp, Landmark } from "lucide-react"

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

    return (
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

            <Card>
                <CardHeader>
                    <CardTitle>Combined Total</CardTitle>
                </CardHeader>
                    <CardContent className="text-4xl font-bold">
                        {formatMoney(combinedTotal)}
                    </CardContent>
            </Card>

            {/* Sample Chart */}
            <Card>
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
            </Card>

        </div>
    );
}

export default Dashboard