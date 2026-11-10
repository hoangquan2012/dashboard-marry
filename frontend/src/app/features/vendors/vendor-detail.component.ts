import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { CONTRACT_STATUS_CLASS } from '../../core/models/enums';
import { Expense, Vendor } from '../../core/models/models';
import { VendorService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { PaymentChipComponent } from '../../shared/components/chips.components';
import { AttachmentsPanelComponent } from '../../shared/components/attachments-panel.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { VendorFormDialogComponent } from './vendor-form-dialog.component';

@Component({
  selector: 'app-vendor-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    PaymentChipComponent,
    AttachmentsPanelComponent,
    LoadingComponent,
    ErrorStateComponent,
    VndPipe
  ],
  template: `
    <div class="page">
      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (vendor(); as v) {
        <div class="page-header">
          <div>
            <div class="eyebrow">{{ v.serviceType || 'Nhà cung cấp' }}</div>
            <h1>{{ v.name }}</h1>
            <div class="subtitle">
              <span [class]="'chip ' + statusClass(v.contractStatus)">{{ v.contractStatusLabel }}</span>
              @if (v.contactPerson) { · {{ v.contactPerson }} }
              @if (v.phone) { · {{ v.phone }} }
            </div>
          </div>
          <div class="actions">
            <a mat-stroked-button routerLink="/vendors"><mat-icon class="material-symbols-rounded">arrow_back</mat-icon> Danh sách</a>
            <button mat-flat-button color="primary" (click)="edit()"><mat-icon class="material-symbols-rounded">edit</mat-icon> Sửa</button>
            <button mat-stroked-button color="warn" (click)="remove()"><mat-icon class="material-symbols-rounded">delete</mat-icon> Xóa</button>
          </div>
        </div>
        <div class="grid cols-2">
          <div class="card">
            <div class="card-title"><h3>Thông tin</h3></div>
            <dl class="kv">
              <dt>Địa chỉ</dt><dd>{{ v.address || '—' }}</dd>
              <dt>Email</dt><dd>{{ v.email || '—' }}</dd>
              <dt>Giá thỏa thuận</dt><dd>{{ v.agreedPrice | vnd }}</dd>
              <dt>Đặt cọc HĐ</dt><dd>{{ v.depositAmount | vnd }}</dd>
              <dt>Chi thực tế</dt><dd>{{ v.expenseActual | vnd }}</dd>
              <dt>Đã trả</dt><dd>{{ v.expensePaid | vnd }}</dd>
              <dt>Còn nợ</dt><dd class="money" [class.money-negative]="v.expenseRemaining > 0">{{ v.expenseRemaining | vnd }}</dd>
              <dt>Ghi chú</dt><dd>{{ v.note || '—' }}</dd>
            </dl>
          </div>
          <div class="card">
            <app-attachments-panel entityType="VENDOR" [entityId]="v.id" title="Hợp đồng & chứng từ" />
          </div>
        </div>
        <div class="card" style="margin-top: 18px">
          <div class="card-title"><h3>Khoản chi liên kết</h3></div>
          @if (expenses().length === 0) { <p class="muted">Chưa gắn khoản chi nào. Tạo khoản chi và chọn nhà cung cấp này.</p> }
          @else {
            <div class="table-wrapper flush">
              <table mat-table [dataSource]="expenses()">
                <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Khoản chi</th>
                  <td mat-cell *matCellDef="let e"><a [routerLink]="['/expenses', e.id]">{{ e.name }}</a></td></ng-container>
                <ng-container matColumnDef="actual"><th mat-header-cell *matHeaderCellDef class="num">Thực tế</th>
                  <td mat-cell *matCellDef="let e" class="num">{{ e.actualAmount | vnd }}</td></ng-container>
                <ng-container matColumnDef="paid"><th mat-header-cell *matHeaderCellDef class="num">Đã trả</th>
                  <td mat-cell *matCellDef="let e" class="num">{{ e.paidAmount | vnd }}</td></ng-container>
                <ng-container matColumnDef="remain"><th mat-header-cell *matHeaderCellDef class="num">Còn nợ</th>
                  <td mat-cell *matCellDef="let e" class="num">{{ e.remainingAmount | vnd }}</td></ng-container>
                <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>TT</th>
                  <td mat-cell *matCellDef="let e"><app-payment-chip [status]="e.paymentStatus" /></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="cols"></tr>
                <tr mat-row *matRowDef="let row; columns: cols"></tr>
              </table>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class VendorDetailComponent implements OnInit {
  readonly id = input.required({ transform: Number });
  private readonly service = inject(VendorService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly router = inject(Router);

  readonly vendor = signal<Vendor | null>(null);
  readonly expenses = signal<Expense[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly cols = ['name', 'actual', 'paid', 'remain', 'status'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.get(Number(this.id())).subscribe({
      next: (v) => {
        this.vendor.set(v);
        this.loading.set(false);
        this.service.expenses(v.id).subscribe({ next: (e) => this.expenses.set(e) });
      },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  statusClass(s: Vendor['contractStatus']): string {
    return CONTRACT_STATUS_CLASS[s];
  }

  edit(): void {
    const v = this.vendor();
    if (!v) return;
    this.dialog.open(VendorFormDialogComponent, { data: { vendor: v }, width: '680px' }).afterClosed().subscribe((saved) => saved && this.load());
  }

  remove(): void {
    const v = this.vendor();
    if (!v) return;
    this.confirm.delete(`nhà cung cấp "${v.name}"`).subscribe((ok) => {
      if (!ok) return;
      this.service.delete(v.id).subscribe({
        next: () => { this.notify.success('Đã xóa nhà cung cấp'); this.router.navigate(['/vendors']); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
