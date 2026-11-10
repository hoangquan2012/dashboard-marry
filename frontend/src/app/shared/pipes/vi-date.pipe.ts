import { Pipe, PipeTransform } from '@angular/core';

const ZONE = 'Asia/Ho_Chi_Minh';

const dateFmt = new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: ZONE });
const dateTimeFmt = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
  timeZone: ZONE
});
const timeFmt = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: ZONE });
const longFmt = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: ZONE });

function parse(value: string | Date | null | undefined): Date | null {
  if (!value) return null;
  if (value instanceof Date) return value;
  // plain yyyy-MM-dd must not be shifted by the timezone
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split('-').map(Number);
    return new Date(Date.UTC(y, m - 1, d, 12));
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** dd/MM/yyyy */
export function formatDate(value: string | Date | null | undefined): string {
  const d = parse(value);
  return d ? dateFmt.format(d) : '—';
}

/** dd/MM/yyyy HH:mm */
export function formatDateTime(value: string | Date | null | undefined): string {
  const d = parse(value);
  return d ? dateTimeFmt.format(d).replace(',', '') : '—';
}

export function formatTime(value: string | null | undefined): string {
  if (!value) return '';
  // backend sends HH:mm:ss for LocalTime
  if (/^\d{2}:\d{2}/.test(value)) return value.substring(0, 5);
  const d = parse(value);
  return d ? timeFmt.format(d) : '';
}

export function formatLongDate(value: string | Date | null | undefined): string {
  const d = parse(value);
  if (!d) return '—';
  const s = longFmt.format(d);
  return s.charAt(0).toUpperCase() + s.slice(1);
}

@Pipe({ name: 'viDate', standalone: true })
export class ViDatePipe implements PipeTransform {
  transform(value: string | Date | null | undefined, mode: 'date' | 'datetime' | 'time' | 'long' = 'date'): string {
    switch (mode) {
      case 'datetime':
        return formatDateTime(value);
      case 'time':
        return formatTime(value as string);
      case 'long':
        return formatLongDate(value);
      default:
        return formatDate(value);
    }
  }
}
