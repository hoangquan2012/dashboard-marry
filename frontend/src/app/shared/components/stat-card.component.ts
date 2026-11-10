import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * KPI card used on the dashboard and module headers.
 */
@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="stat" [class]="'tone-' + tone()">
      <div class="icon-wrap">
        <mat-icon class="material-symbols-rounded">{{ icon() }}</mat-icon>
      </div>
      <div class="body">
        <div class="label">{{ label() }}</div>
        <div class="value">{{ value() }}</div>
        @if (sub()) {
          <div class="sub">{{ sub() }}</div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .stat {
        display: flex;
        gap: 14px;
        align-items: flex-start;
        background: var(--wm-surface);
        border-radius: var(--wm-radius);
        box-shadow: var(--wm-shadow);
        border: 1px solid rgba(232, 223, 213, 0.6);
        padding: 16px 18px;
        height: 100%;
        transition: transform 0.15s ease, box-shadow 0.15s ease;
        position: relative;
        overflow: hidden;
      }
      .stat::after {
        content: '';
        position: absolute;
        right: -30px;
        top: -30px;
        width: 90px;
        height: 90px;
        border-radius: 50%;
        background: var(--tone-bg, var(--wm-champagne-light));
        opacity: 0.45;
      }
      .stat:hover { transform: translateY(-2px); box-shadow: var(--wm-shadow-hover); }
      .icon-wrap {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        display: grid;
        place-items: center;
        background: var(--tone-bg, var(--wm-champagne-light));
        color: var(--tone-fg, var(--wm-gold));
        flex-shrink: 0;
        z-index: 1;
      }
      .body { min-width: 0; z-index: 1; }
      .label { font-size: 12.5px; color: var(--wm-muted); font-weight: 500; }
      .value { font-size: 21px; font-weight: 700; color: var(--wm-text); margin-top: 2px; font-variant-numeric: tabular-nums; line-height: 1.2; word-break: break-word; }
      .sub { font-size: 12px; color: var(--wm-muted); margin-top: 4px; }
      .tone-gold { --tone-bg: var(--wm-champagne-light); --tone-fg: var(--wm-gold); }
      .tone-burgundy { --tone-bg: var(--wm-burgundy-light); --tone-fg: var(--wm-burgundy); }
      .tone-success { --tone-bg: var(--wm-success-bg); --tone-fg: var(--wm-success); }
      .tone-warning { --tone-bg: var(--wm-warning-bg); --tone-fg: var(--wm-warning); }
      .tone-danger { --tone-bg: var(--wm-danger-bg); --tone-fg: var(--wm-danger); }
      .tone-info { --tone-bg: var(--wm-info-bg); --tone-fg: var(--wm-info); }
      .tone-brown { --tone-bg: var(--wm-brown-light); --tone-fg: var(--wm-brown); }
    `
  ]
})
export class StatCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly icon = input<string>('insights');
  readonly sub = input<string | null | undefined>(null);
  readonly tone = input<'gold' | 'burgundy' | 'success' | 'warning' | 'danger' | 'info' | 'brown'>('gold');
}
