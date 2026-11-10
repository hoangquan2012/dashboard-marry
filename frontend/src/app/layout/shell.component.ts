import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { WeddingSettings } from '../core/models/models';
import { SettingsService } from '../core/services/misc.services';
import { ViDatePipe } from '../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    ViDatePipe
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent implements OnInit {
  private readonly breakpoints = inject(BreakpointObserver);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly settingsService = inject(SettingsService);

  readonly isHandset = toSignal(
    this.breakpoints.observe('(max-width: 1023px)').pipe(map((r) => r.matches)),
    { initialValue: false }
  );
  readonly collapsed = signal(false);
  readonly drawerOpened = signal(true);
  readonly settings = signal<WeddingSettings | null>(null);
  readonly coupleNames = computed(() => {
    const s = this.settings();
    return s ? `${s.groomName} & ${s.brideName}` : 'Đám cưới';
  });
  readonly countdown = computed(() => {
    const d = this.settings()?.daysUntilWedding;
    if (d === null || d === undefined) return null;
    if (d > 0) return `Còn ${d} ngày`;
    if (d === 0) return 'Hôm nay là ngày cưới';
    return `Đã cưới ${-d} ngày`;
  });

  readonly navItems = [
    { label: 'Tổng quan', icon: 'dashboard', route: '/dashboard' },
    { label: 'Khách mời', icon: 'groups', route: '/guests' },
    { label: 'Chi phí', icon: 'receipt_long', route: '/expenses' }
  ];

  ngOnInit(): void {
    this.breakpoints
      .observe('(max-width: 1023px)')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((r) => this.drawerOpened.set(!r.matches));

    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        startWith(null),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        if (this.isHandset()) this.drawerOpened.set(false);
        this.settingsService.get().subscribe({ next: (s) => this.settings.set(s), error: () => undefined });
      });
  }

  toggleSidebar(): void {
    if (this.isHandset()) {
      this.drawerOpened.update((v) => !v);
    } else {
      this.collapsed.update((v) => !v);
    }
  }
}
