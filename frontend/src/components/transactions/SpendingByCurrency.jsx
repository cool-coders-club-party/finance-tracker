import { formatEnumLabel } from '../../lib/transactionMetadata';
import { Pie, PieChart, Cell } from 'recharts';
import { Card, CardHeader, CardTitle, CardContent } from '../ui/card';
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../ui/chart';
import { formatMoney } from '../../lib/money';

const palette = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)'];

export default function SpendingByCurrency({ summary }) {
  const categories = summary.spendingByCategory.map(({ category, amount }) => ({
    key: category.toLowerCase(), label: formatEnumLabel(category), amount,
    share: summary.expenses ? amount / summary.expenses * 100 : 0,
  })).sort((a, b) => b.amount - a.amount);
  const config = Object.fromEntries(categories.map((item, index) => [
    item.key, { label: item.label, color: palette[index % palette.length] },
  ]));

  return (
    <Card className="glass bg-card/10">
      <CardHeader><CardTitle>Spending by Category</CardTitle></CardHeader>
      <CardContent>
        {categories.length === 0 ? <p className="text-sm text-muted-foreground">No expenses recorded yet.</p> : (
          <div className="space-y-5">
            <div className="grid gap-6 md:grid-cols-[260px_1fr] md:items-center">
              <ChartContainer config={config} className="mx-auto h-[260px] w-[260px]">
                <PieChart>
                  <ChartTooltip content={<ChartTooltipContent hideLabel formatter={(_value, _name, item) => (
                    <div className="flex justify-between gap-4">
                      <span>{item.payload.label}</span>
                      <span className="font-mono">{formatMoney(item.payload.amount, summary.currency)}</span>
                    </div>
                  )} />} />
                  <Pie data={categories} dataKey="share" nameKey="key" innerRadius="62%" outerRadius="92%" paddingAngle={2} cornerRadius={4}>
                    {categories.map((item) => <Cell key={item.key} fill={`var(--color-${item.key})`} />)}
                  </Pie>
                </PieChart>
              </ChartContainer>
              <ul className="flex flex-col gap-4">
                {categories.map((item) => (
                  <li key={item.key} className="space-y-2">
                    <div className="flex flex-wrap justify-between gap-2 text-sm">
                      <span>{item.label}</span>
                      <span>{formatMoney(item.amount, summary.currency)} <span className="text-muted-foreground">({item.share.toFixed(1)}%)</span></span>
                    </div>
                    <div aria-hidden="true" className="h-1.5 overflow-hidden rounded-full bg-foreground/10">
                      <div className="h-full rounded-full" style={{ width: `${item.share}%`, backgroundColor: config[item.key].color }} />
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
