import assert from "node:assert/strict";
import { test, afterEach } from "node:test";
import { acceptLogin, logout } from "../src/lib/authSession.js";
import { getValuation, getHoldings, getPortfolios } from "../src/api/portfolio.js";

afterEach(() => logout());
function login() {
  acceptLogin({ accessToken: "owner-token", tokenType: "Bearer", expiresIn: 900, id: "owner", email: "owner@example.com" });
}

test("valuation sends bearer authentication and preserves null totals and stale metadata", async (t) => {
  login();
  const data = {
    complete: false, stale: true, unavailableHoldings: 1, currency: "USD", currentValue: null,
    holdings: [{ id: 1, ticker: "AAPL", avgPurchasePrice: 100, currency: null,
      valuationAvailable: false, unavailableReason: "CURRENCY_MISSING",
      quote: { price: 125, currency: "USD", asOf: "2026-11-16T12:00:00Z", stale: true } }],
  };
  const controller = new AbortController();
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/portfolio-api/api/portfolios/5/valuation");
    assert.equal(options.headers.Authorization, "Bearer owner-token");
    assert.equal(options.signal, controller.signal);
    return Response.json(data);
  });
  assert.deepEqual(await getValuation(5, controller.signal), data);
});

test("existing portfolio and holdings endpoints still return arrays", async (t) => {
  login();
  const urls = [];
  t.mock.method(globalThis, "fetch", async (url) => {
    urls.push(url);
    return Response.json([]);
  });
  assert.deepEqual(await getPortfolios(), []);
  assert.deepEqual(await getHoldings(5), []);
  assert.deepEqual(urls, ["/portfolio-api/api/portfolios", "/portfolio-api/api/portfolios/5/holdings"]);
});

test("malformed valuation payloads become readable errors", async (t) => {
  login();
  for (const payload of [null, [], { complete: true }, { holdings: [] }]) {
    t.mock.method(globalThis, "fetch", async () => Response.json(payload));
    await assert.rejects(getValuation(5), /unexpected valuation response/);
    t.mock.restoreAll();
  }
});

test("valuation preserves cancellation and reports service failures", async (t) => {
  login();
  t.mock.method(globalThis, "fetch", async () => { throw new DOMException("Aborted", "AbortError"); });
  await assert.rejects(getValuation(5), { name: "AbortError" });
  t.mock.restoreAll();
  t.mock.method(globalThis, "fetch", async () => new Response(null, { status: 502 }));
  await assert.rejects(getValuation(5), /Cannot reach Portfolio/);
  t.mock.restoreAll();
  t.mock.method(globalThis, "fetch", async () => { throw new TypeError("network failed"); });
  await assert.rejects(getValuation(5), /Cannot reach Portfolio/);
});
