// Node has no localStorage, but session.js stores the refresh token in it.
// This stand-in keeps values in a Map for the duration of a test run. Import it
// first in any test file that starts a session.

const store = new Map();
globalThis.localStorage = {
  getItem: (key) => store.get(key) ?? null,
  setItem: (key, value) => store.set(key, String(value)),
  removeItem: (key) => store.delete(key),
};