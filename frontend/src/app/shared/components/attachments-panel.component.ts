import { Component, effect, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { AttachmentEntityType } from '../../core/models/enums';
import { Attachment } from '../../core/models/models';
import { AttachmentService } from '../../core/services/misc.services';
import { NotificationService } from '../../core/services/notification.service';
import { ConfirmService } from './confirm-dialog.component';
import { ViDatePipe } from '../pipes/vi-date.pipe';

@Component({
  selector: 'app-image-preview-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.name }}</h2>
    <mat-dialog-content>
      <img [src]="data.url" [alt]="data.name" style="max-width: 100%; max-height: 70vh; display: block; margin: 0 auto; border-radius: 8px" />
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Đóng</button>
    </mat-dialog-actions>
  `
})
export class ImagePreviewDialogComponent {
  readonly data = inject<{ url: SafeUrl; name: string }>(MAT_DIALOG_DATA);
}

/**
 * Lists, uploads, previews and deletes files attached to an entity (expense, vendor, ...).
 */
@Component({
  selector: 'app-attachments-panel',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule, ViDatePipe],
  template: `
    <div class="attachments">
      <div class="head">
        <h4>
          <mat-icon class="material-symbols-rounded">attach_file</mat-icon>
          {{ title() }}
          <span class="muted">({{ items().length }})</span>
        </h4>
        @if (!readonly()) {
          <button mat-stroked-button (click)="fileInput.click()" [disabled]="uploading()">
            <mat-icon class="material-symbols-rounded">upload</mat-icon>
            Tải lên
          </button>
          <input #fileInput type="file" hidden accept="image/jpeg,image/png,image/webp,image/gif,application/pdf" (change)="onFile($event)" />
        }
      </div>
      <p class="hint-text">Ảnh JPG/PNG/WEBP/GIF hoặc PDF, tối đa 10MB. Tệp được lưu trên máy chủ, không lưu trong database.</p>
      @if (uploading()) {
        <mat-progress-bar mode="indeterminate"></mat-progress-bar>
      }
      @if (items().length === 0) {
        <div class="empty muted">Chưa có tệp đính kèm</div>
      } @else {
        <div class="files">
          @for (a of items(); track a.id) {
            <div class="file">
              <div class="thumb" (click)="preview(a)" [class.clickable]="a.image">
                @if (a.image && thumbs()[a.id]) {
                  <img [src]="thumbs()[a.id]" [alt]="a.originalName" />
                } @else {
                  <mat-icon class="material-symbols-rounded">{{ a.image ? 'image' : 'picture_as_pdf' }}</mat-icon>
                }
              </div>
              <div class="meta">
                <div class="name truncate" [matTooltip]="a.originalName">{{ a.originalName }}</div>
                <div class="sub">{{ size(a.sizeBytes) }} · {{ a.createdAt | viDate: 'datetime' }} @if (a.uploadedBy) { · {{ a.uploadedBy }} }</div>
                @if (a.description) {
                  <div class="sub">{{ a.description }}</div>
                }
              </div>
              <div class="actions">
                <button mat-icon-button matTooltip="Tải xuống" (click)="download(a)">
                  <mat-icon class="material-symbols-rounded">download</mat-icon>
                </button>
                @if (!readonly()) {
                  <button mat-icon-button matTooltip="Xóa" (click)="remove(a)">
                    <mat-icon class="material-symbols-rounded">delete</mat-icon>
                  </button>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
      h4 { display: flex; align-items: center; gap: 6px; font-size: 14px; margin: 0; }
      h4 mat-icon { color: var(--wm-champagne); }
      .hint-text { margin: 4px 0 10px; }
      .empty { padding: 14px; text-align: center; border: 1px dashed var(--wm-border); border-radius: 10px; }
      .files { display: grid; gap: 10px; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); }
      .file { display: flex; gap: 10px; align-items: center; padding: 8px; border: 1px solid var(--wm-border); border-radius: 10px; background: var(--wm-surface-2); }
      .thumb { width: 54px; height: 54px; border-radius: 8px; background: var(--wm-cream); display: grid; place-items: center; overflow: hidden; flex-shrink: 0; }
      .thumb img { width: 100%; height: 100%; object-fit: cover; }
      .thumb.clickable { cursor: zoom-in; }
      .thumb mat-icon { color: var(--wm-brown); }
      .meta { min-width: 0; flex: 1; }
      .name { font-weight: 500; font-size: 13px; }
      .sub { font-size: 11.5px; color: var(--wm-muted); }
      .actions { display: flex; }
    `
  ]
})
export class AttachmentsPanelComponent {
  private readonly service = inject(AttachmentService);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly dialog = inject(MatDialog);
  private readonly sanitizer = inject(DomSanitizer);

  readonly entityType = input.required<AttachmentEntityType>();
  readonly entityId = input.required<number>();
  readonly title = input('Hóa đơn & chứng từ');
  readonly readonly = input(false);
  readonly changed = output<number>();

  readonly items = signal<Attachment[]>([]);
  readonly thumbs = signal<Record<number, SafeUrl>>({});
  readonly uploading = signal(false);

  constructor() {
    effect(() => {
      const id = this.entityId();
      const type = this.entityType();
      if (id) this.load(type, id);
    });
  }

  load(type = this.entityType(), id = this.entityId()): void {
    this.service.list(type, id).subscribe({
      next: (items) => {
        this.items.set(items);
        items.filter((a) => a.image && !this.thumbs()[a.id]).forEach((a) => this.loadThumb(a));
      },
      error: (err) => this.notify.httpError(err)
    });
  }

  private loadThumb(a: Attachment): void {
    this.service.blob(a).subscribe({
      next: (blob) => {
        const url = this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(blob));
        this.thumbs.update((t) => ({ ...t, [a.id]: url }));
      }
    });
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > 10 * 1024 * 1024) {
      this.notify.error('Tệp vượt quá 10MB');
      return;
    }
    this.uploading.set(true);
    this.service.upload(this.entityType(), this.entityId(), file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.notify.success('Đã tải lên ' + file.name);
        this.load();
        this.changed.emit(this.items().length + 1);
      },
      error: (err) => {
        this.uploading.set(false);
        this.notify.httpError(err, 'Không tải lên được tệp');
      }
    });
  }

  preview(a: Attachment): void {
    if (!a.image) {
      this.download(a);
      return;
    }
    const url = this.thumbs()[a.id];
    if (url) {
      this.dialog.open(ImagePreviewDialogComponent, { data: { url, name: a.originalName }, maxWidth: '95vw' });
    }
  }

  download(a: Attachment): void {
    this.service.download(a).subscribe({ error: (err) => this.notify.httpError(err) });
  }

  remove(a: Attachment): void {
    this.confirm.delete(`tệp "${a.originalName}"`).subscribe((ok) => {
      if (!ok) return;
      this.service.delete(a.id).subscribe({
        next: () => {
          this.notify.success('Đã xóa tệp');
          this.load();
          this.changed.emit(Math.max(0, this.items().length - 1));
        },
        error: (err) => this.notify.httpError(err)
      });
    });
  }

  size(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB';
    return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  }
}
