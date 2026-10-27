import assert from 'node:assert/strict';
import { test } from 'node:test';
import { categoriesForType, categoryAfterTypeChange, formatEnumLabel, validateMetadata } from '../src/lib/transactionMetadata.js';
import { getTransactionMetadata, getTransaction, saveTransaction } from '../src/api/transactions.js';

const metadata = {
  types: ['INCOME', 'EXPENSE'],
  categories: [
    { name: 'SALARY', allowedTypes: ['INCOME'] },
    { name: 'GROCERIES', allowedTypes: ['EXPENSE'] },
    { name: 'GIFT', allowedTypes: ['INCOME', 'EXPENSE'] },
    { name: 'OTHER', allowedTypes: ['INCOME', 'EXPENSE'] },
  ],
};

test('type changes clear incompatible categories but preserve shared categories', () => {
  assert.deepEqual(categoriesForType(metadata, 'INCOME').map(({ name }) => name), ['SALARY', 'GIFT', 'OTHER']);
  assert.deepEqual(categoriesForType(metadata, 'EXPENSE').map(({ name }) => name), ['GROCERIES', 'GIFT', 'OTHER']);
  assert.deepEqual(categoriesForType(metadata, ''), []);
  assert.equal(categoryAfterTypeChange(metadata, 'SALARY', 'EXPENSE'), '');
  assert.equal(categoryAfterTypeChange(metadata, 'GROCERIES', 'INCOME'), '');
  assert.equal(categoryAfterTypeChange(metadata, 'GIFT', 'EXPENSE'), 'GIFT');
  assert.equal(categoryAfterTypeChange(metadata, 'OTHER', 'INCOME'), 'OTHER');
  assert.equal(categoryAfterTypeChange(metadata, 'GIFT', ''), '');
});

test('metadata remains extensible and rejects broken API responses', () => {
  assert.equal(validateMetadata(metadata), metadata);
  const future = { types: ['TRANSFER'], categories: [{ name: 'SAVINGS', allowedTypes: ['TRANSFER'] }] };
  assert.deepEqual(categoriesForType(validateMetadata(future), 'TRANSFER'), future.categories);
  for (const broken of [null, {}, { types: [], categories: [] },
    { ...metadata, types: ['INCOME', 'INCOME'] },
    { ...metadata, categories: [{ name: 'SALARY', allowedTypes: ['UNKNOWN'] }] }]) {
    assert.throws(() => validateMetadata(broken), /options could not be read/);
  }
  assert.equal(formatEnumLabel('INVESTMENT_RETURN'), 'Investment return');
});

test('API uses relative URLs, preserves enum values and decimal text, and exposes backend validation', async (t) => {
  const calls = [];
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    calls.push({ url, options });
    return Response.json(metadata);
  });
  await getTransactionMetadata();
  assert.equal(calls[0].url, '/api/v1/transactions/metadata');
  const payload = { userId: '1', transactionType: 'INCOME', transactionCategory: 'SALARY', amount: '12345678901234567.89' };
  await saveTransaction(payload);
  assert.equal(calls[1].options.method, 'POST');
  assert.deepEqual(JSON.parse(calls[1].options.body), payload);
  await saveTransaction({ transactionCategory: 'GIFT' }, 42);
  assert.equal(calls[2].url, '/api/v1/transactions/42');
  assert.equal(calls[2].options.method, 'PATCH');
  await getTransaction(42);
  assert.equal(calls[3].options.method, undefined);
  globalThis.fetch.mock.mockImplementation(async () => Response.json({ message: 'Validation failed', details: { amount: 'Must be positive' } }, { status: 400 }));
  await assert.rejects(saveTransaction({ amount: '0' }), (error) => error.message === 'Validation failed' && error.details.amount === 'Must be positive');
});

test('network failures, malformed responses, and request cancellation are handled', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('Failed to fetch'); });
  await assert.rejects(getTransactionMetadata(), /Cannot reach Transactions/);
  globalThis.fetch.mock.mockImplementation(async () => new Response('<html>Error</html>', { status: 502 }));
  await assert.rejects(getTransactionMetadata(), /502/);
  globalThis.fetch.mock.mockImplementation(async () => Response.json({ types: [] }));
  await assert.rejects(getTransactionMetadata(), /options could not be read/);
  const controller = new AbortController();
  controller.abort();
  globalThis.fetch.mock.mockImplementation(async (_url, { signal }) => { signal.throwIfAborted(); });
  await assert.rejects(getTransactionMetadata(controller.signal), { name: 'AbortError' });
});
