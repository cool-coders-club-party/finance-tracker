import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import StatCard from "@/components/StatCard";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, ResponsiveContainer } from "recharts";

const formatMoney = (amount) =>
    new Intl.NumberFormat("en-IE", { style: "currency", currency: "EUR" }).format(amount);

const mockDashboardData = {
  transactions: {
    available: true,
    totalIncome: 3200,
    totalExpenses: 1875,
    cashBalance: 1325,
  },
  portfolio: {
    available: true,
    investmentValue: 4820,
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
                    available={transactions.available}
                    value={transactions.totalIncome}
                    formatter={formatMoney}
                />
                <StatCard
                    title="Total Expenses"
                    available={transactions.available}
                    value={transactions.totalExpenses}
                    formatter={formatMoney}
                />
                <StatCard
                    title="Cash Balance"
                    available={transactions.available}
                    value={transactions.cashBalance}
                    formatter={formatMoney}
                />
                <StatCard
                    title="Investment Value"
                    available={portfolio.available}
                    value={portfolio.investmentValue}
                    formatter={formatMoney}
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