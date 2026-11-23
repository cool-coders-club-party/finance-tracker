import { useState } from "react";
import { Button } from "./ui/button";

// Shared styling keeps every field consistent and usable on small screens.
const inputClass =
  "mt-1.5 h-11 w-full min-w-0 rounded-lg border border-white/15 bg-black/20 px-3 text-base text-white placeholder:text-white/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#b8dc89]/70 sm:text-sm";

/**
 * Converts a date into the browser-local value expected by a datetime-local input.
 *
 * @param {Date} date valid instant to display in the user's timezone.
 * @return {string} Local date and time to minute precision, without a timezone suffix.
 */
function toLocalDateTime(date) {
  const localTime = date.getTime() - date.getTimezoneOffset() * 60_000;
  return new Date(localTime).toISOString().slice(0, 16);
}

/**
 * Formats a purchase preview using the supplied currency, defaulting to USD.
 *
 * @param {number} value preview amount.
 * @param {string} [currency] quote currency when available.
 * @return {string} The formatted preview amount.
 */
function formatAmount(value, currency) {
  return new Intl.NumberFormat("en-IE", {
    style: "currency",
    currency: currency || "USD",
  }).format(value);
}

/**
 * Collects a market-price purchase or an earlier purchase with its actual price and local date/time.
 * Quote estimates are display-only; the backend obtains the execution price for a current purchase.
 * The parent owns the ticker, while this form preserves other inputs across ticker changes.
 *
 * @param {object} props form values and callbacks.
 * @param {string} props.ticker parent-controlled ticker text.
 * @param {function(string): void} props.onTickerChange updates ticker text in the parent.
 * @param {import("react").RefObject<HTMLInputElement>} [props.quantityInputRef] allows the parent to focus the shares input.
 * @param {object|null} [props.quoteEstimate] quote with symbol, price and optional currency/asOf/stale metadata.
 * @param {function(object): Promise<*>} props.onSave saves ticker/quantity and, for earlier purchases, price/executedAt; rejects on failure.
 * @return {import("react").ReactElement} The purchase form and its validation/save feedback.
 */
export default function RecordPurchaseForm({
  ticker,
  onTickerChange,
  quantityInputRef,
  quoteEstimate,
  onSave,
}) {
  const [mode, setMode] = useState("now");
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  // Keep decimal inputs as text while editing.
  const [quantityInput, setQuantityInput] = useState("");
  const [priceInput, setPriceInput] = useState("");
  const [purchasedAtInput, setPurchasedAtInput] = useState("");

  const isEarlier = mode === "earlier";
  const symbol = ticker.trim().toUpperCase();
  // Only use the estimate while it still belongs to the ticker in the field.
  const estimate = quoteEstimate?.symbol === symbol ? quoteEstimate : null;
  const currency = estimate?.currency || "USD";

  const quantity = Number(quantityInput);
  const unitPrice = isEarlier ? Number(priceInput) : Number(estimate?.price);
  const total = quantity * unitPrice;
  const totalPreview =
    quantity > 0 && unitPrice > 0 && Number.isFinite(total) ? formatAmount(total, currency) : "—";

  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  /**
   * Validates the inputs, converts earlier purchase times to UTC and awaits the parent save callback.
   * Successful saves clear quantity and price; failures retain inputs and display an error.
   *
   * @param {import("react").FormEvent<HTMLFormElement>} event form submission event.
   * @return {Promise<void>} Resolves after validation or after save feedback has been applied.
   */
  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    // Mirror the backend's validation for a faster, clearer message.
    if (!/^[A-Z]{1,5}$/.test(symbol)) {
      setError("Enter a ticker containing 1–5 letters.");
      return;
    }

    if (!Number.isFinite(quantity) || quantity <= 0) {
      setError("Enter a number of shares greater than zero.");
      return;
    }

    const purchase = { ticker: symbol, quantity };

    if (isEarlier) {
      const price = Number(priceInput);
      const purchasedAt = new Date(purchasedAtInput);

      if (!Number.isFinite(price) || price <= 0) {
        setError("Enter the price per share you paid.");
        return;
      }
      if (!Number.isFinite(purchasedAt.getTime()) || purchasedAt.getTime() > Date.now()) {
        setError("Enter a purchase date and time that is not in the future.");
        return;
      }

      purchase.price = price;
      // Send an unambiguous UTC timestamp.
      purchase.executedAt = purchasedAt.toISOString();
    }

    setIsSaving(true);

    try {
      await onSave(purchase);
      // Clear the amounts so a second click can't repeat the same purchase by accident.
      setQuantityInput("");
      setPriceInput("");
    } catch (saveError) {
      // Keep the inputs so the user can retry.
      setError(saveError.message || "Could not save the purchase. Try again.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      aria-labelledby="purchase-form-heading"
      className="space-y-4 border-t border-white/10 pt-5"
    >
      <header className="space-y-3">
        <h3 id="purchase-form-heading" className="text-base font-semibold text-white">
          Add shares
        </h3>

        <fieldset disabled={isSaving} className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-white/85">
          <legend className="sr-only">How are you adding these shares?</legend>
          <label className="flex items-center gap-2">
            <input
              type="radio"
              name="purchase-mode"
              value="now"
              checked={!isEarlier}
              onChange={() => setMode("now")}
            />
            Buy now at market price
          </label>
          <label className="flex items-center gap-2">
            <input
              type="radio"
              name="purchase-mode"
              value="earlier"
              checked={isEarlier}
              onChange={() => {
                setMode("earlier");
                // Start from "now" so a same-day purchase only needs the time adjusted.
                if (!purchasedAtInput) setPurchasedAtInput(toLocalDateTime(new Date()));
              }}
            />
            Record an earlier purchase
          </label>
        </fieldset>

        <p className="text-xs text-white/75">
          {isEarlier
            ? "Enter the price you actually paid and when you bought, e.g. from your broker's confirmation."
            : "Shares are bought at the current market price when you confirm."}
        </p>
      </header>

      <fieldset
        disabled={isSaving}
        className={`grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2 ${isEarlier ? "lg:grid-cols-[minmax(0,0.8fr)_minmax(0,0.8fr)_minmax(0,1fr)_minmax(0,1.6fr)]" : ""}`}
      >
        <legend className="sr-only">Purchase details</legend>

        <label className="min-w-0 text-sm text-white/85">
          Ticker
          <input
            name="ticker"
            type="text"
            value={ticker}
            onChange={(event) => onTickerChange(event.target.value)}
            placeholder="e.g. AAPL"
            maxLength={5}
            required
            className={inputClass}
          />
        </label>

        <label className="min-w-0 text-sm text-white/85">
          Shares
          <input
            name="quantity"
            type="number"
            ref={quantityInputRef}
            value={quantityInput}
            onChange={(event) => setQuantityInput(event.target.value)}
            min="0"
            step="any"
            placeholder="e.g. 2"
            required
            className={inputClass}
          />
        </label>

        {isEarlier && (
          <>
            <label className="min-w-0 text-sm text-white/85">
              Price paid / share ({currency})
              <input
                name="price"
                type="number"
                value={priceInput}
                onChange={(event) => setPriceInput(event.target.value)}
                min="0"
                step="any"
                placeholder={estimate ? `today ≈ ${Number(estimate.price).toFixed(2)}` : "e.g. 180.00"}
                required
                className={inputClass}
              />
            </label>

            <label className="min-w-0 text-sm text-white/85">
              Date &amp; time bought
              <span id="purchase-timezone" className="ml-2 text-xs text-white/75">
                {timezone}
              </span>
              <input
                name="purchasedAt"
                type="datetime-local"
                value={purchasedAtInput}
                onChange={(event) => setPurchasedAtInput(event.target.value)}
                max={toLocalDateTime(new Date())}
                required
                aria-describedby="purchase-timezone"
                className={inputClass}
              />
            </label>
          </>
        )}
      </fieldset>

      {!isEarlier && estimate && (
        <span className="mt-1 block text-xs text-white/75">
          {estimate.stale ? "Cached quote" : "Quote"}: {Number(estimate.price).toLocaleString("en-IE", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })} {estimate.currency}
          {estimate.asOf && ` as of ${new Date(estimate.asOf).toLocaleString("en-IE")}`}.
          The final price may differ slightly.
        </span>
      )}

      {error && (
        <p role="alert" className="text-sm text-amber-400">
          {error}
        </p>
      )}

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1">
          <span className="text-sm text-white/85">{isEarlier ? "Total paid" : "Estimated total"}</span>
          <output className="text-lg font-semibold tabular-nums text-white">
            {totalPreview}
          </output>
          {!isEarlier && !estimate && (
            <span className="text-xs text-white/75">look up the stock to see an estimate</span>
          )}
        </div>

        {/* One submit button, using the shared Button styling. */}
        <Button type="submit" disabled={isSaving} className="h-11 px-4">
          {isSaving ? "Saving…" : isEarlier ? "Record purchase" : "Buy shares"}
        </Button>
      </div>
    </form>
  );
}
