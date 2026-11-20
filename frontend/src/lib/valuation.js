/**
 * Formats a supplied amount with its explicit currency; never substitutes zero for missing data.
 *
 * @param {number|string|null|undefined} value amount to display.
 * @param {string|null|undefined} currency currency label supplied with the amount.
 * @return {string} Amount with two decimal places and currency, or an em dash if unavailable.
 */
export function formatMoney(value, currency) {
  if (value == null || !currency || !Number.isFinite(Number(value))) return "—";
  return `${Number(value).toLocaleString("en-IE", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`;
}

/**
 * Translates backend valuation reason codes into readable explanations.
 *
 * @param {string|null|undefined} reason backend reason code.
 * @return {string} The matching explanation, or a generic unavailable message.
 */
export function valuationReason(reason) {
  return {
    QUOTE_UNAVAILABLE: "Current quote unavailable",
    INVALID_QUOTE: "Quote metadata unavailable",
    CURRENCY_MISSING: "Purchase or quote currency unavailable",
    CURRENCY_UNSUPPORTED: "Currency cannot be valued in USD",
  }[reason] ?? "Valuation unavailable";
}
