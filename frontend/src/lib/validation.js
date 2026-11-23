// A string of the form text@moreText.something is considered valid
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 72; // bcrypt max (in bytes), also enforced by the database

/** Returns an error message, or '' if the email is fine. */
export function validateEmail(email) {
  const value = email.trim();
  if (value === '') return 'Email can not be blank.';
  if (!EMAIL_PATTERN.test(value)) return 'Invalid email address.';
  return '';
}

/** Rules for choosing a new password (registration). */
export function validateNewPassword(password) {
  if (password === '') return 'A password is required.';
  if (password.length < PASSWORD_MIN) return `Password must be at least ${PASSWORD_MIN} characters.`;
  if (password.length > PASSWORD_MAX) return `Password must be no more than ${PASSWORD_MAX} characters.`;
  return '';
}

export function validateConfirmation(password, confirmation) {
  if (confirmation === '') return 'Please confirm your password.';
  if (confirmation !== password) return 'Passwords do not match.';
  return '';
}

/** 
 * Checks the whole registration form. Returns an object with one message per
 * field that has a problem, so an empty object means "valid". 
 */
export function validateRegistration({ email, password, confirmation }) {
  const errors = {};
  const emailError = validateEmail(email);
  if (emailError) errors.email = emailError;
  const passwordError = validateNewPassword(password);
  if (passwordError) errors.password = passwordError;
  const confirmationError = validateConfirmation(password, confirmation);
  if (confirmationError) errors.confirmation = confirmationError;
  return errors;
}

/** 
 * Rules for the login form. Only "not blank" for the password: the server
 * decides whether it is correct.
 */
export function validateLogin({ email, password }) {
  const errors = {};
  const emailError = validateEmail(email);
  if (emailError) errors.email = emailError;
  if (password === '') errors.password = 'Password is required.';
  return errors;
}