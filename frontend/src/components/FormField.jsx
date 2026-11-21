/**
 * One labelled input with an optional error message underneath.
 */
export default function FormField({ id, label, type, value, onChange, error, autoComplete, disabled }) {
  return (
    <div className="grid gap-2">
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
        className="min-h-11 rounded-lg border border-input bg-background/70 px-3 text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />
      {error && <p id={`${id}-error`} className="text-sm text-destructive">{error}</p>}
    </div>
  );
}