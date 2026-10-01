export const API_BASE = '/api/v1';

export function money(value: number | null | undefined): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value ?? 0);
}

export function pct(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—';
  }
  return `${Number(value).toFixed(2)}%`;
}
