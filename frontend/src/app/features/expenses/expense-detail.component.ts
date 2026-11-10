import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { PAYMENT_METHOD_OPTIONS, PAYMENT_TYPE_OPTIONS, PaymentMethod, PaymentType } from '../../core/models/enums';
import { Expense, ExpenseCategory, PaymentRequest, Vendor, WeddingEvent } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { ExpenseService } from '../../core/services/expense.service';
import { VendorService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { fromIsoDate, toIsoDate, todayIso } from '../../core/utils/vn-date-adapter';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { PaymentChipComponent, SideChipComponent } from '../../shared/components/chips.components';
import { AttachmentsPanelComponent } from '../../shared/components/attachments-panel.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';
import { ExpenseFormDialogComponent } from './expense-form-dialog.component';

@Component({
  selector: 'app-payment-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatDatepickerModule, MoneyInputDirective, VndPipe],
  template: `
    <h2 mat-dialog-title class="dialog-title">Ghi nhận thanh toán</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        <mat-form-field class="full">
          <mat-label>Số tiền</mat-label>
          <input matInput appMoneyInput formControlName="amount" required />
          <span matTextSuffix>₫</span>
          <mat-hint>Còn phải trả {{ remaining | vnd }}</mat-hint>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Loại</mat-label>
          <mat-select formControlName="paymentType">
            @for (o of typeOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Hình thức</mat-label>
          <mat-select formControlName="method">
            @for (o of methodOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ngày thanh toán</mat-label>
          <input matInput [matDatepicker]="p" formControlName="paymentDate" required />
          <mat-datepicker-toggle matIconSuffix [for]="p"></mat-datepicker-toggle>
          <mat-datepicker #p></mat-datepicker>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Người thanh toán</mat-label>
          <input matInput formControlName="paidBy" />
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Ghi chú</mat-label>
          <textarea matInput formControlName="note" rows="2"></textarea>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Lưu</button>
      </mat-dialog-actions>
    </form>
  `
})
export class PaymentFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly remaining: number = inject(MAT_DIALOG_DATA).remaining;
  readonly paidBy = inject(MAT_DIALOG_DATA).paidBy as string | null;
  readonly ref = inject(MatDialogRef<PaymentFormDialogComponent>);
  readonly typeOptions = PAYMENT_TYPE_OPTIONS;
  readonly methodOptions = PAYMENT_METHOD_OPTIONS;
  readonly form = this.fb.group({
    amount: this.fb.nonNullable.control(this.remaining > 0 ? this.remaining : 0, [Validators.required, Validators.min(1)]),
    paymentDate: this.fb.control<Date | null>(new Date(), Validators.required),
    paymentType: this.fb.nonNullable.control<PaymentType>(this.remaining > 0 ? 'PARTIAL' : 'FINAL'),
    method: this.fb.nonNullable.control<PaymentMethod>('BANK_TRANSFER'),
    paidBy: this.fb.control<string | null>(this.paidBy),
    note: this.fb.control<string | null>(null)
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const request: PaymentRequest = {
      amount: v.amount,
      paymentDate: toIsoDate(v.paymentDate) ?? todayIso(),
      paymentType: v.paymentType,
      method: v.method,
      paidBy: v.paidBy?.trim() || null,
      note: v.note?.trim() || null
    };
    this.ref.close(request);
  }
}

@Component({
  selector: 'app-expense-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    SideChipComponent,
    PaymentChipComponent,
    LoadingComponent,
    ErrorStateComponent,
    AttachmentsPanelComponent,
    VndPipe,
    ViDatePipe
  ],
  template: `
    <div class="page">
      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (expense(); as e) {
        <div class="page-header">
          <div>
            <div class="eyebrow">{{ e.categoryName }}</div>
            <h1>{{ e.name }}</h1>
            <div class="subtitle">
              <app-side-chip [side]="e.side" /> · <app-payment-chip [status]="e.paymentStatus" />
              @if (e.vendorName) { · {{ e.vendorName }} }
            </div>
          </div>
          <div class="actions">
            <a mat-stroked-button routerLink="/expenses"><mat-icon class="material-symbols-rounded">arrow_back</mat-icon> Danh sách</a>
            <button mat-stroked-button (click)="pay()"><mat-icon class="material-symbols-rounded">payments</mat-icon> Thanh toán</button>
            <button mat-flat-button color="primary" (click)="edit()"><mat-icon class="material-symbols-rounded">edit</mat-icon> Sửa</button>
          </div>
        </div>

        <div class="grid cols-3">
          <div class="card">
            <div class="card-title"><h3>Số tiền</h3></div>
            <dl class="kv">
              <dt>Dự kiến</dt><dd>{{ e.estimatedAmount | vnd }}</dd>
              <dt>Thực tế</dt><dd class="money">{{ e.actualAmount | vnd }}</dd>
              <dt>Đã thanh toán</dt><dd>{{ e.paidAmount | vnd }}</dd>
              <dt>Đặt cọc</dt><dd>{{ e.depositAmount | vnd }}</dd>
              <dt>Còn phải trả</dt><dd class="money" [class.money-negative]="e.remainingAmount > 0">{{ e.remainingAmount | vnd }}</dd>
              @if (e.overBudget) { <dt>Vượt dự kiến</dt><dd class="money-negative">{{ e.overBudgetAmount | vnd }}</dd> }
            </dl>
          </div>
          <div class="card">
            <div class="card-title"><h3>Thông tin</h3></div>
            <dl class="kv">
              <dt>Nhà cung cấp</dt>
              <dd>@if (e.vendorId) { <a [routerLink]="['/vendors', e.vendorId]">{{ e.vendorName }}</a> } @else { — }</dd>
              <dt>Sự kiện</dt><dd>{{ e.eventName || '—' }}</dd>
              <dt>Ngày phát sinh</dt><dd>{{ e.expenseDate | viDate }}</dd>
              <dt>Hạn thanh toán</dt><dd>{{ e.dueDate | viDate }} @if (e.overdue) { <span class="chip chip-danger">Quá hạn</span> }</dd>
              <dt>Người TT</dt><dd>{{ e.paidBy || '—' }}</dd>
              <dt>Ghi chú</dt><dd>{{ e.note || '—' }}</dd>
            </dl>
          </div>
          <div class="card">
            <div class="card-title"><h3>Thanh toán</h3></div>
            <div class="progress" style="margin-bottom: 12px">
              <div class="bar bar-success" [style.width.%]="paidPercent(e)"></div>
            </div>
            <p class="muted">{{ paidPercent(e) }}% đã thanh toán</p>
            <button mat-flat-button color="primary" (click)="pay()" [disabled]="e.remainingAmount <= 0">Ghi nhận lần thanh toán</button>
          </div>
        </div>

        <div class="card" style="margin-top: 18px">
          <div class="card-title"><h3>Lịch sử thanh toán</h3></div>
          @if (e.payments.length === 0) { <p class="muted">Chưa có lần thanh toán nào.</p> }
          @else {
            <div class="table-wrapper flush">
              <table mat-table [dataSource]="e.payments">
                <ng-container matColumnDef="date"><th mat-header-cell *matHeaderCellDef>Ngày</th>
                  <td mat-cell *matCellDef="let p">{{ p.paymentDate | viDate }}</td></ng-container>
                <ng-container matColumnDef="type"><th mat-header-cell *matHeaderCellDef>Loại</th>
                  <td mat-cell *matCellDef="let p">{{ p.paymentTypeLabel }}</td></ng-container>
                <ng-container matColumnDef="method"><th mat-header-cell *matHeaderCellDef>Hình thức</th>
                  <td mat-cell *matCellDef="let p">{{ p.methodLabel }}</td></ng-container>
                <ng-container matColumnDef="amount"><th mat-header-cell *matHeaderCellDef class="num">Số tiền</th>
                  <td mat-cell *matCellDef="let p" class="num money">{{ p.amount | vnd }}</td></ng-container>
                <ng-container matColumnDef="by"><th mat-header-cell *matHeaderCellDef>Người TT</th>
                  <td mat-cell *matCellDef="let p">{{ p.paidBy || '—' }}</td></ng-container>
                <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let p" class="text-right">
                    <button mat-icon-button (click)="deletePayment(p.id)"><mat-icon class="material-symbols-rounded">delete</mat-icon></button>
                  </td></ng-container>
                <tr mat-header-row *matHeaderRowDef="payCols"></tr>
                <tr mat-row *matRowDef="let row; columns: payCols"></tr>
              </table>
            </div>
          }
        </div>

        <div class="card" style="margin-top: 18px">
          <app-attachments-panel entityType="EXPENSE" [entityId]="e.id" title="Hóa đơn & chứng từ" />
        </div>
      }
    </div>
  `
})
export class ExpenseDetailComponent implements OnInit {
  readonly id = input.required({ transform: Number });
  private readonly service = inject(ExpenseService);
  private readonly vendorsApi = inject(VendorService);
  private readonly eventsApi = inject(EventService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly router = inject(Router);

  readonly expense = signal<Expense | null>(null);
  readonly categories = signal<ExpenseCategory[]>([]);
  readonly vendors = signal<Vendor[]>([]);
  readonly events = signal<WeddingEvent[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly payCols = ['date', 'type', 'method', 'amount', 'by', 'actions'];

  ngOnInit(): void {
    this.service.categories().subscribe({ next: (c) => this.categories.set(c) });
    this.vendorsApi.list().subscribe({ next: (v) => this.vendors.set(v) });
    this.eventsApi.list().subscribe({ next: (e) => this.events.set(e) });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.get(Number(this.id())).subscribe({
      next: (e) => { this.expense.set(e); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  paidPercent(e: Expense): number {
    if (e.actualAmount <= 0) return e.paidAmount > 0 ? 100 : 0;
    return Math.min(100, Math.round((e.paidAmount / e.actualAmount) * 100));
  }

  edit(): void {
    const e = this.expense();
    if (!e) return;
    this.dialog
      .open(ExpenseFormDialogComponent, { data: { expense: e, categories: this.categories(), vendors: this.vendors(), events: this.events() }, width: '720px' })
      .afterClosed()
      .subscribe((saved) => saved && this.load());
  }

  pay(): void {
    const e = this.expense();
    if (!e) return;
    this.dialog
      .open(PaymentFormDialogComponent, { data: { remaining: e.remainingAmount, paidBy: e.paidBy }, width: '520px' })
      .afterClosed()
      .subscribe((req?: PaymentRequest) => {
        if (!req) return;
        this.service.addPayment(e.id, req).subscribe({
          next: () => { this.notify.success('Đã ghi nhận thanh toán'); this.load(); },
          error: (err) => this.notify.httpError(err)
        });
      });
  }

  deletePayment(paymentId: number): void {
    const e = this.expense();
    if (!e) return;
    this.confirm.delete('lần thanh toán này').subscribe((ok) => {
      if (!ok) return;
      this.service.deletePayment(e.id, paymentId).subscribe({
        next: () => { this.notify.success('Đã xóa lần thanh toán'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
