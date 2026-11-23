import { authenticatedFetch } from '../lib/authenticatedFetch.js';

export async function downloadFinancialReport({ from, to, signal }) {
  const params = new URLSearchParams({ from, to }); // Builds query string and encodes dates.
// Send request through Vite Proxy, expecting csv back.
  const response = await authenticatedFetch(
    `/api/reports/export?${params.toString()}`,
    {
      headers: { Accept: 'text/csv' },
      signal,
    },
  );
// Handling bad/invalid responses (invalid date range etc.).
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(
      body?.detail
        || body?.message
        || `Report request failed (${response.status}). Please try again.`,
    );
  }
// Find and extract filename, or use fallback filename value.
  const disposition = response.headers.get('Content-Disposition');
  const filename = disposition?.match(/filename="?([^";]+)"?/i)?.[1]
    || `financial-summary-${from}-to-${to}.csv`;
// Return CSV as a browser object.
  return {
    blob: await response.blob(),
    filename,
  };
}