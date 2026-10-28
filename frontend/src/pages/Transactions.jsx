import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getTransaction } from '../api/transactions';
import { ArrowLeft, CheckCircle2, Loader2, Plus, Receipt, RefreshCw } from 'lucide-react';
import DashboardShell from '../components/DashboardShell';
import TransactionForm from '../components/transactions/TransactionForm';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import useTransactionMetadata from '../hooks/useTransactionMetadata';

export default function Transactions() {
  const { transactionId } = useParams();
  return <TransactionEditor key={transactionId ?? 'new'} transactionId={transactionId} />;
}

function TransactionEditor({ transactionId }) {
  const navigate = useNavigate();
  const metadata = useTransactionMetadata();
  const [attempt, setAttempt] = useState(0);
  const [record, setRecord] = useState({ status: transactionId ? 'loading' : 'ready', data: null, error: '' });
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!transactionId) return;
    const controller = new AbortController();
    getTransaction(transactionId, controller.signal).then(
      (data) => {
        if (!controller.signal.aborted) setRecord({ status: 'ready', data, error: '' });
      },
      (error) => {
        if (!controller.signal.aborted) setRecord({ status: 'error', data: null, error: error.message });
      },
    );
    return () => controller.abort();
  }, [transactionId, attempt]);

  function retryRecord() {
    setRecord({ status: 'loading', data: null, error: '' });
    setAttempt((value) => value + 1);
  }

  function handleSaved(transaction) {
    if (!transactionId) {
      navigate(`/transactions/${transaction.id}/edit`, { replace: true });
    } else {
      setRecord({ status: 'ready', data: transaction, error: '' });
      setNotice('Transaction saved.');
    }
  }

  const isLoading = metadata.status === 'loading' || record.status === 'loading';
  const isReady = metadata.status === 'ready' && record.status === 'ready';

  return (
    <DashboardShell>
      <main className="space-y-6 text-left">
        <nav aria-label="Transactions navigation">
          <Link to="/" className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-border/60 bg-background/35 px-3 text-sm text-foreground/90 backdrop-blur-sm transition-colors hover:bg-background/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <ArrowLeft aria-hidden="true" className="size-4" />
            Back to dashboard
          </Link>
        </nav>
        <header className="flex flex-wrap items-center justify-between gap-4">
          <div className="space-y-2">
            <h1 className="!my-0 text-3xl font-bold !text-foreground">{transactionId ? `Edit transaction #${transactionId}` : 'New transaction'}</h1>
            <p className="text-sm text-foreground/90">Record your income and expenses with the right category.</p>
          </div>
          {transactionId && (
            <Button asChild variant="outline" className="h-10 border-border/70 bg-background/35">
              <Link to="/transactions"><Plus aria-hidden="true" />New transaction</Link>
            </Button>
          )}
        </header>
        <Card className="glass border border-border/60 bg-card/10">
          <CardHeader className="gap-2 border-b border-border/60 pb-5">
            <CardTitle className="flex items-center gap-2"><Receipt aria-hidden="true" className="size-4 text-muted-foreground" />Transaction details</CardTitle>
            <CardDescription>Choose a type to see its available categories.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5 pt-1" aria-busy={isLoading}>
            {metadata.status === 'error' && (
              <div role="alert" className="space-y-3 rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm">
                <p>{metadata.error}</p>
                <Button type="button" variant="outline" onClick={metadata.retry}><RefreshCw aria-hidden="true" />Retry loading options</Button>
              </div>
            )}
            {record.status === 'error' && (
              <div role="alert" className="space-y-3 rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm">
                <p>{record.error}</p>
                <Button type="button" variant="outline" onClick={retryRecord}><RefreshCw aria-hidden="true" />Retry loading transaction</Button>
              </div>
            )}
            {isLoading && (
              <p role="status" className="flex min-h-48 items-center justify-center gap-2 text-sm text-muted-foreground">
                <Loader2 aria-hidden="true" className="size-4 animate-spin motion-reduce:animate-none" />Loading transaction details…
              </p>
            )}
            {notice && <p role="status" className="flex items-center gap-2 rounded-lg border border-border/60 bg-background/35 p-3 text-sm"><CheckCircle2 aria-hidden="true" className="size-4 text-green-400" />{notice}</p>}
            {isReady && <TransactionForm key={record.data ? JSON.stringify(record.data) : 'new'}
              metadata={metadata.data} transaction={record.data} onSaved={handleSaved} />}
          </CardContent>
        </Card>
      </main>
    </DashboardShell>
  );
}
