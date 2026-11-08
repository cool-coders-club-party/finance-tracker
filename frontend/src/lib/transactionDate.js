const pad = (value) => String(value).padStart(2, '0');

export function localDateTime(now = new Date()) {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

export function transactionDate(occurredAt, now = new Date()) {
  // LocalDateTime has no timezone: retain the recorded calendar date verbatim.
  return (occurredAt || localDateTime(now)).slice(0, 10);
}

export function occurredAtForDate(date, original, now = new Date()) {
  const current = localDateTime(now);
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  const parsed = new Date(`${date}T00:00:00`);
  if (!match || !Number.isFinite(parsed.getTime()) || transactionDate(undefined, parsed) !== date
      || date > current.slice(0, 10)) {
    throw new Error('Choose a valid transaction date on or before today.');
  }
  if (original && date === transactionDate(original)) return undefined;
  const candidate = `${date}T${(original || current).split('T')[1]}`;
  // Moving a past transaction to today must not create a future time.
  return candidate > current ? current : candidate;
}
