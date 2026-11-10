import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { USER_ROLE_OPTIONS, UserRole } from '../../core/models/enums';
import { UserInfo } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { SystemService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatSlideToggleModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.user ? 'Sửa tài khoản' : 'Tạo tài khoản' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        @if (!data.user) {
          <mat-form-field class="full">
            <mat-label>Tên đăng nhập</mat-label>
            <input matInput formControlName="username" required />
          </mat-form-field>
          <mat-form-field class="full">
            <mat-label>Mật khẩu</mat-label>
            <input matInput type="password" formControlName="password" required />
          </mat-form-field>
        }
        <mat-form-field class="full">
          <mat-label>Họ tên</mat-label>
          <input matInput formControlName="fullName" required />
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Vai trò</mat-label>
          <mat-select formControlName="role">
            @for (o of roles; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        @if (data.user) {
          <label class="full"><mat-slide-toggle formControlName="enabled">Đang hoạt động</mat-slide-toggle></label>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Lưu</button>
      </mat-dialog-actions>
    </form>
  `
})
export class UserFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<{ user?: UserInfo }>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<UserFormDialogComponent>);
  readonly roles = USER_ROLE_OPTIONS;
  readonly form = this.fb.group({
    username: this.fb.nonNullable.control(this.data.user?.username ?? '', this.data.user ? [] : Validators.required),
    password: this.fb.nonNullable.control('', this.data.user ? [] : [Validators.required, Validators.minLength(8)]),
    fullName: this.fb.nonNullable.control(this.data.user?.fullName ?? '', Validators.required),
    role: this.fb.nonNullable.control<UserRole>(this.data.user?.role ?? 'STAFF'),
    enabled: this.fb.nonNullable.control(this.data.user?.enabled ?? true)
  });

  submit(): void {
    this.ref.close(this.form.getRawValue());
  }
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatTableModule, LoadingComponent, ErrorStateComponent, LabelPipe, ViDatePipe],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Cài đặt</div>
          <h1>Tài khoản quản trị</h1>
          <div class="subtitle">Không có đăng ký công khai. Chỉ quản trị viên mới tạo được tài khoản.</div>
        </div>
        <div class="actions">
          <a mat-stroked-button routerLink="/settings"><mat-icon class="material-symbols-rounded">arrow_back</mat-icon> Cài đặt</a>
          <button mat-flat-button color="primary" (click)="openForm()"><mat-icon class="material-symbols-rounded">person_add</mat-icon> Tạo tài khoản</button>
        </div>
      </div>
      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else {
        <div class="table-wrapper">
          <table mat-table [dataSource]="users()">
            <ng-container matColumnDef="user"><th mat-header-cell *matHeaderCellDef>Tài khoản</th>
              <td mat-cell *matCellDef="let u"><div class="cell-main">{{ u.fullName }}</div><div class="cell-sub">{{ u.username }}</div></td></ng-container>
            <ng-container matColumnDef="role"><th mat-header-cell *matHeaderCellDef>Vai trò</th>
              <td mat-cell *matCellDef="let u">{{ u.role | label: 'role' }}</td></ng-container>
            <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>Trạng thái</th>
              <td mat-cell *matCellDef="let u"><span [class]="u.enabled ? 'chip chip-success' : 'chip chip-neutral'">{{ u.enabled ? 'Hoạt động' : 'Khóa' }}</span></td></ng-container>
            <ng-container matColumnDef="login"><th mat-header-cell *matHeaderCellDef>Đăng nhập gần nhất</th>
              <td mat-cell *matCellDef="let u">{{ u.lastLoginAt | viDate: 'datetime' }}</td></ng-container>
            <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let u" class="text-right">
                <button mat-icon-button (click)="openForm(u)"><mat-icon class="material-symbols-rounded">edit</mat-icon></button>
                <button mat-icon-button (click)="resetPw(u)"><mat-icon class="material-symbols-rounded">password</mat-icon></button>
                <button mat-icon-button (click)="remove(u)" [disabled]="u.id === me()"><mat-icon class="material-symbols-rounded">delete</mat-icon></button>
              </td></ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols"></tr>
          </table>
        </div>
      }
    </div>
  `
})
export class UsersComponent implements OnInit {
  private readonly system = inject(SystemService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly auth = inject(AuthService);

  readonly users = signal<UserInfo[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly cols = ['user', 'role', 'status', 'login', 'actions'];

  me(): number | undefined {
    return this.auth.user()?.id;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.system.users().subscribe({
      next: (u) => { this.users.set(u); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  openForm(user?: UserInfo): void {
    this.dialog.open(UserFormDialogComponent, { data: { user }, width: '440px' }).afterClosed().subscribe((v) => {
      if (!v) return;
      if (user) {
        this.system.updateUser(user.id, { fullName: v.fullName, role: v.role, enabled: v.enabled }).subscribe({
          next: () => { this.notify.success('Đã cập nhật tài khoản'); this.load(); },
          error: (err) => this.notify.httpError(err)
        });
      } else {
        this.system.createUser({ username: v.username, password: v.password, fullName: v.fullName, role: v.role }).subscribe({
          next: () => { this.notify.success('Đã tạo tài khoản'); this.load(); },
          error: (err) => this.notify.httpError(err)
        });
      }
    });
  }

  resetPw(u: UserInfo): void {
    const pw = prompt(`Mật khẩu mới cho ${u.username} (tối thiểu 8 ký tự)`);
    if (!pw) return;
    this.system.resetPassword(u.id, pw).subscribe({
      next: () => this.notify.success('Đã đặt lại mật khẩu'),
      error: (err) => this.notify.httpError(err)
    });
  }

  remove(u: UserInfo): void {
    this.confirm.delete(`tài khoản "${u.username}"`).subscribe((ok) => {
      if (!ok) return;
      this.system.deleteUser(u.id).subscribe({
        next: () => { this.notify.success('Đã xóa tài khoản'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
