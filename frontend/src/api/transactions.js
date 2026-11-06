import { validateMetadata } from '../lib/transactionMetadata.js';

const BASE_URL = '/api/v1/transactions';

export class TransactionApiError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'TransactionApiError';
    this.details = details;
  }
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers: { Accept: 'application/json', ...options.headers },
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new TransactionApiError('Cannot reach Transactions. Check your connection and try again.');
  }
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new TransactionApiError(
      body?.message || `Transactions request failed (${response.status}). Please try again.`,
      body?.details && typeof body.details === 'object' ? body.details : {},
    );
  }
  if (!body) throw new TransactionApiError('Transactions returned an empty or unreadable response.');
  return body;
}

export async function getTransactionMetadata(signal) {
  return validateMetadata(await request('/metadata', { signal }));
}

export async function getTransactionSummary(userId, currency, signal) {
  const params = new URLSearchParams({ userId, currency });
  const summary = await request(`/summary?${params}`, { signal });
  const isAmount = (value) => typeof value === 'number' && Number.isSafeInteger(Math.round(value * 100));
  if (summary.currency !== currency || !isAmount(summary.income) || !isAmount(summary.expenses)
      || !isAmount(summary.netIncome) || summary.income < 0 || summary.expenses < 0
      || !Array.isArray(summary.spendingByCategory)
      || !summary.spendingByCategory.every((row) => row && typeof row.category === 'string'
        && /^[A-Z_]+$/.test(row.category) && isAmount(row.amount) && row.amount >= 0)) {
    throw new TransactionApiError('The transaction summary could not be displayed accurately.');
  }
  return summary;
}

// Expenses for chart
export function getTransactionHistory(
  userId,
  { page = 0, size = 100, type = 'EXPENSE', signal } = {},
) {
  const params = new URLSearchParams({
    userId: String(userId),
    page: String(page),
    size: String(size),
    sortBy: 'date',
    direction: 'desc',
  });

  if (type) params.set('type', type);

  return request(`?${params.toString()}`, { signal });
}

export function getTransaction(id, signal) {
  return request(`/${encodeURIComponent(id)}`, { signal });
}

export function saveTransaction(payload, id, signal) {
  return request(id ? `/${encodeURIComponent(id)}` : '', {
    method: id ? 'PATCH' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal,
  });
}
