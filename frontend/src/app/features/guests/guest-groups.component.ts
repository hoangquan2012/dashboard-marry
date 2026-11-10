import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { GUEST_CATEGORY_OPTIONS, SIDE_OPTIONS, GuestCategory, Side } from '../../core/models/enums';
import { GuestGroup, GuestGroupRequest } from '../../core/models/models';
import { GuestService } from '../../core/services/guest.service';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { SideChipComponent } from '../../shared/components/chips.components';
import { EmptyStateComponent, ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { VndPipe, ViNumberPipe } from '../../shared/pipes/vnd.pipe';

@Component({
  selector: 'app-guest-group-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.group ? 'Sửa nhóm khách' : 'Tạo nhóm khách' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        <mat-form-field class="full">
          <mat-label>Tên nhóm</mat-label>
          <input matInput formControlName="name" required placeholder="VD: Gia đình bác Hùng" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Bên</mat-label>
          <mat-select formControlName="side">
            @for (o of sideOptions; track o.value) {
              <mat-option [value]="o.value">{{ o.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Nhóm khách</mat-label>
          <mat-select formControlName="category">
            <mat-option [value]="null">Không chọn</mat-option>
            @for (o of categoryOptions; track o.value) {
              <mat-option [value]="o.value">{{ o.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Người đại diện</mat-label>
          <input matInput formControlName="representativeName" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Số điện thoại đại diện</mat-label>
          <input matInput formControlName="representativePhone" />
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Mô tả</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Lưu</button>
      </mat-dialog-actions>
    </form>
  `
})
export class GuestGroupFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<{ group?: GuestGroup }>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<GuestGroupFormDialogComponent>);
  readonly sideOptions = SIDE_OPTIONS;
  readonly categoryOptions = GUEST_CATEGORY_OPTIONS;
  readonly form = this.fb.group({
    name: this.fb.nonNullable.control(this.data.group?.name ?? '', Validators.required),
    side: this.fb.nonNullable.control<Side>(this.data.group?.side ?? 'GROOM'),
    category: this.fb.control<GuestCategory | null>(this.data.group?.category ?? null),
    representativeName: this.fb.control<string | null>(this.data.group?.representativeName ?? null),
    representativePhone: this.fb.control<string | null>(this.data.group?.representativePhone ?? null),
    description: this.fb.control<string | null>(this.data.group?.description ?? null)
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const request: GuestGroupRequest = {
      name: v.name.trim(),
      side: v.side,
      category: v.category,
      representativeName: v.representativeName?.trim() || null,
      representativePhone: v.representativePhone?.trim() || null,
      description: v.description?.trim() || null
    };
    this.ref.close(request);
  }
}

@Component({
  selector: 'app-guest-groups',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    SideChipComponent,
    LoadingComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    LabelPipe,
    VndPipe,
    ViNumberPipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Khách mời</div>
          <h1>Nhóm khách / hộ gia đình</h1>
          <div class="subtitle">Xem số thành viên, số người tham dự và tổng tiền mừng của từng nhóm.</div>
        </div>
        <div class="actions">
          <a mat-stroked-button routerLink="/guests">
            <mat-icon class="material-symbols-rounded">arrow_back</mat-icon>
            Danh sách khách
          </a>
          <button mat-flat-button color="primary" (click)="openForm()">
            <mat-icon class="material-symbols-rounded">group_add</mat-icon>
            Tạo nhóm
          </button>
        </div>
      </div>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (groups().length === 0) {
        <div class="card">
          <app-empty icon="diversity_3" title="Chưa có nhóm khách" message="Tạo nhóm như Gia đình bác A, Nhóm bạn đại học để quản lý hộ gia đình." actionText="Tạo nhóm" (action)="openForm()" />
        </div>
      } @else {
        <div class="table-wrapper">
          <table mat-table [dataSource]="groups()">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Tên nhóm</th>
              <td mat-cell *matCellDef="let g">
                <div class="cell-main">{{ g.name }}</div>
                @if (g.representativeName) { <div class="cell-sub">{{ g.representativeName }} @if (g.representativePhone) { · {{ g.representativePhone }} }</div> }
              </td>
            </ng-container>
            <ng-container matColumnDef="side">
              <th mat-header-cell *matHeaderCellDef>Bên</th>
              <td mat-cell *matCellDef="let g"><app-side-chip [side]="g.side" /></td>
            </ng-container>
            <ng-container matColumnDef="category">
              <th mat-header-cell *matHeaderCellDef>Loại</th>
              <td mat-cell *matCellDef="let g">{{ g.category ? (g.category | label: 'category') : '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="members">
              <th mat-header-cell *matHeaderCellDef class="num">Thành viên</th>
              <td mat-cell *matCellDef="let g" class="num">{{ g.memberCount | viNumber }}</td>
            </ng-container>
            <ng-container matColumnDef="invited">
              <th mat-header-cell *matHeaderCellDef class="num">Mời / XN / Đến</th>
              <td mat-cell *matCellDef="let g" class="num">{{ g.totalInvited }} / {{ g.totalConfirmed }} / {{ g.totalActual }}</td>
            </ng-container>
            <ng-container matColumnDef="gift">
              <th mat-header-cell *matHeaderCellDef class="num">Tiền mừng</th>
              <td mat-cell *matCellDef="let g" class="num">
                @if (g.giftCount) { <span class="money">{{ g.giftTotal | vnd }}</span><div class="cell-sub">{{ g.giftCount }} lượt</div> }
                @else { <span class="muted">—</span> }
              </td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let g" class="text-right">
                <a mat-icon-button [routerLink]="['/guests']" [queryParams]="{ groupId: g.id }" matTooltip="Xem thành viên">
                  <mat-icon class="material-symbols-rounded">visibility</mat-icon>
                </a>
                <button mat-icon-button (click)="openForm(g)" matTooltip="Sửa">
                  <mat-icon class="material-symbols-rounded">edit</mat-icon>
                </button>
                <button mat-icon-button (click)="remove(g)" matTooltip="Xóa nhóm">
                  <mat-icon class="material-symbols-rounded">delete</mat-icon>
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols"></tr>
          </table>
        </div>
      }
    </div>
  `
})
export class GuestGroupsComponent implements OnInit {
  private readonly service = inject(GuestService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);

  readonly cols = ['name', 'side', 'category', 'members', 'invited', 'gift', 'actions'];
  readonly groups = signal<GuestGroup[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.groups().subscribe({
      next: (g) => {
        this.groups.set(g);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractMessage(err));
        this.loading.set(false);
      }
    });
  }

  openForm(group?: GuestGroup): void {
    this.dialog
      .open(GuestGroupFormDialogComponent, { data: { group }, width: '540px' })
      .afterClosed()
      .subscribe((request?: GuestGroupRequest) => {
        if (!request) return;
        const call = group ? this.service.updateGroup(group.id, request) : this.service.createGroup(request);
        call.subscribe({
          next: () => {
            this.notify.success(group ? 'Đã cập nhật nhóm' : 'Đã tạo nhóm khách');
            this.load();
          },
          error: (err) => this.notify.httpError(err)
        });
      });
  }

  remove(g: GuestGroup): void {
    this.confirm
      .delete(`nhóm "${g.name}"`, 'Các thành viên vẫn được giữ lại, chỉ bỏ liên kết nhóm.')
      .subscribe((ok) => {
        if (!ok) return;
        this.service.deleteGroup(g.id).subscribe({
          next: () => {
            this.notify.success('Đã xóa nhóm');
            this.load();
          },
          error: (err) => this.notify.httpError(err)
        });
      });
  }
}
