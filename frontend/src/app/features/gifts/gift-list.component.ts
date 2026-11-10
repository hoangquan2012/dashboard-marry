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
import { GIFT_TYPE_OPTIONS, GiftType, SIDE_OPTIONS, Side } from '../../core/models/enums';
import { Gift, GiftFilter, GiftStats, PageResponse } from '../../core/models/models';
import { GiftService } from '../../core/services/gift.service';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent } from '../../shared/components/chips.components';
import { EmptyStateComponent, ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { VndPipe, ViNumberPipe } from '../../shared/pipes/vnd.pipe';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';
import { GiftFormDialogComponent } from './gift-form-dialog.component';

@Component({
  selector: 'app-gift-list',
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
    StatCardComponent,
    VndPipe,
    ViNumberPipe,
    ViDatePipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Tiền mừng</div>
          <h1>Tiền mừng cưới</h1>
        </div>
        <div class="actions">
          <button mat-flat-button color="primary" (click)="openForm()">
            <mat-icon class="material-symbols-rounded">add</mat-icon> Ghi nhận
          </button>
        </div>
      </div>

      @if (stats(); as s) {
        <div class="grid cols-3" style="margin-bottom: 18px">
          <app-stat-card label="Tổng tiền mừng" [value]="s.totalAmount | vnd" icon="redeem" tone="gold" [sub]="(s.giftCount | viNumber) + ' khoản'" />
          <app-stat-card label="Nhà trai" [value]="groomAmount() | vnd" icon="man" tone="info" />
          <app-stat-card label="Nhà gái" [value]="brideAmount() | vnd" icon="woman" tone="burgundy" />
        </div>
      }

      <form class="toolbar card" [formGroup]="filterForm">
        <mat-form-field class="search-field">
          <mat-label>Tìm khách</mat-label>
          <input matInput formControlName="q" placeholder="Tên, số điện thoại..." />
          <mat-icon matPrefix class="material-symbols-rounded">search</mat-icon>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side">
            <mat-option [value]="null">Tất cả</mat-option>
            @for (o of sideOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Hình thức</mat-label>
          <mat-select formControlName="type">
            <mat-option [value]="null">Tất cả</mat-option>
            @for (o of typeOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>

      @if (loading() && !page()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="reload()" /> }
      @else if ((page()?.content?.length ?? 0) === 0) {
        <div class="card">
          <app-empty icon="redeem" title="Chưa có tiền mừng" message="Ghi nhận khi khách mang phong bì hoặc chuyển khoản." actionText="Ghi nhận" (action)="openForm()" />
        </div>
      } @else {
        <div class="table-wrapper">
          <table mat-table [dataSource]="page()!.content">
            <ng-container matColumnDef="giver">
              <th mat-header-cell *matHeaderCellDef>Khách mời</th>
              <td mat-cell *matCellDef="let g">
                <div class="cell-main">{{ g.giverName }}</div>
              </td>
            </ng-container>
            <ng-container matColumnDef="side">
              <th mat-header-cell *matHeaderCellDef>Nhà</th>
              <td mat-cell *matCellDef="let g"><app-side-chip [side]="g.side" /></td>
            </ng-container>
            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Hình thức</th>
              <td mat-cell *matCellDef="let g">{{ g.giftTypeLabel }}</td>
            </ng-container>
            <ng-container matColumnDef="amount">
              <th mat-header-cell *matHeaderCellDef class="num">Số tiền</th>
              <td mat-cell *matCellDef="let g" class="num money">{{ g.amount | vnd:'—' }}</td>
            </ng-container>
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Ngày nhận</th>
              <td mat-cell *matCellDef="let g">{{ g.receivedAt | viDate }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let g" class="text-right">
                <button mat-icon-button (click)="openForm(g)" matTooltip="Sửa"><mat-icon class="material-symbols-rounded">edit</mat-icon></button>
                <button mat-icon-button (click)="remove(g)" matTooltip="Xóa"><mat-icon class="material-symbols-rounded">delete</mat-icon></button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols"></tr>
          </table>
          <mat-paginator [length]="page()!.totalElements" [pageIndex]="pageIndex()" [pageSize]="pageSize()" (page)="onPage($event)" />
        </div>
      }
    </div>
  `
})
export class GiftListComponent implements OnInit {
  private readonly service = inject(GiftService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly sideOptions = SIDE_OPTIONS;
  readonly typeOptions = GIFT_TYPE_OPTIONS;
  readonly cols = ['giver', 'side', 'type', 'amount', 'date', 'actions'];
  readonly filterForm = this.fb.group({
    q: this.fb.control(''),
    side: this.fb.control<Side | null>(null),
    type: this.fb.control<GiftType | null>(null)
  });
  readonly page = signal<PageResponse<Gift> | null>(null);
  readonly stats = signal<GiftStats | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);

  ngOnInit(): void {
    this.filterForm.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload(true));
    this.reload();
  }

  private filter(): GiftFilter {
    const v = this.filterForm.getRawValue();
    return { q: v.q?.trim() || undefined, side: v.side, type: v.type, page: this.pageIndex(), size: this.pageSize() };
  }

  reload(reset = false): void {
    if (reset) this.pageIndex.set(0);
    this.loading.set(true);
    this.error.set(null);
    const f = this.filter();
    this.service.list(f).subscribe({
      next: (p) => {
        this.page.set(p);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractMessage(err));
        this.loading.set(false);
      }
    });
    this.service.stats(f).subscribe({ next: (s) => this.stats.set(s) });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.reload();
  }

  groomAmount(): number {
    return this.stats()?.bySide.find((s) => s.side === 'GROOM')?.amount ?? 0;
  }
  brideAmount(): number {
    return this.stats()?.bySide.find((s) => s.side === 'BRIDE')?.amount ?? 0;
  }

  openForm(gift?: Gift): void {
    this.dialog
      .open(GiftFormDialogComponent, { data: { gift }, width: '560px' })
      .afterClosed()
      .subscribe((saved) => saved && this.reload());
  }

  remove(g: Gift): void {
    this.confirm.delete(`khoản mừng của "${g.giverName}"`).subscribe((ok) => {
      if (!ok) return;
      this.service.delete(g.id).subscribe({
        next: () => {
          this.notify.success('Đã xóa khoản mừng');
          this.reload();
        },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
