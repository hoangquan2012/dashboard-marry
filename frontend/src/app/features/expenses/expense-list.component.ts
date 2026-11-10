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
import { SIDE_OPTIONS, Side } from '../../core/models/enums';
import { Expense, ExpenseCategory, ExpenseFilter, ExpenseStats, PageResponse } from '../../core/models/models';
import { ExpenseService } from '../../core/services/expense.service';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent } from '../../shared/components/chips.components';
import { EmptyStateComponent, ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { VndPipe, ViNumberPipe } from '../../shared/pipes/vnd.pipe';
import { ExpenseFormDialogComponent } from './expense-form-dialog.component';

@Component({
  selector: 'app-expense-list',
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
    ViNumberPipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Chi phí</div>
          <h1>Chi phí đám cưới</h1>
        </div>
        <div class="actions">
          <button mat-flat-button color="primary" (click)="openForm()">
            <mat-icon class="material-symbols-rounded">add</mat-icon> Thêm khoản chi
          </button>
        </div>
      </div>

      @if (stats(); as s) {
        <div class="grid cols-3" style="margin-bottom: 18px">
          <app-stat-card label="Tổng chi" [value]="s.actualTotal | vnd" icon="receipt_long" tone="burgundy" />
          <app-stat-card label="Đã thanh toán" [value]="s.paidTotal | vnd" icon="check_circle" tone="success" />
          <app-stat-card label="Còn phải trả" [value]="s.remainingTotal | vnd" icon="pending" tone="warning" [sub]="(s.unpaidCount | viNumber) + ' khoản'" />
        </div>
      }

      <form class="toolbar card" [formGroup]="filterForm">
        <mat-form-field class="search-field">
          <mat-label>Tìm khoản chi</mat-label>
          <input matInput formControlName="q" />
          <mat-icon matPrefix class="material-symbols-rounded">search</mat-icon>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Danh mục</mat-label>
          <mat-select formControlName="categoryId">
            <mat-option [value]="null">Tất cả</mat-option>
            @for (c of categories(); track c.id) { <mat-option [value]="c.id">{{ c.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side">
            <mat-option [value]="null">Tất cả</mat-option>
            @for (o of sideOptions; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>

      @if (loading() && !page()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="reload()" /> }
      @else if ((page()?.content?.length ?? 0) === 0) {
        <div class="card">
          <app-empty icon="receipt_long" title="Chưa có khoản chi" message="Thêm nhà hàng, studio, trang trí..." actionText="Thêm khoản chi" (action)="openForm()" />
        </div>
      } @else {
        <div class="table-wrapper">
          <table mat-table [dataSource]="page()!.content">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Khoản chi</th>
              <td mat-cell *matCellDef="let e">
                <div class="cell-main">{{ e.name }}</div>
                <div class="cell-sub">{{ e.categoryName }}</div>
              </td>
            </ng-container>
            <ng-container matColumnDef="side">
              <th mat-header-cell *matHeaderCellDef>Bên</th>
              <td mat-cell *matCellDef="let e"><app-side-chip [side]="e.side" /></td>
            </ng-container>
            <ng-container matColumnDef="actual">
              <th mat-header-cell *matHeaderCellDef class="num">Số tiền</th>
              <td mat-cell *matCellDef="let e" class="num money">{{ e.actualAmount | vnd }}</td>
            </ng-container>
            <ng-container matColumnDef="paid">
              <th mat-header-cell *matHeaderCellDef class="num">Đã trả</th>
              <td mat-cell *matCellDef="let e" class="num">{{ e.paidAmount | vnd }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let e" class="text-right">
                <button mat-icon-button (click)="openForm(e)" matTooltip="Sửa"><mat-icon class="material-symbols-rounded">edit</mat-icon></button>
                <button mat-icon-button (click)="remove(e)" matTooltip="Xóa"><mat-icon class="material-symbols-rounded">delete</mat-icon></button>
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
export class ExpenseListComponent implements OnInit {
  private readonly service = inject(ExpenseService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly sideOptions = SIDE_OPTIONS;
  readonly cols = ['name', 'side', 'actual', 'paid', 'actions'];
  readonly filterForm = this.fb.group({
    q: this.fb.control(''),
    categoryId: this.fb.control<number | null>(null),
    side: this.fb.control<Side | null>(null)
  });
  readonly page = signal<PageResponse<Expense> | null>(null);
  readonly stats = signal<ExpenseStats | null>(null);
  readonly categories = signal<ExpenseCategory[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);

  ngOnInit(): void {
    this.service.categories().subscribe({ next: (c) => this.categories.set(c) });
    this.filterForm.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload(true));
    this.reload();
  }

  private filter(): ExpenseFilter {
    const v = this.filterForm.getRawValue();
    return { q: v.q?.trim() || undefined, categoryId: v.categoryId, side: v.side, page: this.pageIndex(), size: this.pageSize() };
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

  openForm(expense?: Expense): void {
    this.dialog
      .open(ExpenseFormDialogComponent, {
        data: { expense, categories: this.categories() },
        width: '560px'
      })
      .afterClosed()
      .subscribe((saved) => saved && this.reload());
  }

  remove(e: Expense): void {
    this.confirm.delete(`khoản chi "${e.name}"`).subscribe((ok) => {
      if (!ok) return;
      this.service.delete(e.id).subscribe({
        next: () => {
          this.notify.success('Đã xóa khoản chi');
          this.reload();
        },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
