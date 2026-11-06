import assert from 'node:assert/strict';
import { test } from 'node:test';
import { currencyLabel, formatMoney, groupExpensesByCurrency } from '../src/lib/money.js';

const expense = (currency, amount, transactionCategory = 'GROCERIES') => ({
  transactionType: 'EXPENSE', currency, amount, transactionCategory,
});

test('given mixed currencies, when grouped, then totals and categories stay separate', () => {
  const groups = groupExpensesByCurrency([
    expense('EUR', '10.10'), expense('USD', '20.00'), expense('EUR', '0.20'),
    expense('GBP', '5.00'), expense('USD', '30.00', 'HOUSING'),
    { ...expense('EUR', '1000.00'), transactionType: 'INCOME' },
  ]);
  assert.deepEqual(groups.map(({ currency, total }) => ({ currency, total })), [
    { currency: 'EUR', total: '10.30' }, { currency: 'GBP', total: '5.00' }, { currency: 'USD', total: '50.00' },
  ]);
  assert.deepEqual(groups[2].categories.map(({ label, amount, share }) => ({ label, amount, share })), [
    { label: 'Housing', amount: '30.00', share: 60 }, { label: 'Groceries', amount: '20.00', share: 40 },
  ]);
});

test('given decimal amounts, when summed and formatted, then cents are preserved', () => {
  assert.equal(groupExpensesByCurrency([expense('EUR', 0.1), expense('EUR', 0.2)])[0].total, '0.30');
  const amount = '12345678901234567.89';
  assert.match(formatMoney(amount, 'EUR'), /12,345,678,901,234,567\.89/);
  assert.equal(groupExpensesByCurrency([expense('EUR', amount), expense('EUR', '0.01')])[0].total, '12345678901234567.90');
});

test('given supported currencies, labels are readable and amounts include unambiguous codes', () => {
  for (const currency of ['EUR', 'USD', 'GBP']) {
    assert.match(currencyLabel(currency), new RegExp(`^${currency} — .+`));
    assert.match(formatMoney('42.50', currency), new RegExp(`${currency}.*42\\.50`));
  }
});

test('given incomplete or invalid expense data, totals fail rather than silently omit records', () => {
  for (const record of [expense(undefined, '1.00'), expense('EUR', null), expense('EUR', '-1.00'),
    expense('EUR', '0.00'), expense('EUR', '1.234'), expense('EUR', 1e17), expense('EUR', '1.00', '')]) {
    assert.throws(() => groupExpensesByCurrency([expense('EUR', '5.00'), record]));
  }
  assert.deepEqual(groupExpensesByCurrency([]), []);
});
