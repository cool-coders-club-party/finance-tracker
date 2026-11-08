import assert from 'node:assert/strict';
import { test } from 'node:test';
import { localDateTime, transactionDate, occurredAtForDate } from '../src/lib/transactionDate.js';

const now = new Date(2026, 10, 8, 12, 30, 45);

test('new transactions default to the local calendar date and retain automatic time', () => {
  assert.equal(transactionDate(undefined, now), '2026-11-08');
  assert.equal(localDateTime(now), '2026-11-08T12:30:45');
  assert.equal(occurredAtForDate('2026-11-07', undefined, now), '2026-11-07T12:30:45');
});

test('reading the recorded date never converts it to another timezone', () => {
  assert.equal(transactionDate('2026-01-02T00:15:00'), '2026-01-02');
});

test('unchanged dates are omitted from patches, preserving timestamp precision', () => {
  assert.equal(occurredAtForDate('2026-11-01', '2026-11-01T23:15:30.123456', now), undefined);
});

test('date edits preserve the original time and fractional seconds', () => {
  assert.equal(occurredAtForDate('2026-11-02', '2026-11-01T23:15:30.123456', now), '2026-11-02T23:15:30.123456');
});

test('moving to today caps the saved time at now to satisfy past-or-present validation', () => {
  assert.equal(occurredAtForDate('2026-11-08', '2026-11-01T23:15:30', now), '2026-11-08T12:30:45');
});

test('empty, impossible and future dates are rejected; leap days are accepted', () => {
  for (const date of ['', 'invalid', '2026-02-30', '2026-02-29', '2026-11-09']) {
    assert.throws(() => occurredAtForDate(date, undefined, now), /valid transaction date/);
  }
  assert.equal(occurredAtForDate('2024-02-29', undefined, now), '2024-02-29T12:30:45');
});
