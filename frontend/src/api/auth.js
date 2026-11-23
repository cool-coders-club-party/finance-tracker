// Talks to the users service. Vite forwards /api/auth to port 8081 (vite.config.js)
const BASE_URL = '/api/auth';

// A dedicated error type lets the UI check `error.status` (401, 409, ...)
// instead of guessing from the message text.
export class AuthApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'AuthApiError';
    this.status = status; // 0 means "the request never reached the server"
  }
}

/**
 * Sends one request to the users service and returns the parsed body.
 *
 * @param {string} path endpoint under /api/auth, such as "/login".
 * @param {RequestInit} options fetch options (method, headers, body).
 * @return {Promise<object|null>} the JSON body, or null for a 204 response.
 * @throws {AuthApiError} status 0 if the network failed, otherwise the HTTP status.
 */
async function send(path, options) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, options);
  } catch {
    // fetch only throws when the network fails, not on 4xx/5xx responses.
    throw new AuthApiError('Cannot reach the server. Please try again.', 0);
  }

  if (response.status === 204) return null; // logout: success, no body

  // Error bodies are not guaranteed to be JSON (a 400 has no fixed shape),
  // so a parse failure becomes null instead of a crash.
  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new AuthApiError(body?.message ?? `Request failed (${response.status})`, response.status);
  }
  return body;
}

/**
 * Sends a JSON POST request. Used by every endpoint that takes a body.
 *
 * @param {string} path endpoint under /api/auth.
 * @param {object} payload object to send as the JSON body.
 * @return {Promise<object|null>} the parsed response body.
 */
function postJson(path, payload) {
  return send(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(payload),
  });
}

// Emails are trimmed here; the server also lower-cases them.
export const register = (email, password) => postJson('/register', { email: email.trim(), password });
export const login = (email, password) => postJson('/login', { email: email.trim(), password });
export const refresh = (refreshToken) => postJson('/refresh', { refreshToken });
export const logout = (refreshToken) => postJson('/logout', { refreshToken });

export const getMe = (accessToken) =>
  send('/me', { headers: { Accept: 'application/json', Authorization: `Bearer ${accessToken}` } });