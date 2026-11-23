/**
 * Checks the dashboard's portfolio summary figures: totals policy, realised profit and allocation.
 */
import assert from "node:assert/strict";
import { test } from "node:test";
import { summarisePortfolio } from "../src/lib/portfolioSummary.js";

function holding(ticker, quantity, currentValue, valuationAvailable = true) {
  return { id: ticker, ticker, quantity, currentValue, valuationAvailable };
}

const complete = {
  currency: "USD", complete: true, stale: false, unavailableHoldings: 0,
  costBasis: 2000, currentValue: 2500, unrealisedGainLoss: 500,
  holdings: [holding("TSLA", 2, 500), holding("AAPL", 10, 2000), holding("MSFT", 0, 0)],
};

test("totals and unrealised percentage come from a complete valuation", () => {
  const summary = summarisePortfolio(complete, []);
  assert.equal(summary.currentValue, 2500);
  assert.equal(summary.unrealised, 500);
  assert.equal(summary.unrealisedPercent, 25);
  assert.equal(summary.complete, true);
});

test("allocation is largest first, shares add to 100, and sold-out holdings are left out", () => {
  const { allocation, excludedFromAllocation } = summarisePortfolio(complete, []);
  assert.deepEqual(allocation.map((item) => [item.ticker, item.share]), [["AAPL", 80], ["TSLA", 20]]);
  assert.equal(excludedFromAllocation, 0);
});

test("realised profit sums every stock, including closed positions", () => {
  const performance = [{ ticker: "AAPL", realizedPnl: 12.5 }, { ticker: "MSFT", realizedPnl: -2.5 }];
  assert.equal(summarisePortfolio(complete, performance).realised, 10);
});

test("realised profit is unavailable when performance couldn't be loaded", () => {
  assert.equal(summarisePortfolio(complete, null).realised, null);
});

test("an incomplete valuation keeps totals unavailable instead of summing what can be priced", () => {
  const incomplete = {
    ...complete, complete: false, unavailableHoldings: 1,
    costBasis: null, currentValue: null, unrealisedGainLoss: null,
    holdings: [holding("AAPL", 10, 2000), holding("XYZ", 3, null, false)],
  };
  const summary = summarisePortfolio(incomplete, []);
  assert.equal(summary.currentValue, null);
  assert.equal(summary.unrealised, null);
  assert.equal(summary.unrealisedPercent, null);
  // Allocation still shows what can be priced, and says how many were left out.
  assert.deepEqual(summary.allocation.map((item) => [item.ticker, item.share]), [["AAPL", 100]]);
  assert.equal(summary.excludedFromAllocation, 1);
});

test("an empty portfolio has zero totals and no allocation", () => {
  const empty = { ...complete, costBasis: 0, currentValue: 0, unrealisedGainLoss: 0, holdings: [] };
  const summary = summarisePortfolio(empty, []);
  assert.equal(summary.currentValue, 0);
  assert.equal(summary.unrealisedPercent, null);
  assert.deepEqual(summary.allocation, []);
  assert.equal(summary.realised, 0);
});
