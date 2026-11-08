import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Dialog } from 'radix-ui';
import { ArchiveRestore, ChevronLeft, ChevronRight, Pencil, Plus, Receipt, Trash2 } from 'lucide-react';
import DashboardShell from '../components/DashboardShell';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import useTransactionMetadata from '../hooks/useTransactionMetadata';
import { deleteTransaction, getTransactionHistory, restoreTransaction } from '../api/transactions';
import { formatMoney } from '../lib/money';
import { formatEnumLabel } from '../lib/transactionMetadata';

const fieldClass = 'min-h-11 w-full rounded-lg border border-input bg-background/70 px-3 text-sm text-foreground';
const emptyFilters = { from: '', to: '', type: '', category: '' };
const dateFormatter = new Intl.DateTimeFormat('en-IE', { day: 'numeric', month: 'short', year: 'numeric' });

function Filters({ filters, metadata, onApply }) {
  const [draft, setDraft] = useState(filters);
  const [error, setError] = useState('');
  const categories = (metadata.data?.categories ?? []).filter((category) =>
    !draft.type || category.allowedTypes.includes(draft.type));
  function changeType(type) {
    setDraft((previous) => ({ ...previous, type, category: !type || metadata.data.categories.some(
      (category) => category.name === previous.category && category.allowedTypes.includes(type),
    ) ? previous.category : '' }));
  }
  function submit(event) {
    event.preventDefault();
    if (draft.from && draft.to && draft.from > draft.to) {
      setError('The start date must be on or before the end date.');
      return;
    }
    setError('');
    onApply(draft);
  }
  return <form onSubmit={submit} className="space-y-4">
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <label className="grid gap-2 text-sm">From
        <input className={fieldClass} type="date" value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} />
      </label>
      <label className="grid gap-2 text-sm">To
        <input className={fieldClass} type="date" value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} />
      </label>
      <label className="grid gap-2 text-sm">Type
        <select className={fieldClass} value={draft.type} disabled={metadata.status !== 'ready'} onChange={(e) => changeType(e.target.value)}>
          <option value="">All types</option>
          {(metadata.data?.types ?? []).map((type) => <option key={type} value={type}>{formatEnumLabel(type)}</option>)}
        </select>
      </label>
      <label className="grid gap-2 text-sm">Category
        <select className={fieldClass} value={draft.category} disabled={metadata.status !== 'ready'} onChange={(e) => setDraft({ ...draft, category: e.target.value })}>
          <option value="">All categories</option>
          {categories.map(({ name }) => <option key={name} value={name}>{formatEnumLabel(name)}</option>)}
        </select>
      </label>
    </div>
    {error && <p role="alert" className="text-sm text-red-300">{error}</p>}
    {metadata.status === 'loading' && <p role="status" className="text-sm text-muted-foreground">Loading filter options…</p>}
    {metadata.error && <div role="alert" className="text-sm">{metadata.error} <Button type="button" variant="outline" onClick={metadata.retry}>Retry options</Button></div>}
    <div className="flex flex-wrap gap-2">
      <Button type="submit" className="h-10">Apply filters</Button>
      <Button type="button" variant="outline" className="h-10" onClick={() => { setDraft(emptyFilters); setError(''); onApply(emptyFilters); }}>Clear filters</Button>
    </div>
  </form>;
}

export default function TransactionHistory() {
  const metadata = useTransactionMetadata();
  const [params, setParams] = useSearchParams();
  const deleted = params.get('view') === 'deleted';
  const rawPage = Number(params.get('page') ?? 0);
  const page = Number.isSafeInteger(rawPage) && rawPage >= 0 && rawPage <= 2147483647 ? rawPage : 0;
  const size = [10, 20, 50].includes(Number(params.get('size'))) ? Number(params.get('size')) : 20;
  const filters = Object.fromEntries(Object.keys(emptyFilters).map((key) => [key, params.get(key) || '']));
  const { from, to, type, category } = filters;
  const [revision, setRevision] = useState(0);
  const queryKey = JSON.stringify({ deleted, page, size, from, to, type, category, revision });
  const [result, setResult] = useState(null);
  const [selection, setSelection] = useState(null);
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const mutation = useRef(null);
  const actionTrigger = useRef(null);
  const current = result?.key === queryKey ? result : null;
  const data = current?.data;
  const returnTo = `/transactions${params.size ? `?${params}` : ''}`;

  useEffect(() => {
    const controller = new AbortController();
    getTransactionHistory({ page, size, type, category, from, to, deleted, signal: controller.signal }).then(
      (response) => {
        if (controller.signal.aborted) return;
        if (!Array.isArray(response.content) || !Number.isInteger(response.totalPages) || !Number.isInteger(response.totalElements)) {
          setResult({ key: queryKey, error: 'Transaction history could not be read. Please try again.' });
          return;
        }
        try {
          const content = response.content.map((record) => ({
            ...record,
            dateLabel: dateFormatter.format(new Date(record.occurredAt)),
            amountLabel: formatMoney(record.amount, record.currency),
            typeLabel: formatEnumLabel(record.transactionType),
            categoryLabel: formatEnumLabel(record.transactionCategory),
          }));
          setResult({ key: queryKey, data: { ...response, content } });
        } catch {
          setResult({ key: queryKey, error: 'Some transaction details could not be displayed accurately. Please try again.' });
        }
      },
      (error) => { if (!controller.signal.aborted) setResult({ key: queryKey, error: error.message }); },
    );
    return () => controller.abort();
  }, [queryKey, page, size, type, category, from, to, deleted]);

  useEffect(() => () => mutation.current?.abort(), []);

  // A deletion or a change in another tab can remove the last page.
  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) {
      setParams((previous) => {
        const next = new URLSearchParams(previous);
        next.set('page', String(Math.max(0, data.totalPages - 1)));
        return next;
      }, { replace: true });
    }
  }, [data, page, setParams]);

  function updateQuery(values) {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      Object.entries(values).forEach(([key, value]) => value === '' ? next.delete(key) : next.set(key, String(value)));
      return next;
    });
    setNotice('');
  }

  async function confirmAction() {
    if (!selection || mutation.current) return;
    const controller = new AbortController();
    mutation.current = controller;
    setPending(true);
    setActionError('');
    try {
      await (selection.deleted ? restoreTransaction : deleteTransaction)(selection.record.id, controller.signal);
      if (controller.signal.aborted) return;
      setNotice(selection.deleted ? 'Transaction restored. It is available in Active transactions.' : 'Transaction deleted. You can restore it from Deleted transactions.');
      setSelection(null);
      setRevision((value) => value + 1);
    } catch (error) {
      if (!controller.signal.aborted) setActionError(error.message);
    } finally {
      if (!controller.signal.aborted) setPending(false);
      mutation.current = null;
    }
  }

  return <DashboardShell>
    <main className="space-y-6 text-left">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div className="space-y-2">
          <h1 className="!my-0 text-3xl font-bold !text-foreground">Transactions</h1>
          <p className="text-sm text-foreground/90">Your income and expenses, in their original currencies.</p>
        </div>
        <Button asChild className="h-11"><Link to="/transactions/new" state={{ returnTo }}><Plus aria-hidden="true" />New transaction</Link></Button>
      </header>
      <Card className="glass border border-border/60 bg-card/10">
        <CardContent className="space-y-5">
          <div className="flex flex-wrap gap-2" role="group" aria-label="Transaction status">
            <Button variant={!deleted ? 'default' : 'outline'} className="h-10" aria-pressed={!deleted} onClick={() => updateQuery({ view: '', page: 0 })}>Active</Button>
            <Button variant={deleted ? 'default' : 'outline'} className="h-10" aria-pressed={deleted} onClick={() => updateQuery({ view: 'deleted', page: 0 })}><Trash2 aria-hidden="true" />Deleted</Button>
          </div>
          {deleted && <p className="text-sm text-muted-foreground">Deleted transactions are excluded from your totals. Restore one to include it again.</p>}
          <Filters key={JSON.stringify(filters)} filters={filters} metadata={metadata} onApply={(values) => updateQuery({ ...values, page: 0 })} />
        </CardContent>
      </Card>
      {notice && <p role="status" className="rounded-lg border border-border/60 bg-background/35 p-4 text-sm">{notice}</p>}
      <Card className="glass overflow-hidden border border-border/60 bg-card/10">
        <CardContent className="space-y-5" aria-busy={!current}>
          {!current ? <p role="status" className="py-12 text-center text-sm text-muted-foreground">Loading transactions…</p>
            : current.error ? <div role="alert" className="space-y-3 py-6">
              <p>{current.error}</p>
              <Button variant="outline" onClick={() => setRevision((value) => value + 1)}>Retry history</Button>
            </div> : data.content.length === 0 ? <div className="space-y-2 py-12 text-center">
              <Receipt aria-hidden="true" className="mx-auto size-8 text-muted-foreground" />
              <h2 className="text-lg font-semibold">No {deleted ? 'deleted ' : ''}transactions found</h2>
              <p className="text-sm text-muted-foreground">{Object.values(filters).some(Boolean) ? 'Try clearing or changing your filters.' : deleted ? 'Transactions you delete will appear here.' : 'Add your first income or expense to get started.'}</p>
            </div> : <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <caption className="sr-only">{deleted ? 'Deleted' : 'Active'} transaction history</caption>
                <thead><tr className="border-b border-border/60 text-left text-muted-foreground">
                  {['Date', 'Description', 'Type / category', 'Amount', 'Actions'].map((label) => <th key={label} scope="col" className={`px-3 py-3 font-medium ${label === 'Amount' ? 'text-right' : ''}`}>{label}</th>)}
                </tr></thead>
                <tbody>{data.content.map((record) => <tr key={record.id} className="border-b border-border/40 last:border-0">
                  <td className="whitespace-nowrap px-3 py-4"><time dateTime={record.occurredAt}>{record.dateLabel}</time></td>
                  <td className="min-w-40 max-w-xs break-words px-3 py-4">{record.description || <span className="text-muted-foreground">No description</span>}</td>
                  <td className="px-3 py-4"><span className={`block text-xs ${record.transactionType === 'INCOME' ? 'text-emerald-300' : 'text-muted-foreground'}`}>{formatEnumLabel(record.transactionType)}</span>{formatEnumLabel(record.transactionCategory)}</td>
                  <td className="whitespace-nowrap px-3 py-4 text-right font-medium tabular-nums">{record.amountLabel}</td>
                  <td className="px-3 py-4"><div className="flex gap-2">
                    {!deleted && <Button asChild variant="outline" className="h-10"><Link to={`/transactions/${record.id}/edit`} state={{ returnTo }} aria-label={`Edit ${record.description || formatEnumLabel(record.transactionCategory)}`}><Pencil aria-hidden="true" />Edit</Link></Button>}
                    <Button variant="outline" className="h-10" onClick={(event) => { actionTrigger.current = event.currentTarget; setActionError(''); setSelection({ record, deleted }); }} aria-label={`${deleted ? 'Restore' : 'Delete'} ${record.description || formatEnumLabel(record.transactionCategory)}`}>
                      {deleted ? <ArchiveRestore aria-hidden="true" /> : <Trash2 aria-hidden="true" />}{deleted ? 'Restore' : 'Delete'}
                    </Button>
                  </div></td>
                </tr>)}</tbody>
              </table>
            </div>}
          <div className="flex flex-wrap items-center justify-between gap-4 border-t border-border/60 pt-4">
            <label className="flex items-center gap-2 text-sm">Rows per page
              <select className="h-10 rounded-lg border border-input bg-background/70 px-2" value={size} onChange={(event) => updateQuery({ size: event.target.value, page: 0 })}>
                {[10, 20, 50].map((value) => <option key={value}>{value}</option>)}
              </select>
            </label>
            <nav aria-label="History pages" className="flex items-center gap-3 text-sm">
              {data && <span>{data.totalElements} transactions · Page {data.totalPages ? page + 1 : 0} of {data.totalPages}</span>}
              <Button variant="outline" className="h-10" disabled={!data || page === 0} onClick={() => updateQuery({ page: page - 1 })} aria-label="Previous page"><ChevronLeft aria-hidden="true" /></Button>
              <Button variant="outline" className="h-10" disabled={!data || page + 1 >= data.totalPages} onClick={() => updateQuery({ page: page + 1 })} aria-label="Next page"><ChevronRight aria-hidden="true" /></Button>
            </nav>
          </div>
        </CardContent>
      </Card>
    </main>
    <Dialog.Root open={!!selection} onOpenChange={(open) => { if (!open && !pending) setSelection(null); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/65 backdrop-blur-sm" />
        <Dialog.Content onCloseAutoFocus={(event) => { event.preventDefault(); actionTrigger.current?.focus(); }} className="dark fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 space-y-5 rounded-xl border border-border bg-background p-6 text-foreground shadow-xl" onEscapeKeyDown={(event) => { if (pending) event.preventDefault(); }} onInteractOutside={(event) => event.preventDefault()}>
          <Dialog.Title className="text-xl font-semibold">{selection?.deleted ? 'Restore transaction?' : 'Delete transaction?'}</Dialog.Title>
          <Dialog.Description className="text-sm text-muted-foreground">{selection?.deleted ? 'This transaction will return to your history and be included in your totals.' : 'This transaction will be removed from your totals. You can restore it later from Deleted transactions.'}</Dialog.Description>
          {selection && <p className="break-words text-sm">{selection.record.description || formatEnumLabel(selection.record.transactionCategory)} · {formatMoney(selection.record.amount, selection.record.currency)}</p>}
          {actionError && <p role="alert" className="text-sm text-red-300">{actionError}</p>}
          <div className="flex justify-end gap-3">
            <Dialog.Close asChild><Button variant="outline" className="h-11" disabled={pending}>Cancel</Button></Dialog.Close>
            <Button className="h-11" disabled={pending} onClick={confirmAction}>{pending ? 'Saving…' : selection?.deleted ? 'Restore transaction' : 'Delete transaction'}</Button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  </DashboardShell>;
}
