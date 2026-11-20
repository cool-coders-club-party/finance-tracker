import assert from 'node:assert/strict';
import { test, beforeEach, afterEach } from 'node:test';
import { authenticatedFetch } from '../src/lib/authenticatedFetch.js';
import { startSession, clearSession, getAccessToken, getRefreshToken } from '../src/lib/session.js';

// Node has no localStorage, so give the tests a tiny stand-in.
const store = new Map();
globalThis.localStorage = {
  getItem: (key) => store.get(key) ?? null,
  setItem: (key, value) => store.set(key, String(value)),
  removeItem: (key) => store.delete(key),
};

const tokens = (n, expiresIn = 900) => ({
  id: 'u1', email: 'a@b.com', accessToken: `access-${n}`, refreshToken: `refresh-${n}`,
  tokenType: 'Bearer', expiresIn,
});
const isRefresh = (url) => url === '/api/auth/refresh';

beforeEach(() => store.clear());
afterEach(() => clearSession());

test('adds the Bearer header and keeps the caller\'s own options', async (t) => {
  startSession(tokens(1));
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json({ ok: true }));
  const response = await authenticatedFetch('/api/v1/transactions', {
    method: 'POST', headers: { Accept: 'application/json' }, body: '{"a":1}',
  });
  assert.equal(response.status, 200);
  const [url, options] = fetchMock.mock.calls[0].arguments;
  assert.equal(url, '/api/v1/transactions');
  assert.equal(options.method, 'POST');
  assert.equal(options.body, '{"a":1}');
  assert.equal(options.headers.Accept, 'application/json');
  assert.equal(options.headers.Authorization, 'Bearer access-1');
});

test('an expired access token is refreshed before the request is sent', async (t) => {
  t.mock.timers.enable({ apis: ['Date'] });
  startSession(tokens(1, 900));
  t.mock.timers.tick(900_000);
  const seen = [];
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    seen.push([url, options.headers?.Authorization]);
    return isRefresh(url) ? Response.json(tokens(2)) : Response.json({ ok: true });
  });
  await authenticatedFetch('/api/v1/transactions');
  assert.deepEqual(seen, [
    ['/api/auth/refresh', undefined],
    ['/api/v1/transactions', 'Bearer access-2'],
  ]);
});

test('a 401 triggers one refresh and one retry with the new token', async (t) => {
  startSession(tokens(1));
  const seen = [];
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    seen.push([url, options.headers?.Authorization]);
    if (isRefresh(url)) return Response.json(tokens(2));
    return options.headers.Authorization === 'Bearer access-1'
      ? new Response(null, { status: 401 })
      : Response.json({ ok: true });
  });
  const response = await authenticatedFetch('/api/v1/transactions');
  assert.equal(response.status, 200);
  assert.deepEqual(seen, [
    ['/api/v1/transactions', 'Bearer access-1'],
    ['/api/auth/refresh', undefined],
    ['/api/v1/transactions', 'Bearer access-2'],
  ]);
});

test('it retries only once: a second 401 is returned and the session is cleared', async (t) => {
  startSession(tokens(1));
  let requests = 0;
  t.mock.method(globalThis, 'fetch', async (url) => {
    if (isRefresh(url)) return Response.json(tokens(2));
    requests += 1;
    return new Response(null, { status: 401 });
  });
  const response = await authenticatedFetch('/api/v1/transactions');
  assert.equal(response.status, 401);
  assert.equal(requests, 2);
  assert.equal(getAccessToken(), null);
  assert.equal(getRefreshToken(), null);
});

test('errors that are not 401 are returned without any refresh', async (t) => {
  startSession(tokens(1));
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 403 }));
  assert.equal((await authenticatedFetch('/a')).status, 403);
  fetchMock.mock.mockImplementation(async () => new Response(null, { status: 500 }));
  assert.equal((await authenticatedFetch('/a')).status, 500);
  assert.equal(fetchMock.mock.callCount(), 2);
});

test('two requests that both get a 401 share one refresh', async (t) => {
  startSession(tokens(1));
  let refreshCalls = 0;
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    if (isRefresh(url)) { refreshCalls += 1; return Response.json(tokens(2)); }
    return options.headers.Authorization === 'Bearer access-1'
      ? new Response(null, { status: 401 })
      : Response.json({ ok: true });
  });
  const [a, b] = await Promise.all([authenticatedFetch('/a'), authenticatedFetch('/b')]);
  assert.equal(a.status, 200);
  assert.equal(b.status, 200);
  assert.equal(refreshCalls, 1);
});

test('with no session at all it throws without calling the server', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch');
  await assert.rejects(authenticatedFetch('/a'), (e) => e.status === 401);
  assert.equal(fetchMock.mock.callCount(), 0);
});

test('a rejected refresh token throws and clears the session', async (t) => {
  startSession(tokens(1));
  t.mock.method(globalThis, 'fetch', async (url) =>
    isRefresh(url) ? Response.json({ message: 'Invalid refresh token' }, { status: 401 })
      : new Response(null, { status: 401 }));
  await assert.rejects(authenticatedFetch('/a'), (e) => e.status === 401);
  assert.equal(getRefreshToken(), null);
  assert.equal(getAccessToken(), null);
});