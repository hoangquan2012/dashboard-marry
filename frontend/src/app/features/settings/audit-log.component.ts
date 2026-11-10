import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { AuditLog, PageResponse } from '../../core/models/models';
import { SystemService } from '../../core/services/misc.services';
import { extractMessage } from '../../core/services/notification.service';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    LoadingComponent,
    ErrorStateComponent,
    ViDatePipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Cài đặt</div>
          <h1>Nhật ký thao tác</h1>
          <div class="subtitle">Ai đã chỉnh sửa tiền mừng, khoản chi, trạng thái khách mời...</div>
        </div>
        <div class="actions">
          <a mat-stroked-button routerLink="/settings"><mat-icon class="material-symbols-rounded">arrow_back</mat-icon> Cài đặt</a>
        </div>
      </div>

      <form class="toolbar card" [formGroup]="filter">
        <mat-form-field>
          <mat-label>Loại bản ghi</mat-label>
          <mat-select formControlName="entityType" (selectionChange)="reload(true)">
            <mat-option value="">Tất cả</mat-option>
            <mat-option value="GUEST">Khách mời</mat-option>
            <mat-option value="GIFT">Tiền mừng</mat-option>
            <mat-option value="EXPENSE">Chi phí</mat-option>
            <mat-option value="TASK">Công việc</mat-option>
            <mat-option value="VENDOR">Nhà cung cấp</mat-option>
            <mat-option value="EVENT">Sự kiện</mat-option>
            <mat-option value="INVITATION">Lời mời</mat-option>
            <mat-option value="SETTINGS">Cài đặt</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Hành động</mat-label>
          <mat-select formControlName="action" (selectionChange)="reload(true)">
            <mat-option value="">Tất cả</mat-option>
            <mat-option value="CREATE">Tạo</mat-option>
            <mat-option value="UPDATE">Sửa</mat-option>
            <mat-option value="DELETE">Xóa</mat-option>
            <mat-option value="CHECKIN">Check-in</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Người thao tác</mat-label>
          <input matInput formControlName="performedBy" (keyup.enter)="reload(true)" />
        </mat-form-field>
      </form>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="reload()" /> }
      @else if (page(); as p) {
        <div class="table-wrapper">
          <table mat-table [dataSource]="p.content">
            <ng-container matColumnDef="time"><th mat-header-cell *matHeaderCellDef>Thời điểm</th>
              <td mat-cell *matCellDef="let a">{{ a.performedAt | viDate: 'datetime' }}</td></ng-container>
            <ng-container matColumnDef="who"><th mat-header-cell *matHeaderCellDef>Người</th>
              <td mat-cell *matCellDef="let a">{{ a.performedBy || 'Hệ thống' }}</td></ng-container>
            <ng-container matColumnDef="action"><th mat-header-cell *matHeaderCellDef>Hành động</th>
              <td mat-cell *matCellDef="let a"><span class="chip chip-neutral">{{ a.action }}</span></td></ng-container>
            <ng-container matColumnDef="entity"><th mat-header-cell *matHeaderCellDef>Đối tượng</th>
              <td mat-cell *matCellDef="let a">{{ a.entityType }} @if (a.entityLabel) { · {{ a.entityLabel }} }</td></ng-container>
            <ng-container matColumnDef="summary"><th mat-header-cell *matHeaderCellDef>Nội dung</th>
              <td mat-cell *matCellDef="let a">{{ a.summary || '—' }}</td></ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols"></tr>
          </table>
          <mat-paginator [length]="p.totalElements" [pageIndex]="pageIndex()" [pageSize]="pageSize()" (page)="onPage($event)" />
        </div>
      }
    </div>
  `
})
export class AuditLogComponent implements OnInit {
  private readonly system = inject(SystemService);
  private readonly fb = inject(FormBuilder);

  readonly page = signal<PageResponse<AuditLog> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(30);
  readonly cols = ['time', 'who', 'action', 'entity', 'summary'];
  readonly filter = this.fb.group({
    entityType: this.fb.control(''),
    action: this.fb.control(''),
    performedBy: this.fb.control('')
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(reset = false): void {
    if (reset) this.pageIndex.set(0);
    this.loading.set(true);
    const v = this.filter.getRawValue();
    this.system.auditLogs({
      entityType: v.entityType || undefined,
      action: v.action || undefined,
      performedBy: v.performedBy || undefined,
      page: this.pageIndex(),
      size: this.pageSize()
    }).subscribe({
      next: (p) => { this.page.set(p); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.reload();
  }
}
