import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { GUEST_CATEGORY_OPTIONS, GuestCategory, SIDE_OPTIONS, Side } from '../../core/models/enums';
import { Guest, GuestFilter, GuestStats, PageResponse } from '../../core/models/models';
import { GuestService } from '../../core/services/guest.service';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent } from '../../shared/components/chips.components';
import { EmptyStateComponent, ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { VndPipe, ViNumberPipe } from '../../shared/pipes/vnd.pipe';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { GuestFormDialogComponent } from './guest-form-dialog.component';

@Component({
  selector: 'app-guest-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    SideChipComponent,
    LoadingComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    VndPipe,
    ViNumberPipe,
    LabelPipe
  ],
  templateUrl: './guest-list.component.html',
  styleUrl: './guest-list.component.scss'
})
export class GuestListComponent implements OnInit {
  private readonly guestService = inject(GuestService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly confirm = inject(ConfirmService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly sideOptions = SIDE_OPTIONS;
  readonly categoryOptions = GUEST_CATEGORY_OPTIONS;
  readonly columns = ['fullName', 'side', 'category', 'gift', 'actions'];

  readonly filterForm = this.fb.group({
    q: this.fb.control(''),
    side: this.fb.control<Side | null>(null),
    category: this.fb.control<GuestCategory | null>(null)
  });

  readonly page = signal<PageResponse<Guest> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly stats = signal<GuestStats | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);

  ngOnInit(): void {
    this.filterForm.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload(true));
    this.loadStats();
    this.reload();
  }

  private currentFilter(): GuestFilter {
    const v = this.filterForm.getRawValue();
    return {
      q: v.q?.trim() || undefined,
      side: v.side,
      category: v.category,
      page: this.pageIndex(),
      size: this.pageSize(),
      sortBy: 'fullName',
      direction: 'asc'
    };
  }

  reload(resetPage = false): void {
    if (resetPage) this.pageIndex.set(0);
    this.loading.set(true);
    this.error.set(null);
    this.guestService.list(this.currentFilter()).subscribe({
      next: (p) => {
        this.page.set(p);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractMessage(err));
        this.loading.set(false);
      }
    });
  }

  loadStats(): void {
    this.guestService.stats().subscribe({ next: (s) => this.stats.set(s) });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.reload();
  }

  hasActiveFilter(): boolean {
    const v = this.filterForm.getRawValue();
    return !!(v.q || v.side || v.category);
  }

  clearFilters(): void {
    this.filterForm.reset({ q: '', side: null, category: null });
  }

  openForm(guest?: Guest): void {
    this.dialog
      .open(GuestFormDialogComponent, { data: { guest }, width: '520px' })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.reload();
          this.loadStats();
        }
      });
  }

  remove(g: Guest): void {
    this.confirm
      .confirm({
        title: 'Xóa khách mời',
        message: `Xóa <b>${g.fullName}</b> khỏi danh sách?`,
        confirmText: 'Xóa',
        icon: 'delete'
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.guestService.archive(g.id).subscribe({
          next: () => {
            this.notify.success(`Đã xóa ${g.fullName}`);
            this.reload();
            this.loadStats();
          },
          error: (err) => this.notify.httpError(err)
        });
      });
  }
}
