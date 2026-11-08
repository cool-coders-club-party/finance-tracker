import { useEffect, useId, useRef, useState } from 'react';
import { Button } from '../ui/button';
import { Link } from 'react-router-dom';
import { Loader2, Plus, Save } from 'lucide-react';
import { localDateTime, transactionDate, occurredAtForDate } from '../../lib/transactionDate';
import { currencyLabel, formatMoney } from '../../lib/money';
import { saveTransaction } from '../../api/transactions';
import { categoriesForType, categoryAfterTypeChange, formatEnumLabel } from '../../lib/transactionMetadata';

const inputClass = 'min-h-11 w-full rounded-lg border border-input bg-background/70 px-3 py-2 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground hover:border-ring/60 focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive';

function initialValues(transaction) {
  return {
    transactionType: transaction?.transactionType ?? '',
    transactionCategory: transaction?.transactionCategory ?? '',
    currency: transaction?.currency ?? '',
    amount: String(transaction?.amount ?? ''),
    description: transaction?.description ?? '',
    date: transactionDate(transaction?.occurredAt),
  };
}

// Both create and edit use the same metadata-driven fields and validation.
export default function TransactionForm({ metadata, transaction = null, onSaved, cancelTo = "/transactions" }) {
  const id = useId();
  const [values, setValues] = useState(() => initialValues(transaction));
  const [feedback, setFeedback] = useState({ message: '', details: {} });
  const [isSaving, setIsSaving] = useState(false);
  const pendingRequest = useRef(null);
  useEffect(() => () => pendingRequest.current?.abort(), []);

  const categories = categoriesForType(metadata, values.transactionType);
  const pairIsValid = categories.some(({ name }) => name === values.transactionCategory);

  const currencyIsValid = metadata.currencies.includes(values.currency);

  function updateField(event) {
    const { name, value } = event.target;
    setFeedback({ message: '', details: {} });
    setValues((current) => ({
      ...current,
      [name]: value,
      ...(name === 'transactionType' ? {
        transactionCategory: categoryAfterTypeChange(metadata, current.transactionCategory, value),
      } : {}),
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!pairIsValid || !currencyIsValid || pendingRequest.current) return;
    const original = initialValues(transaction);
    const editable = ['transactionType', 'transactionCategory', 'amount', 'currency', 'description'];
    const payload = Object.fromEntries(editable
      .filter((key) => !transaction || values[key] !== original[key])
      .map((key) => [key, values[key]]));
    try {
      const occurredAt = occurredAtForDate(values.date, transaction?.occurredAt);
      if (occurredAt !== undefined) payload.occurredAt = occurredAt;
    } catch (error) {
      setFeedback({ message: error.message, details: { occurredAt: error.message } });
      return;
    }
    if (!Object.keys(payload).length) {
      setFeedback({ message: 'No changes to save.', details: {} });
      return;
    }
    const controller = new AbortController();
    pendingRequest.current = controller;
    setIsSaving(true);
    setFeedback({ message: '', details: {} });
    try {
      const saved = await saveTransaction(payload, transaction?.id, controller.signal);
      if (!controller.signal.aborted) {
        onSaved(saved);
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        setFeedback({ message: error.message, details: error.details ?? {} });
      }
    } finally {
      pendingRequest.current = null;
      if (!controller.signal.aborted) setIsSaving(false);
    }
  }

  function fieldError(name) {
    const message = feedback.details[name];
    return message ? <p id={`${id}-${name}-error`} className="text-sm text-destructive">{String(message)}</p> : null;
  }

  let savedAmount = '';
  if (transaction) {
    try {
      savedAmount = formatMoney(transaction.amount, transaction.currency);
    } catch {
      savedAmount = 'Unavailable — check the transaction amount and currency.';
    }
  }

  const fields = [
    { name: 'amount', label: values.currency ? `Amount (${values.currency})` : 'Amount', type: 'text', inputMode: 'decimal', pattern: '[0-9]{1,17}(\\.[0-9]{1,2})?', required: true },
  ];

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {feedback.message && (
        <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
          {feedback.message}
        </div>
      )}
      {transaction && (
        <p className="text-sm text-muted-foreground">Saved amount: <span className="font-medium text-foreground">{savedAmount}</span></p>
      )}
      <fieldset disabled={isSaving} className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
        <legend className="sr-only">Transaction details</legend>
        <label className="grid content-start gap-2 text-sm font-medium">
          <span>Transaction type</span>
          <select className={inputClass} name="transactionType" required value={values.transactionType} onChange={updateField}
            aria-invalid={Boolean(feedback.details.transactionType)} aria-describedby={feedback.details.transactionType ? `${id}-transactionType-error` : undefined}>
            <option value="">Select a type</option>
            {metadata.types.map((type) => <option key={type} value={type}>{formatEnumLabel(type)}</option>)}
          </select>
          <span className="text-xs font-normal text-muted-foreground">Choose income or expense.</span>
          {fieldError('transactionType')}
        </label>
        <label className="grid content-start gap-2 text-sm font-medium">
          <span>Transaction category</span>
          <select className={inputClass} name="transactionCategory" required value={values.transactionCategory}
            onChange={updateField} disabled={!values.transactionType}
            aria-invalid={Boolean(feedback.details.transactionCategory)} aria-describedby={`${id}-category-help${feedback.details.transactionCategory ? ` ${id}-transactionCategory-error` : ''}`}>
            <option value="">{values.transactionType ? 'Select a category' : 'Select a type first'}</option>
            {categories.map(({ name }) => <option key={name} value={name}>{formatEnumLabel(name)}</option>)}
          </select>
          <span id={`${id}-category-help`} className="text-xs font-normal text-muted-foreground">Only categories available for the selected type are shown.</span>
          {fieldError('transactionCategory')}
        </label>
        {fields.map(({ name, label, ...attributes }) => (
          <label key={name} className="grid content-start gap-2 text-sm font-medium">
            <span>{label}</span>
            <input {...attributes} name={name} value={values[name]} onChange={updateField} className={inputClass}
              aria-invalid={Boolean(feedback.details[name])} aria-describedby={feedback.details[name] ? `${id}-${name}-error` : undefined} />
            {fieldError(name)}
          </label>
        ))}
        <label className="grid content-start gap-2 text-sm font-medium">
          <span>Currency</span>
          <select name="currency" required value={values.currency} onChange={updateField} className={inputClass}
            aria-invalid={Boolean(feedback.details.currency)}
            aria-describedby={`${id}-currency-help${feedback.details.currency ? ` ${id}-currency-error` : ''}`}>
            <option value="">Select a currency</option>
            {metadata.currencies.map((currency) => <option key={currency} value={currency}>{currencyLabel(currency)}</option>)}
          </select>
          <span id={`${id}-currency-help`} className="text-xs font-normal text-muted-foreground">
            {transaction ? 'Changing currency does not convert the amount.' : 'Choose the currency of the original amount.'}
          </span>
          {fieldError('currency')}
        </label>
        <label className="grid content-start gap-2 text-sm font-medium">
          <span>Transaction date</span>
          <input type="date" name="date" required min="0001-01-01" max={localDateTime().slice(0, 10)}
            value={values.date} onChange={updateField} className={inputClass}
            aria-invalid={Boolean(feedback.details.occurredAt)}
            aria-describedby={`${id}-date-help${feedback.details.occurredAt ? ` ${id}-occurredAt-error` : ''}`} />
          <span id={`${id}-date-help`} className="text-xs font-normal text-muted-foreground">When the transaction happened. This date is also used for currency conversion.</span>
          {fieldError('occurredAt')}
        </label>
        <label className="grid content-start gap-2 text-sm font-medium sm:col-span-2">
          <span>Description (optional)</span>
          <textarea name="description" maxLength={500} rows={3} value={values.description} onChange={updateField} className={`${inputClass} min-h-28 resize-y`}
            aria-invalid={Boolean(feedback.details.description)} aria-describedby={feedback.details.description ? `${id}-description-error` : undefined} />
          {fieldError('description')}
        </label>
      </fieldset>
      <div className="flex flex-col-reverse gap-3 border-t border-border/60 pt-5 sm:flex-row sm:items-center sm:justify-end">
        <Button asChild variant="ghost" className="h-11 px-5">
          <Link to={cancelTo}>Cancel</Link>
        </Button>
        <Button type="submit" className="h-11 gap-2 px-5" disabled={isSaving || !pairIsValid || !currencyIsValid}>
          {isSaving ? <Loader2 aria-hidden="true" className="animate-spin motion-reduce:animate-none" /> : transaction ? <Save aria-hidden="true" /> : <Plus aria-hidden="true" />}
          {isSaving ? 'Saving…' : transaction ? 'Save changes' : 'Create transaction'}
        </Button>
      </div>
    </form>
  );
}
