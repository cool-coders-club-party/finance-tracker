import { formatEnumLabel } from './transactionMetadata.js';

const currencyNames = new Intl.DisplayNames(['en'], { type: 'currency' });

export function currencyLabel(currency) {
  const name = currencyNames.of(currency);
  return name && name !== currency ? `${currency} — ${name}` : currency;
}

// Keep arithmetic in integer cents; only chart geometry uses floating-point values.
function cents(amount) {
  if (typeof amount === 'number' && !Number.isSafeInteger(Math.round(amount * 100))) {
    throw new Error('An amount is too large to read accurately. Decimal amounts must be supplied as text.');
  }
  if ((typeof amount !== 'string' && typeof amount !== 'number')
      || !/^\d+(\.\d{1,2})?$/.test(String(amount))) {
    throw new Error('A transaction has an invalid amount.');
  }
  const [whole, fraction = ''] = String(amount).split('.');
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'));
}

function decimal(value) {
  return `${value / 100n}.${String(value % 100n).padStart(2, '0')}`;
}

export function formatMoney(amount, currency) {
  if (typeof currency !== 'string' || !/^[A-Z]{3}$/.test(currency)) {
    throw new Error('A transaction is missing a valid currency.');
  }
  return new Intl.NumberFormat('en-IE', {
    style: 'currency', currency, currencyDisplay: 'code',
  }).format(decimal(cents(amount)));
}

export function groupExpensesByCurrency(records) {
  const groups = new Map();
  for (const record of records) {
    if (record.transactionType !== 'EXPENSE') continue;
    const { currency, transactionCategory: category } = record;
    if (typeof currency !== 'string' || !/^[A-Z]{3}$/.test(currency)
        || typeof category !== 'string' || !/^[A-Z_]+$/.test(category)) {
      throw new Error('Some expenses are missing a valid currency or category. Totals cannot be shown.');
    }
    const amount = cents(record.amount);
    if (amount <= 0n) throw new Error('Expense amounts must be greater than zero.');
    if (!groups.has(currency)) groups.set(currency, new Map());
    const categories = groups.get(currency);
    categories.set(category, (categories.get(category) ?? 0n) + amount);
  }
  return [...groups].sort(([a], [b]) => a.localeCompare(b)).map(([currency, categories]) => {
    const total = [...categories.values()].reduce((sum, value) => sum + value, 0n);
    return {
      currency,
      total: decimal(total),
      categories: [...categories].sort(([a, x], [b, y]) => x === y ? a.localeCompare(b) : x > y ? -1 : 1)
        .map(([category, amount]) => ({
          key: category.toLowerCase(), label: formatEnumLabel(category), amount: decimal(amount),
          // Approximate ratios are for drawing only; labels always use exact amounts.
          share: Number(amount) / Number(total) * 100,
        })),
    };
  });
}
