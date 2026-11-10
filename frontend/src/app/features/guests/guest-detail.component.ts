import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Gift, Guest, GuestGroup, WeddingEvent } from '../../core/models/models';
import { GuestService } from '../../core/services/guest.service';
import { EventService } from '../../core/services/event.service';
import { GiftService } from '../../core/services/gift.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent, StatusChipComponent } from '../../shared/components/chips.components';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { SecureImageComponent } from '../../shared/components/secure-image.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';
import { GuestFormDialogComponent } from './guest-form-dialog.component';
import { GiftFormDialogComponent } from '../gifts/gift-form-dialog.component';

@Component({
  selector: 'app-guest-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    SideChipComponent,
    StatusChipComponent,
    LoadingComponent,
    ErrorStateComponent,
    SecureImageComponent,
    LabelPipe,
    VndPipe,
    ViDatePipe
  ],
  template: `
    <div class="page">
      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (guest(); as g) {
        <div class="page-header">
          <div>
            <div class="eyebrow">{{ g.code }}</div>
            <h1>{{ g.fullName }} @if (g.vip) { <span class="chip chip-vip">VIP</span> }</h1>
            <div class="subtitle">
              <app-side-chip [side]="g.side" />
              @if (g.relationship) { · {{ g.relationship }} }
              · {{ g.category | label: 'category' }}
            </div>
          </div>
          <div class="actions">
            <a mat-stroked-button routerLink="/guests">
              <mat-icon class="material-symbols-rounded">arrow_back</mat-icon> Danh sách
            </a>
            <button mat-stroked-button (click)="openGift()">
              <mat-icon class="material-symbols-rounded">redeem</mat-icon> Ghi tiền mừng
            </button>
            <button mat-flat-button color="primary" (click)="edit()">
              <mat-icon class="material-symbols-rounded">edit</mat-icon> Sửa
            </button>
          </div>
        </div>

        <div class="grid cols-3">
          <div class="card">
            <div class="card-title"><h3>Thông tin</h3></div>
            <dl class="kv">
              <dt>Mã khách</dt><dd>{{ g.code }}</dd>
              <dt>Điện thoại</dt><dd>{{ g.phone || '—' }}</dd>
              <dt>Email</dt><dd>{{ g.email || '—' }}</dd>
              <dt>Địa chỉ</dt><dd>{{ g.address || '—' }}</dd>
              <dt>Giới tính</dt><dd>{{ g.gender ? (g.gender | label: 'gender') : '—' }}</dd>
              <dt>Nhóm</dt>
              <dd>
                @if (g.groupId) { <a [routerLink]="['/guests']" [queryParams]="{ groupId: g.groupId }">{{ g.groupName }}</a> }
                @else { — }
              </dd>
              <dt>Người liên hệ</dt><dd>{{ g.contactPerson || '—' }}</dd>
              <dt>Ghi chú</dt><dd>{{ g.note || '—' }}</dd>
            </dl>
          </div>
          <div class="card">
            <div class="card-title"><h3>Tham dự</h3></div>
            <dl class="kv">
              <dt>Trạng thái chung</dt><dd><app-status-chip [status]="g.overallStatus" /></dd>
              <dt>Số người mời</dt><dd>{{ g.totalInvited }}</dd>
              <dt>Dự kiến</dt><dd>{{ g.totalExpected }}</dd>
              <dt>Xác nhận</dt><dd>{{ g.totalConfirmed }}</dd>
              <dt>Thực tế đến</dt><dd>{{ g.totalActual }}</dd>
              <dt>Bàn tiệc</dt>
              <dd>
                @if (g.tables.length) {
                  @for (t of g.tables; track t.tableId) { {{ t.tableName }} ({{ t.eventName }}) }
                } @else { Chưa xếp bàn }
              </dd>
            </dl>
          </div>
          <div class="card">
            <div class="card-title"><h3>Tiền mừng</h3></div>
            <div class="money" style="font-size: 26px">{{ g.giftTotal | vnd }}</div>
            <p class="muted">{{ g.giftCount }} lượt mừng</p>
            <button mat-stroked-button (click)="openGift()">Thêm khoản mừng</button>
          </div>
        </div>

        <div class="card" style="margin-top: 18px">
          <div class="card-title"><h3>Lời mời theo sự kiện</h3></div>
          <div class="table-wrapper flush">
            <table mat-table [dataSource]="g.invitations">
              <ng-container matColumnDef="event"><th mat-header-cell *matHeaderCellDef>Sự kiện</th>
                <td mat-cell *matCellDef="let i">{{ i.eventName }}<div class="cell-sub">{{ i.eventDate | viDate }}</div></td></ng-container>
              <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>Trạng thái</th>
                <td mat-cell *matCellDef="let i"><app-status-chip [status]="i.status" /></td></ng-container>
              <ng-container matColumnDef="people"><th mat-header-cell *matHeaderCellDef class="num">Mời / XN / Đến</th>
                <td mat-cell *matCellDef="let i" class="num">{{ i.invitedCount }} / {{ i.confirmedCount }} / {{ i.actualCount }}</td></ng-container>
              <ng-container matColumnDef="table"><th mat-header-cell *matHeaderCellDef>Bàn</th>
                <td mat-cell *matCellDef="let i">{{ i.tableName || 'Chưa xếp' }}</td></ng-container>
              <ng-container matColumnDef="qr"><th mat-header-cell *matHeaderCellDef>QR</th>
                <td mat-cell *matCellDef="let i">
                  <app-secure-image [src]="qrUrl(i.id)" [alt]="'QR ' + i.eventName" [size]="72" />
                </td></ng-container>
              <tr mat-header-row *matHeaderRowDef="invCols"></tr>
              <tr mat-row *matRowDef="let row; columns: invCols"></tr>
            </table>
          </div>
        </div>

        <div class="card" style="margin-top: 18px">
          <div class="card-title"><h3>Lịch sử tiền mừng</h3></div>
          @if (gifts().length === 0) { <p class="muted">Chưa có khoản mừng.</p> }
          @else {
            <div class="table-wrapper flush">
              <table mat-table [dataSource]="gifts()">
                <ng-container matColumnDef="date"><th mat-header-cell *matHeaderCellDef>Ngày</th>
                  <td mat-cell *matCellDef="let x">{{ x.receivedAt | viDate }}</td></ng-container>
                <ng-container matColumnDef="event"><th mat-header-cell *matHeaderCellDef>Sự kiện</th>
                  <td mat-cell *matCellDef="let x">{{ x.eventName || '—' }}</td></ng-container>
                <ng-container matColumnDef="type"><th mat-header-cell *matHeaderCellDef>Hình thức</th>
                  <td mat-cell *matCellDef="let x">{{ x.giftTypeLabel }}</td></ng-container>
                <ng-container matColumnDef="amount"><th mat-header-cell *matHeaderCellDef class="num">Số tiền</th>
                  <td mat-cell *matCellDef="let x" class="num money">{{ x.amount | vnd:'Chưa định giá' }}</td></ng-container>
                <ng-container matColumnDef="items"><th mat-header-cell *matHeaderCellDef>Hiện vật</th>
                  <td mat-cell *matCellDef="let x">{{ x.itemsSummary || '—' }}</td></ng-container>
                <tr mat-header-row *matHeaderRowDef="giftCols"></tr>
                <tr mat-row *matRowDef="let row; columns: giftCols"></tr>
              </table>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class GuestDetailComponent implements OnInit {
  readonly id = input.required({ transform: Number });
  private readonly guestService = inject(GuestService);
  private readonly giftService = inject(GiftService);
  private readonly eventService = inject(EventService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly guest = signal<Guest | null>(null);
  readonly gifts = signal<Gift[]>([]);
  readonly events = signal<WeddingEvent[]>([]);
  readonly groups = signal<GuestGroup[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly invCols = ['event', 'status', 'people', 'table', 'qr'];
  readonly giftCols = ['date', 'event', 'type', 'amount', 'items'];

  ngOnInit(): void {
    this.eventService.list().subscribe({ next: (e) => this.events.set(e) });
    this.guestService.groups().subscribe({ next: (g) => this.groups.set(g) });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.guestService.get(Number(this.id())).subscribe({
      next: (g) => {
        this.guest.set(g);
        this.loading.set(false);
        this.guestService.gifts(g.id).subscribe({ next: (list) => this.gifts.set(list) });
      },
      error: (err) => {
        this.error.set(extractMessage(err));
        this.loading.set(false);
      }
    });
  }

  qrUrl(invitationId: number): string {
    return this.guestService.qrUrl(invitationId, 160);
  }

  edit(): void {
    const g = this.guest();
    if (!g) return;
    this.dialog
      .open(GuestFormDialogComponent, { data: { guest: g, events: this.events(), groups: this.groups() }, width: '820px' })
      .afterClosed()
      .subscribe((saved) => saved && this.load());
  }

  openGift(): void {
    const g = this.guest();
    if (!g) return;
    this.dialog
      .open(GiftFormDialogComponent, { data: { guest: g, events: this.events() }, width: '640px' })
      .afterClosed()
      .subscribe((saved) => saved && this.load());
  }
}
