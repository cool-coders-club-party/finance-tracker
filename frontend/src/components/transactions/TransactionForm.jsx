import { useEffect, useId, useRef, useState } from 'react';
import { Button } from '../ui/button';
import { Link } from 'react-router-dom';
import { Loader2, Plus, Save } from 'lucide-react';
import { saveTransaction } from '../../api/transactions';
import { categoriesForType, categoryAfterTypeChange, formatEnumLabel } from '../../lib/transactionMetadata';

const inputClass = 'min-h-11 w-full rounded-lg border border-input bg-background/70 px-3 py-2 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground hover:border-ring/60 focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive';

function localDateTimeNow() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

function initialValues(transaction) {
  return {
    userId: String(transaction?.userId ?? ''),
    transactionType: transaction?.transactionType ?? '',
    transactionCategory: transaction?.transactionCategory ?? '',
    amount: String(transaction?.amount ?? ''),
    description: transaction?.description ?? '',
  };
}

// Both create and edit use the same metadata-driven fields and validation.
export default function TransactionForm({ metadata, transaction = null, onSaved }) {
  const id = useId();
  const [values, setValues] = useState(() => initialValues(transaction));
  const [feedback, setFeedback] = useState({ message: '', details: {} });
  const [isSaving, setIsSaving] = useState(false);
  const pendingRequest = useRef(null);
  useEffect(() => () => pendingRequest.current?.abort(), []);

  const categories = categoriesForType(metadata, values.transactionType);
  const pairIsValid = categories.some(({ name }) => name === values.transactionCategory);

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
    if (!pairIsValid || pendingRequest.current) return;
    const original = initialValues(transaction);
    const editable = ['transactionType', 'transactionCategory', 'amount', 'description'];
    const payload = Object.fromEntries(editable
      .filter((key) => !transaction || values[key] !== original[key])
      .map((key) => [key, values[key]]));
    if (!transaction) {
      payload.userId = values.userId;
      // The API requires occurredAt on creation; edits must preserve the stored value.
      payload.occurredAt = localDateTimeNow();
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
      if (!controller.signal.aborted) onSaved(saved);
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

  const fields = [
    ...(!transaction ? [{ name: 'userId', label: 'User ID', type: 'text', inputMode: 'numeric', pattern: '[1-9][0-9]*', required: true }] : []),
    { name: 'amount', label: 'Amount', type: 'text', inputMode: 'decimal', pattern: '[0-9]{1,17}(\\.[0-9]{1,2})?', required: true },
  ];

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {feedback.message && (
        <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
          {feedback.message}
        </div>
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
        <label className="grid content-start gap-2 text-sm font-medium sm:col-span-2">
          <span>Description (optional)</span>
          <textarea name="description" maxLength={500} rows={3} value={values.description} onChange={updateField} className={`${inputClass} min-h-28 resize-y`}
            aria-invalid={Boolean(feedback.details.description)} aria-describedby={feedback.details.description ? `${id}-description-error` : undefined} />
          {fieldError('description')}
        </label>
      </fieldset>
      <div className="flex flex-col-reverse gap-3 border-t border-border/60 pt-5 sm:flex-row sm:items-center sm:justify-end">
        <Button asChild variant="ghost" className="h-11 px-5">
          <Link to="/dashboard">Cancel</Link>
        </Button>
        <Button type="submit" className="h-11 gap-2 px-5" disabled={isSaving || !pairIsValid}>
          {isSaving ? <Loader2 aria-hidden="true" className="animate-spin motion-reduce:animate-none" /> : transaction ? <Save aria-hidden="true" /> : <Plus aria-hidden="true" />}
          {isSaving ? 'Saving…' : transaction ? 'Save changes' : 'Create transaction'}
        </Button>
      </div>
    </form>
  );
}
