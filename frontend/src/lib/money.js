const currencyNames = new Intl.DisplayNames(['en'], { type: 'currency' });

export function currencyLabel(currency) {
  const name = currencyNames.of(currency);
  return name && name !== currency ? `${currency} — ${name}` : currency;
}

// Normalize decimal amounts through integer cents before formatting.
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
  //Formats amounts using currency codes such as EUR, USD, GBP
  return new Intl.NumberFormat('en-IE', {
    style: 'currency', currency, currencyDisplay: 'code',
  }).format(decimal(cents(amount)));
}
