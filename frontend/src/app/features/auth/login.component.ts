import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { extractMessage } from '../../core/services/notification.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="login-page">
      <div class="login-art">
        <div class="art-inner">
          <div class="mark"><mat-icon class="material-symbols-rounded icon-filled">favorite</mat-icon></div>
          <h1 class="serif">Wedding<br />Management</h1>
          <p>Quản lý khách mời, tiền mừng, chi phí, ngân sách, sơ đồ bàn tiệc và check-in – tất cả trong một nơi, dành riêng cho ngày trọng đại của bạn.</p>
          <ul>
            <li><mat-icon class="material-symbols-rounded">groups</mat-icon> Danh sách khách nhà trai, nhà gái</li>
            <li><mat-icon class="material-symbols-rounded">redeem</mat-icon> Theo dõi tiền mừng chính xác từng khách</li>
            <li><mat-icon class="material-symbols-rounded">account_balance_wallet</mat-icon> Ngân sách, chi phí, công nợ rõ ràng</li>
            <li><mat-icon class="material-symbols-rounded">qr_code_scanner</mat-icon> Check-in bằng QR trong ngày cưới</li>
          </ul>
        </div>
      </div>
      <div class="login-form-wrap">
        <form class="login-card" [formGroup]="form" (ngSubmit)="submit()">
          <div class="eyebrow">Hệ thống quản trị riêng tư</div>
          <h2 class="serif">Đăng nhập</h2>
          <p class="muted">Nhập tài khoản quản trị để tiếp tục.</p>

          <mat-form-field>
            <mat-label>Tên đăng nhập</mat-label>
            <mat-icon matPrefix class="material-symbols-rounded">person</mat-icon>
            <input matInput formControlName="username" autocomplete="username" autofocus />
            @if (form.controls.username.hasError('required')) {
              <mat-error>Vui lòng nhập tên đăng nhập</mat-error>
            }
          </mat-form-field>

          <mat-form-field>
            <mat-label>Mật khẩu</mat-label>
            <mat-icon matPrefix class="material-symbols-rounded">lock</mat-icon>
            <input matInput [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" />
            <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())" [attr.aria-label]="'Hiện mật khẩu'">
              <mat-icon class="material-symbols-rounded">{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (form.controls.password.hasError('required')) {
              <mat-error>Vui lòng nhập mật khẩu</mat-error>
            }
          </mat-form-field>

          @if (error()) {
            <div class="error-box">
              <mat-icon class="material-symbols-rounded">error</mat-icon>
              {{ error() }}
            </div>
          }

          <button mat-flat-button color="primary" class="submit" type="submit" [disabled]="loading()">
            @if (loading()) {
              <mat-spinner [diameter]="20"></mat-spinner>
            } @else {
              <span>Đăng nhập</span>
            }
          </button>

          <p class="hint-text footer">Tài khoản quản trị đầu tiên được tạo từ biến môi trường <code>APP_ADMIN_USERNAME</code> / <code>APP_ADMIN_PASSWORD</code>. Không có chức năng tự đăng ký.</p>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .login-page { min-height: 100vh; display: grid; grid-template-columns: 1.1fr 1fr; background: var(--wm-bg); }
      .login-art {
        background: radial-gradient(circle at 20% 20%, #a0345a 0%, #7a1f3d 40%, #4d0f24 100%);
        color: #fbeff2; display: flex; align-items: center; justify-content: center; padding: 48px; position: relative; overflow: hidden;
      }
      .login-art::before { content: ''; position: absolute; inset: -40%; background: radial-gradient(circle, rgba(228,194,133,0.18) 0, transparent 45%); }
      .art-inner { position: relative; max-width: 460px; }
      .mark { width: 60px; height: 60px; border-radius: 18px; background: linear-gradient(135deg, #e4c285, #c9a96e); display: grid; place-items: center; color: #5c132c; margin-bottom: 22px; box-shadow: 0 10px 30px rgba(0,0,0,0.25); }
      .mark mat-icon { font-size: 32px; width: 32px; height: 32px; }
      .login-art h1 { font-size: 44px; line-height: 1.05; color: #fff; margin-bottom: 18px; }
      .login-art p { font-size: 15px; line-height: 1.7; color: rgba(255,255,255,0.85); }
      .login-art ul { list-style: none; padding: 0; margin: 26px 0 0; display: grid; gap: 12px; }
      .login-art li { display: flex; align-items: center; gap: 10px; font-size: 14px; color: #ffe7c2; }
      .login-art li mat-icon { color: #e4c285; }
      .login-form-wrap { display: flex; align-items: center; justify-content: center; padding: 32px; }
      .login-card { width: 100%; max-width: 420px; background: #fff; border-radius: 22px; padding: 36px 34px 28px; box-shadow: var(--wm-shadow-hover); display: flex; flex-direction: column; gap: 14px; }
      .eyebrow { text-transform: uppercase; letter-spacing: 0.14em; font-size: 11px; color: var(--wm-gold); font-weight: 600; }
      h2 { font-size: 30px; color: var(--wm-burgundy-dark); }
      .login-card > p.muted { margin: -8px 0 6px; }
      .submit { height: 48px; font-size: 15px; margin-top: 4px; }
      .submit mat-spinner { margin: 0 auto; }
      .error-box { display: flex; gap: 8px; align-items: center; background: var(--wm-danger-bg); color: var(--wm-danger); padding: 10px 12px; border-radius: 10px; font-size: 13px; }
      .footer { margin: 10px 0 0; text-align: center; }
      code { background: var(--wm-cream); padding: 1px 5px; border-radius: 4px; font-size: 11.5px; }
      @media (max-width: 900px) { .login-page { grid-template-columns: 1fr; } .login-art { display: none; } }
    `
  ]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl && returnUrl !== '/login' ? returnUrl : '/dashboard');
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractMessage(err, 'Đăng nhập thất bại'));
      }
    });
  }
}
