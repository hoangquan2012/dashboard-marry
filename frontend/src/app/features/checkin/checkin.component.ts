import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime } from 'rxjs';
import { BrowserMultiFormatReader, IScannerControls } from '@zxing/browser';
import { CheckinResult, CheckinStats, Invitation, WeddingEvent } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { CheckinService } from '../../core/services/checkin.service';
import { NotificationService, apiErrorOf, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent, StatusChipComponent } from '../../shared/components/chips.components';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { ViNumberPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-checkin',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    SideChipComponent,
    StatusChipComponent,
    StatCardComponent,
    LoadingComponent,
    ErrorStateComponent,
    ViNumberPipe,
    ViDatePipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Ngày cưới</div>
          <h1>Check-in khách mời</h1>
          <div class="subtitle">Quét QR trên thiệp hoặc tìm tên để ghi nhận khách đến. Thông tin tiền mừng không hiển thị tại đây.</div>
        </div>
      </div>

      <form class="toolbar card" [formGroup]="filter">
        <mat-form-field>
          <mat-label>Sự kiện đang diễn ra</mat-label>
          <mat-select formControlName="eventId" (selectionChange)="onEvent()">
            @for (e of events(); track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field class="search-field">
          <mat-label>Tìm khách theo tên / SĐT / mã</mat-label>
          <input matInput formControlName="q" />
          <mat-icon matPrefix class="material-symbols-rounded">search</mat-icon>
        </mat-form-field>
        <button mat-stroked-button type="button" (click)="toggleScan()">
          <mat-icon class="material-symbols-rounded">{{ scanning() ? 'videocam_off' : 'qr_code_scanner' }}</mat-icon>
          {{ scanning() ? 'Tắt camera' : 'Quét QR' }}
        </button>
      </form>

      @if (scanning()) {
        <div class="card scan-box">
          <video #video autoplay playsinline muted></video>
          <p class="muted">Hướng camera vào mã QR trên thiệp. Nếu camera không dùng được, hãy tìm tên bên dưới.</p>
        </div>
      }

      <mat-form-field class="qr-manual">
        <mat-label>Hoặc dán / gõ mã QR</mat-label>
        <input matInput [formControl]="qrCode" (keyup.enter)="checkinQr()" placeholder="WM:..." />
        <button matSuffix mat-icon-button type="button" (click)="checkinQr()"><mat-icon class="material-symbols-rounded">login</mat-icon></button>
      </mat-form-field>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (stats(); as s) {
        <div class="grid cols-4" style="margin-bottom: 18px">
          <app-stat-card label="Lời mời" [value]="s.totalInvitations | viNumber" icon="mail" />
          <app-stat-card label="Khách đã check-in" [value]="s.checkedInGuests | viNumber" icon="how_to_reg" tone="success" />
          <app-stat-card label="Số người đã đến" [value]="s.checkedInPeople | viNumber" icon="groups" tone="burgundy" [sub]="'Dự kiến ' + s.expectedPeople" />
          <app-stat-card label="Chưa đến" [value]="s.notArrived | viNumber" icon="schedule" tone="warning" />
        </div>

        <div class="grid cols-2">
          <div class="card">
            <div class="card-title"><h3>Kết quả tìm kiếm</h3></div>
            @if (results().length === 0) { <p class="muted">Nhập tên khách để check-in thủ công.</p> }
            @for (i of results(); track i.id) {
              <div class="guest-row">
                <div>
                  <div class="cell-main">{{ i.guestName }} <span class="muted">{{ i.guestCode }}</span></div>
                  <div class="cell-sub">
                    <app-side-chip [side]="i.guestSide" /> · <app-status-chip [status]="i.status" />
                    · mời {{ i.invitedCount }} · XN {{ i.confirmedCount }}
                    @if (i.checkedInAt) { · đến lúc {{ i.checkedInAt | viDate: 'datetime' }} }
                  </div>
                </div>
                <div class="row-act">
                  <mat-form-field subscriptSizing="dynamic" style="width: 88px">
                    <mat-label>Số người</mat-label>
                    <input matInput type="number" min="1" [value]="actualOf(i)" (change)="setActual(i.id, $event)" />
                  </mat-form-field>
                  @if (i.checkedInAt) {
                    <button mat-stroked-button (click)="undo(i)">Hủy CI</button>
                    <button mat-flat-button color="primary" (click)="checkin(i, true)">Cập nhật</button>
                  } @else {
                    <button mat-flat-button color="primary" (click)="checkin(i, false)">Check-in</button>
                  }
                </div>
              </div>
            }
          </div>
          <div class="card">
            <div class="card-title"><h3>Vừa check-in</h3></div>
            @for (i of s.recent; track i.id) {
              <div class="guest-row">
                <div>
                  <div class="cell-main">{{ i.guestName }}</div>
                  <div class="cell-sub">{{ i.checkedInAt | viDate: 'datetime' }} · {{ i.actualCount }} người · bàn {{ i.tableName || '—' }}</div>
                </div>
              </div>
            }
            @if (s.recent.length === 0) { <p class="muted">Chưa có khách nào check-in.</p> }
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .scan-box video { width: 100%; max-height: 280px; border-radius: 12px; background: #111; object-fit: cover; }
      .qr-manual { width: 100%; max-width: 420px; }
      .guest-row { display: flex; justify-content: space-between; gap: 12px; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--wm-border); }
      .row-act { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    `
  ]
})
export class CheckinComponent implements OnInit, OnDestroy {
  @ViewChild('video') video?: ElementRef<HTMLVideoElement>;
  private readonly eventsApi = inject(EventService);
  private readonly checkinApi = inject(CheckinService);
  private readonly fb = inject(FormBuilder);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly route = inject(ActivatedRoute);

  readonly events = signal<WeddingEvent[]>([]);
  readonly stats = signal<CheckinStats | null>(null);
  readonly results = signal<Invitation[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly scanning = signal(false);
  readonly qrCode = this.fb.control('');
  readonly filter = this.fb.group({
    eventId: this.fb.control<number | null>(null),
    q: this.fb.control('')
  });
  private actuals = new Map<number, number>();
  private controls: IScannerControls | null = null;
  private lastQr = '';

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap.get('eventId');
    this.eventsApi.list().subscribe({
      next: (list) => {
        this.events.set(list);
        const main = list.find((e) => e.main) ?? list[0];
        this.filter.controls.eventId.setValue(qp ? Number(qp) : main?.id ?? null);
        this.load();
      }
    });
    this.filter.controls.q.valueChanges.pipe(debounceTime(250)).subscribe(() => this.search());
  }

  ngOnDestroy(): void {
    this.stopScan();
  }

  onEvent(): void {
    this.load();
  }

  load(): void {
    const id = this.filter.controls.eventId.value;
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.checkinApi.stats(id).subscribe({
      next: (s) => { this.stats.set(s); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
    this.search();
  }

  search(): void {
    const id = this.filter.controls.eventId.value;
    if (!id) return;
    const q = this.filter.controls.q.value?.trim();
    this.checkinApi.search(id, q || undefined).subscribe({ next: (list) => this.results.set(list.slice(0, 20)) });
  }

  actualOf(i: Invitation): number {
    return this.actuals.get(i.id) ?? (i.actualCount || i.confirmedCount || i.expectedCount || 1);
  }

  setActual(id: number, event: Event): void {
    const n = Number((event.target as HTMLInputElement).value);
    this.actuals.set(id, Number.isFinite(n) && n > 0 ? n : 1);
  }

  checkin(i: Invitation, force: boolean): void {
    this.checkinApi.checkin(i.id, this.actualOf(i), force).subscribe({
      next: (r) => this.after(r, () => this.checkin(i, true)),
      error: (err) => this.handleDeclined(err, () => this.checkin(i, true))
    });
  }

  checkinQr(): void {
    const code = this.qrCode.value?.trim();
    if (!code) return;
    this.doQr(code, false);
  }

  private doQr(code: string, force: boolean): void {
    this.checkinApi.checkinByQr(code, this.filter.controls.eventId.value, null, force).subscribe({
      next: (r) => {
        this.qrCode.setValue('');
        this.after(r, () => this.doQr(code, true));
      },
      error: (err) => this.handleDeclined(err, () => this.doQr(code, true))
    });
  }

  private after(r: CheckinResult, retryForce: () => void): void {
    if (r.alreadyCheckedIn) {
      this.confirm
        .confirm({ title: 'Khách đã check-in', message: r.message, confirmText: 'Cập nhật lại', icon: 'how_to_reg' })
        .subscribe((ok) => ok && retryForce());
      return;
    }
    this.notify.success(r.message || `Đã check-in ${r.invitation.guestName}`);
    this.load();
  }

  private handleDeclined(err: unknown, retry: () => void): void {
    const api = apiErrorOf(err);
    if (api?.code === 'DECLINED_GUEST') {
      this.confirm.confirm({ title: 'Khách đã từ chối', message: api.message, confirmText: 'Vẫn check-in', icon: 'warning' }).subscribe((ok) => ok && retry());
      return;
    }
    this.notify.httpError(err);
  }

  undo(i: Invitation): void {
    this.checkinApi.undo(i.id).subscribe({
      next: () => { this.notify.success(`Đã hủy check-in ${i.guestName}`); this.load(); },
      error: (err) => this.notify.httpError(err)
    });
  }

  async toggleScan(): Promise<void> {
    if (this.scanning()) {
      this.stopScan();
      return;
    }
    this.scanning.set(true);
    await new Promise((r) => setTimeout(r, 50));
    const video = this.video?.nativeElement;
    if (!video) {
      this.notify.error('Không mở được camera. Hãy check-in thủ công.');
      this.scanning.set(false);
      return;
    }
    try {
      const reader = new BrowserMultiFormatReader();
      this.controls = await reader.decodeFromVideoDevice(undefined, video, (result) => {
        const text = result?.getText();
        if (text && text !== this.lastQr) {
          this.lastQr = text;
          this.doQr(text, false);
        }
      });
    } catch {
      this.notify.error('Không truy cập được camera. Dùng tìm kiếm tên hoặc dán mã QR.');
      this.scanning.set(false);
    }
  }

  private stopScan(): void {
    this.controls?.stop();
    this.controls = null;
    this.scanning.set(false);
  }
}
