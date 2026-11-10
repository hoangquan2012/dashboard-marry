import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { BackupInfo, RestoreResult } from '../../core/models/models';
import { SystemService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-backup',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, LoadingComponent, ErrorStateComponent, ViDatePipe],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Cài đặt</div>
          <h1>Sao lưu và khôi phục</h1>
          <div class="subtitle">Xuất toàn bộ dữ liệu đám cưới ra file JSON. Khôi phục sẽ tạo bản snapshot trước khi ghi đè. Tài khoản người dùng không bị ghi đè.</div>
        </div>
        <div class="actions">
          <a mat-stroked-button routerLink="/settings"><mat-icon class="material-symbols-rounded">arrow_back</mat-icon> Cài đặt</a>
        </div>
      </div>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (info(); as i) {
        <div class="card" style="margin-bottom: 18px">
          <dl class="kv">
            <dt>Phiên bản</dt><dd>{{ i.version }}</dd>
            <dt>Thời điểm</dt><dd>{{ i.generatedAt | viDate: 'datetime' }}</dd>
            <dt>Số bảng</dt><dd>{{ i.tables.length }}</dd>
            <dt>Tổng dòng</dt><dd>{{ i.totalRows }}</dd>
            <dt>Tên file gợi ý</dt><dd>{{ i.fileName }}</dd>
          </dl>
          <button mat-flat-button color="primary" (click)="export()" style="margin-top: 16px">
            <mat-icon class="material-symbols-rounded">download</mat-icon> Tải bản sao lưu
          </button>
        </div>
      }

      <div class="card">
        <div class="card-title"><h3>Khôi phục từ file</h3></div>
        <p class="muted">Chọn file JSON đã xuất. Hệ thống tạo bản backup hiện tại trước khi ghi. Không ghi đè tài khoản đăng nhập.</p>
        <button mat-stroked-button type="button" (click)="fileInput.click()">
          <mat-icon class="material-symbols-rounded">upload</mat-icon> Chọn file backup
        </button>
        <input #fileInput type="file" hidden accept="application/json,.json" (change)="restore($event)" />
        @if (result(); as r) {
          <div class="result" style="margin-top: 14px">
            <p><strong>{{ r.message }}</strong></p>
            <p class="muted">Đã khôi phục {{ r.restoredRows }} dòng. Snapshot trước khôi phục: {{ r.preRestoreBackupFile }}</p>
          </div>
        }
      </div>
    </div>
  `
})
export class BackupComponent implements OnInit {
  private readonly system = inject(SystemService);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);

  readonly info = signal<BackupInfo | null>(null);
  readonly result = signal<RestoreResult | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.system.backupInfo().subscribe({
      next: (i) => { this.info.set(i); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  export(): void {
    this.system.exportBackup().subscribe({
      next: (n) => this.notify.success('Đã tải ' + n),
      error: (err) => this.notify.httpError(err)
    });
  }

  restore(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.confirm
      .confirm({
        title: 'Khôi phục dữ liệu',
        message: `Khôi phục từ <b>${file.name}</b>? Dữ liệu hiện tại sẽ được snapshot rồi thay thế. Tài khoản đăng nhập được giữ nguyên.`,
        confirmText: 'Khôi phục',
        danger: true,
        icon: 'restore'
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.system.restoreBackup(file).subscribe({
          next: (r) => {
            this.result.set(r);
            this.notify.success(r.message);
            this.load();
          },
          error: (err) => this.notify.httpError(err)
        });
      });
  }
}
