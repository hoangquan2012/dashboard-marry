import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { EVENT_STATUS_CLASS, EVENT_STATUS_LABELS, INVITATION_STATUS_OPTIONS, InvitationStatus, SIDE_OPTIONS, Side } from '../../core/models/enums';
import { GuestSummary, Invitation, WeddingEvent } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { GuestService } from '../../core/services/guest.service';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent, StatusChipComponent } from '../../shared/components/chips.components';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { AttachmentsPanelComponent } from '../../shared/components/attachments-panel.component';
import { VndPipe, ViNumberPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';
import { EventFormDialogComponent } from './event-form-dialog.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    SideChipComponent,
    StatusChipComponent,
    StatCardComponent,
    LoadingComponent,
    ErrorStateComponent,
    AttachmentsPanelComponent,
    VndPipe,
    ViNumberPipe,
    ViDatePipe
  ],
  template: `
    <div class="page">
      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (event(); as e) {
        <div class="page-header">
          <div>
            <div class="eyebrow">{{ e.eventDate | viDate: 'long' }} @if (e.startTime) { · {{ e.startTime | viDate: 'time' }} }</div>
            <h1>{{ e.name }} @if (e.main) { <span class="chip chip-vip">Sự kiện chính</span> }</h1>
            <div class="subtitle">
              <app-side-chip [side]="e.side" />
              · <span [class]="'chip ' + statusClass(e.status)">{{ statusLabel(e.status) }}</span>
              · {{ e.venue || 'Chưa có địa điểm' }}
            </div>
          </div>
          <div class="actions">
            <a mat-stroked-button routerLink="/events"><mat-icon class="material-symbols-rounded">arrow_back</mat-icon> Danh sách</a>
            <a mat-stroked-button [routerLink]="['/seating']" [queryParams]="{ eventId: e.id }"><mat-icon class="material-symbols-rounded">table_restaurant</mat-icon> Sơ đồ bàn</a>
            <a mat-stroked-button [routerLink]="['/checkin']" [queryParams]="{ eventId: e.id }"><mat-icon class="material-symbols-rounded">qr_code_scanner</mat-icon> Check-in</a>
            <button mat-flat-button color="primary" (click)="edit()"><mat-icon class="material-symbols-rounded">edit</mat-icon> Sửa</button>
          </div>
        </div>

        <div class="grid cols-5" style="margin-bottom: 18px">
          <app-stat-card label="Khách mời" [value]="e.stats.guestCount | viNumber" icon="groups" [sub]="'Nhà trai ' + e.stats.groomGuests + ' · nhà gái ' + e.stats.brideGuests" />
          <app-stat-card label="Người được mời" [value]="e.stats.invitedPeople | viNumber" icon="mail" tone="info" />
          <app-stat-card label="Đã xác nhận" [value]="e.stats.confirmedPeople | viNumber" icon="check_circle" tone="success" />
          <app-stat-card label="Thực tế đến" [value]="e.stats.actualPeople | viNumber" icon="how_to_reg" tone="burgundy" />
          <app-stat-card label="Tiền mừng" [value]="e.stats.giftTotal | vnd" icon="redeem" tone="gold" />
        </div>

        <div class="card" style="margin-bottom: 18px">
          <div class="card-title">
            <h3>Danh sách khách của sự kiện</h3>
            <div class="actions">
              <mat-form-field>
                <mat-label>Tìm</mat-label>
                <input matInput [formControl]="q" />
              </mat-form-field>
              <mat-form-field>
                <mat-label>Trạng thái</mat-label>
                <mat-select [formControl]="status">
                  <mat-option [value]="null">Tất cả</mat-option>
                  @for (o of statusOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <mat-form-field>
                <mat-label>Bên</mat-label>
                <mat-select [formControl]="side">
                  <mat-option [value]="null">Tất cả</mat-option>
                  @for (o of sideOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
                </mat-select>
              </mat-form-field>
            </div>
          </div>
          <div class="invite-bar">
            <mat-form-field class="search-field">
              <mat-label>Thêm khách vào sự kiện</mat-label>
              <input matInput [formControl]="lookup" placeholder="Tìm tên hoặc SĐT..." />
            </mat-form-field>
            @if (lookupHits().length) {
              <div class="hits">
                @for (g of lookupHits(); track g.id) {
                  <button mat-stroked-button type="button" (click)="invite(g)">{{ g.fullName }} · {{ g.code }}</button>
                }
              </div>
            }
          </div>
          <div class="table-wrapper flush">
            <table mat-table [dataSource]="guests()">
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>Khách</th>
                <td mat-cell *matCellDef="let i">
                  <a [routerLink]="['/guests', i.guestId]" class="cell-main">{{ i.guestName }}</a>
                  <div class="cell-sub">{{ i.guestCode }} @if (i.groupName) { · {{ i.groupName }} }</div>
                </td>
              </ng-container>
              <ng-container matColumnDef="side">
                <th mat-header-cell *matHeaderCellDef>Bên</th>
                <td mat-cell *matCellDef="let i"><app-side-chip [side]="i.guestSide" /></td>
              </ng-container>
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Trạng thái</th>
                <td mat-cell *matCellDef="let i"><app-status-chip [status]="i.status" /></td>
              </ng-container>
              <ng-container matColumnDef="people">
                <th mat-header-cell *matHeaderCellDef class="num">Mời / XN / Đến</th>
                <td mat-cell *matCellDef="let i" class="num">{{ i.invitedCount }} / {{ i.confirmedCount }} / {{ i.actualCount }}</td>
              </ng-container>
              <ng-container matColumnDef="table">
                <th mat-header-cell *matHeaderCellDef>Bàn</th>
                <td mat-cell *matCellDef="let i">{{ i.tableName || 'Chưa xếp' }}</td>
              </ng-container>
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef></th>
                <td mat-cell *matCellDef="let i" class="text-right">
                  <button mat-icon-button (click)="removeGuest(i)" matTooltip="Gỡ khỏi sự kiện">
                    <mat-icon class="material-symbols-rounded">person_remove</mat-icon>
                  </button>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="cols"></tr>
              <tr mat-row *matRowDef="let row; columns: cols"></tr>
            </table>
          </div>
        </div>

        <div class="card">
          <app-attachments-panel entityType="EVENT" [entityId]="e.id" title="Ảnh & tài liệu sự kiện" />
        </div>
      }
    </div>
  `,
  styles: [
    `
      .invite-bar { margin-bottom: 12px; }
      .hits { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
    `
  ]
})
export class EventDetailComponent implements OnInit {
  readonly id = input.required({ transform: Number });
  private readonly service = inject(EventService);
  private readonly guestsApi = inject(GuestService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);

  readonly event = signal<WeddingEvent | null>(null);
  readonly guests = signal<Invitation[]>([]);
  readonly lookupHits = signal<GuestSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly cols = ['name', 'side', 'status', 'people', 'table', 'actions'];
  readonly statusOptions = INVITATION_STATUS_OPTIONS;
  readonly sideOptions = SIDE_OPTIONS;
  readonly q = new FormControl('', { nonNullable: true });
  readonly status = new FormControl<InvitationStatus | null>(null);
  readonly side = new FormControl<Side | null>(null);
  readonly lookup = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.q.valueChanges.pipe(debounceTime(300)).subscribe(() => this.loadGuests());
    this.status.valueChanges.subscribe(() => this.loadGuests());
    this.side.valueChanges.subscribe(() => this.loadGuests());
    this.lookup.valueChanges.pipe(debounceTime(250)).subscribe((q) => {
      if (!q.trim()) { this.lookupHits.set([]); return; }
      this.guestsApi.lookup(q, 8).subscribe({ next: (list) => this.lookupHits.set(list) });
    });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.get(Number(this.id())).subscribe({
      next: (e) => { this.event.set(e); this.loading.set(false); this.loadGuests(); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  loadGuests(): void {
    const e = this.event();
    if (!e) return;
    this.service.guests(e.id, this.status.value, this.side.value, this.q.value || undefined).subscribe({
      next: (list) => this.guests.set(list)
    });
  }

  statusClass(s: WeddingEvent['status']): string { return EVENT_STATUS_CLASS[s]; }
  statusLabel(s: WeddingEvent['status']): string { return EVENT_STATUS_LABELS[s]; }

  edit(): void {
    const e = this.event();
    if (!e) return;
    this.dialog.open(EventFormDialogComponent, { data: { event: e }, width: '640px' }).afterClosed().subscribe((saved) => saved && this.load());
  }

  invite(g: GuestSummary): void {
    const e = this.event();
    if (!e) return;
    this.service.invite(e.id, [g.id], 1).subscribe({
      next: (r) => {
        this.notify.success(r.invited ? `Đã mời ${g.fullName}` : `${g.fullName} đã có trong sự kiện`);
        this.lookup.setValue('');
        this.lookupHits.set([]);
        this.load();
      },
      error: (err) => this.notify.httpError(err)
    });
  }

  removeGuest(i: Invitation): void {
    const e = this.event();
    if (!e) return;
    this.confirm.delete(`lời mời của "${i.guestName}" khỏi sự kiện này`).subscribe((ok) => {
      if (!ok) return;
      this.service.removeGuest(e.id, i.guestId).subscribe({
        next: () => { this.notify.success('Đã gỡ khách khỏi sự kiện'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
