import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  validateEmail, validateNewPassword, validateConfirmation, validateRegistration, validateLogin
} from '../src/lib/validation.js';

test('email: blank and badly formed values are rejected', () => {
  assert.match(validateEmail(''), /not be blank/i);
  assert.match(validateEmail('   '), /not be blank/i);
  assert.match(validateEmail('plainaddress'), /invalid/i);
  assert.match(validateEmail('a@b'), /invalid/i);
  assert.match(validateEmail('a b@c.com'), /invalid/i);
});

test('email: surrounding spaces are ignored', () => {
  assert.equal(validateEmail('  a@b.com  '), '');
});

test('password: blank is rejected', () => {
  assert.match(validateNewPassword(''), /required/i);
});

test('password: 8 to 72 characters are accepted, outside that is rejected', () => {
  assert.match(validateNewPassword('a'.repeat(7)), /at least 8/);
  assert.equal(validateNewPassword('a'.repeat(8)), '');
  assert.equal(validateNewPassword('a'.repeat(72)), '');
  assert.match(validateNewPassword('a'.repeat(73)), /no more than 72/);
});

test('confirmation must be present and match', () => {
  assert.match(validateConfirmation('secret123', ''), /confirm/i);
  assert.match(validateConfirmation('secret123', 'secret124'), /match/i);
  assert.equal(validateConfirmation('secret123', 'secret123'), '');
});

test('validateRegistration returns one message per bad field', () => {
  assert.deepEqual(
    validateRegistration({ email: 'a@b.com', password: 'secret123', confirmation: 'secret123' }),
    {},
  );
  const errors = validateRegistration({ email: 'nope', password: 'short', confirmation: 'other' });
  assert.deepEqual(Object.keys(errors).sort(), ['confirmation', 'email', 'password']);
});

test('login: only blank or badly formed values are rejected, not short passwords', () => {
  assert.deepEqual(validateLogin({ email: 'a@b.com', password: 'abc' }), {});
  assert.ok(validateLogin({ email: '', password: 'x' }).email);
  assert.ok(validateLogin({ email: 'a@b.com', password: '' }).password);
  assert.deepEqual(
    Object.keys(validateLogin({ email: 'nope', password: '' })).sort(),
    ['email', 'password'],
  );
});