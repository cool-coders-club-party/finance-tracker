const STORAGE_KEY = 'finance-tracker:transaction-user';

export function isValidUserId(value) {
  return typeof value === 'string' && /^[1-9]\d{0,18}$/.test(value)
    && BigInt(value) <= 9223372036854775807n;
}

export function readTransactionUser() {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return isValidUserId(value) ? value : '';
  } catch {
    return '';
  }
}

export function rememberTransactionUser(value) {
  if (!isValidUserId(value)) return;
  try {
    localStorage.setItem(STORAGE_KEY, value);
  } catch {
    // Selection still works when browser storage is unavailable.
  }
}
