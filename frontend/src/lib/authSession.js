const STORAGE_KEY = 'finance-tracker:auth';
const listeners = new Set();
let state = { status: 'loading', session: null, message: '' };
let restorePromise;
let expiryTimer;

function publish(next) {
  state = next;
  listeners.forEach((listener) => listener());
}

function storedSession() {
  try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY)); } catch { return null; }
}

export function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getAuthSnapshot() { return state; }

export function logout(message = '') {
  clearTimeout(expiryTimer);
  try { sessionStorage.removeItem(STORAGE_KEY); } catch { /* Memory session still works. */ }
  publish({ status: 'ready', session: null, message });
}

function establish(session) {
  clearTimeout(expiryTimer);
  try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session)); } catch { /* Memory session still works. */ }
  publish({ status: 'ready', session, message: '' });
  expiryTimer = setTimeout(() => logout('Your session expired. Please sign in again.'),
    Math.max(0, session.expiresAt - Date.now()));
}

export function acceptLogin(body) {
  if (!body || typeof body.accessToken !== 'string' || !body.accessToken
      || body.tokenType !== 'Bearer' || !Number.isFinite(body.expiresIn) || body.expiresIn <= 0
      || typeof body.id !== 'string' || typeof body.email !== 'string') {
    throw new Error('The sign-in response could not be read. Please try again.');
  }
  establish({ token: body.accessToken, user: { id: body.id, email: body.email },
    expiresAt: Date.now() + body.expiresIn * 1000 });
}

export function restoreSession() {
  if (state.status === 'ready') return Promise.resolve();
  if (restorePromise) return restorePromise;
  restorePromise = (async () => {
    const saved = storedSession();
    if (!saved?.token || !Number.isFinite(saved.expiresAt) || saved.expiresAt <= Date.now()) {
      logout(saved ? 'Your session expired. Please sign in again.' : '');
      return;
    }
    try {
      const response = await fetch('/api/auth/me', {
        headers: { Authorization: `Bearer ${saved.token}`, Accept: 'application/json' },
      });
      if (state.status !== 'loading') return;
      if (!response.ok) throw new Error('Please sign in again to continue.');
      const user = await response.json();
      if (typeof user.id !== 'string' || typeof user.email !== 'string') throw new Error('Please sign in again.');
      if (state.status !== 'loading') return;
      if (saved.expiresAt <= Date.now()) throw new Error('Your session expired. Please sign in again.');
      establish({ ...saved, user });
    } catch {
      if (state.status === 'loading') logout('Could not restore your session. Please sign in again.');
    }
  })();
  return restorePromise;
}

export async function authenticatedFetch(url, options = {}) {
  const session = state.session;
  if (!session || session.expiresAt <= Date.now()) {
    logout('Please sign in to continue.');
    throw new Error('Please sign in to continue.');
  }
  const response = await fetch(url, { ...options,
    headers: { ...options.headers, Authorization: `Bearer ${session.token}` },
  });
  if (response.status === 401 && state.session?.token === session.token) {
    logout('Your session expired. Please sign in again.');
  }
  return response;
}
