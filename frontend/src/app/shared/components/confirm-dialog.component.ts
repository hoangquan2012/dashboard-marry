import { Component, Injectable, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Observable } from 'rxjs';

export interface ConfirmData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
  icon?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon class="material-symbols-rounded" [class.danger]="data.danger">{{ data.icon ?? (data.danger ? 'warning' : 'help') }}</mat-icon>
      {{ data.title }}
    </h2>
    <mat-dialog-content>
      <p [innerHTML]="data.message"></p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>{{ data.cancelText ?? 'Hủy' }}</button>
      <button mat-flat-button [color]="data.danger ? 'warn' : 'primary'" [mat-dialog-close]="true" cdkFocusInitial>
        {{ data.confirmText ?? 'Xác nhận' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      h2 { display: flex; align-items: center; gap: 10px; }
      mat-icon { color: var(--wm-champagne); }
      mat-icon.danger { color: var(--wm-danger); }
      p { margin: 0; line-height: 1.6; max-width: 460px; }
    `
  ]
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<ConfirmDialogComponent>);
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(MatDialog);

  confirm(data: ConfirmData): Observable<boolean | undefined> {
    return this.dialog
      .open(ConfirmDialogComponent, { data, width: '440px', autoFocus: false })
      .afterClosed();
  }

  delete(what: string, extra = ''): Observable<boolean | undefined> {
    return this.confirm({
      title: 'Xác nhận xóa',
      message: `Bạn có chắc chắn muốn xóa <b>${what}</b>?${extra ? '<br>' + extra : ''}`,
      confirmText: 'Xóa',
      danger: true,
      icon: 'delete'
    });
  }
}
