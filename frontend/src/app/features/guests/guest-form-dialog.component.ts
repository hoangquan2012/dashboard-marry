import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { concat, of, switchMap } from 'rxjs';
import { GUEST_CATEGORY_OPTIONS, GuestCategory, SIDE_OPTIONS, Side } from '../../core/models/enums';
import { GiftRequest, Guest, GuestRequest } from '../../core/models/models';
import { GiftService } from '../../core/services/gift.service';
import { GuestService } from '../../core/services/guest.service';
import { NotificationService } from '../../core/services/notification.service';
import { todayIso } from '../../core/utils/vn-date-adapter';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';

export interface GuestFormData {
  guest?: Guest;
}

@Component({
  selector: 'app-guest-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MoneyInputDirective
  ],
  templateUrl: './guest-form-dialog.component.html'
})
export class GuestFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(GuestService);
  private readonly giftService = inject(GiftService);
  private readonly notify = inject(NotificationService);
  private readonly ref = inject(MatDialogRef<GuestFormDialogComponent>);
  readonly data = inject<GuestFormData>(MAT_DIALOG_DATA);

  readonly sideOptions = SIDE_OPTIONS;
  readonly categoryOptions = GUEST_CATEGORY_OPTIONS;
  readonly saving = signal(false);

  readonly form = this.fb.group({
    fullName: this.fb.nonNullable.control(this.data.guest?.fullName ?? '', [Validators.required, Validators.maxLength(150)]),
    phone: this.fb.control<string | null>(this.data.guest?.phone ?? null, [Validators.pattern(/^[0-9+ .()-]*$/), Validators.maxLength(30)]),
    side: this.fb.nonNullable.control<Side>(this.data.guest?.side ?? 'GROOM', Validators.required),
    category: this.fb.nonNullable.control<GuestCategory>(this.data.guest?.category ?? 'FRIEND'),
    relationship: this.fb.control<string | null>(this.data.guest?.relationship ?? null),
    giftAmount: this.fb.control<number | null>(this.data.guest?.giftTotal || null),
    note: this.fb.control<string | null>(this.data.guest?.note ?? null)
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const request: GuestRequest = {
      fullName: v.fullName.trim(),
      gender: this.data.guest?.gender ?? null,
      phone: v.phone?.trim() || null,
      email: this.data.guest?.email ?? null,
      address: this.data.guest?.address ?? null,
      side: v.side,
      relationship: v.relationship?.trim() || null,
      category: v.category,
      groupId: this.data.guest?.groupId ?? null,
      contactPerson: this.data.guest?.contactPerson ?? null,
      vip: this.data.guest?.vip ?? false,
      note: v.note?.trim() || null,
      invitations: null,
      ignoreDuplicates: true
    };
    this.saving.set(true);
    const call = this.data.guest ? this.service.update(this.data.guest.id, request) : this.service.create(request);
    call
      .pipe(switchMap((guest) => this.syncGift(guest).pipe(switchMap(() => of(guest)))))
      .subscribe({
        next: (guest) => {
          this.notify.success(this.data.guest ? 'Đã cập nhật khách mời' : `Đã thêm ${guest.fullName}`);
          this.ref.close(guest);
        },
        error: (err) => {
          this.saving.set(false);
          this.notify.httpError(err, 'Không lưu được khách mời');
        }
      });
  }

  private syncGift(guest: Guest) {
    const amount = this.form.controls.giftAmount.value;
    const giftAmount = amount && amount > 0 ? amount : null;
    const body: GiftRequest | null = giftAmount
      ? {
          guestId: guest.id,
          giverName: guest.fullName,
          side: guest.side,
          eventId: null,
          giftType: 'CASH',
          amount: giftAmount,
          receivedAt: todayIso(),
          note: null,
          items: []
        }
      : null;

    return this.service.gifts(guest.id).pipe(
      switchMap((gifts) => {
        const ops = [...gifts.map((g) => this.giftService.delete(g.id)), ...(body ? [this.giftService.create(body)] : [])];
        return ops.length ? concat(...ops) : of(null);
      })
    );
  }
}
