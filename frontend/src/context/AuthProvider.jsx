import { useEffect, useState } from 'react';
import { login } from '../api/auth.js';
import { startSession, endSession, getUser, getRefreshToken, restoreSession, subscribe } from '../lib/session.js';
import { AuthContext } from './AuthContext.js';

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

  async function signIn(email, password) {
    const tokens = await login(email, password); // throws AuthApiError on failure
    startSession(tokens);                         // notifies, so `user` updates
  }

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