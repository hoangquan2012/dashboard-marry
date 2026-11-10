import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ChartConfiguration, ChartData } from 'chart.js';
import { DashboardSummary } from '../../core/models/models';
import { DashboardService } from '../../core/services/misc.services';
import { extractMessage } from '../../core/services/notification.service';
import { CHART_COLORS, ChartCardComponent, moneyAxis, moneyTooltip } from '../../shared/components/chart-card.component';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { VndPipe, ViNumberPipe, formatVnd } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    ChartCardComponent,
    StatCardComponent,
    LoadingComponent,
    ErrorStateComponent,
    VndPipe,
    ViNumberPipe,
    ViDatePipe
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly service = inject(DashboardService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<DashboardSummary | null>(null);
  readonly figures = computed(() => this.data()?.figures ?? null);

  readonly guestsBySide = computed<ChartData<'doughnut'>>(() => {
    const items = (this.data()?.charts.guestsBySide ?? []).filter((i) => i.count > 0);
    return {
      labels: items.map((i) => i.label),
      datasets: [{ data: items.map((i) => i.count), backgroundColor: items.map((i) => sideColor(i.key)), borderWidth: 0 }]
    };
  });

  readonly giftsBySide = computed<ChartData<'bar'>>(() => {
    const items = (this.data()?.charts.giftsBySide ?? []).filter((i) => i.key !== 'BOTH' || (i.amount ?? 0) > 0);
    return {
      labels: items.map((i) => i.label),
      datasets: [
        {
          label: 'Tiền mừng',
          data: items.map((i) => Number(i.amount ?? 0)),
          backgroundColor: items.map((i) => sideColor(i.key)),
          borderRadius: 8,
          maxBarThickness: 56
        }
      ]
    };
  });

  readonly expensesByCategory = computed<ChartData<'doughnut'>>(() => {
    const items = this.data()?.charts.expensesByCategory ?? [];
    const top = items.slice(0, 6);
    const rest = items.slice(6).reduce((s, i) => s + Number(i.actual), 0);
    const labels = top.map((i) => i.label);
    const values = top.map((i) => Number(i.actual));
    if (rest > 0) {
      labels.push('Khác');
      values.push(rest);
    }
    return { labels, datasets: [{ data: values, backgroundColor: CHART_COLORS, borderWidth: 0 }] };
  });

  readonly moneyBarOptions = { ...moneyTooltip(), ...moneyAxis('y'), plugins: { ...(moneyTooltip()?.plugins ?? {}), legend: { display: false } } };
  readonly doughnutOptions: ChartConfiguration<'doughnut'>['options'] = { cutout: '62%' };
  readonly moneyDoughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    cutout: '62%',
    plugins: { tooltip: { callbacks: { label: (ctx) => `${ctx.label}: ${formatVnd(ctx.parsed)}` } } }
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.summary({}).subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractMessage(err));
        this.loading.set(false);
      }
    });
  }

  countdownLabel(days: number | null | undefined): string {
    if (days === null || days === undefined) return '';
    if (days > 0) return `Còn ${days} ngày đến ngày cưới`;
    if (days === 0) return 'Hôm nay là ngày cưới';
    return `Đám cưới đã diễn ra ${-days} ngày trước`;
  }
}

function sideColor(key: string): string {
  switch (key) {
    case 'GROOM':
      return '#3b6fa5';
    case 'BRIDE':
      return '#d47f8f';
    default:
      return '#c9a96e';
  }
}
