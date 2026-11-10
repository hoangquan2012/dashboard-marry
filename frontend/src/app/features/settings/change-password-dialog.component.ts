import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">Đổi mật khẩu</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-form-field>
          <mat-label>Mật khẩu hiện tại</mat-label>
          <input matInput type="password" formControlName="currentPassword" autocomplete="current-password" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Mật khẩu mới</mat-label>
          <input matInput type="password" formControlName="newPassword" autocomplete="new-password" />
          <mat-hint>Ít nhất 8 ký tự</mat-hint>
          @if (form.controls.newPassword.hasError('minlength')) {
            <mat-error>Mật khẩu mới phải có ít nhất 8 ký tự</mat-error>
          }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Nhập lại mật khẩu mới</mat-label>
          <input matInput type="password" formControlName="confirm" autocomplete="new-password" />
          @if (form.hasError('mismatch') && form.controls.confirm.touched) {
            <mat-error>Mật khẩu nhập lại không khớp</mat-error>
          }
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">Lưu mật khẩu</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: [`mat-form-field { display: block; margin-bottom: 8px; }`]
})
export class ChangePasswordDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly ref = inject(MatDialogRef<ChangePasswordDialogComponent>);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required]
    },
    { validators: (g) => (g.get('newPassword')?.value === g.get('confirm')?.value ? null : { mismatch: true }) }
  );

  submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.auth.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.notify.success('Đã đổi mật khẩu');
        this.ref.close(true);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.httpError(err);
      }
    });
  }
}
