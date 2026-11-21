import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import AuthLayout from '../components/AuthLayout';
import FormField from '../components/FormField';
import { Button } from '../components/ui/button';
//import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { useAuth } from '../context/useAuth.js';
import { loginErrorMessage } from '../lib/authMessages.js';
import { safeRedirectPath } from '../lib/redirect.js';
import { validateLogin } from '../lib/validation.js';

export default function SignIn() {
  const { status, signIn } = useAuth();
  const location = useLocation();
  // Where to go after signing in: the page the user was trying to reach, or home.
  const destination = safeRedirectPath(location.state?.from);
  // The registration page passes the new account's email along; prefill it if present.
  const [email, setEmail] = useState(location.state?.email ?? '');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [pending, setPending] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (pending) return;

    const found = validateLogin({ email, password });
    setErrors(found);
    setServerError('');
    if (Object.keys(found).length > 0) return;

    setPending(true);
    try {
      await signIn(email, password); // on success the session store updates `status`
    } catch (error) {
      setServerError(loginErrorMessage(error));
    } finally {
      setPending(false);
    }
  }

  if (status === 'loading') {
    return <p role="status" className="p-8">Checking your session…</p>;
  }

  // if already signed in, be redirected away form this page.
  if (status === 'authenticated') {
    return <Navigate to={destination} replace />;
  }

    return (
    <AuthLayout heading="Welcome back to Póca!" title="Sign in">
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {location.state?.registered && (
          <p role="status" className="text-sm">Account created. Sign in to continue.</p>
        )}
        {serverError && <p role="alert" className="text-sm text-destructive">{serverError}</p>}
        <FormField id="email" label="Email" type="email" autoComplete="username"
          value={email} onChange={setEmail} error={errors.email} disabled={pending} />
        <FormField id="password" label="Password" type="password"
          autoComplete="current-password" value={password} onChange={setPassword}
          error={errors.password} disabled={pending} />
        <Button type="submit" className="min-h-11 w-full" disabled={pending}>
          {pending ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>
      <p className="mt-5 text-sm">
        New here?{' '}
        <Link to="/register" className="underline underline-offset-4">Create an account</Link>
      </p>
    </AuthLayout>
  );
}