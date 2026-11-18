import { useState } from "react";

const inputClass =
  "mt-1.5 h-11 w-full min-w-0 rounded-lg border border-white/15 bg-black/20 px-3 text-base text-white placeholder:text-white/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#b8dc89]/70 sm:text-sm";

// Sells part or all of one holding at the current market price.
export default function SellSharesForm({ holding, onSell, onCancel, quantityInputRef }) {
  const [quantityInput, setQuantityInput] = useState("");
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);

  const owned = Number(holding.quantity);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const quantity = Number(quantityInput);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setError("Enter a number of shares greater than zero.");
      return;
    }
    if (quantity > owned) {
      setError(`You only hold ${holding.quantity} ${holding.ticker} shares.`);
      return;
    }

    setIsSaving(true);
    try {
      await onSell({ ticker: holding.ticker, quantity });
    } catch (sellError) {
      // The backend still has the final say, e.g. INSUFFICIENT_SHARES (409).
      setError(sellError.message || "Could not sell shares. Try again.");
      setIsSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      aria-labelledby="sell-form-heading"
      className="space-y-4 border-t border-white/10 pt-5"
    >
      <header>
        <h3 id="sell-form-heading" className="text-base font-semibold text-white">
          Sell {holding.ticker}
        </h3>
        <p className="text-xs text-white/75">
          You hold {holding.quantity} shares. They are sold at the current market price.
        </p>
      </header>

      <fieldset disabled={isSaving} className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <legend className="sr-only">Sale details</legend>
        <label className="min-w-0 flex-1 text-sm text-white/85">
          Shares to sell
          <input
            name="quantity"
            type="number"
            ref={quantityInputRef}
            value={quantityInput}
            onChange={(event) => setQuantityInput(event.target.value)}
            min="0"
            max={holding.quantity}
            step="any"
            required
            className={inputClass}
          />
        </label>

        <button
          type="button"
          onClick={() => setQuantityInput(String(holding.quantity))}
          className="min-h-11 rounded-lg border border-white/20 px-4 py-2 text-sm"
        >
          Sell all
        </button>
        <button
          type="submit"
          className="min-h-11 rounded-lg bg-red-300 px-4 py-2 text-sm font-semibold text-black disabled:opacity-50"
        >
          {isSaving ? "Selling…" : "Confirm sale"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="min-h-11 rounded-lg px-4 py-2 text-sm text-white/80 underline"
        >
          Cancel
        </button>
      </fieldset>

      {error && (
        <p role="alert" className="text-sm text-amber-400">
          {error}
        </p>
      )}
    </form>
  );
}
