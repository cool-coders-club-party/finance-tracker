import assert from 'node:assert/strict';
import { test, beforeEach, afterEach } from 'node:test';
import './helpers/localStorage.js';
import { startSession, clearSession } from '../src/lib/session.js';
import { getTransactionSummary, TransactionApiError } from '../src/api/transactions.js';

beforeEach(() => startSession({ accessToken: 'test-token', refreshToken: 'test-refresh', expiresIn: 900, id: 'owner', email: 'owner@example.com' }));
afterEach(() => clearSession());

function summary(currency = 'EUR') {
  return {
    currency, income: 3000, expenses: 400, netIncome: 2600,
    spendingByCategory: [{ category: 'GROCERIES', amount: 400 }],
    incomeByCategory: [{ category: 'SALARY', amount: 2500 }, { category: 'FREELANCE', amount: 500 }],
  };
}

test('one authenticated summary request returns both category breakdowns in the requested currency', async (t) => {
  const body = summary('GBP');
  const fetch = t.mock.method(globalThis, 'fetch', async () => Response.json(body));
  assert.deepEqual(await getTransactionSummary('GBP'), body);
  assert.equal(fetch.mock.calls.length, 1);
  const [url, options] = fetch.mock.calls[0].arguments;
  assert.equal(url, '/api/v1/transactions/summary?currency=GBP');
  assert.equal(options.headers.Authorization, 'Bearer test-token');
});

test('empty income and expense breakdowns are valid, including negative net income', async (t) => {
  const body = { ...summary(), income: 0, netIncome: -400, incomeByCategory: [] };
  t.mock.method(globalThis, 'fetch', async () => Response.json(body));
  assert.deepEqual(await getTransactionSummary('EUR'), body);
  Object.assign(body, { expenses: 0, netIncome: 0, spendingByCategory: [] });
  assert.deepEqual(await getTransactionSummary('EUR'), body);
});

for (const field of ['incomeByCategory', 'spendingByCategory']) {
  test(`${field} rejects missing arrays and malformed category amounts`, async (t) => {
    const body = summary();
    t.mock.method(globalThis, 'fetch', async () => Response.json(body));
    for (const invalid of [undefined, null, {}, [null], [{ category: 'salary', amount: 1 }],
      [{ category: 'SALARY', amount: -1 }], [{ category: 'SALARY', amount: '1.00' }],
      [{ category: 'SALARY', amount: Number.MAX_SAFE_INTEGER }]]) {
      body[field] = invalid;
      await assert.rejects(getTransactionSummary('EUR'), TransactionApiError);
    }
  });
}

test('summary rejects a response in the wrong display currency', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json(summary('USD')));
  await assert.rejects(getTransactionSummary('EUR'), TransactionApiError);
});

test('unavailable conversion preserves the backend error instead of returning partial chart data', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ message: 'Rate unavailable' }, { status: 503 }));
  await assert.rejects(getTransactionSummary('EUR'), { name: 'TransactionApiError', message: 'Rate unavailable' });
});
