import { refresh as requestRefresh, AuthApiError } from '../api/auth.js';

// Where the refresh token lives between page loads
const REFRESH_TOKEN_KEY = 'finance-tracker:refresh-token';

// Module-level variables: they exist once per page load and vanish on reload
let accessToken = null;
let accessTokenExpiresAt = 0; // in milliseconds
let user = null;
let pendingRefresh = null;   // pending refresh request, if it exists

const listeners = new Set(); // a Set holds each callback once

/** 
 * Call `listener` whenever the session starts, is replaced or is cleared.
 * Returns a function that stops listening.
 */
export function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function notify() {
  listeners.forEach((listener) => listener());
}

/** Save the response from login or refresh. */
export function startSession(tokenResponse) {
  accessToken = tokenResponse.accessToken;
  // expiresIn is in seconds; Date.now() is in milliseconds.
  accessTokenExpiresAt = Date.now() + tokenResponse.expiresIn * 1000;
  user = { id: tokenResponse.id, email: tokenResponse.email };
  localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken);
  notify();
}

/** Forget everything (logout, or the refresh token was rejected). */
export function clearSession() {
  accessToken = null;
  accessTokenExpiresAt = 0;
  user = null;
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  notify();
}

/** The access token, or null if we have none or it has expired. */
export function getAccessToken() {
  return accessToken && Date.now() < accessTokenExpiresAt ? accessToken : null;
}

export const getUser = () => user;
export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);

/**
 * Trade the stored refresh token for a new access token AND a new refresh token.
 * Concurrent callers share one request, because the server invalidates a refresh
 * token the moment it is used: a second request with the same token would fail.
 */
export function refreshSession() {
  if (pendingRefresh) return pendingRefresh;

  const token = getRefreshToken();
  if (!token) return Promise.reject(new AuthApiError('No refresh token', 401));

  pendingRefresh = requestRefresh(token)
    .then((tokenResponse) => {
      startSession(tokenResponse); // replaces BOTH tokens (rotation)
      return tokenResponse;
    })
    .catch((error) => {
      // Only a 401 means the token is invalid. A network failure (status 0)
      // says nothing about the token, so the user stays signed in and can retry.
      if (error.status === 401) clearSession();
      throw error;
    })
    .finally(() => {
      pendingRefresh = null; // allow a fresh refresh next time
    });

  return pendingRefresh;
}