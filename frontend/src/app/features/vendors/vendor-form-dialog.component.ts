import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CONTRACT_STATUS_OPTIONS, ContractStatus } from '../../core/models/enums';
import { Vendor, VendorRequest } from '../../core/models/models';
import { VendorService } from '../../core/services/misc.services';
import { NotificationService } from '../../core/services/notification.service';
import { fromIsoDate, toIsoDate } from '../../core/utils/vn-date-adapter';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';

@Component({
  selector: 'app-vendor-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MoneyInputDirective
  ],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.vendor ? 'Sửa nhà cung cấp' : 'Thêm nhà cung cấp' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        <mat-form-field class="full">
          <mat-label>Tên nhà cung cấp</mat-label>
          <input matInput formControlName="name" required />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Dịch vụ</mat-label>
          <input matInput formControlName="serviceType" placeholder="Nhà hàng, studio, makeup..." />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Người liên hệ</mat-label>
          <input matInput formControlName="contactPerson" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Số điện thoại</mat-label>
          <input matInput formControlName="phone" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Email</mat-label>
          <input matInput formControlName="email" />
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Địa chỉ</mat-label>
          <input matInput formControlName="address" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Giá thỏa thuận</mat-label>
          <input matInput appMoneyInput formControlName="agreedPrice" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Tiền đặt cọc</mat-label>
          <input matInput appMoneyInput formControlName="depositAmount" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Trạng thái hợp đồng</mat-label>
          <mat-select formControlName="contractStatus">
            @for (o of statuses; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ngày ký HĐ</mat-label>
          <input matInput [matDatepicker]="p" formControlName="contractDate" />
          <mat-datepicker-toggle matIconSuffix [for]="p"></mat-datepicker-toggle>
          <mat-datepicker #p></mat-datepicker>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Mô tả dịch vụ</mat-label>
          <textarea matInput formControlName="serviceDescription" rows="2"></textarea>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Ghi chú</mat-label>
          <textarea matInput formControlName="note" rows="2"></textarea>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">Lưu</button>
      </mat-dialog-actions>
    </form>
  `
})
export class VendorFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(VendorService);
  private readonly notify = inject(NotificationService);
  readonly data = inject<{ vendor?: Vendor }>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<VendorFormDialogComponent>);
  readonly statuses = CONTRACT_STATUS_OPTIONS;
  readonly saving = signal(false);

  readonly form = this.fb.group({
    name: this.fb.nonNullable.control(this.data.vendor?.name ?? '', Validators.required),
    serviceType: this.fb.control<string | null>(this.data.vendor?.serviceType ?? null),
    contactPerson: this.fb.control<string | null>(this.data.vendor?.contactPerson ?? null),
    phone: this.fb.control<string | null>(this.data.vendor?.phone ?? null),
    email: this.fb.control<string | null>(this.data.vendor?.email ?? null),
    address: this.fb.control<string | null>(this.data.vendor?.address ?? null),
    serviceDescription: this.fb.control<string | null>(this.data.vendor?.serviceDescription ?? null),
    agreedPrice: this.fb.control<number | null>(this.data.vendor?.agreedPrice ?? null),
    depositAmount: this.fb.nonNullable.control(this.data.vendor?.depositAmount ?? 0),
    contractStatus: this.fb.nonNullable.control<ContractStatus>(this.data.vendor?.contractStatus ?? 'NEGOTIATING'),
    contractDate: this.fb.control<Date | null>(fromIsoDate(this.data.vendor?.contractDate)),
    rating: this.fb.control<number | null>(this.data.vendor?.rating ?? null),
    note: this.fb.control<string | null>(this.data.vendor?.note ?? null)
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const request: VendorRequest = {
      name: v.name.trim(),
      serviceType: v.serviceType?.trim() || null,
      contactPerson: v.contactPerson?.trim() || null,
      phone: v.phone?.trim() || null,
      email: v.email?.trim() || null,
      address: v.address?.trim() || null,
      serviceDescription: v.serviceDescription?.trim() || null,
      agreedPrice: v.agreedPrice,
      depositAmount: v.depositAmount ?? 0,
      contractStatus: v.contractStatus,
      contractDate: toIsoDate(v.contractDate),
      rating: v.rating,
      note: v.note?.trim() || null
    };
    this.saving.set(true);
    const call = this.data.vendor ? this.service.update(this.data.vendor.id, request) : this.service.create(request);
    call.subscribe({
      next: (vendor) => {
        this.notify.success(this.data.vendor ? 'Đã cập nhật nhà cung cấp' : 'Đã thêm nhà cung cấp');
        this.ref.close(vendor);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.httpError(err);
      }
    });
  }
}
