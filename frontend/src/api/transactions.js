import { authenticatedFetch } from '../lib/authSession.js';
import { validateMetadata } from '../lib/transactionMetadata.js';

const BASE_URL = '/api/v1/transactions';

export class TransactionApiError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'TransactionApiError';
    this.details = details;
  }
}

async function request(path, options = {}, allowEmpty = false) {
  let response;
  try {
    response = await authenticatedFetch(`${BASE_URL}${path}`, {
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
  if (allowEmpty && !body) return null;
  if (!body) throw new TransactionApiError('Transactions returned an empty or unreadable response.');
  return body;
}

export async function getTransactionMetadata(signal) {
  return validateMetadata(await request('/metadata', { signal }));
}

export async function getTransactionSummary(currency, signal) {
  const params = new URLSearchParams({ currency });
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

// Defaults preserve existing expense-chart callers; history passes an explicit type.
export function getTransactionHistory(
  { page = 0, size = 100, type = 'EXPENSE', category, from, to, deleted = false, signal } = {},
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sortBy: 'date',
    direction: 'desc',
  });

  for (const [key, value] of Object.entries({ type, category, from, to })) {
    if (value) params.set(key, value);
  }

  return request(`${deleted ? '/deleted' : ''}?${params.toString()}`, { signal });
}

export function getTransaction(id, signal) {
  return request(`/${encodeURIComponent(id)}`, { signal });
}

export function saveTransaction(payload, id, signal) {
  return request(id ? `/${encodeURIComponent(id)}` : '', {
    method: id ? 'PATCH' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(Object.fromEntries(Object.entries(payload).filter(([key]) =>
      ['transactionType', 'transactionCategory', 'amount', 'currency', 'description', 'occurredAt'].includes(key)))),
    signal,
  });
}

export function deleteTransaction(id, signal) {
  return request(`/${encodeURIComponent(id)}`, { method: 'DELETE', signal }, true);
}

export function restoreTransaction(id, signal) {
  return request(`/${encodeURIComponent(id)}/restore`, { method: 'POST', signal });
}
