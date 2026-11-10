import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { HttpErrorResponse } from '@angular/common/http';
import { SIDE_OPTIONS, Side } from '../../core/models/enums';
import { SeatingOverview, TableRequest, UnassignedGuest, WeddingEvent, WeddingTable } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { SeatingService } from '../../core/services/seating.service';
import { NotificationService, apiErrorOf, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent } from '../../shared/components/chips.components';
import { EmptyStateComponent, ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';

@Component({
  selector: 'app-table-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.table ? 'Sửa bàn' : data.bulk ? 'Tạo nhiều bàn' : 'Tạo bàn' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        @if (data.bulk) {
          <mat-form-field>
            <mat-label>Tiền tố</mat-label>
            <input matInput formControlName="prefix" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Số bàn</mat-label>
            <input matInput type="number" formControlName="count" min="1" max="50" />
          </mat-form-field>
        } @else {
          <mat-form-field class="full">
            <mat-label>Tên bàn</mat-label>
            <input matInput formControlName="name" required />
          </mat-form-field>
        }
        <mat-form-field>
          <mat-label>Số chỗ</mat-label>
          <input matInput type="number" formControlName="capacity" min="1" max="30" required />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side">
            @for (o of sideOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Khu vực</mat-label>
          <mat-select formControlName="areaId">
            <mat-option [value]="null">Không phân khu</mat-option>
            @for (a of data.areas; track a.id) { <mat-option [value]="a.id">{{ a.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        @if (!data.bulk) {
          <mat-form-field class="full">
            <mat-label>Ghi chú</mat-label>
            <input matInput formControlName="note" />
          </mat-form-field>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Lưu</button>
      </mat-dialog-actions>
    </form>
  `
})
export class TableFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<{ table?: WeddingTable; bulk?: boolean; eventId: number; areas: { id: number; name: string }[] }>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<TableFormDialogComponent>);
  readonly sideOptions = SIDE_OPTIONS;
  readonly form = this.fb.group({
    name: this.fb.nonNullable.control(this.data.table?.name ?? '', this.data.bulk ? [] : Validators.required),
    prefix: this.fb.nonNullable.control('Bàn '),
    count: this.fb.nonNullable.control(8),
    capacity: this.fb.nonNullable.control(this.data.table?.capacity ?? 10, [Validators.required, Validators.min(1)]),
    side: this.fb.nonNullable.control<Side>(this.data.table?.side ?? 'BOTH'),
    areaId: this.fb.control<number | null>(this.data.table?.areaId ?? null),
    note: this.fb.control<string | null>(this.data.table?.note ?? null)
  });

  submit(): void {
    this.ref.close(this.form.getRawValue());
  }
}

@Component({
  selector: 'app-seating',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DragDropModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    SideChipComponent,
    LoadingComponent,
    EmptyStateComponent,
    ErrorStateComponent
  ],
  template: `
    <div class="page seating">
      <div class="page-header">
        <div>
          <div class="eyebrow">Ngày cưới</div>
          <h1>Sơ đồ bàn tiệc</h1>
          <div class="subtitle">Kéo khách vào bàn. Không xếp vượt sức chứa trừ khi bạn xác nhận.</div>
        </div>
        <div class="actions">
          <button mat-stroked-button (click)="createArea()" [disabled]="!eventId()"><mat-icon class="material-symbols-rounded">grid_view</mat-icon> Khu vực</button>
          <button mat-stroked-button (click)="openTable(true)" [disabled]="!eventId()"><mat-icon class="material-symbols-rounded">library_add</mat-icon> Tạo nhiều bàn</button>
          <button mat-flat-button color="primary" (click)="openTable()" [disabled]="!eventId()"><mat-icon class="material-symbols-rounded">add</mat-icon> Tạo bàn</button>
        </div>
      </div>

      <form class="toolbar card" [formGroup]="filter">
        <mat-form-field>
          <mat-label>Sự kiện</mat-label>
          <mat-select formControlName="eventId" (selectionChange)="load()">
            @for (e of events(); track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side" (selectionChange)="load()">
            <mat-option [value]="null">Tất cả</mat-option>
            @for (o of sideOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field class="search-field">
          <mat-label>Tìm khách đang ngồi bàn nào</mat-label>
          <input matInput formControlName="q" (keyup.enter)="load()" />
        </mat-form-field>
      </form>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (!overview()) {
        <div class="card"><app-empty icon="table_restaurant" title="Chọn sự kiện" message="Hãy chọn tiệc cưới để xếp bàn." /></div>
      } @else {
        <div class="stats muted" style="margin-bottom: 12px">
          {{ overview()!.tableCount }} bàn · {{ overview()!.seatsTaken }}/{{ overview()!.totalSeats }} chỗ ·
          {{ overview()!.seatedGuests }} khách đã xếp · {{ overview()!.unassignedGuests }} chưa xếp
        </div>
        <div class="layout" cdkDropListGroup>
          <div class="floor">
            @if (overview()!.tables.length === 0) {
              <div class="card"><app-empty icon="table_restaurant" title="Chưa có bàn" actionText="Tạo bàn" (action)="openTable()" /></div>
            }
            @for (t of overview()!.tables; track t.id) {
              <div class="table-card" [class.full]="t.seatsAvailable <= 0">
                <div class="thead">
                  <div>
                    <strong>{{ t.name }}</strong>
                    <div class="cell-sub">{{ t.areaName || 'Sảnh chính' }} · <app-side-chip [side]="t.side" /></div>
                  </div>
                  <div class="cap">{{ t.seatsTaken }}/{{ t.capacity }}</div>
                </div>
                <div class="progress"><div class="bar" [class.bar-danger]="t.seatsTaken > t.capacity" [style.width.%]="fill(t)"></div></div>
                <div class="seats" cdkDropList [id]="'t'+t.id" (cdkDropListDropped)="dropOnTable($event, t)">
                  @for (s of t.seats; track s.assignmentId) {
                    <div class="seat" cdkDrag [cdkDragData]="{ assignmentId: s.assignmentId, guestId: s.guestId, fromTable: t.id }">
                      <span class="truncate">{{ s.guestName }}</span>
                      <span class="muted">{{ s.seats }} chỗ</span>
                      <button mat-icon-button (click)="unassign(s.assignmentId)"><mat-icon class="material-symbols-rounded">close</mat-icon></button>
                    </div>
                  }
                </div>
                <div class="tfoot">
                  <button mat-button (click)="openTable(false, t)">Sửa</button>
                  <button mat-button color="warn" (click)="deleteTable(t)">Xóa</button>
                </div>
              </div>
            }
          </div>
          <aside class="unassigned card">
            <div class="card-title"><h3>Chưa xếp bàn ({{ unassigned().length }})</h3></div>
            <div class="seats" cdkDropList id="unassigned" (cdkDropListDropped)="dropUnassigned($event)">
              @for (g of unassigned(); track g.guestId) {
                <div class="seat" cdkDrag [cdkDragData]="{ guestId: g.guestId, seats: g.confirmedCount || g.expectedCount }">
                  <div>
                    <div class="cell-main">{{ g.fullName }}</div>
                    <div class="cell-sub">{{ g.code }} · {{ g.confirmedCount || g.expectedCount }} người</div>
                  </div>
                </div>
              }
              @if (unassigned().length === 0) {
                <p class="muted">Tất cả khách đã được xếp bàn.</p>
              }
            </div>
          </aside>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .layout { display: grid; grid-template-columns: 1fr 280px; gap: 18px; align-items: start; }
      .floor { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
      .table-card { background: var(--wm-surface); border: 1px solid var(--wm-border); border-radius: 16px; padding: 12px; box-shadow: var(--wm-shadow); min-height: 180px; display: flex; flex-direction: column; }
      .table-card.full { border-color: var(--wm-champagne); }
      .thead { display: flex; justify-content: space-between; gap: 8px; }
      .cap { font-weight: 700; color: var(--wm-burgundy); }
      .seats { min-height: 72px; flex: 1; display: flex; flex-direction: column; gap: 6px; margin: 10px 0; }
      .seat { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 8px; background: var(--wm-cream); border-radius: 8px; cursor: grab; font-size: 13px; }
      .tfoot { display: flex; justify-content: flex-end; }
      .unassigned { position: sticky; top: 16px; max-height: calc(100vh - 160px); overflow: auto; }
      @media (max-width: 960px) { .layout { grid-template-columns: 1fr; } .unassigned { position: static; max-height: none; } }
    `
  ]
})
export class SeatingComponent implements OnInit {
  private readonly seating = inject(SeatingService);
  private readonly eventsApi = inject(EventService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly route = inject(ActivatedRoute);

  readonly sideOptions = SIDE_OPTIONS;
  readonly events = signal<WeddingEvent[]>([]);
  readonly overview = signal<SeatingOverview | null>(null);
  readonly unassigned = signal<UnassignedGuest[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly filter = this.fb.group({
    eventId: this.fb.control<number | null>(null),
    side: this.fb.control<Side | null>(null),
    q: this.fb.control('')
  });

  eventId(): number | null {
    return this.filter.controls.eventId.value;
  }

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap.get('eventId');
    this.eventsApi.list().subscribe({
      next: (list) => {
        this.events.set(list);
        const main = list.find((e) => e.main) ?? list[0];
        const id = qp ? Number(qp) : main?.id ?? null;
        this.filter.controls.eventId.setValue(id);
        if (id) this.load();
      }
    });
  }

  load(): void {
    const id = this.eventId();
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    const side = this.filter.controls.side.value;
    const q = this.filter.controls.q.value?.trim() || undefined;
    this.seating.overview(id, side, q).subscribe({
      next: (o) => { this.overview.set(o); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
    this.seating.unassigned(id, side, q).subscribe({ next: (u) => this.unassigned.set(u) });
  }

  fill(t: WeddingTable): number {
    return t.capacity ? Math.min(100, Math.round((t.seatsTaken / t.capacity) * 100)) : 0;
  }

  openTable(bulk = false, table?: WeddingTable): void {
    const eventId = this.eventId();
    if (!eventId) return;
    this.dialog
      .open(TableFormDialogComponent, { data: { table, bulk, eventId, areas: this.overview()?.areas ?? [] }, width: '480px' })
      .afterClosed()
      .subscribe((v) => {
        if (!v) return;
        if (bulk) {
          this.seating.bulkCreate(eventId, v.areaId, v.prefix, v.count, v.capacity, v.side).subscribe({
            next: () => { this.notify.success('Đã tạo bàn'); this.load(); },
            error: (err) => this.notify.httpError(err)
          });
          return;
        }
        const req: TableRequest = { eventId, areaId: v.areaId, name: v.name, capacity: v.capacity, side: v.side, note: v.note };
        const call = table ? this.seating.updateTable(table.id, req) : this.seating.createTable(req);
        call.subscribe({
          next: () => { this.notify.success(table ? 'Đã cập nhật bàn' : 'Đã tạo bàn'); this.load(); },
          error: (err) => this.notify.httpError(err)
        });
      });
  }

  createArea(): void {
    const eventId = this.eventId();
    if (!eventId) return;
    const name = prompt('Tên khu vực (VD: Sảnh chính, Sân vườn)');
    if (!name?.trim()) return;
    this.seating.createArea(eventId, name.trim(), null).subscribe({
      next: () => { this.notify.success('Đã tạo khu vực'); this.load(); },
      error: (err) => this.notify.httpError(err)
    });
  }

  deleteTable(t: WeddingTable): void {
    this.confirm.delete(`bàn "${t.name}"`).subscribe((ok) => {
      if (!ok) return;
      this.seating.deleteTable(t.id).subscribe({
        next: () => { this.notify.success('Đã xóa bàn'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }

  dropOnTable(event: CdkDragDrop<unknown>, table: WeddingTable): void {
    const data = event.item.data as { assignmentId?: number; guestId: number; seats?: number };
    if (data.assignmentId) {
      this.move(data.assignmentId, table.id, false);
    } else {
      this.assign(table.id, data.guestId, data.seats ?? null, false);
    }
  }

  dropUnassigned(event: CdkDragDrop<unknown>): void {
    const data = event.item.data as { assignmentId?: number };
    if (data.assignmentId) this.unassign(data.assignmentId);
  }

  assign(tableId: number, guestId: number, seats: number | null, force: boolean): void {
    this.seating.assign(tableId, guestId, seats, force).subscribe({
      next: () => this.load(),
      error: (err) => this.handleCapacity(err, () => this.assign(tableId, guestId, seats, true))
    });
  }

  move(assignmentId: number, tableId: number, force: boolean): void {
    this.seating.move(assignmentId, tableId, force).subscribe({
      next: () => this.load(),
      error: (err) => this.handleCapacity(err, () => this.move(assignmentId, tableId, true))
    });
  }

  unassign(assignmentId: number): void {
    this.seating.unassign(assignmentId).subscribe({
      next: () => this.load(),
      error: (err) => this.notify.httpError(err)
    });
  }

  private handleCapacity(err: unknown, retry: () => void): void {
    const api = apiErrorOf(err);
    if (err instanceof HttpErrorResponse && err.status === 409 && api?.code === 'OVER_CAPACITY') {
      this.confirm
        .confirm({ title: 'Vượt sức chứa', message: api.message + '<br>Vẫn xếp khách này?', confirmText: 'Vẫn xếp', icon: 'event_seat' })
        .subscribe((ok) => ok && retry());
      return;
    }
    this.notify.httpError(err);
  }
}
