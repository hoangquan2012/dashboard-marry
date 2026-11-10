import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ImportResult } from '../../core/models/models';
import { GuestService } from '../../core/services/guest.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-guest-import-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatCheckboxModule, MatProgressBarModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">Nhập khách mời từ Excel</h2>
    <mat-dialog-content>
      <p>Tải file mẫu, điền danh sách rồi tải lại. Cột <b>Họ và tên</b> và <b>Bên</b> là bắt buộc.</p>
      <div class="actions">
        <button mat-stroked-button type="button" (click)="downloadTemplate()">
          <mat-icon class="material-symbols-rounded">download</mat-icon>
          Tải file Excel mẫu
        </button>
        <button mat-flat-button color="primary" type="button" (click)="fileInput.click()">
          <mat-icon class="material-symbols-rounded">upload_file</mat-icon>
          Chọn file .xlsx
        </button>
        <input #fileInput type="file" hidden accept=".xlsx,.xls" (change)="onFile($event)" />
      </div>
      @if (fileName()) {
        <p class="file">Đã chọn: <strong>{{ fileName() }}</strong></p>
      }
      <label class="skip">
        <mat-checkbox [checked]="skipDuplicates()" (change)="skipDuplicates.set($event.checked)">
          Bỏ qua khách trùng số điện thoại
        </mat-checkbox>
      </label>
      @if (uploading()) {
        <mat-progress-bar mode="indeterminate"></mat-progress-bar>
      }
      @if (result(); as r) {
        <div class="result">
          <p><strong>Đã nhập {{ r.imported }}</strong> / {{ r.totalRows }} dòng
            · bỏ qua {{ r.skipped }} · lỗi {{ r.failed }}</p>
          @if (r.skippedRows.length) {
            <details open>
              <summary>Dòng bỏ qua</summary>
              <ul>
                @for (row of r.skippedRows; track row.row) {
                  <li>Dòng {{ row.row }}{{ row.name ? ' (' + row.name + ')' : '' }}: {{ row.message }}</li>
                }
              </ul>
            </details>
          }
          @if (r.errors.length) {
            <details open>
              <summary>Dòng lỗi</summary>
              <ul>
                @for (row of r.errors; track row.row) {
                  <li>Dòng {{ row.row }}{{ row.name ? ' (' + row.name + ')' : '' }}: {{ row.message }}</li>
                }
              </ul>
            </details>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Đóng</button>
      @if (result()?.imported) {
        <button mat-flat-button color="primary" [mat-dialog-close]="true">Xem danh sách</button>
      }
    </mat-dialog-actions>
  `,
  styles: [
    `
      .actions { display: flex; gap: 10px; flex-wrap: wrap; margin: 12px 0; }
      .file { margin: 8px 0; }
      .skip { display: block; margin: 8px 0 14px; }
      .result { margin-top: 12px; background: var(--wm-surface-2); border-radius: 10px; padding: 12px; }
      ul { margin: 6px 0 0; padding-left: 18px; font-size: 13px; }
    `
  ]
})
export class GuestImportDialogComponent {
  private readonly service = inject(GuestService);
  private readonly notify = inject(NotificationService);
  readonly ref = inject(MatDialogRef<GuestImportDialogComponent>);

  readonly fileName = signal<string | null>(null);
  readonly skipDuplicates = signal(true);
  readonly uploading = signal(false);
  readonly result = signal<ImportResult | null>(null);

  downloadTemplate(): void {
    this.service.downloadTemplate().subscribe({
      next: (name) => this.notify.success('Đã tải ' + name),
      error: (err) => this.notify.httpError(err)
    });
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.fileName.set(file.name);
    this.uploading.set(true);
    this.result.set(null);
    this.service.import(file, this.skipDuplicates()).subscribe({
      next: (r) => {
        this.uploading.set(false);
        this.result.set(r);
        if (r.imported > 0) this.notify.success(`Đã nhập ${r.imported} khách mời`);
        else this.notify.info('Không có khách nào được nhập');
      },
      error: (err) => {
        this.uploading.set(false);
        this.notify.httpError(err, 'Không nhập được file Excel');
      }
    });
  }
}
