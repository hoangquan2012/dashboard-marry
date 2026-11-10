import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { CONTRACT_STATUS_CLASS, CONTRACT_STATUS_OPTIONS, ContractStatus } from '../../core/models/enums';
import { Vendor } from '../../core/models/models';
import { VendorService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { EmptyStateComponent, ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { VendorFormDialogComponent } from './vendor-form-dialog.component';

@Component({
  selector: 'app-vendor-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    LoadingComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    VndPipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Đối tác</div>
          <h1>Nhà cung cấp</h1>
          <div class="subtitle">Nhà hàng, studio, makeup, trang trí, xe... gắn với các khoản chi tương ứng.</div>
        </div>
        <div class="actions">
          <button mat-flat-button color="primary" (click)="openForm()">
            <mat-icon class="material-symbols-rounded">add</mat-icon> Thêm nhà cung cấp
          </button>
        </div>
      </div>

      <form class="toolbar card">
        <mat-form-field class="search-field">
          <mat-label>Tìm</mat-label>
          <input matInput [formControl]="q" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Hợp đồng</mat-label>
          <mat-select [formControl]="status">
            <mat-option [value]="null">Tất cả</mat-option>
            @for (o of statuses; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else if (vendors().length === 0) {
        <div class="card">
          <app-empty icon="storefront" title="Chưa có nhà cung cấp" actionText="Thêm nhà cung cấp" (action)="openForm()" />
        </div>
      } @else {
        <div class="grid cols-3">
          @for (v of vendors(); track v.id) {
            <a class="card vendor" [routerLink]="['/vendors', v.id]">
              <div class="card-title">
                <h3>{{ v.name }}</h3>
                <span [class]="'chip ' + statusClass(v.contractStatus)">{{ v.contractStatusLabel }}</span>
              </div>
              <p class="muted">{{ v.serviceType || 'Dịch vụ' }} · {{ v.contactPerson || '' }} {{ v.phone || '' }}</p>
              <dl class="kv">
                <dt>Giá thỏa thuận</dt><dd>{{ v.agreedPrice | vnd }}</dd>
                <dt>Chi phí thực tế</dt><dd>{{ v.expenseActual | vnd }}</dd>
                <dt>Còn nợ</dt><dd [class.money-negative]="v.expenseRemaining > 0">{{ v.expenseRemaining | vnd }}</dd>
              </dl>
            </a>
          }
        </div>
      }
    </div>
  `,
  styles: [`.vendor { display: block; color: inherit; } .vendor:hover { box-shadow: var(--wm-shadow-hover); }`]
})
export class VendorListComponent implements OnInit {
  private readonly service = inject(VendorService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);

  readonly statuses = CONTRACT_STATUS_OPTIONS;
  readonly vendors = signal<Vendor[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly q = new FormControl('', { nonNullable: true });
  readonly status = new FormControl<ContractStatus | null>(null);

  ngOnInit(): void {
    this.q.valueChanges.pipe(debounceTime(300)).subscribe(() => this.load());
    this.status.valueChanges.subscribe(() => this.load());
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list(this.q.value || undefined, this.status.value).subscribe({
      next: (v) => { this.vendors.set(v); this.loading.set(false); },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
  }

  statusClass(s: ContractStatus): string {
    return CONTRACT_STATUS_CLASS[s];
  }

  openForm(vendor?: Vendor): void {
    this.dialog.open(VendorFormDialogComponent, { data: { vendor }, width: '680px' }).afterClosed().subscribe((saved) => saved && this.load());
  }
}
