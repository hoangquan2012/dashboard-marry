import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { EVENT_STATUS_OPTIONS, EventStatus, SIDE_OPTIONS, Side } from '../../core/models/enums';
import { EventRequest, WeddingEvent } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { NotificationService } from '../../core/services/notification.service';
import { fromIsoDate, toIsoDate, todayIso } from '../../core/utils/vn-date-adapter';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';

@Component({
  selector: 'app-event-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatCheckboxModule,
    MoneyInputDirective
  ],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.event ? 'Sửa sự kiện' : 'Tạo sự kiện' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        <mat-form-field class="full">
          <mat-label>Tên sự kiện</mat-label>
          <input matInput formControlName="name" required placeholder="VD: Tiệc cưới nhà trai" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ngày tổ chức</mat-label>
          <input matInput [matDatepicker]="p" formControlName="eventDate" required />
          <mat-datepicker-toggle matIconSuffix [for]="p"></mat-datepicker-toggle>
          <mat-datepicker #p></mat-datepicker>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Giờ bắt đầu</mat-label>
          <input matInput type="time" formControlName="startTime" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Giờ kết thúc</mat-label>
          <input matInput type="time" formControlName="endTime" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side">
            @for (o of sideOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Địa điểm</mat-label>
          <input matInput formControlName="venue" />
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Địa chỉ</mat-label>
          <input matInput formControlName="address" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ngân sách sự kiện</mat-label>
          <input matInput appMoneyInput formControlName="budget" />
          <span matTextSuffix>₫</span>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Trạng thái</mat-label>
          <mat-select formControlName="status">
            @for (o of statusOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Mô tả</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>
        <label class="full">
          <mat-checkbox formControlName="main">Sự kiện chính (ngày cưới)</mat-checkbox>
        </label>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">Lưu</button>
      </mat-dialog-actions>
    </form>
  `
})
export class EventFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EventService);
  private readonly notify = inject(NotificationService);
  readonly data = inject<{ event?: WeddingEvent }>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<EventFormDialogComponent>);
  readonly sideOptions = SIDE_OPTIONS;
  readonly statusOptions = EVENT_STATUS_OPTIONS;
  readonly saving = signal(false);

  readonly form = this.fb.group({
    name: this.fb.nonNullable.control(this.data.event?.name ?? '', Validators.required),
    eventDate: this.fb.control<Date | null>(fromIsoDate(this.data.event?.eventDate) ?? new Date(), Validators.required),
    startTime: this.fb.control<string | null>(this.data.event?.startTime?.substring(0, 5) ?? null),
    endTime: this.fb.control<string | null>(this.data.event?.endTime?.substring(0, 5) ?? null),
    venue: this.fb.control<string | null>(this.data.event?.venue ?? null),
    address: this.fb.control<string | null>(this.data.event?.address ?? null),
    side: this.fb.nonNullable.control<Side>(this.data.event?.side ?? 'BOTH'),
    description: this.fb.control<string | null>(this.data.event?.description ?? null),
    budget: this.fb.nonNullable.control(this.data.event?.budget ?? 0),
    status: this.fb.nonNullable.control<EventStatus>(this.data.event?.status ?? 'PLANNED'),
    main: this.fb.nonNullable.control(this.data.event?.main ?? false)
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const request: EventRequest = {
      name: v.name.trim(),
      eventDate: toIsoDate(v.eventDate) ?? todayIso(),
      startTime: v.startTime || null,
      endTime: v.endTime || null,
      venue: v.venue?.trim() || null,
      address: v.address?.trim() || null,
      side: v.side,
      description: v.description?.trim() || null,
      budget: v.budget ?? 0,
      status: v.status,
      main: v.main
    };
    this.saving.set(true);
    const call = this.data.event ? this.service.update(this.data.event.id, request) : this.service.create(request);
    call.subscribe({
      next: (e) => {
        this.notify.success(this.data.event ? 'Đã cập nhật sự kiện' : 'Đã tạo sự kiện');
        this.ref.close(e);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.httpError(err);
      }
    });
  }
}
