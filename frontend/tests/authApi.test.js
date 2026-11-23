import assert from 'node:assert/strict';
import { test } from 'node:test';
import { register, login, refresh, logout, getMe, AuthApiError } from '../src/api/auth.js';

test('login posts trimmed email and password as JSON', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json({ accessToken: 'a' }));
  const body = await login('  a@b.com ', 'secret123');
  const [url, options] = fetchMock.mock.calls[0].arguments;
  assert.equal(url, '/api/auth/login');
  assert.equal(options.method, 'POST');
  assert.equal(options.headers['Content-Type'], 'application/json');
  assert.deepEqual(JSON.parse(options.body), { email: 'a@b.com', password: 'secret123' });
  assert.equal(body.accessToken, 'a');
});

test('register returns the 201 body', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ id: 'u1', email: 'a@b.com' }, { status: 201 }));
  assert.deepEqual(await register('a@b.com', 'secret123'), { id: 'u1', email: 'a@b.com' });
});

test('401 and 409 become AuthApiError carrying status and server message', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch',
    async () => Response.json({ status: 401, message: 'Invalid email or password' }, { status: 401 }));
  await assert.rejects(login('a@b.com', 'x'), (e) =>
    e instanceof AuthApiError && e.status === 401 && e.message === 'Invalid email or password');
  fetchMock.mock.mockImplementation(async () => Response.json({ message: 'exists' }, { status: 409 }));
  await assert.rejects(register('a@b.com', 'secret123'), (e) => e.status === 409);
});

test('a 400 with no JSON body still gives a usable error', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response('not json', { status: 400 }));
  await assert.rejects(register('a@b.com', 'short'), (e) => e.status === 400 && /400/.test(e.message));
});

test('network failure becomes status 0', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('Failed to fetch'); });
  await assert.rejects(login('a@b.com', 'x'), (e) => e instanceof AuthApiError && e.status === 0);
});

test('logout accepts 204 with no body', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
  assert.equal(await logout('r1'), null);
  assert.deepEqual(JSON.parse(fetchMock.mock.calls[0].arguments[1].body), { refreshToken: 'r1' });
});

test('refresh sends the refresh token; getMe sends the Bearer header', async (t) => {
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json({ id: 'u1', email: 'a@b.com' }));
  await refresh('r1');
  assert.equal(fetchMock.mock.calls[0].arguments[0], '/api/auth/refresh');
  await getMe('tok');
  assert.equal(fetchMock.mock.calls[1].arguments[1].headers.Authorization, 'Bearer tok');
});