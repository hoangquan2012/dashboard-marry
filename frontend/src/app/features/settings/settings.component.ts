import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { WeddingSettings } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { SettingsService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { fromIsoDate, toIsoDate } from '../../core/utils/vn-date-adapter';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { MoneyInputDirective } from '../../shared/directives/money-input.directive';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    LoadingComponent,
    ErrorStateComponent,
    MoneyInputDirective
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Hệ thống</div>
          <h1>Cài đặt đám cưới</h1>
          <div class="subtitle">Tên cô dâu chú rể, ngày cưới và ngân sách tổng. Website không cho đăng ký công khai.</div>
        </div>
      </div>

      <div class="grid cols-3" style="margin-bottom: 18px">
        <a class="card link" routerLink="/settings/audit">
          <h3><mat-icon class="material-symbols-rounded">history</mat-icon> Nhật ký</h3>
          <p class="muted">Ai sửa tiền mừng, khoản chi, trạng thái khách.</p>
        </a>
        @if (auth.isAdmin()) {
          <a class="card link" routerLink="/settings/users">
            <h3><mat-icon class="material-symbols-rounded">manage_accounts</mat-icon> Tài khoản</h3>
            <p class="muted">Tạo nhân viên hỗ trợ check-in, không tự đăng ký.</p>
          </a>
          <a class="card link" routerLink="/settings/backup">
            <h3><mat-icon class="material-symbols-rounded">cloud_download</mat-icon> Sao lưu</h3>
            <p class="muted">Xuất JSON và khôi phục an toàn.</p>
          </a>
        }
      </div>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else {
        <form class="card form-grid" [formGroup]="form" (ngSubmit)="save()">
          <mat-form-field>
            <mat-label>Tên chú rể</mat-label>
            <input matInput formControlName="groomName" required />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Tên cô dâu</mat-label>
            <input matInput formControlName="brideName" required />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Ngày cưới</mat-label>
            <input matInput [matDatepicker]="p" formControlName="weddingDate" />
            <mat-datepicker-toggle matIconSuffix [for]="p"></mat-datepicker-toggle>
            <mat-datepicker #p></mat-datepicker>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Ngân sách tổng</mat-label>
            <input matInput appMoneyInput formControlName="totalBudget" />
            <span matTextSuffix>₫</span>
          </mat-form-field>
          <mat-form-field class="full">
            <mat-label>Địa điểm chính</mat-label>
            <input matInput formControlName="mainVenue" />
          </mat-form-field>
          <mat-form-field class="full">
            <mat-label>Thông tin nhà trai</mat-label>
            <textarea matInput formControlName="groomFamilyInfo" rows="2"></textarea>
          </mat-form-field>
          <mat-form-field class="full">
            <mat-label>Thông tin nhà gái</mat-label>
            <textarea matInput formControlName="brideFamilyInfo" rows="2"></textarea>
          </mat-form-field>
          <mat-form-field class="full">
            <mat-label>Ghi chú</mat-label>
            <textarea matInput formControlName="note" rows="2"></textarea>
          </mat-form-field>
          <div class="full" style="text-align: right">
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">Lưu cài đặt</button>
          </div>
        </form>
      }
    </div>
  `,
  styles: [`.link { display: block; color: inherit; } .link h3 { display: flex; gap: 8px; align-items: center; } .link:hover { box-shadow: var(--wm-shadow-hover); }`]
})
export class SettingsComponent implements OnInit {
  private readonly service = inject(SettingsService);
  private readonly fb = inject(FormBuilder);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly form = this.fb.group({
    groomName: this.fb.nonNullable.control('', Validators.required),
    brideName: this.fb.nonNullable.control('', Validators.required),
    weddingDate: this.fb.control<Date | null>(null),
    totalBudget: this.fb.nonNullable.control(0),
    mainVenue: this.fb.control<string | null>(null),
    groomFamilyInfo: this.fb.control<string | null>(null),
    brideFamilyInfo: this.fb.control<string | null>(null),
    note: this.fb.control<string | null>(null)
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.get().subscribe({
      next: (s) => { this.patch(s); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  private patch(s: WeddingSettings): void {
    this.form.patchValue({
      groomName: s.groomName,
      brideName: s.brideName,
      weddingDate: fromIsoDate(s.weddingDate),
      totalBudget: s.totalBudget,
      mainVenue: s.mainVenue,
      groomFamilyInfo: s.groomFamilyInfo,
      brideFamilyInfo: s.brideFamilyInfo,
      note: s.note
    });
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.service.update({
      groomName: v.groomName.trim(),
      brideName: v.brideName.trim(),
      weddingDate: toIsoDate(v.weddingDate),
      totalBudget: v.totalBudget ?? 0,
      mainVenue: v.mainVenue?.trim() || null,
      groomFamilyInfo: v.groomFamilyInfo?.trim() || null,
      brideFamilyInfo: v.brideFamilyInfo?.trim() || null,
      note: v.note?.trim() || null
    }).subscribe({
      next: (s) => { this.saving.set(false); this.patch(s); this.notify.success('Đã lưu cài đặt'); },
      error: (err) => { this.saving.set(false); this.notify.httpError(err); }
    });
  }
}
