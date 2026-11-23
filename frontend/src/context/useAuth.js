import { useContext } from 'react';
import { AuthContext } from './AuthContext.js';

/**
 * Gives a component the current auth state and actions.
 *
 * @return {{user: ({id: string, email: string}|null),
 *           status: ('loading'|'authenticated'|'anonymous'),
 *           signIn: function(string, string): Promise<void>,
 *           logout: function(): void}} the auth context value.
 * @throws {Error} if called outside an AuthProvider.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}