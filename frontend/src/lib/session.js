/*
 * The session store: tracks who is signed in.
 *
 * Keeps the access token and user in memory, and the refresh token in
 * localStorage so a page reload can sign the user back in quietly. It is plain
 * JavaScript with no React, so the API helpers can use it too. AuthProvider
 * mirrors it into React state through subscribe().
 *
 * Exports:
 *   startSession / clearSession: begin or end a session locally
 *   getAccessToken / getUser / getRefreshToken: read the current state
 *   refreshSession:              swap the refresh token for new tokens (one request shared by every caller)
 *   restoreSession:              on app start, try to resume a stored session
 *   endSession:                  sign out: clear locally, then revoke on the server
 *   subscribe:                   be told whenever the session changes
 */
import { refresh as requestRefresh, logout as requestLogout, AuthApiError } from '../api/auth.js';

// Where the refresh token lives between page loads
const REFRESH_TOKEN_KEY = 'finance-tracker:refresh-token';

// Module-level variables: they exist once per page load and vanish on reload
let accessToken = null;
let accessTokenExpiresAt = 0; // in milliseconds
let user = null;
let pendingRefresh = null;   // pending refresh request, if it exists
let sessionVersion = 0; // goes up every time the session starts or is cleared

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
  sessionVersion += 1;
  pendingRefresh = null;
  notify();
}

/** Forget everything (logout, or the refresh token was rejected). */
export function clearSession() {
  accessToken = null;
  accessTokenExpiresAt = 0;
  user = null;
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionVersion += 1;
  pendingRefresh = null;
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

  const versionAtStart = sessionVersion;
  const promise = requestRefresh(token)
    .then((tokenResponse) => {
      if (versionAtStart !== sessionVersion) {
        // The user signed out (or signed in again) while this request was in flight.
        // Applying the result would undo that, so throw it away.
        throw new AuthApiError('The session changed during the refresh', 401);
      }
      startSession(tokenResponse); // replaces BOTH tokens (rotation)
      return tokenResponse;
    })
    .catch((error) => {
      // Only a 401 means this token is invalid, and only if the session is still the
      // one this request started with. A network failure (status 0) keeps the session.
      if (error.status === 401 && versionAtStart === sessionVersion) clearSession();
      throw error;
    })
    .finally(() => {
      if (pendingRefresh === promise) pendingRefresh = null;
    });
  pendingRefresh = promise;
  return promise;
}

/**
 * Called once when the app starts. If a refresh token is stored, trade it for a
 * new session.
 */
export async function restoreSession() {
  if (!getRefreshToken()) return; // nothing to restore
  try {
    await refreshSession();
  } catch {
    // A rejected token has already cleared the session (see refreshSession).
    // A network failure leaves the token stored, so the next page load tries again.
  }
}

/**
 * Sign out: forget the session here at once, then ask the server to revoke the refresh token.
 */
export async function endSession() {
  const refreshToken = getRefreshToken();
  clearSession(); // the user is signed out on this device immediately
  if (!refreshToken) return;
  try {
    await requestLogout(refreshToken);
  } catch {
    // The user is signed out on this device either way. If the server could not be
    // reached, the token simply expires on its own.
  }
}