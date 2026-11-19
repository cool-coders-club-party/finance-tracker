import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";
import { getTopStocks } from "../src/api/marketData.js";

const snapshot = JSON.parse(await readFile(new URL(
  "../../backend/services/market-data-service/src/main/resources/top-stocks-snapshot.json",
  import.meta.url,
), "utf8"));

test("top stocks accepts a dated snapshot without expiry, even years after capture", async (t) => {
  const historical = { ...snapshot, capturedOn: "2000-01-01" };
  const controller = new AbortController();
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/market-data/api/rankings/top-stocks");
    assert.equal(options.signal, controller.signal);
    return Response.json(historical);
  });

  assert.deepEqual(await getTopStocks(controller.signal), historical);
});

test("top stocks rejects incomplete rankings and invalid capture dates", async (t) => {
  for (const payload of [
    { ...snapshot, rows: snapshot.rows.slice(0, 4) },
    { ...snapshot, capturedOn: "invalid" },
  ]) {
    t.mock.method(globalThis, "fetch", async () => Response.json(payload));
    await assert.rejects(getTopStocks(), /Invalid top stocks snapshot/);
    t.mock.restoreAll();
  }
});

test("top stocks reports an unavailable backend", async (t) => {
  t.mock.method(globalThis, "fetch", async () => new Response(null, { status: 503 }));
  await assert.rejects(getTopStocks(), /Ranking unavailable/);
});
