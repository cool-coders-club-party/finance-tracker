import assert from 'node:assert/strict';
import { test } from 'node:test';
import { registrationErrorMessage, loginErrorMessage } from '../src/lib/authMessages.js';
import { AuthApiError } from '../src/api/auth.js';

test('409 tells the user the email is taken', () => {
  assert.match(registrationErrorMessage(new AuthApiError('x', 409)), /already exists/i);
});

test('400 asks the user to check their details', () => {
  assert.match(registrationErrorMessage(new AuthApiError('x', 400)), /check your email/i);
});

test('status 0 says the server cannot be reached', () => {
  assert.match(registrationErrorMessage(new AuthApiError('x', 0)), /cannot reach/i);
});

test('any other failure gets a generic message, even for odd errors', () => {
  assert.match(registrationErrorMessage(new AuthApiError('x', 500)), /failed/i);
  assert.match(registrationErrorMessage(new Error('boom')), /failed/i);
  assert.match(registrationErrorMessage(undefined), /failed/i);
});

test('login: 401 gives one message that does not say which part was wrong', () => {
  const message = loginErrorMessage(new AuthApiError('x', 401));
  assert.match(message, /incorrect/i);
  assert.doesNotMatch(message, /no account|not found|unknown/i);
});

test('login: other failures map to readable messages', () => {
  assert.match(loginErrorMessage(new AuthApiError('x', 0)), /cannot reach/i);
  assert.match(loginErrorMessage(new AuthApiError('x', 500)), /failed/i);
  assert.match(loginErrorMessage(undefined), /failed/i);
});