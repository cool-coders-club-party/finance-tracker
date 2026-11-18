import { useState } from 'react';
import { downloadFinancialReport } from '../api/reports';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';

const fieldClass =
  'min-h-11 w-full rounded-lg border border-input bg-background/70 px-3 text-sm text-foreground';

export default function FinancialReportExport() {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const invalidRange = from && to && from > to;

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setNotice('');

    if (!from || !to || invalidRange) {
      setError('Choose a valid date range.');
      return;
    }

    setPending(true);

    try {
      const { blob, filename } = await downloadFinancialReport({ from, to });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');

      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();

      window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
      setNotice('Your CSV report has downloaded.');
    } catch (downloadError) {
      setError(downloadError.message || 'The report could not be downloaded.');
    } finally {
      setPending(false);
    }
  }

  return (
    <Card className="glass bg-card/40">
      <CardHeader>
        <CardTitle>Export financial report</CardTitle>
      </CardHeader>

      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Choose the transaction date range for your CSV report.
          </p>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="grid gap-2 text-sm">
              From
              <input
                className={fieldClass}
                type="date"
                required
                value={from}
                onChange={(event) => setFrom(event.target.value)}
              />
            </label>

            <label className="grid gap-2 text-sm">
              To
              <input
                className={fieldClass}
                type="date"
                required
                value={to}
                onChange={(event) => setTo(event.target.value)}
              />
            </label>
          </div>

          {invalidRange && (
            <p role="alert" className="text-sm text-red-300">
              The start date must be on or before the end date.
            </p>
          )}
          {error && <p role="alert" className="text-sm text-red-300">{error}</p>}
          {notice && <p role="status" className="text-sm">{notice}</p>}

          <Button type="submit" disabled={pending || !from || !to || invalidRange}>
            {pending ? 'Preparing report…' : 'Download CSV'}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}