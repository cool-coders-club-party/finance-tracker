import test from "node:test";
import assert from "node:assert/strict";
import { formatMoney, valuationReason } from "../src/lib/valuation.js";

test("missing totals and currency never look like zero USD", () => {
  assert.equal(formatMoney(null, "USD"), "—");
  assert.equal(formatMoney(100, null), "—");
  assert.equal(formatMoney(undefined, "USD"), "—");
  assert.equal(formatMoney("invalid", "USD"), "—");
});
test("zero values and losses retain their currency", () => {
  assert.equal(formatMoney(0, "USD"), "0.00 USD");
  assert.equal(formatMoney(-12.5, "USD"), "-12.50 USD");
});
test("unavailable reasons explain currency and quote failures", () => {
  assert.match(valuationReason("CURRENCY_MISSING"), /currency unavailable/);
  assert.match(valuationReason("CURRENCY_UNSUPPORTED"), /cannot be valued/);
  assert.match(valuationReason("QUOTE_UNAVAILABLE"), /quote unavailable/);
});
