import assert from "node:assert/strict";
import { test, afterEach } from "node:test";
import { acceptLogin, logout } from "../src/lib/authSession.js";
import { buy, createPortfolio, getHoldings, getPortfolios, getTrades, getValuation, sell } from "../src/api/portfolio.js";

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

test("buy and sell post ticker and quantity as JSON with bearer authentication", async (t) => {
  login();
  const calls = [];
  t.mock.method(globalThis, "fetch", async (url, options) => {
    calls.push({ url, options });
    return Response.json({ id: 3, ticker: "AAPL", quantity: 2, avgPurchasePrice: 190, currency: "USD" });
  });

  await buy(5, "AAPL", 2);
  await sell(5, "AAPL", 1.5);

  assert.deepEqual(calls.map((call) => call.url), [
    "/portfolio-api/api/portfolios/5/buy",
    "/portfolio-api/api/portfolios/5/sell",
  ]);
  for (const { options } of calls) {
    assert.equal(options.method, "POST");
    assert.equal(options.headers.Authorization, "Bearer owner-token");
    assert.equal(options.headers["Content-Type"], "application/json");
  }
  assert.deepEqual(JSON.parse(calls[0].options.body), { ticker: "AAPL", quantity: 2 });
  assert.deepEqual(JSON.parse(calls[1].options.body), { ticker: "AAPL", quantity: 1.5 });
});

test("buy sends price and executedAt only when recording an earlier purchase", async (t) => {
  login();
  const bodies = [];
  t.mock.method(globalThis, "fetch", async (url, options) => {
    bodies.push(JSON.parse(options.body));
    return Response.json({ id: 3, ticker: "AAPL", quantity: 3, avgPurchasePrice: 180, currency: "USD" });
  });

  await buy(5, "AAPL", 3, { price: 180, executedAt: "2026-11-17T14:32:00.000Z" });
  await buy(5, "AAPL", 1, { price: undefined, executedAt: undefined });

  assert.deepEqual(bodies[0], { ticker: "AAPL", quantity: 3, price: 180, executedAt: "2026-11-17T14:32:00.000Z" });
  assert.deepEqual(bodies[1], { ticker: "AAPL", quantity: 1 });
});

test("createPortfolio posts the name and returns the created portfolio", async (t) => {
  login();
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/portfolio-api/api/portfolios");
    assert.equal(options.method, "POST");
    assert.deepEqual(JSON.parse(options.body), { name: "Growth" });
    return Response.json({ id: 9, name: "Growth" }, { status: 201 });
  });
  assert.deepEqual(await createPortfolio("Growth"), { id: 9, name: "Growth" });
});

test("trades endpoint returns an array", async (t) => {
  login();
  t.mock.method(globalThis, "fetch", async (url) => {
    assert.equal(url, "/portfolio-api/api/portfolios/5/trades");
    return Response.json([{ id: 1, ticker: "AAPL", type: "BUY", quantity: 2, price: 190, executedAt: "2026-11-18T10:00:00Z" }]);
  });
  assert.equal((await getTrades(5)).length, 1);
});

test("backend error messages and codes are surfaced to the user", async (t) => {
  login();
  t.mock.method(globalThis, "fetch", async () =>
    Response.json({ error: "INSUFFICIENT_SHARES", message: "Cannot sell 5 AAPL: only 2 held" }, { status: 409 })
  );
  await assert.rejects(sell(5, "AAPL", 5), (error) => {
    assert.equal(error.message, "Cannot sell 5 AAPL: only 2 held");
    assert.equal(error.code, "INSUFFICIENT_SHARES");
    assert.equal(error.status, 409);
    return true;
  });
  t.mock.restoreAll();
  t.mock.method(globalThis, "fetch", async () => new Response("", { status: 500 }));
  await assert.rejects(buy(5, "AAPL", 1), /Portfolio request failed \(500\)/);
});
