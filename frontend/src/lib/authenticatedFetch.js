/*
 * fetch() for endpoints that need a login.
 *
 * Adds the access token to every request. If the token is missing or expired it
 * refreshes first, and if the server answers 401 it refreshes and retries once.
 * If that still fails the session is cleared and the caller gets an
 * AuthApiError. The Transactions, Portfolio and Reports API modules use this
 * instead of calling fetch directly, so token handling lives in one place.
 *
 * Exports: authenticatedFetch(url, options)
 */

import { AuthApiError } from '../api/auth.js';
import { getAccessToken, refreshSession, clearSession } from './session.js';

/** Send one request with the given access token in the Authorization header. */
function sendWithToken(url, options, token) {
  return fetch(url, {
    ...options,
    headers: { ...options.headers, Authorization: `Bearer ${token}` },
  });
}

/** Get a usable access token by refreshing the session. Throws if that is impossible. */
async function obtainFreshToken() {
  await refreshSession(); // throws AuthApiError when the refresh token is missing or rejected
  const token = getAccessToken();
  if (!token) throw new AuthApiError('Please sign in to continue.', 401);
  return token;
}

/**
 * Like fetch(), but for endpoints that need a login. Adds the Bearer token and
 * returns a normal Response. If the token is missing, expired or rejected (401),
 * it refreshes the session and retries the request ONCE.
 * Throws AuthApiError if no valid session can be obtained.
 */
export async function authenticatedFetch(url, options = {}) {
  // No token, or it has expired (e.g. after a page reload): refresh before sending.
  const token = getAccessToken() ?? (await obtainFreshToken());

  const response = await sendWithToken(url, options, token);
  if (response.status !== 401) return response; // success, or an error that is not about the token

  // The server rejected the token. Refresh and try once more, never in a loop.
  const freshToken = await obtainFreshToken();
  const retry = await sendWithToken(url, options, freshToken);
  if (retry.status === 401) clearSession(); // a fresh token was rejected too: give up
  return retry;
}