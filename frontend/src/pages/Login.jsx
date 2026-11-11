import { useRef, useState, useEffect } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import useAuth from '../hooks/useAuth';
import { acceptLogin } from '../lib/authSession';

export default function Login({ registration = false }) {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [email, setEmail] = useState(location.state?.email ?? '');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const request = useRef(null);
  useEffect(() => () => request.current?.abort(), []);
  const requestedPath = location.state?.from;
  const destination = typeof requestedPath === 'string' && requestedPath.startsWith('/')
    && !requestedPath.startsWith('//') && !requestedPath.startsWith('/login') && !requestedPath.startsWith('/register') ? requestedPath : '/';

  async function signIn(event) {
    event.preventDefault();
    if (request.current) return;
    if (registration && password !== confirmation) {
      setError('Passwords must match.');
      return;
    }
    if (registration && new TextEncoder().encode(password).length > 72) {
      setError('Password must be at most 72 bytes. Some characters use more than one byte.');
      return;
    }
    const controller = new AbortController();
    request.current = controller;
    setPending(true);
    setError('');
    try {
      const response = await fetch(registration ? '/api/auth/register' : '/api/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: email.trim(), password }), signal: controller.signal,
      });
      const body = await response.json().catch(() => null);
      if (!response.ok) {
        if (registration && response.status === 409) throw new Error('An account with this email already exists. Please sign in.');
        if (registration && response.status === 400) throw new Error('Check your email and use a password between 8 and 72 characters.');
        throw new Error(response.status === 401
          ? 'Email or password is incorrect.'
          : registration ? 'Registration failed. Please try again.' : 'Sign-in failed. Please try again.');
      }
      if (!controller.signal.aborted) {
        if (registration) {
          navigate('/login', { replace: true, state: {
            email: email.trim(), registered: true, from: destination,
          } });
        } else {
          acceptLogin(body);
        }
      }
    } catch (failure) {
      if (!controller.signal.aborted) setError(failure instanceof TypeError
        ? 'Cannot reach the Users service. Please try again.' : failure.message);
    } finally {
      request.current = null;
      if (!controller.signal.aborted) setPending(false);
    }
  }

  if (auth.status === 'loading') return <p role="status" className="p-8">Checking your session…</p>;
  if (auth.session) return <Navigate to={destination} replace />;
  const inputClass = 'min-h-11 rounded-lg border border-input bg-background/70 px-3 text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring';
  return (
      <main className="login-page">
          <section className="login-card">
              <div className="login-media">
                  <p>Video will go here!</p>
              </div>

              <div className="login-content">
                  <h1 className="login-title">
                      {registration ? 'Welcome to Póca!' : 'Welcome back to Póca!'}
                  </h1>
    <Card className="glass mx-auto w-full max-w-md bg-card/10">
      <CardHeader><CardTitle>{registration ? 'Create account' : 'Sign in'}</CardTitle></CardHeader>
      <CardContent>
        <form onSubmit={signIn} className="space-y-5">
          {!registration && location.state?.registered && <p role="status" className="text-sm">Account created. Sign in to continue.</p>}
          {auth.message && <p role="status" className="text-sm">{auth.message}</p>}
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <label className="grid gap-2 text-sm">Email
            <input className={inputClass} type="email" autoComplete="username" required value={email}
              onChange={(event) => setEmail(event.target.value)} disabled={pending} />
          </label>
          <label className="grid gap-2 text-sm">Password
            <input className={inputClass} type="password" autoComplete={registration ? 'new-password' : 'current-password'} required
              minLength={registration ? 8 : undefined} maxLength={registration ? 72 : undefined} value={password}
              onChange={(event) => setPassword(event.target.value)} disabled={pending} />
          </label>
          {registration && <>
            <p className="text-xs text-muted-foreground">Use 8–72 characters for your password.</p>
            <label className="grid gap-2 text-sm">Confirm password
              <input className={inputClass} type="password" autoComplete="new-password" required
                value={confirmation} onChange={(event) => setConfirmation(event.target.value)} disabled={pending} />
            </label>
          </>}
          <Button type="submit"  className="min-h-11 w-full" disabled={pending}>{pending ? (registration ? 'Creating account…' : 'Signing in…') : (registration ? 'Create account' : 'Sign in')}</Button>
        </form>
        <p className="mt-5 text-sm">
          {registration ? 'Already have an account? ' : 'New here? '}
          <Link className="underline underline-offset-4" to={registration ? '/login' : '/register'}
            state={{ from: destination }}>{registration ? 'Sign in' : 'Create an account'}</Link>
        </p>
      </CardContent>
    </Card>
  </div>
  </section>
  </main>
  );
}
