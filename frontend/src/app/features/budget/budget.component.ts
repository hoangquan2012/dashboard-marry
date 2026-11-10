import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ChartData } from 'chart.js';
import { PAYMENT_METHOD_OPTIONS, PaymentMethod, SIDE_OPTIONS, Side } from '../../core/models/enums';
import { BudgetOverview, ExpenseCategory, OtherIncome, OtherIncomeRequest } from '../../core/models/models';
import { ExpenseService } from '../../core/services/expense.service';
import { BudgetService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { fromIsoDate, toIsoDate, todayIso } from '../../core/utils/vn-date-adapter';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { CHART_COLORS, ChartCardComponent, moneyAxis, moneyTooltip } from '../../shared/components/chart-card.component';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { SideChipComponent } from '../../shared/components/chips.components';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-income-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatDatepickerModule, MoneyInputDirective],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.income ? 'Sửa nguồn thu' : 'Thêm nguồn thu khác' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        <mat-form-field class="full">
          <mat-label>Nguồn thu</mat-label>
          <input matInput formControlName="source" required placeholder="VD: Bố mẹ hỗ trợ, tiết kiệm..." />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side">
            @for (o of sideOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Hình thức</mat-label>
          <mat-select formControlName="method">
            @for (o of methods; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Số tiền</mat-label>
          <input matInput appMoneyInput formControlName="amount" required />
          <span matTextSuffix>₫</span>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ngày nhận</mat-label>
          <input matInput [matDatepicker]="p" formControlName="receivedAt" />
          <mat-datepicker-toggle matIconSuffix [for]="p"></mat-datepicker-toggle>
          <mat-datepicker #p></mat-datepicker>
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
export class IncomeFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<{ income?: OtherIncome }>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<IncomeFormDialogComponent>);
  readonly sideOptions = SIDE_OPTIONS;
  readonly methods = PAYMENT_METHOD_OPTIONS;
  readonly form = this.fb.group({
    source: this.fb.nonNullable.control(this.data.income?.source ?? '', Validators.required),
    side: this.fb.nonNullable.control<Side>(this.data.income?.side ?? 'BOTH'),
    amount: this.fb.nonNullable.control(this.data.income?.amount ?? 0, [Validators.required, Validators.min(1)]),
    receivedAt: this.fb.control<Date | null>(fromIsoDate(this.data.income?.receivedAt) ?? new Date()),
    method: this.fb.nonNullable.control<PaymentMethod>(this.data.income?.method ?? 'BANK_TRANSFER'),
    note: this.fb.control<string | null>(this.data.income?.note ?? null)
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const request: OtherIncomeRequest = {
      source: v.source.trim(),
      side: v.side,
      amount: v.amount,
      receivedAt: toIsoDate(v.receivedAt) ?? todayIso(),
      method: v.method,
      note: v.note?.trim() || null
    };
    this.ref.close(request);
  }
}

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    ChartCardComponent,
    StatCardComponent,
    LoadingComponent,
    ErrorStateComponent,
    SideChipComponent,
    MoneyInputDirective,
    VndPipe,
    ViDatePipe,
    LabelPipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Tài chính</div>
          <h1>Ngân sách đám cưới</h1>
          <div class="subtitle">Tiền mừng và chi phí là hai luồng độc lập. Số dư = Tổng thu − Tổng chi.</div>
        </div>
        <div class="actions">
          <button mat-stroked-button (click)="saveBudgets()" [disabled]="saving()">
            <mat-icon class="material-symbols-rounded">save</mat-icon> Lưu ngân sách hạng mục
          </button>
          <button mat-flat-button color="primary" (click)="openIncome()">
            <mat-icon class="material-symbols-rounded">add</mat-icon> Nguồn thu khác
          </button>
        </div>
      </div>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (overview(); as o) {
        <div class="grid cols-4" style="margin-bottom: 18px">
          <app-stat-card label="Ngân sách dự kiến" [value]="o.totalBudget | vnd" icon="savings" tone="gold" [sub]="'Còn ' + (o.budgetRemaining | vnd)" />
          <app-stat-card label="Tổng thu" [value]="o.totalIncome | vnd" icon="south" tone="success" [sub]="'Mừng ' + (o.giftTotal | vnd) + ' + khác ' + (o.otherIncomeTotal | vnd)" />
          <app-stat-card label="Tổng chi thực tế" [value]="o.actualTotal | vnd" icon="north" tone="burgundy" [sub]="'Đã trả ' + (o.paidTotal | vnd)" />
          <app-stat-card label="Số dư thực tế" [value]="o.actualBalance | vnd" icon="account_balance" [tone]="o.actualBalance >= 0 ? 'success' : 'danger'" [sub]="'Dự kiến ' + (o.projectedBalance | vnd)" />
        </div>

        <div class="grid cols-2" style="margin-bottom: 18px">
          <app-stat-card label="Tiền mặt nhận" [value]="o.cashIncome | vnd" icon="payments" tone="info" [sub]="'Chi tiền mặt ' + (o.cashExpense | vnd)" />
          <app-stat-card label="Chuyển khoản nhận" [value]="o.transferIncome | vnd" icon="account_balance" tone="brown" [sub]="'Chi CK ' + (o.transferExpense | vnd)" />
        </div>

        @if (o.alerts.length) {
          <div class="card" style="margin-bottom: 18px">
            <div class="card-title"><h3>Cảnh báo ngân sách</h3></div>
            @for (a of o.alerts; track $index) {
              <div class="alert" [class]="a.severity" style="margin-bottom: 8px">
                {{ a.message }}
              </div>
            }
          </div>
        }

        <div class="grid cols-2" style="margin-bottom: 18px">
          <app-chart-card title="Ngân sách vs thực tế theo hạng mục" icon="bar_chart" type="bar" [data]="budgetChart()" [options]="barOpts" [height]="320" />
          <app-chart-card title="Thu − chi theo bên" icon="pie_chart" type="bar" [data]="sideChart()" [options]="barOpts" [height]="320" />
        </div>

        <div class="card" style="margin-bottom: 18px">
          <div class="card-title"><h3>Ngân sách từng hạng mục</h3><span class="hint">Nếu chi thực tế vượt ngân sách sẽ cảnh báo</span></div>
          <div class="table-wrapper flush">
            <table mat-table [dataSource]="categories()">
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>Hạng mục</th>
                <td mat-cell *matCellDef="let c">{{ c.name }}</td>
              </ng-container>
              <ng-container matColumnDef="budget">
                <th mat-header-cell *matHeaderCellDef class="num">Ngân sách</th>
                <td mat-cell *matCellDef="let c">
                  <mat-form-field subscriptSizing="dynamic" style="width: 160px; margin: 0">
                    <input matInput appMoneyInput [value]="c.budgetAmount" (ngModelChange)="setBudget(c, $event)" [ngModel]="c.budgetAmount" />
                  </mat-form-field>
                </td>
              </ng-container>
              <ng-container matColumnDef="actual">
                <th mat-header-cell *matHeaderCellDef class="num">Thực tế</th>
                <td mat-cell *matCellDef="let c" class="num" [class.money-negative]="c.overBudget">{{ c.actualTotal | vnd }}</td>
              </ng-container>
              <ng-container matColumnDef="paid">
                <th mat-header-cell *matHeaderCellDef class="num">Đã trả</th>
                <td mat-cell *matCellDef="let c" class="num">{{ c.paidTotal | vnd }}</td>
              </ng-container>
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef></th>
                <td mat-cell *matCellDef="let c">
                  @if (c.overBudget) { <span class="chip chip-danger">Vượt ngân sách</span> }
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="catCols"></tr>
              <tr mat-row *matRowDef="let row; columns: catCols"></tr>
            </table>
          </div>
        </div>

        <div class="card">
          <div class="card-title">
            <h3>Nguồn thu khác (không phải tiền mừng)</h3>
            <button mat-stroked-button (click)="openIncome()"><mat-icon class="material-symbols-rounded">add</mat-icon> Thêm</button>
          </div>
          @if (incomes().length === 0) { <p class="muted">Chưa có nguồn thu khác.</p> }
          @else {
            <div class="table-wrapper flush">
              <table mat-table [dataSource]="incomes()">
                <ng-container matColumnDef="source"><th mat-header-cell *matHeaderCellDef>Nguồn</th>
                  <td mat-cell *matCellDef="let i">{{ i.source }}</td></ng-container>
                <ng-container matColumnDef="side"><th mat-header-cell *matHeaderCellDef>Bên</th>
                  <td mat-cell *matCellDef="let i"><app-side-chip [side]="i.side" /></td></ng-container>
                <ng-container matColumnDef="amount"><th mat-header-cell *matHeaderCellDef class="num">Số tiền</th>
                  <td mat-cell *matCellDef="let i" class="num money">{{ i.amount | vnd }}</td></ng-container>
                <ng-container matColumnDef="date"><th mat-header-cell *matHeaderCellDef>Ngày</th>
                  <td mat-cell *matCellDef="let i">{{ i.receivedAt | viDate }} · {{ i.method | label: 'paymentMethod' }}</td></ng-container>
                <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let i" class="text-right">
                    <button mat-icon-button (click)="openIncome(i)"><mat-icon class="material-symbols-rounded">edit</mat-icon></button>
                    <button mat-icon-button (click)="deleteIncome(i)"><mat-icon class="material-symbols-rounded">delete</mat-icon></button>
                  </td></ng-container>
                <tr mat-header-row *matHeaderRowDef="incCols"></tr>
                <tr mat-row *matRowDef="let row; columns: incCols"></tr>
              </table>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class BudgetComponent implements OnInit {
  private readonly budgetApi = inject(BudgetService);
  private readonly expenseApi = inject(ExpenseService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);

  readonly overview = signal<BudgetOverview | null>(null);
  readonly categories = signal<ExpenseCategory[]>([]);
  readonly incomes = signal<OtherIncome[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly catCols = ['name', 'budget', 'actual', 'paid', 'status'];
  readonly incCols = ['source', 'side', 'amount', 'date', 'actions'];
  readonly barOpts = { ...moneyTooltip(), ...moneyAxis('y') };
  private budgetEdits: Record<number, number> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.budgetApi.overview().subscribe({
      next: (o) => { this.overview.set(o); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
    this.expenseApi.categories().subscribe({ next: (c) => this.categories.set(c) });
    this.budgetApi.incomes().subscribe({ next: (i) => this.incomes.set(i) });
  }

  setBudget(c: ExpenseCategory, amount: number | null): void {
    this.budgetEdits[c.id] = amount ?? 0;
    c.budgetAmount = amount ?? 0;
  }

  saveBudgets(): void {
    const budgets: Record<number, number> = {};
    for (const c of this.categories()) budgets[c.id] = this.budgetEdits[c.id] ?? c.budgetAmount;
    this.saving.set(true);
    this.expenseApi.updateBudgets(budgets).subscribe({
      next: () => {
        this.saving.set(false);
        this.notify.success('Đã lưu ngân sách hạng mục');
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.httpError(err);
      }
    });
  }

  budgetChart(): ChartData<'bar'> {
    const cats = (this.overview()?.categories ?? []).filter((c) => c.budget > 0 || c.actual > 0).slice(0, 10);
    return {
      labels: cats.map((c) => c.categoryName),
      datasets: [
        { label: 'Ngân sách', data: cats.map((c) => c.budget), backgroundColor: CHART_COLORS[1] },
        { label: 'Thực tế', data: cats.map((c) => c.actual), backgroundColor: CHART_COLORS[0] }
      ]
    };
  }

  sideChart(): ChartData<'bar'> {
    const sides = this.overview()?.bySide ?? [];
    return {
      labels: sides.map((s) => s.label),
      datasets: [
        { label: 'Thu (mừng + khác)', data: sides.map((s) => s.giftTotal + s.otherIncome), backgroundColor: CHART_COLORS[4] },
        { label: 'Chi thực tế', data: sides.map((s) => s.expenseActual), backgroundColor: CHART_COLORS[0] }
      ]
    };
  }

  openIncome(income?: OtherIncome): void {
    this.dialog.open(IncomeFormDialogComponent, { data: { income }, width: '520px' }).afterClosed().subscribe((req?: OtherIncomeRequest) => {
      if (!req) return;
      const call = income ? this.budgetApi.updateIncome(income.id, req) : this.budgetApi.createIncome(req);
      call.subscribe({
        next: () => { this.notify.success(income ? 'Đã cập nhật nguồn thu' : 'Đã thêm nguồn thu'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }

  deleteIncome(i: OtherIncome): void {
    this.confirm.delete(`nguồn thu "${i.source}"`).subscribe((ok) => {
      if (!ok) return;
      this.budgetApi.deleteIncome(i.id).subscribe({
        next: () => { this.notify.success('Đã xóa nguồn thu'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
