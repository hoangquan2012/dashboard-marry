import { Pipe, PipeTransform } from '@angular/core';

const formatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
const numberFormatter = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

/** Formats VND amounts the Vietnamese way: 1.000.000 ₫ */
export function formatVnd(value: number | string | null | undefined, emptyText = '—'): string {
  if (value === null || value === undefined || value === '') return emptyText;
  const n = typeof value === 'string' ? Number(value) : value;
  if (Number.isNaN(n)) return emptyText;
  return formatter.format(n);
}

/** Compact money for charts / cards: 1,2 tr; 85 tr; 1,25 tỷ */
export function formatVndShort(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  const abs = Math.abs(value);
  const sign = value < 0 ? '-' : '';
  if (abs >= 1_000_000_000) return `${sign}${trim((abs / 1_000_000_000).toFixed(2))} tỷ`;
  if (abs >= 1_000_000) return `${sign}${trim((abs / 1_000_000).toFixed(1))} tr`;
  if (abs >= 1_000) return `${sign}${trim((abs / 1_000).toFixed(0))} k`;
  return `${sign}${abs}`;
}

function trim(s: string): string {
  return s.replace(/\.0+$/, '').replace(/(\.\d*[1-9])0+$/, '$1').replace('.', ',');
}

export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) return '0';
  return numberFormatter.format(value);
}

@Pipe({ name: 'vnd', standalone: true })
export class VndPipe implements PipeTransform {
  transform(value: number | string | null | undefined, emptyText = '—'): string {
    return formatVnd(value, emptyText);
  }
}

@Pipe({ name: 'vndShort', standalone: true })
export class VndShortPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatVndShort(value);
  }
}

@Pipe({ name: 'viNumber', standalone: true })
export class ViNumberPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatNumber(value);
  }
}
