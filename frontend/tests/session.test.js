import assert from 'node:assert/strict';
import { test, beforeEach, afterEach } from 'node:test';
import {
  startSession, clearSession, endSession, getAccessToken, getUser, getRefreshToken, refreshSession, restoreSession, subscribe
} from '../src/lib/session.js';

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

beforeEach(() => store.clear());
afterEach(() => clearSession());

test('startSession keeps the access token in memory and the refresh token in storage', () => {
  startSession(tokens(1));
  assert.equal(getAccessToken(), 'access-1');
  assert.deepEqual(getUser(), { id: 'u1', email: 'a@b.com' });
  assert.equal(getRefreshToken(), 'refresh-1');
  assert.ok(![...store.values()].includes('access-1'), 'access token must not be in storage');
});

test('clearSession forgets everything', () => {
  startSession(tokens(1));
  clearSession();
  assert.equal(getAccessToken(), null);
  assert.equal(getUser(), null);
  assert.equal(getRefreshToken(), null);
});

test('getAccessToken returns null once the token has expired', (t) => {
  t.mock.timers.enable({ apis: ['Date'] });
  startSession(tokens(1, 900));
  t.mock.timers.tick(899_000);
  assert.equal(getAccessToken(), 'access-1');
  t.mock.timers.tick(1_000);
  assert.equal(getAccessToken(), null);
});

test('refreshSession sends the stored token and replaces both tokens', async (t) => {
  startSession(tokens(1));
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json(tokens(2)));
  await refreshSession();
  assert.equal(JSON.parse(fetchMock.mock.calls[0].arguments[1].body).refreshToken, 'refresh-1');
  assert.equal(getAccessToken(), 'access-2');
  assert.equal(getRefreshToken(), 'refresh-2');
});

test('simultaneous refreshes share one request', async (t) => {
  startSession(tokens(1));
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json(tokens(2)));
  await Promise.all([refreshSession(), refreshSession(), refreshSession()]);
  assert.equal(fetchMock.mock.callCount(), 1);
});

test('a later refresh makes a new request once the first has finished', async (t) => {
  startSession(tokens(1));
  let n = 1;
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json(tokens(++n)));
  await refreshSession();
  await refreshSession();
  assert.equal(fetchMock.mock.callCount(), 2);
  assert.equal(JSON.parse(fetchMock.mock.calls[1].arguments[1].body).refreshToken, 'refresh-2');
});

test('a rejected refresh token (401) clears the session', async (t) => {
  startSession(tokens(1));
  t.mock.method(globalThis, 'fetch', async () =>
    Response.json({ message: 'Invalid refresh token' }, { status: 401 }));
  await assert.rejects(refreshSession(), (e) => e.status === 401);
  assert.equal(getAccessToken(), null);
  assert.equal(getRefreshToken(), null);
});

test('a network failure keeps the session so the user can retry', async (t) => {
  startSession(tokens(1));
  t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('Failed to fetch'); });
  await assert.rejects(refreshSession(), (e) => e.status === 0);
  assert.equal(getRefreshToken(), 'refresh-1');
  assert.equal(getAccessToken(), 'access-1');
});

test('refreshing with no stored token fails without calling the server', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch');
  await assert.rejects(refreshSession(), (e) => e.status === 401);
  assert.equal(fetchMock.mock.callCount(), 0);
});

test('listeners are told when the session starts and clears, until they unsubscribe', () => {
  let calls = 0;
  const unsubscribe = subscribe(() => { calls += 1; });
  startSession(tokens(1));
  clearSession();
  assert.equal(calls, 2);
  unsubscribe();
  startSession(tokens(2));
  assert.equal(calls, 2); // no longer listening
});

const REFRESH_KEY = 'finance-tracker:refresh-token';

test('restoreSession does nothing when there is no stored refresh token', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch');
  await restoreSession();
  assert.equal(fetchMock.mock.callCount(), 0);
  assert.equal(getUser(), null);
});

test('restoreSession signs the user back in from a stored refresh token', async (t) => {
  localStorage.setItem(REFRESH_KEY, 'refresh-1');
  t.mock.method(globalThis, 'fetch', async () => Response.json(tokens(2)));
  await restoreSession();
  assert.deepEqual(getUser(), { id: 'u1', email: 'a@b.com' });
  assert.equal(getAccessToken(), 'access-2');
  assert.equal(getRefreshToken(), 'refresh-2');
});

test('restoreSession ends signed out, without throwing, if the token is rejected', async (t) => {
  localStorage.setItem(REFRESH_KEY, 'stale');
  t.mock.method(globalThis, 'fetch', async () =>
    Response.json({ message: 'Invalid refresh token' }, { status: 401 }));
  await restoreSession();
  assert.equal(getUser(), null);
  assert.equal(getRefreshToken(), null);
});

test('restoreSession keeps the stored token on a network failure and does not throw', async (t) => {
  localStorage.setItem(REFRESH_KEY, 'refresh-1');
  t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('Failed to fetch'); });
  await restoreSession();
  assert.equal(getUser(), null);
  assert.equal(getRefreshToken(), 'refresh-1');
});

test('two restoreSession calls at once send a single request', async (t) => {
  localStorage.setItem(REFRESH_KEY, 'refresh-1');
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json(tokens(2)));
  await Promise.all([restoreSession(), restoreSession()]);
  assert.equal(fetchMock.mock.callCount(), 1);
});

test('a refresh that finishes after sign-out does not sign the user back in', async (t) => {
  startSession(tokens(1));
  let finish;
  t.mock.method(globalThis, 'fetch', () => new Promise((resolve) => { finish = resolve; }));
  const pending = refreshSession();
  clearSession();
  finish(Response.json(tokens(2)));
  await assert.rejects(pending);
  assert.equal(getUser(), null);
  assert.equal(getRefreshToken(), null);
  assert.equal(getAccessToken(), null);
});

test('a stale rejected refresh does not wipe a newer sign-in', async (t) => {
  startSession(tokens(1));
  let finish;
  t.mock.method(globalThis, 'fetch', () => new Promise((resolve) => { finish = resolve; }));
  const pending = refreshSession();
  clearSession();
  startSession(tokens(9)); // someone signs in again before the old request answers
  finish(Response.json({ message: 'Invalid refresh token' }, { status: 401 }));
  await assert.rejects(pending);
  assert.equal(getAccessToken(), 'access-9');
  assert.equal(getRefreshToken(), 'refresh-9');
});

test('endSession clears the session and revokes the stored refresh token', async (t) => {
  startSession(tokens(1));
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
  await endSession();
  const [url, options] = fetchMock.mock.calls[0].arguments;
  assert.equal(url, '/api/auth/logout');
  assert.equal(JSON.parse(options.body).refreshToken, 'refresh-1');
  assert.equal(getUser(), null);
  assert.equal(getRefreshToken(), null);
});

test('endSession signs out locally even if the server cannot be reached', async (t) => {
  startSession(tokens(1));
  t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('Failed to fetch'); });
  await endSession(); // must not throw
  assert.equal(getUser(), null);
  assert.equal(getAccessToken(), null);
  assert.equal(getRefreshToken(), null);
});

test('endSession with no stored token sends nothing', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch');
  await endSession();
  assert.equal(fetchMock.mock.callCount(), 0);
});