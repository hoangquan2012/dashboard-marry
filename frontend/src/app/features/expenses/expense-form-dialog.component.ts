import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Expense, ExpenseCategory, ExpenseRequest } from '../../core/models/models';
import { ExpenseService } from '../../core/services/expense.service';
import { NotificationService } from '../../core/services/notification.service';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';

export interface ExpenseFormData {
  expense?: Expense;
  categories: ExpenseCategory[];
}

@Component({
  selector: 'app-expense-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MoneyInputDirective
  ],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.expense ? 'Sửa khoản chi' : 'Thêm khoản chi' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <div class="fields">
          <mat-form-field>
            <mat-label>Tên khoản chi</mat-label>
            <input matInput formControlName="name" required placeholder="VD: Nhà hàng, studio, hoa..." />
          </mat-form-field>
          <div class="row">
            <mat-form-field>
              <mat-label>Số tiền</mat-label>
              <input matInput appMoneyInput formControlName="actualAmount" />
              <span matTextSuffix>₫</span>
            </mat-form-field>
            <mat-form-field>
              <mat-label>Đã trả</mat-label>
              <input matInput appMoneyInput formControlName="paidAmount" />
              <span matTextSuffix>₫</span>
            </mat-form-field>
          </div>
          <mat-form-field>
            <mat-label>Ghi chú</mat-label>
            <textarea matInput formControlName="note" rows="3"></textarea>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">Lưu</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .fields {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding-top: 4px;
    }

    .row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    mat-form-field {
      width: 100%;
    }

    mat-dialog-content {
      min-width: 360px;
    }
  `
})
export class ExpenseFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ExpenseService);
  private readonly notify = inject(NotificationService);
  readonly ref = inject(MatDialogRef<ExpenseFormDialogComponent>);
  readonly data = inject<ExpenseFormData>(MAT_DIALOG_DATA);
  readonly saving = signal(false);

  readonly form = this.fb.group({
    name: this.fb.nonNullable.control(this.data.expense?.name ?? '', Validators.required),
    actualAmount: this.fb.nonNullable.control(this.data.expense?.actualAmount ?? 0),
    paidAmount: this.fb.nonNullable.control(this.data.expense?.paidAmount ?? 0),
    note: this.fb.control<string | null>(this.data.expense?.note ?? null)
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const amount = v.actualAmount ?? 0;
    const paid = v.paidAmount ?? 0;
    if (paid > amount) {
      this.notify.error('Số tiền đã trả không được lớn hơn số tiền khoản chi');
      return;
    }
    const existing = this.data.expense;
    const request: ExpenseRequest = {
      name: v.name.trim(),
      categoryId: existing?.categoryId ?? this.data.categories[0]?.id ?? 0,
      vendorId: existing?.vendorId ?? null,
      eventId: existing?.eventId ?? null,
      side: existing?.side ?? 'BOTH',
      estimatedAmount: amount,
      actualAmount: amount,
      expenseDate: existing?.expenseDate ?? null,
      dueDate: existing?.dueDate ?? null,
      paidBy: existing?.paidBy ?? null,
      note: v.note?.trim() || null,
      paidAmount: paid
    };
    this.saving.set(true);
    const call = existing ? this.service.update(existing.id, request) : this.service.create(request);
    call.subscribe({
      next: (e) => {
        this.notify.success(existing ? 'Đã cập nhật khoản chi' : 'Đã thêm khoản chi');
        this.ref.close(e);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.httpError(err);
      }
    });
  }
}
