import { createContext } from 'react';

// The shared channel for auth state. Components do not use this directly: the
// AuthProvider fills it in and the useAuth hook reads it. It starts as null so
// useAuth can detect a component that is outside the provider.
export const AuthContext = createContext(null);