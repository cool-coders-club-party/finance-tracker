import assert from 'node:assert/strict';
import { test, afterEach } from 'node:test';
import { acceptLogin, logout, getAuthSnapshot, authenticatedFetch } from '../src/lib/authSession.js';
import { getTransactionHistory, getTransactionSummary, saveTransaction } from '../src/api/transactions.js';

function login(token = 'alice-token') {
  acceptLogin({ accessToken: token, tokenType: 'Bearer', expiresIn: 900, id: token, email: 'test@example.com' });
}
afterEach(() => logout());

test('transactions carry the token and omit caller-supplied ownership', async (t) => {
  login();
  const calls = [];
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    calls.push({ url, options });
    return Response.json({currency:'EUR', income:0, expenses:0, netIncome:0, spendingByCategory:[]});
  });
  await getTransactionHistory();
  await getTransactionSummary('EUR');
  await saveTransaction({userId:'forged', amount:'12.34', currency:'EUR'});
  assert.ok(calls.every(({options}) => options.headers.Authorization === 'Bearer alice-token'));
  assert.ok(calls.every(({url}) => !url.includes('userId')));
  assert.deepEqual(JSON.parse(calls[2].options.body), {amount:'12.34', currency:'EUR'});
});

test('a 401 clears the session but a forbidden response preserves it', async (t) => {
  login();
  const fetch = t.mock.method(globalThis, 'fetch', async () => new Response(null, {status:403}));
  await authenticatedFetch('/api/v1/transactions/1');
  assert.ok(getAuthSnapshot().session);
  fetch.mock.mockImplementation(async () => new Response(null, {status:401}));
  await authenticatedFetch('/api/v1/transactions');
  assert.equal(getAuthSnapshot().session, null);
});

test('a late 401 from an old session cannot sign out a newly logged-in user', async (t) => {
  login();
  let complete;
  t.mock.method(globalThis, 'fetch', () => new Promise((resolve) => { complete = resolve; }));
  const pending = authenticatedFetch('/api/v1/transactions');
  logout();
  login('bob-token');
  complete(new Response(null, {status:401}));
  await pending;
  assert.equal(getAuthSnapshot().session.token, 'bob-token');
});

test('logout prevents further authenticated requests', async (t) => {
  login();
  logout();
  const fetch = t.mock.method(globalThis, 'fetch');
  await assert.rejects(authenticatedFetch('/api/v1/transactions'), /sign in/);
  assert.equal(fetch.mock.callCount(), 0);
});

test('expiry clears the session without waiting for a request', (t) => {
  t.mock.timers.enable({apis:['setTimeout', 'Date']});
  login();
  t.mock.timers.tick(900000);
  assert.equal(getAuthSnapshot().session, null);
});
