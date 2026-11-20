import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/useAuth.js';

/** 
 * Wraps pages that need a login. Used as a "layout route" in the router:
 * signed-in users see the child page (rendered by <Outlet />), others go to login.
 */
export default function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();

  // Unsure yet whether the user is signed in (a stored session is being
  // restored). Deciding now would bounce signed-in users to the login page on reload.
  if (status === 'loading') {
    return <p role="status" className="p-8">Checking your session…</p>;
  }

  if (status === 'anonymous') {
    // Remember where the user was going. The login page sends them back here
    // after signing in (it checks the path with safeRedirectPath first).
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  return <Outlet />;
}