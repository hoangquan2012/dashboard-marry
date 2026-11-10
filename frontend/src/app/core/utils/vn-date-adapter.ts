import { Injectable } from '@angular/core';
import { MatDateFormats, NativeDateAdapter } from '@angular/material/core';

/**
 * Date adapter for Vietnamese dd/MM/yyyy input in Material datepickers.
 */
@Injectable()
export class VnDateAdapter extends NativeDateAdapter {
  override parse(value: unknown): Date | null {
    if (typeof value === 'string' && value.trim()) {
      const m = /^(\d{1,2})[\/.-](\d{1,2})[\/.-](\d{4})$/.exec(value.trim());
      if (m) {
        const d = Number(m[1]);
        const mo = Number(m[2]) - 1;
        const y = Number(m[3]);
        const date = new Date(y, mo, d);
        return date.getMonth() === mo ? date : this.invalid();
      }
      if (/^\d{4}-\d{2}-\d{2}$/.test(value.trim())) {
        const [y, mo, d] = value.trim().split('-').map(Number);
        return new Date(y, mo - 1, d);
      }
    }
    return super.parse(value);
  }

  override format(date: Date, displayFormat: object): string {
    const fmt = displayFormat as { month?: string };
    if (fmt.month === 'long') {
      return date.toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' });
    }
    const dd = String(date.getDate()).padStart(2, '0');
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    return `${dd}/${mm}/${date.getFullYear()}`;
  }

  override getFirstDayOfWeek(): number {
    return 1;
  }
}

export const VN_DATE_FORMATS: MatDateFormats = {
  parse: { dateInput: 'dd/MM/yyyy' },
  display: {
    dateInput: { day: '2-digit', month: '2-digit', year: 'numeric' },
    monthYearLabel: { month: 'long', year: 'numeric' },
    dateA11yLabel: { day: '2-digit', month: '2-digit', year: 'numeric' },
    monthYearA11yLabel: { month: 'long', year: 'numeric' }
  }
};

/** Date -> yyyy-MM-dd (local date, no timezone shift) for the API. */
export function toIsoDate(value: Date | string | null | undefined): string | null {
  if (!value) return null;
  if (typeof value === 'string') {
    return /^\d{4}-\d{2}-\d{2}/.test(value) ? value.substring(0, 10) : null;
  }
  const y = value.getFullYear();
  const m = String(value.getMonth() + 1).padStart(2, '0');
  const d = String(value.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/** yyyy-MM-dd -> Date (local) for form controls. */
export function fromIsoDate(value: string | null | undefined): Date | null {
  if (!value) return null;
  const [y, m, d] = value.substring(0, 10).split('-').map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

export function todayIso(): string {
  return toIsoDate(new Date())!;
}
