import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { GIFT_TYPE_OPTIONS, GiftType, SIDE_OPTIONS, Side } from '../../core/models/enums';
import { Gift, GiftRequest, Guest, GuestSummary } from '../../core/models/models';
import { GiftService } from '../../core/services/gift.service';
import { GuestService } from '../../core/services/guest.service';
import { NotificationService } from '../../core/services/notification.service';
import { fromIsoDate, toIsoDate, todayIso } from '../../core/utils/vn-date-adapter';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';

export interface GiftFormData {
  gift?: Gift;
  guest?: Guest | GuestSummary;
}

@Component({
  selector: 'app-gift-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatAutocompleteModule,
    MoneyInputDirective
  ],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.gift ? 'Sửa khoản tiền mừng' : 'Ghi nhận tiền mừng' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        <mat-form-field class="full">
          <mat-label>Người mừng</mat-label>
          <input matInput formControlName="giverName" [matAutocomplete]="auto" required (focus)="searchGuests()" />
          <mat-autocomplete #auto="matAutocomplete" [displayWith]="displayGuest" (optionSelected)="onGuest($event.option.value)">
            @for (g of suggestions(); track g.id) {
              <mat-option [value]="g">{{ g.fullName }} · {{ g.code }}{{ g.phone ? ' · ' + g.phone : '' }}</mat-option>
            }
          </mat-autocomplete>
          @if (selectedGuest()) {
            <mat-hint>Đã gắn với khách {{ selectedGuest()!.code }}</mat-hint>
          }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side">
            @for (o of sideOptions; track o.value) {
              <mat-option [value]="o.value">{{ o.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Hình thức</mat-label>
          <mat-select formControlName="giftType">
            @for (o of typeOptions; track o.value) {
              <mat-option [value]="o.value">{{ o.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>{{ needsItems() ? 'Giá trị quy đổi (tùy chọn)' : 'Số tiền' }}</mat-label>
          <input matInput appMoneyInput formControlName="amount" />
          <span matTextSuffix>₫</span>
          @if (needsItems()) {
            <mat-hint>Để trống nếu chưa biết giá trị. Hệ thống không tự đoán.</mat-hint>
          }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ngày nhận</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="receivedAt" required />
          <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Ghi chú</mat-label>
          <textarea matInput formControlName="note" rows="2"></textarea>
        </mat-form-field>

        @if (needsItems()) {
          <div class="full items">
            <div class="section-title">Vàng / hiện vật</div>
            <div formArrayName="items">
              @for (item of items.controls; track $index; let i = $index) {
                <div class="item-row" [formGroupName]="i">
                  <mat-form-field>
                    <mat-label>Tên / loại</mat-label>
                    <input matInput formControlName="itemName" placeholder="VD: Chỉ vàng 9999" />
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Số lượng</mat-label>
                    <input matInput type="number" formControlName="quantity" min="0" step="0.01" />
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Đơn vị</mat-label>
                    <input matInput formControlName="unit" placeholder="chỉ, hộp..." />
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Giá trị (tùy chọn)</mat-label>
                    <input matInput appMoneyInput formControlName="estimatedValue" />
                  </mat-form-field>
                  <button mat-icon-button type="button" (click)="removeItem(i)">
                    <mat-icon class="material-symbols-rounded">close</mat-icon>
                  </button>
                </div>
              }
            </div>
            <button mat-stroked-button type="button" (click)="addItem()">
              <mat-icon class="material-symbols-rounded">add</mat-icon>
              Thêm hiện vật
            </button>
          </div>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">
          {{ saving() ? 'Đang lưu...' : 'Lưu' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: [
    `
      .item-row { display: grid; grid-template-columns: 1.4fr 90px 90px 1fr 40px; gap: 8px; align-items: start; }
      @media (max-width: 640px) { .item-row { grid-template-columns: 1fr 1fr; } }
    `
  ]
})
export class GiftFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly giftService = inject(GiftService);
  private readonly guestService = inject(GuestService);
  private readonly notify = inject(NotificationService);
  readonly ref = inject(MatDialogRef<GiftFormDialogComponent>);
  readonly data = inject<GiftFormData>(MAT_DIALOG_DATA);

  readonly sideOptions = SIDE_OPTIONS;
  readonly typeOptions = GIFT_TYPE_OPTIONS;
  readonly saving = signal(false);
  readonly suggestions = signal<GuestSummary[]>([]);
  readonly selectedGuest = signal<GuestSummary | Guest | null>(this.data.guest ?? (this.data.gift?.guestId
    ? { id: this.data.gift.guestId, code: this.data.gift.guestCode ?? '', fullName: this.data.gift.giverName, phone: this.data.gift.guestPhone, side: this.data.gift.side, groupName: this.data.gift.groupName }
    : null));

  readonly form = this.fb.group({
    guestId: this.fb.control<number | null>(this.data.gift?.guestId ?? this.data.guest?.id ?? null),
    giverName: this.fb.nonNullable.control(this.data.gift?.giverName ?? this.data.guest?.fullName ?? '', Validators.required),
    side: this.fb.control<Side | null>(this.data.gift?.side ?? this.data.guest?.side ?? 'GROOM'),
    eventId: this.fb.control<number | null>(this.data.gift?.eventId ?? null),
    giftType: this.fb.nonNullable.control<GiftType>(this.data.gift?.giftType ?? 'CASH'),
    amount: this.fb.control<number | null>(this.data.gift?.amount ?? null),
    receivedAt: this.fb.control<Date | null>(fromIsoDate(this.data.gift?.receivedAt) ?? new Date(), Validators.required),
    note: this.fb.control<string | null>(this.data.gift?.note ?? null),
    items: this.fb.array((this.data.gift?.items ?? []).map((i) => this.itemGroup(i.itemName, i.quantity, i.unit, i.estimatedValue)))
  });

  constructor() {
    this.form.controls.giverName.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), switchMap((q) => this.guestService.lookup(q ?? '', 12)))
      .subscribe({ next: (list) => this.suggestions.set(list) });
  }

  get items(): FormArray {
    return this.form.controls.items;
  }

  needsItems(): boolean {
    const t = this.form.controls.giftType.value;
    return t === 'GOLD' || t === 'IN_KIND' || t === 'OTHER';
  }

  displayGuest = (v: GuestSummary | string | null): string => {
    if (!v) return '';
    if (typeof v === 'string') return v;
    return v.fullName;
  };

  searchGuests(): void {
    const q = this.form.controls.giverName.value;
    if (typeof q === 'string' && q.trim()) {
      this.guestService.lookup(q, 12).subscribe({ next: (list) => this.suggestions.set(list) });
    }
  }

  onGuest(g: GuestSummary): void {
    this.selectedGuest.set(g);
    this.form.patchValue({ guestId: g.id, giverName: g.fullName, side: g.side });
  }

  addItem(): void {
    this.items.push(this.itemGroup('', 1, null, null));
  }

  removeItem(i: number): void {
    this.items.removeAt(i);
  }

  private itemGroup(itemName: string, quantity: number, unit: string | null, estimatedValue: number | null) {
    return this.fb.group({
      itemName: this.fb.nonNullable.control(itemName, Validators.required),
      quantity: this.fb.nonNullable.control(quantity, [Validators.required, Validators.min(0)]),
      unit: this.fb.control<string | null>(unit),
      estimatedValue: this.fb.control<number | null>(estimatedValue)
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const giver = typeof v.giverName === 'string' ? v.giverName.trim() : this.selectedGuest()?.fullName ?? '';
    const request: GiftRequest = {
      guestId: v.guestId,
      giverName: giver || null,
      side: v.side,
      eventId: this.data.gift?.eventId ?? null,
      giftType: v.giftType,
      amount: v.amount,
      receivedAt: toIsoDate(v.receivedAt) ?? todayIso(),
      note: v.note?.trim() || null,
      items: this.needsItems()
        ? v.items
            .filter((i) => i.itemName.trim())
            .map((i) => ({
              itemName: i.itemName.trim(),
              quantity: Number(i.quantity) || 1,
              unit: i.unit?.trim() || null,
              estimatedValue: i.estimatedValue
            }))
        : []
    };
    this.saving.set(true);
    const call = this.data.gift ? this.giftService.update(this.data.gift.id, request) : this.giftService.create(request);
    call.subscribe({
      next: (gift) => {
        this.notify.success(this.data.gift ? 'Đã cập nhật khoản mừng' : `Đã ghi nhận mừng cưới của ${gift.giverName}`);
        this.ref.close(gift);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.httpError(err, 'Không lưu được khoản tiền mừng');
      }
    });
  }
}
