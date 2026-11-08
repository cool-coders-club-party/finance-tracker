import assert from 'node:assert/strict';
import { test, afterEach } from 'node:test';
import { acceptLogin, logout } from '../src/lib/authSession.js';
import { deleteTransaction, getTransactionHistory, restoreTransaction } from '../src/api/transactions.js';

function login() {
  acceptLogin({ accessToken: 'owner-token', tokenType: 'Bearer', expiresIn: 900, id: 'owner', email: 'owner@example.com' });
}
afterEach(() => logout());

test('history sends pagination and all filters without caller-provided ownership', async (t) => {
  login();
  let request;
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    request = { url: new URL(url, 'http://localhost'), options };
    return Response.json({ content: [], totalPages: 0, totalElements: 0 });
  });
  await getTransactionHistory({ page: 2, size: 10, type: 'INCOME', category: 'SALARY', from: '2026-01-01', to: '2026-02-01', userId: 'forged' });
  assert.equal(request.url.pathname, '/api/v1/transactions');
  assert.deepEqual(Object.fromEntries(request.url.searchParams), {
    page: '2', size: '10', sortBy: 'date', direction: 'desc', type: 'INCOME', category: 'SALARY', from: '2026-01-01', to: '2026-02-01',
  });
  assert.equal(request.options.headers.Authorization, 'Bearer owner-token');
});

test('deleted history uses a dedicated endpoint and supports all transaction types', async (t) => {
  login();
  let path;
  t.mock.method(globalThis, 'fetch', async (url) => {
    path = new URL(url, 'http://localhost');
    return Response.json({ content: [] });
  });
  await getTransactionHistory({ deleted: true, type: '' });
  assert.equal(path.pathname, '/api/v1/transactions/deleted');
  assert.equal(path.searchParams.has('type'), false);
});

test('soft delete accepts both empty 200 and 204 responses and never requests permanent deletion', async (t) => {
  login();
  const calls = [];
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    calls.push({ url, options });
    return new Response(null, { status: calls.length === 1 ? 200 : 204 });
  });
  assert.equal(await deleteTransaction(7), null);
  assert.equal(await deleteTransaction(8), null);
  assert.deepEqual(calls.map(({ url }) => url), ['/api/v1/transactions/7', '/api/v1/transactions/8']);
  assert.ok(calls.every(({ options }) => options.method === 'DELETE' && options.headers.Authorization === 'Bearer owner-token'));
});

test('restore posts to the restore endpoint and returns the original transaction', async (t) => {
  login();
  const record = { id: 7, currency: 'GBP', amount: 42.5 };
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, '/api/v1/transactions/7/restore');
    assert.equal(options.method, 'POST');
    assert.equal(options.headers.Authorization, 'Bearer owner-token');
    return Response.json(record);
  });
  assert.deepEqual(await restoreTransaction(7), record);
});

test('failed mutations preserve backend error messages', async (t) => {
  login();
  t.mock.method(globalThis, 'fetch', async () => Response.json({ message: 'Transaction not found' }, { status: 404 }));
  await assert.rejects(deleteTransaction(7), /Transaction not found/);
  await assert.rejects(restoreTransaction(7), /Transaction not found/);
});

test('history propagates cancellation rather than reporting a connection failure', async (t) => {
  login();
  const controller = new AbortController();
  controller.abort();
  t.mock.method(globalThis, 'fetch', async (_url, options) => {
    assert.equal(options.signal, controller.signal);
    throw new DOMException('Aborted', 'AbortError');
  });
  await assert.rejects(getTransactionHistory({ signal: controller.signal }), { name: 'AbortError' });
});
