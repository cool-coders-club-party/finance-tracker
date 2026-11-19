import { createContext, useContext, useEffect, useState } from 'react';
import { login } from '../api/auth.js';
import { startSession, clearSession, getUser, subscribe } from '../lib/session.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // React's own copy of the current user. Changing it re-renders the components that use it.
  const [user, setUser] = useState(getUser());

  // When the session store changes (login, refresh, logout, a rejected refresh),
  // copy the new user into React state. The returned function unsubscribes
  // when the provider is removed.
  useEffect(() => subscribe(() => setUser(getUser())), []);

  async function signIn(email, password) {
    const tokens = await login(email, password); // throws AuthApiError on failure
    startSession(tokens);                         // notifies, so `user` updates
  }

  function signOut() {
    clearSession();
  }

  const value = {
    user,
    status: user ? 'authenticated' : 'anonymous', // 'loading' arrives in Stage 4
    signIn,
    signOut,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}