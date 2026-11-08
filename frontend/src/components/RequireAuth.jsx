import { Navigate, Outlet, useLocation } from 'react-router-dom';
import useAuth from '../hooks/useAuth';

export default function RequireAuth() {
  const auth = useAuth();
  const location = useLocation();
  if (auth.status === 'loading') return <p role="status" className="p-8">Checking your session…</p>;
  if (!auth.session) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  return <Outlet key={auth.session.user.id} />;
}
