import { useEffect, useSyncExternalStore } from 'react';
import { subscribe, getAuthSnapshot, restoreSession } from '../lib/authSession';

export default function useAuth() {
  const auth = useSyncExternalStore(subscribe, getAuthSnapshot);
  useEffect(() => { restoreSession(); }, []);
  return auth;
}
