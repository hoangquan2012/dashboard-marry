import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ChartData } from 'chart.js';
import { Side } from '../../core/models/enums';
import { ExpenseReport, FinancialReport, GiftReport, GuestReport, WeddingEvent } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { ReportService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { toIsoDate } from '../../core/utils/vn-date-adapter';
import { CHART_COLORS, ChartCardComponent, moneyAxis, moneyTooltip } from '../../shared/components/chart-card.component';
import { PaymentChipComponent, SideChipComponent } from '../../shared/components/chips.components';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { VndPipe, ViNumberPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatIconModule,
    MatTableModule,
    ChartCardComponent,
    StatCardComponent,
    SideChipComponent,
    PaymentChipComponent,
    LoadingComponent,
    ErrorStateComponent,
    VndPipe,
    ViNumberPipe,
    ViDatePipe
  ],
  template: `
    <div class="page reports">
      <div class="page-header">
        <div>
          <div class="eyebrow">Tổng kết</div>
          <h1>Báo cáo và thống kê</h1>
          <div class="subtitle">Số liệu lấy từ backend. Có thể in hoặc xuất Excel.</div>
        </div>
        <div class="actions no-print">
          <button mat-stroked-button (click)="export()"><mat-icon class="material-symbols-rounded">download</mat-icon> Xuất Excel</button>
          <button mat-flat-button color="primary" (click)="print()"><mat-icon class="material-symbols-rounded">print</mat-icon> In / PDF</button>
        </div>
      </div>

      <form class="filters card no-print" [formGroup]="filter">
        <mat-button-toggle-group formControlName="side" (change)="load()">
          <mat-button-toggle value="ALL">Toàn bộ</mat-button-toggle>
          <mat-button-toggle value="GROOM">Nhà trai</mat-button-toggle>
          <mat-button-toggle value="BRIDE">Nhà gái</mat-button-toggle>
        </mat-button-toggle-group>
        <mat-form-field>
          <mat-label>Sự kiện</mat-label>
          <mat-select formControlName="eventId" (selectionChange)="load()">
            <mat-option [value]="null">Tất cả</mat-option>
            @for (e of events(); track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Từ ngày</mat-label>
          <input matInput [matDatepicker]="f" formControlName="from" (dateChange)="load()" />
          <mat-datepicker-toggle matIconSuffix [for]="f"></mat-datepicker-toggle>
          <mat-datepicker #f></mat-datepicker>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Đến ngày</mat-label>
          <input matInput [matDatepicker]="t" formControlName="to" (dateChange)="load()" />
          <mat-datepicker-toggle matIconSuffix [for]="t"></mat-datepicker-toggle>
          <mat-datepicker #t></mat-datepicker>
        </mat-form-field>
      </form>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else {
        @if (fin(); as f) {
          <section class="card" style="margin-bottom: 18px">
            <div class="card-title"><h3>Tổng kết tài chính — {{ f.header.groomName }} &amp; {{ f.header.brideName }}</h3>
              <span class="hint">{{ f.header.generatedAt | viDate: 'datetime' }}</span></div>
            <div class="grid cols-4">
              <app-stat-card label="Tổng thu" [value]="f.totalIncome | vnd" icon="south" tone="success" [sub]="'Mừng ' + (f.giftTotal | vnd)" />
              <app-stat-card label="Tổng chi" [value]="f.expenseActual | vnd" icon="north" tone="burgundy" />
              <app-stat-card label="Số dư" [value]="f.balance | vnd" icon="account_balance" [tone]="f.balance >= 0 ? 'success' : 'danger'" />
              <app-stat-card label="Chi / khách thực tế" [value]="f.costPerAttendee | vnd" icon="person" tone="gold" [sub]="'TB mừng ' + (f.giftPerGiver | vnd)" />
            </div>
          </section>
        }

        @if (guests(); as g) {
          <section class="card" style="margin-bottom: 18px">
            <div class="card-title"><h3>Báo cáo khách mời</h3></div>
            <div class="grid cols-4" style="margin-bottom: 14px">
              <app-stat-card label="Tổng khách" [value]="g.totalGuests | viNumber" icon="groups" [sub]="'Trai ' + g.groomGuests + ' · Gái ' + g.brideGuests" />
              <app-stat-card label="Người được mời" [value]="g.invitedPeople | viNumber" icon="mail" />
              <app-stat-card label="Thực tế đến" [value]="g.actualPeople | viNumber" icon="how_to_reg" tone="success" />
              <app-stat-card label="Tỷ lệ tham dự" [value]="pct(g.attendanceRatePeople)" icon="percent" tone="info" />
            </div>
            <div class="table-wrapper flush">
              <table mat-table [dataSource]="g.byCategory">
                <ng-container matColumnDef="label"><th mat-header-cell *matHeaderCellDef>Nhóm</th><td mat-cell *matCellDef="let r">{{ r.label }}</td></ng-container>
                <ng-container matColumnDef="guests"><th mat-header-cell *matHeaderCellDef class="num">Khách</th><td mat-cell *matCellDef="let r" class="num">{{ r.guests }}</td></ng-container>
                <ng-container matColumnDef="invited"><th mat-header-cell *matHeaderCellDef class="num">Mời</th><td mat-cell *matCellDef="let r" class="num">{{ r.invitedPeople }}</td></ng-container>
                <ng-container matColumnDef="actual"><th mat-header-cell *matHeaderCellDef class="num">Đến</th><td mat-cell *matCellDef="let r" class="num">{{ r.actualPeople }}</td></ng-container>
                <ng-container matColumnDef="rate"><th mat-header-cell *matHeaderCellDef class="num">Tỷ lệ</th><td mat-cell *matCellDef="let r" class="num">{{ pct(r.attendanceRate) }}</td></ng-container>
                <tr mat-header-row *matHeaderRowDef="catCols"></tr>
                <tr mat-row *matRowDef="let row; columns: catCols"></tr>
              </table>
            </div>
          </section>
        }

        @if (gifts(); as g) {
          <section class="card print-break" style="margin-bottom: 18px">
            <div class="card-title"><h3>Báo cáo tiền mừng</h3></div>
            <div class="grid cols-4" style="margin-bottom: 14px">
              <app-stat-card label="Tổng mừng" [value]="g.totalAmount | vnd" icon="redeem" tone="gold" />
              <app-stat-card label="Tiền mặt" [value]="g.cashAmount | vnd" icon="payments" />
              <app-stat-card label="Chuyển khoản" [value]="g.transferAmount | vnd" icon="account_balance" />
              <app-stat-card label="Hiện vật (đã định giá)" [value]="g.inKindValue | vnd" icon="card_giftcard" />
            </div>
            <app-chart-card title="Tiền mừng theo hình thức" type="doughnut" [data]="giftTypeChart()" [height]="240" />
            <div class="table-wrapper flush" style="margin-top: 14px">
              <table mat-table [dataSource]="g.details.slice(0, 80)">
                <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Khách mời</th>
                  <td mat-cell *matCellDef="let r">{{ r.giverName }}</td></ng-container>
                <ng-container matColumnDef="side"><th mat-header-cell *matHeaderCellDef>Nhà</th>
                  <td mat-cell *matCellDef="let r"><app-side-chip [side]="r.side" /></td></ng-container>
                <ng-container matColumnDef="event"><th mat-header-cell *matHeaderCellDef>Sự kiện</th>
                  <td mat-cell *matCellDef="let r">{{ r.eventName || '—' }}</td></ng-container>
                <ng-container matColumnDef="type"><th mat-header-cell *matHeaderCellDef>Hình thức</th>
                  <td mat-cell *matCellDef="let r">{{ r.giftTypeLabel }}</td></ng-container>
                <ng-container matColumnDef="amount"><th mat-header-cell *matHeaderCellDef class="num">Số tiền</th>
                  <td mat-cell *matCellDef="let r" class="num money">{{ r.amount | vnd:'Chưa định giá' }}</td></ng-container>
                <tr mat-header-row *matHeaderRowDef="giftCols"></tr>
                <tr mat-row *matRowDef="let row; columns: giftCols"></tr>
              </table>
            </div>
          </section>
        }

        @if (expenses(); as e) {
          <section class="card print-break">
            <div class="card-title"><h3>Báo cáo chi phí</h3></div>
            <div class="grid cols-4" style="margin-bottom: 14px">
              <app-stat-card label="Thực tế" [value]="e.actualTotal | vnd" icon="receipt_long" tone="burgundy" />
              <app-stat-card label="Đã thanh toán" [value]="e.paidTotal | vnd" icon="check_circle" tone="success" />
              <app-stat-card label="Còn nợ" [value]="e.remainingTotal | vnd" icon="pending" tone="warning" />
              <app-stat-card label="Vượt ngân sách" [value]="e.overBudgetAmount | vnd" icon="trending_up" tone="danger" />
            </div>
            <app-chart-card title="Cơ cấu chi phí" type="bar" [data]="expenseChart()" [options]="barOpts" [height]="280" />
            <h4 style="margin: 16px 0 8px">Chưa thanh toán hết</h4>
            <div class="table-wrapper flush">
              <table mat-table [dataSource]="e.unpaid">
                <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Khoản</th>
                  <td mat-cell *matCellDef="let r">{{ r.name }}</td></ng-container>
                <ng-container matColumnDef="remain"><th mat-header-cell *matHeaderCellDef class="num">Còn nợ</th>
                  <td mat-cell *matCellDef="let r" class="num">{{ r.remainingAmount | vnd }}</td></ng-container>
                <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>TT</th>
                  <td mat-cell *matCellDef="let r"><app-payment-chip [status]="r.paymentStatus" /></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="unpaidCols"></tr>
                <tr mat-row *matRowDef="let row; columns: unpaidCols"></tr>
              </table>
            </div>
          </section>
        }
      }
    </div>
  `
})
export class ReportsComponent implements OnInit {
  private readonly reports = inject(ReportService);
  private readonly eventsApi = inject(EventService);
  private readonly fb = inject(FormBuilder);
  private readonly notify = inject(NotificationService);

  readonly events = signal<WeddingEvent[]>([]);
  readonly guests = signal<GuestReport | null>(null);
  readonly gifts = signal<GiftReport | null>(null);
  readonly expenses = signal<ExpenseReport | null>(null);
  readonly fin = signal<FinancialReport | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly catCols = ['label', 'guests', 'invited', 'actual', 'rate'];
  readonly giftCols = ['name', 'side', 'event', 'type', 'amount'];
  readonly unpaidCols = ['name', 'remain', 'status'];
  readonly barOpts = { ...moneyTooltip(), ...moneyAxis('y') };
  readonly filter = this.fb.group({
    side: this.fb.control<Side | 'ALL'>('ALL'),
    eventId: this.fb.control<number | null>(null),
    from: this.fb.control<Date | null>(null),
    to: this.fb.control<Date | null>(null)
  });

  ngOnInit(): void {
    this.eventsApi.list().subscribe({ next: (e) => this.events.set(e) });
    this.load();
  }

  private params() {
    const v = this.filter.getRawValue();
    return {
      side: v.side === 'ALL' ? null : v.side,
      eventId: v.eventId,
      from: toIsoDate(v.from),
      to: toIsoDate(v.to)
    };
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const p = this.params();
    this.reports.guests(p).subscribe({ next: (r) => this.guests.set(r), error: (err) => this.error.set(extractMessage(err)) });
    this.reports.gifts(p).subscribe({ next: (r) => this.gifts.set(r) });
    this.reports.expenses(p).subscribe({ next: (r) => this.expenses.set(r) });
    this.reports.financial(p).subscribe({
      next: (r) => { this.fin.set(r); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  pct(n: number): string {
    return `${Math.round((n ?? 0) * (n <= 1 ? 100 : 1))}%`;
  }

  giftTypeChart(): ChartData<'doughnut'> {
    const items = this.gifts()?.byType ?? [];
    return { labels: items.map((i) => i.label), datasets: [{ data: items.map((i) => i.amount), backgroundColor: CHART_COLORS, borderWidth: 0 }] };
  }

  expenseChart(): ChartData<'bar'> {
    const items = this.expenses()?.byCategory ?? [];
    return { labels: items.map((i) => i.categoryName), datasets: [{ label: 'Thực tế', data: items.map((i) => i.actual), backgroundColor: CHART_COLORS[0] }] };
  }

  export(): void {
    this.reports.export(this.params()).subscribe({
      next: (n) => this.notify.success('Đã xuất ' + n),
      error: (err) => this.notify.httpError(err)
    });
  }

  print(): void {
    window.print();
  }
}
