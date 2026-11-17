// Never format absent values as zero or silently infer a currency.
export function formatMoney(value, currency) {
  if (value == null || !currency || !Number.isFinite(Number(value))) return "—";
  return `${Number(value).toLocaleString("en-IE", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`;
}

export function valuationReason(reason) {
  return {
    QUOTE_UNAVAILABLE: "Current quote unavailable",
    INVALID_QUOTE: "Quote metadata unavailable",
    CURRENCY_MISSING: "Purchase or quote currency unavailable",
    CURRENCY_UNSUPPORTED: "Currency cannot be valued in USD",
  }[reason] ?? "Valuation unavailable";
}
