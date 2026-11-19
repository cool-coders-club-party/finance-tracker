import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { register } from '../api/auth.js';
import { registrationErrorMessage } from '../lib/authMessages.js';
import { validateRegistration } from '../lib/validation.js';

/** One labelled input with an optional error message underneath. */
function Field({ id, label, type, value, onChange, error, autoComplete, disabled }) {
  return (
    <div className="grid gap-2">
      <label htmlFor={id} className="text-sm">{label}</label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        disabled={disabled}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className="min-h-11 rounded-lg border border-input bg-background px-3 text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />
      {error && <p id={`${id}-error`} className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

export default function Register() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [errors, setErrors] = useState({});       // field-level, from our own validation
  const [serverError, setServerError] = useState(''); // from the server or the network
  const [pending, setPending] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (pending) return;

    const found = validateRegistration({ email, password, confirmation });
    setErrors(found);
    setServerError('');
    if (Object.keys(found).length > 0) return;

    setPending(true);
    try {
      await register(email, password);
      // 201: the account exists but we have no tokens, so the user signs in next.
      // We pass the email (never the password) so the login page can prefill it.
      navigate('/login', { state: { email: email.trim(), registered: true } });
    } catch (error) {
      setServerError(registrationErrorMessage(error));
      setPending(false);
    }
  }

  return (
    <main className="grid min-h-screen place-items-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Create account</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            {serverError && <p role="alert" className="text-sm text-destructive">{serverError}</p>}
            <Field id="email" label="Email" type="email" autoComplete="email"
              value={email} onChange={setEmail} error={errors.email} disabled={pending} />
            <Field id="password" label="Password (8 to 72 characters)" type="password"
              autoComplete="new-password" value={password} onChange={setPassword}
              error={errors.password} disabled={pending} />
            <Field id="confirmation" label="Confirm password" type="password"
              autoComplete="new-password" value={confirmation} onChange={setConfirmation}
              error={errors.confirmation} disabled={pending} />
            <Button type="submit" className="min-h-11 w-full" disabled={pending}>
              {pending ? 'Creating account…' : 'Create account'}
            </Button>
          </form>
          <p className="mt-5 text-sm">
            Already have an account?{' '}
            <Link to="/login" className="underline underline-offset-4">Sign in</Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}