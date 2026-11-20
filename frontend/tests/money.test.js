import assert from 'node:assert/strict';
import { test } from 'node:test';
import { currencyLabel, formatMoney } from '../src/lib/money.js';

test('given decimal amounts, when formatted, then cents are preserved', () => {
  const amount = '12345678901234567.89';
  assert.match(formatMoney(amount, 'EUR'), /12,345,678,901,234,567\.89/);
});

test('given supported currencies, labels are readable and amounts include unambiguous codes', () => {
  for (const currency of ['EUR', 'USD', 'GBP']) {
    assert.match(currencyLabel(currency), new RegExp(`^${currency} — .+`));
    assert.match(formatMoney('42.50', currency), new RegExp(`${currency}.*42\\.50`));
  }
});
