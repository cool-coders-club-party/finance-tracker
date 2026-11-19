import assert from 'node:assert/strict';
import { test } from 'node:test';
import { safeRedirectPath } from '../src/lib/redirect.js';

test('a normal in-app path is kept, including its query string', () => {
  assert.equal(safeRedirectPath('/portfolio'), '/portfolio');
  assert.equal(safeRedirectPath('/transactions?page=2'), '/transactions?page=2');
});

test('a missing or non-string value falls back to the home page', () => {
  assert.equal(safeRedirectPath(undefined), '/');
  assert.equal(safeRedirectPath(null), '/');
  assert.equal(safeRedirectPath(42), '/');
});

test('values that could leave the app are rejected', () => {
  assert.equal(safeRedirectPath('https://malicious.example'), '/');
  assert.equal(safeRedirectPath('//malicious.example'), '/');
  assert.equal(safeRedirectPath('/\\malicious.example'), '/');
  assert.equal(safeRedirectPath('portfolio'), '/');
});

test('login and register paths are rejected so we never loop', () => {
  assert.equal(safeRedirectPath('/login'), '/');
  assert.equal(safeRedirectPath('/register'), '/');
});