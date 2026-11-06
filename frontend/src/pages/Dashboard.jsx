import { useEffect, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import StatCard from "@/components/StatCard";
import DashboardShell from "@/components/DashboardShell";
import { Wallet, Receipt, TrendingUp, Landmark } from "lucide-react"
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
      </DashboardShell>
    );
}

export default Dashboard
