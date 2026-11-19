import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { validateRegistration } from '../lib/validation.js';
import { register } from '../api/auth.js';
import { registrationErrorMessage } from '../lib/authMessages';

/** One labelled input with an optional error message underneath. */
function Field({ id, label, type, value, onChange, error, autoComplete }) {
  return (
    <div className="grid gap-2">
      <label htmlFor={id} className="text-sm">{label}</label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className="min-h-11 rounded-lg border border-input bg-background px-3 text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />
      {error && <p id={`${id}-error`} className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

export default function Register() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [errors, setErrors] = useState({}); // { email?, password?, confirmation? }

  function handleSubmit(event) {
    event.preventDefault(); // stop the browser reloading the page
    const found = validateRegistration({ email, password, confirmation });
    setErrors(found);
    if (Object.keys(found).length > 0) return; // invalid: show errors, send nothing
  }

  return (
    <main className="grid min-h-screen place-items-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Create account</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            <Field id="email" label="Email" type="email" autoComplete="email"
              value={email} onChange={setEmail} error={errors.email} />
            <Field id="password" label="Password (8 to 72 characters)" type="password"
              autoComplete="new-password" value={password} onChange={setPassword}
              error={errors.password} />
            <Field id="confirmation" label="Confirm password" type="password"
              autoComplete="new-password" value={confirmation} onChange={setConfirmation}
              error={errors.confirmation} />
            <Button type="submit" className="min-h-11 w-full">Create account</Button>
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