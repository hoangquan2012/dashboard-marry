import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { EVENT_STATUS_CLASS, EVENT_STATUS_LABELS } from '../../core/models/enums';
import { WeddingEvent } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent } from '../../shared/components/chips.components';
import { EmptyStateComponent, ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { VndPipe, ViNumberPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';
import { EventFormDialogComponent } from './event-form-dialog.component';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    SideChipComponent,
    LoadingComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    VndPipe,
    ViNumberPipe,
    ViDatePipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Lịch trình</div>
          <h1>Sự kiện đám cưới</h1>
          <div class="subtitle">Ăn hỏi, tiệc nhà gái, lễ thành hôn, tiệc nhà trai... Mỗi sự kiện có khách, tiền mừng và chi phí riêng.</div>
        </div>
        <div class="actions">
          <button mat-flat-button color="primary" (click)="openForm()">
            <mat-icon class="material-symbols-rounded">add</mat-icon> Tạo sự kiện
          </button>
        </div>
      </div>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (events().length === 0) {
        <div class="card">
          <app-empty icon="event" title="Chưa có sự kiện" message="Tạo lễ ăn hỏi, tiệc cưới nhà trai/nhà gái..." actionText="Tạo sự kiện" (action)="openForm()" />
        </div>
      } @else {
        <div class="grid cols-2">
          @for (e of events(); track e.id) {
            <a class="card event-card" [routerLink]="['/events', e.id]">
              <div class="card-title">
                <h3>{{ e.name }} @if (e.main) { <span class="chip chip-vip">Sự kiện chính</span> }</h3>
                <span [class]="'chip ' + statusClass(e.status)">{{ statusLabel(e.status) }}</span>
              </div>
              <p class="muted">{{ e.eventDate | viDate: 'long' }} @if (e.startTime) { · {{ e.startTime | viDate: 'time' }} } · {{ e.venue || 'Chưa có địa điểm' }}</p>
              <div class="meta">
                <app-side-chip [side]="e.side" />
                <span>{{ e.stats.guestCount | viNumber }} khách</span>
                <span>{{ e.stats.invitedPeople | viNumber }} người mời</span>
                <span>{{ e.stats.actualPeople | viNumber }} đến</span>
              </div>
              <dl class="kv" style="margin-top: 12px">
                <dt>Tiền mừng</dt><dd class="money">{{ e.stats.giftTotal | vnd }}</dd>
                <dt>Chi phí thực tế</dt><dd>{{ e.stats.expenseActual | vnd }}</dd>
                <dt>Ngân sách sự kiện</dt><dd>{{ e.budget | vnd }}</dd>
              </dl>
              <div class="row-actions">
                <button mat-stroked-button type="button" (click)="edit($event, e)">Sửa</button>
                @if (!e.main) {
                  <button mat-button type="button" (click)="setMain($event, e)">Đặt làm chính</button>
                  <button mat-button type="button" color="warn" (click)="remove($event, e)">Xóa</button>
                }
              </div>
            </a>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .event-card { display: block; color: inherit; transition: transform 0.15s, box-shadow 0.15s; }
      .event-card:hover { transform: translateY(-2px); box-shadow: var(--wm-shadow-hover); }
      .meta { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; margin-top: 8px; font-size: 13px; color: var(--wm-text-2); }
      .row-actions { display: flex; gap: 8px; margin-top: 12px; }
    `
  ]
})
export class EventListComponent implements OnInit {
  private readonly service = inject(EventService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);

  readonly events = signal<WeddingEvent[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list().subscribe({
      next: (e) => { this.events.set(e); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  statusClass(s: WeddingEvent['status']): string {
    return EVENT_STATUS_CLASS[s];
  }
  statusLabel(s: WeddingEvent['status']): string {
    return EVENT_STATUS_LABELS[s];
  }

  openForm(event?: WeddingEvent): void {
    this.dialog.open(EventFormDialogComponent, { data: { event }, width: '640px' }).afterClosed().subscribe((saved) => saved && this.load());
  }

  edit(ev: MouseEvent, e: WeddingEvent): void {
    ev.preventDefault();
    ev.stopPropagation();
    this.openForm(e);
  }

  setMain(ev: MouseEvent, e: WeddingEvent): void {
    ev.preventDefault();
    ev.stopPropagation();
    this.service.setMain(e.id).subscribe({
      next: () => { this.notify.success(`Đã đặt "${e.name}" làm sự kiện chính`); this.load(); },
      error: (err) => this.notify.httpError(err)
    });
  }

  remove(ev: MouseEvent, e: WeddingEvent): void {
    ev.preventDefault();
    ev.stopPropagation();
    this.confirm.delete(`sự kiện "${e.name}"`, 'Chỉ xóa được khi chưa có khách hoặc chi phí gắn với sự kiện.').subscribe((ok) => {
      if (!ok) return;
      this.service.delete(e.id).subscribe({
        next: () => { this.notify.success('Đã xóa sự kiện'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
