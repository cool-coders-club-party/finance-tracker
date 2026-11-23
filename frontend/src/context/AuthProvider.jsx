import { useEffect, useState } from 'react';
import { login } from '../api/auth.js';
import { startSession, endSession, getUser, getRefreshToken, restoreSession, subscribe } from '../lib/session.js';
import { AuthContext } from './AuthContext.js';

/**
 * Holds the signed-in user for the whole app and shares it through context.
 * The real session lives in lib/session.js; this component mirrors it into
 * React state so pages re-render when it changes.
 *
 * `status` is "loading" while a stored session is being restored on startup,
 * then "authenticated" or "anonymous".
 *
 * @param {object} props component props.
 * @param {import("react").ReactNode} props.children the app, rendered inside the provider.
 * @return {import("react").ReactElement} the provider wrapping its children.
 */
export function AuthProvider({ children }) {
  // React's own copy of the current user. Changing it re-renders the components that use it.
  const [user, setUser] = useState(getUser());

  // True while we are trying to restore a session from a stored refresh token.
  // Users with no stored token never see it: they are simply signed out straight away.
  const [restoring, setRestoring] = useState(() => getRefreshToken() !== null);

  // When the session store changes (login, refresh, logout, a rejected refresh),
  // copy the new user into React state. The returned function unsubscribes
  // when the provider is removed.
  useEffect(() => subscribe(() => setUser(getUser())), []);

  // Once, on app start: try to restore the session from the stored refresh token.
  useEffect(() => {
    let cancelled = false;
    restoreSession().finally(() => {
      if (!cancelled) setRestoring(false);
    });
    return () => { cancelled = true; };
  }, []);

  // 

  /**
   * Logs in, then stores the tokens. A wrong password throws an AuthApiError,
   * which the login page catches and turns into a message.
   */
  async function signIn(email, password) {
    const tokens = await login(email, password); // throws AuthApiError on failure
    startSession(tokens);                         // notifies, so `user` updates
  }

  /**
   * Clears the session locally straight away; revoking the token on the server
   * happens in the background and its failure is ignored.
   */
  function logout() {
    endSession();
  }

  const value = {
    user,
    status: restoring ? 'loading' : user ? 'authenticated' : 'anonymous',
    signIn,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}