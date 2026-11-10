import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="state-box loading">
      <mat-spinner [diameter]="36"></mat-spinner>
      <p>{{ text() }}</p>
    </div>
  `,
  styles: [`.loading p { margin-top: 14px; }`]
})
export class LoadingComponent {
  readonly text = input('Đang tải dữ liệu...');
}

@Component({
  selector: 'app-empty',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  template: `
    <div class="state-box">
      <mat-icon class="material-symbols-rounded">{{ icon() }}</mat-icon>
      <h4>{{ title() }}</h4>
      @if (message()) {
        <p>{{ message() }}</p>
      }
      @if (actionText()) {
        <button mat-flat-button color="primary" (click)="action.emit()">
          <mat-icon class="material-symbols-rounded">add</mat-icon>
          {{ actionText() }}
        </button>
      }
    </div>
  `
})
export class EmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input('Chưa có dữ liệu');
  readonly message = input<string | null>(null);
  readonly actionText = input<string | null>(null);
  readonly action = output<void>();
}

@Component({
  selector: 'app-error-state',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  template: `
    <div class="state-box">
      <mat-icon class="material-symbols-rounded" style="color: var(--wm-danger)">error</mat-icon>
      <h4>Không tải được dữ liệu</h4>
      <p>{{ message() }}</p>
      <button mat-stroked-button (click)="retry.emit()">
        <mat-icon class="material-symbols-rounded">refresh</mat-icon>
        Thử lại
      </button>
    </div>
  `
})
export class ErrorStateComponent {
  readonly message = input('Máy chủ không phản hồi hoặc đã xảy ra lỗi.');
  readonly retry = output<void>();
}
