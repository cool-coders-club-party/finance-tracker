import assert from 'node:assert/strict';
import { test } from 'node:test';

const key = 'finance-tracker:auth';
let instance = 0;
async function freshSession(t, saved) {
  const storage = new Map(saved ? [[key, JSON.stringify(saved)]] : []);
  const descriptor = Object.getOwnPropertyDescriptor(globalThis, 'sessionStorage');
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
    getItem: (name) => storage.get(name) ?? null,
    setItem: (name, value) => storage.set(name, value),
    removeItem: (name) => storage.delete(name),
  } });
  const auth = await import(`../src/lib/authSession.js?restoration-test=${++instance}`);
  t.after(() => {
    auth.logout();
    if (descriptor) Object.defineProperty(globalThis, 'sessionStorage', descriptor);
    else delete globalThis.sessionStorage;
  });
  return { auth, storage };
}

for (const count of [1, 2]) {
  test(`${count} private request(s) wait for one restoration and retain the saved token`, async (t) => {
    const saved = { token: 'saved-token', user: { id: 'owner', email: 'owner@example.com' }, expiresAt: Date.now() + 60000 };
    const { auth, storage } = await freshSession(t, saved);
    let finish;
    const me = new Promise((resolve) => { finish = resolve; });
    const calls = [];
    t.mock.method(globalThis, 'fetch', async (url, options) => {
      calls.push({ url, options });
      return url === '/api/auth/me' ? me : Response.json({ ok: true });
    });
    const restoration = auth.restoreSession();
    const requests = Array.from({ length: count }, (_, index) => auth.authenticatedFetch(`/private/${index}`));
    assert.deepEqual(calls.map(({ url }) => url), ['/api/auth/me']);
    assert.equal(auth.getAuthSnapshot().status, 'loading');
    assert.equal(JSON.parse(storage.get(key)).token, saved.token);
    finish(Response.json(saved.user));
    await Promise.all([restoration, ...requests]);
    assert.equal(calls.length, count + 1);
    assert.ok(calls.every(({ options }) => options.headers.Authorization === 'Bearer saved-token'));
    assert.equal(auth.getAuthSnapshot().session.token, saved.token);
    assert.equal(JSON.parse(storage.get(key)).token, saved.token);
  });
}

for (const scenario of ['missing', 'expired', 'rejected']) {
  test(`${scenario} saved session never sends a private request`, async (t) => {
    const saved = scenario === 'missing' ? null : {
      token: 'saved-token', user: { id: 'owner', email: 'owner@example.com' },
      expiresAt: Date.now() + (scenario === 'expired' ? -1000 : 60000),
    };
    const { auth, storage } = await freshSession(t, saved);
    const calls = [];
    t.mock.method(globalThis, 'fetch', async (url) => {
      calls.push(url);
      return new Response(null, { status: 401 });
    });
    await assert.rejects(auth.authenticatedFetch('/private'), /sign in/);
    assert.deepEqual(calls, scenario === 'rejected' ? ['/api/auth/me'] : []);
    assert.equal(auth.getAuthSnapshot().session, null);
    assert.equal(auth.getAuthSnapshot().status, 'ready');
    assert.ok(auth.getAuthSnapshot().message);
    assert.equal(storage.has(key), false);
  });
}
