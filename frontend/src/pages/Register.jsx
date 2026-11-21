import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from '../components/AuthLayout';
import { Button } from '../components/ui/button';
import { register } from '../api/auth.js';
import { registrationErrorMessage } from '../lib/authMessages.js';
import { validateRegistration } from '../lib/validation.js';
import FormField from '../components/FormField';
import ErrorTooltip from '@/components/ErrorTooltip';

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
    <AuthLayout heading="Welcome to Póca!" title="Create account">
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        <FormField id="email" label="Email" type="email" autoComplete="email"
          value={email} onChange={setEmail} error={errors.email} disabled={pending} />
        <FormField id="password" label="Password (8 to 72 characters)" type="password"
          autoComplete="new-password" value={password} onChange={setPassword}
          error={errors.password} disabled={pending} />
        <FormField id="confirmation" label="Confirm password" type="password"
          autoComplete="new-password" value={confirmation} onChange={setConfirmation}
          error={errors.confirmation} disabled={pending} />
        <div className="relative">
          <Button type="submit" className="min-h-11 w-full" disabled={pending}>
            {pending ? 'Creating account…' : 'Create account'}
          </Button>
          {serverError && <ErrorTooltip id="form-error">{serverError}</ErrorTooltip>}
        </div>
      </form>
      <p className="mt-5 text-sm">
        Already have an account?{' '}
        <Link to="/login" className="underline underline-offset-4">Sign in</Link>
      </p>
    </AuthLayout>
  );
}