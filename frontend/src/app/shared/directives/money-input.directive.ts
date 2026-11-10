import { Directive, ElementRef, HostListener, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Formats a numeric form control as Vietnamese currency while typing (1.000.000)
 * and keeps the control value as a plain number (VND, no decimals).
 *
 * <input matInput appMoneyInput formControlName="amount">
 */
@Directive({
  selector: 'input[appMoneyInput]',
  standalone: true,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MoneyInputDirective), multi: true }],
  host: { inputmode: 'numeric', autocomplete: 'off' }
})
export class MoneyInputDirective implements ControlValueAccessor {
  private readonly el = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private onChange: (value: number | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  writeValue(value: number | string | null): void {
    this.el.nativeElement.value = this.format(value);
  }

  registerOnChange(fn: (value: number | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.el.nativeElement.disabled = isDisabled;
  }

  @HostListener('input', ['$event'])
  onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/[^\d]/g, '');
    const num = digits ? Number(digits) : null;
    const caretFromEnd = input.value.length - (input.selectionStart ?? input.value.length);
    input.value = this.format(num);
    const pos = Math.max(0, input.value.length - caretFromEnd);
    input.setSelectionRange(pos, pos);
    this.onChange(num);
  }

  @HostListener('blur')
  onBlur(): void {
    this.onTouched();
  }

  private format(value: number | string | null): string {
    if (value === null || value === undefined || value === '') return '';
    const n = typeof value === 'string' ? Number(value.replace(/[^\d]/g, '')) : value;
    if (Number.isNaN(n)) return '';
    return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(n);
  }
}
