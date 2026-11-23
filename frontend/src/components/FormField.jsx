import ErrorTooltip from './ErrorTooltip';

/**
 * One labelled input. When `error` is set the field is outlined in red and a
 * tooltip with the message hangs just below it.
 */
export default function FormField({ id, label, type, value, onChange, error, autoComplete, disabled }) {
  return (
    <div className="relative grid gap-2">
      <label htmlFor={id} className="text-sm">{label}</label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        disabled={disabled}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`min-h-11 rounded-lg border bg-background/70 px-3 text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${
          error ? 'border-destructive' : 'border-input'
        }`}
      />
      {error && <ErrorTooltip id={`${id}-error`}>{error}</ErrorTooltip>}
    </div>
  );
}