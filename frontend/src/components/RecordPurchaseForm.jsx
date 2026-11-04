import { useState } from "react";

// Shared styling keeps every field consistent and usable on small screens.
const inputClass =
  "mt-1.5 h-11 w-full min-w-0 rounded-lg border border-white/15 bg-black/20 px-3 text-base text-white placeholder:text-white/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#b8dc89]/70 sm:text-sm";

export default function RecordPurchaseForm({
  ticker,
  onTickerChange,
  quantityInputRef,
  onSave,
}) {
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  // Keep decimal inputs as text while editing; calculate only a display preview here.
  const [quantityInput, setQuantityInput] = useState("");
  const [priceInput, setPriceInput] = useState("");
  const quantity = Number(quantityInput);
  const price = Number(priceInput);
  const purchaseCost = quantity * price;
  const costPreview =
    quantity > 0 && price > 0 && Number.isFinite(purchaseCost)
      ? new Intl.NumberFormat("en-US", {
          style: "currency",
          currency: "USD",
        }).format(purchaseCost)
      : "—";

  // datetime-local inputs use the browser's local timezone.
  // Display it so the user knows how their entered time will be interpreted.
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    // Read the inputs using their name attributes.
    const formData = new FormData(event.currentTarget);
    const ticker = String(formData.get("ticker")).trim().toUpperCase();
    const quantity = Number(formData.get("quantity"));
    const pricePerShare = Number(formData.get("pricePerShare"));
    const purchasedAt = new Date(String(formData.get("purchasedAt")));

    // Browser validation handles required fields.
    // These checks enforce additional rules before contacting the backend.
    if (!/^[A-Z]{1,5}$/.test(ticker)) {
      setError("Enter a ticker containing 1–5 letters.");
      return;
    }

    if (!Number.isFinite(quantity) || quantity <= 0) {
      setError("Enter a number of shares greater than zero.");
      return;
    }

    if (!Number.isFinite(pricePerShare) || pricePerShare <= 0) {
      setError("Enter a price per share greater than zero.");
      return;
    }

    if (
      !Number.isFinite(purchasedAt.getTime()) ||
      purchasedAt.getTime() > Date.now()
    ) {
      setError("Enter a valid purchase date and time that is not in the future.");
      return;
    }

    // Do not submit to the old endpoint before backend support is ready.
    if (typeof onSave !== "function") {
      setError("Purchase recording is not available yet.");
      return;
    }

    setIsSaving(true);

    try {
      // Convert local date/time to an unambiguous UTC timestamp.
      // onSave will be supplied by Portfolio.jsx when integration is ready.
      await onSave({
        ticker,
        quantity,
        pricePerShare,
        currency: "USD",
        purchasedAt: purchasedAt.toISOString(),
      });
    } catch (saveError) {
      // Keep the form mounted and preserve its inputs so the user can retry.
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
      <header>
        <h3 id="purchase-form-heading" className="text-base font-semibold text-white">
          Add a purchase
        </h3>
      </header>

      {/* A compact desktop row becomes two columns, then one, as space narrows. */}
      <fieldset
        disabled={isSaving}
        className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,0.8fr)_minmax(0,1.1fr)_minmax(0,1.8fr)]"
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

        <label className="min-w-0 text-sm text-white/85">
          Price / share (USD)
          <input
            name="pricePerShare"
            type="number"
            value={priceInput}
            onChange={(event) => setPriceInput(event.target.value)}
            min="0"
            step="any"
            placeholder="e.g. 300.00"
            required
            className={inputClass}
          />
        </label>

        <label className="min-w-0 text-sm text-white/85">
          Date &amp; time
          <span id="purchase-timezone" className="ml-2 text-xs text-white/75">
            {timezone}
          </span>
          <input
            name="purchasedAt"
            type="datetime-local"
            step="1"
            required
            aria-describedby="purchase-timezone"
            className={inputClass}
          />
        </label>
      </fieldset>

      {error && (
        <p role="alert" className="text-sm text-amber-400">
          {error}
        </p>
      )}

      {/* One quiet total line replaces a stack of repeated currency and helper notes. */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1">
          <span className="text-sm text-white/85">Total (USD)</span>
          <output className="text-lg font-semibold tabular-nums text-white">
            {costPreview}
          </output>
          <span className="text-xs text-white/75">excl. fees</span>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-3">
          {typeof onSave !== "function" && (
            <p id="purchase-save-status" className="text-xs text-amber-200">
              Saving unavailable
            </p>
          )}
          <button
            type="submit"
            disabled={isSaving || typeof onSave !== "function"}
            aria-describedby={typeof onSave !== "function" ? "purchase-save-status" : undefined}
            className="min-h-11 rounded-lg bg-[#b8dc89] px-4 py-2 text-sm font-semibold text-black disabled:opacity-50"
          >
            {isSaving ? "Saving…" : "Save purchase"}
          </button>
        </div>
      </div>
    </form>
  );
}
