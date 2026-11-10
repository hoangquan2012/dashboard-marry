import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { formatVnd, formatVndShort } from '../pipes/vnd.pipe';

/** Brand palette for charts (burgundy, champagne, brown, sage, slate...). */
export const CHART_COLORS = ['#7a1f3d', '#c9a96e', '#8c6a4f', '#5f8f74', '#3b6fa5', '#d47f8f', '#b8903f', '#6d5c7a', '#e0b084', '#4d8a95', '#a0345a', '#9aa58c'];

export const CHART_FONT = "'Be Vietnam Pro', 'Segoe UI', sans-serif";

export function moneyTooltip(): ChartConfiguration['options'] {
  return {
    plugins: {
      tooltip: {
        callbacks: {
          label: (ctx) => {
            const v = typeof ctx.parsed === 'number' ? ctx.parsed : (ctx.parsed as { y?: number; x?: number })?.y ?? (ctx.parsed as { x?: number })?.x;
            return `${ctx.dataset.label ? ctx.dataset.label + ': ' : ''}${formatVnd(v as number)}`;
          }
        }
      }
    }
  };
}

export function moneyAxis(axis: 'x' | 'y' = 'y'): ChartConfiguration['options'] {
  return {
    scales: {
      [axis]: {
        ticks: { callback: (v) => formatVndShort(Number(v)) },
        grid: { color: 'rgba(0,0,0,0.05)' }
      }
    }
  };
}

@Component({
  selector: 'app-chart-card',
  standalone: true,
  imports: [BaseChartDirective, MatIconModule],
  template: `
    <div class="card chart-card">
      <div class="card-title">
        <h3>
          @if (icon()) {
            <mat-icon class="material-symbols-rounded">{{ icon() }}</mat-icon>
          }
          {{ title() }}
        </h3>
        @if (hint()) {
          <span class="hint">{{ hint() }}</span>
        }
      </div>
      @if (hasData()) {
        <div class="chart-wrap" [style.height.px]="height()">
          <canvas baseChart [type]="type()" [data]="data()" [options]="mergedOptions()"></canvas>
        </div>
      } @else {
        <div class="state-box small">
          <mat-icon class="material-symbols-rounded">bar_chart</mat-icon>
          <p>Chưa có dữ liệu để vẽ biểu đồ</p>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .chart-card { height: 100%; display: flex; flex-direction: column; }
      .chart-wrap { position: relative; width: 100%; }
      .state-box.small { padding: 24px; }
      .state-box.small mat-icon { font-size: 36px; width: 36px; height: 36px; }
    `
  ]
})
export class ChartCardComponent {
  readonly title = input.required<string>();
  readonly icon = input<string | null>(null);
  readonly hint = input<string | null>(null);
  readonly type = input<ChartType>('bar');
  readonly data = input.required<ChartData>();
  readonly options = input<ChartConfiguration['options'] | object>({});
  readonly height = input(260);

  readonly hasData = computed(() => {
    const d = this.data();
    return !!d && d.datasets.some((ds) => Array.isArray(ds.data) && ds.data.some((v) => v !== null && v !== undefined && Number(v) !== 0));
  });

  readonly mergedOptions = computed<ChartConfiguration['options']>(() => {
    const base: ChartConfiguration['options'] = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { usePointStyle: true, boxWidth: 8, padding: 16, font: { family: CHART_FONT, size: 12 } }
        },
        tooltip: { bodyFont: { family: CHART_FONT }, titleFont: { family: CHART_FONT } }
      }
    };
    return deepMerge(base, this.options() ?? {});
  });
}

function deepMerge<T>(a: T, b: Partial<T>): T {
  const out: Record<string, unknown> = { ...(a as Record<string, unknown>) };
  for (const [k, v] of Object.entries(b as Record<string, unknown>)) {
    const existing = out[k];
    if (v && typeof v === 'object' && !Array.isArray(v) && existing && typeof existing === 'object' && !Array.isArray(existing)) {
      out[k] = deepMerge(existing, v as Record<string, unknown>);
    } else {
      out[k] = v;
    }
  }
  return out as T;
}
