import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Attachment,
  AuditLog,
  BackupInfo,
  BudgetOverview,
  DashboardFilter,
  DashboardSummary,
  ExpenseReport,
  FinancialReport,
  GiftReport,
  GuestReport,
  OtherIncome,
  OtherIncomeRequest,
  PageResponse,
  ReminderSummary,
  ReportFilter,
  RestoreResult,
  SearchResponse,
  Task,
  TaskRequest,
  TaskStats,
  UserInfo,
  Vendor,
  VendorRequest,
  WeddingSettings,
  WeddingSettingsRequest,
  Expense
} from '../models/models';
import { AttachmentEntityType, ContractStatus, TaskPriority, TaskStatus, UserRole } from '../models/enums';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly api = inject(ApiService);

  get(): Observable<WeddingSettings> {
    return this.api.get<WeddingSettings>('/settings');
  }

  update(request: WeddingSettingsRequest): Observable<WeddingSettings> {
    return this.api.put<WeddingSettings>('/settings', request);
  }
}

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly api = inject(ApiService);

  overview(): Observable<BudgetOverview> {
    return this.api.get<BudgetOverview>('/budget/overview');
  }

  incomes(): Observable<OtherIncome[]> {
    return this.api.get<OtherIncome[]>('/other-incomes');
  }

  createIncome(request: OtherIncomeRequest): Observable<OtherIncome> {
    return this.api.post<OtherIncome>('/other-incomes', request);
  }

  updateIncome(id: number, request: OtherIncomeRequest): Observable<OtherIncome> {
    return this.api.put<OtherIncome>(`/other-incomes/${id}`, request);
  }

  deleteIncome(id: number): Observable<void> {
    return this.api.delete(`/other-incomes/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class VendorService {
  private readonly api = inject(ApiService);

  list(q?: string, status?: ContractStatus | null, serviceType?: string | null): Observable<Vendor[]> {
    return this.api.get<Vendor[]>('/vendors', { q, status, serviceType });
  }

  get(id: number): Observable<Vendor> {
    return this.api.get<Vendor>(`/vendors/${id}`);
  }

  expenses(id: number): Observable<Expense[]> {
    return this.api.get<Expense[]>(`/vendors/${id}/expenses`);
  }

  create(request: VendorRequest): Observable<Vendor> {
    return this.api.post<Vendor>('/vendors', request);
  }

  update(id: number, request: VendorRequest): Observable<Vendor> {
    return this.api.put<Vendor>(`/vendors/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.api.delete(`/vendors/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly api = inject(ApiService);

  list(filter?: { q?: string; status?: TaskStatus | null; priority?: TaskPriority | null; assignee?: string; overdue?: boolean | null; eventId?: number | null }): Observable<Task[]> {
    return this.api.get<Task[]>('/tasks', filter as Record<string, unknown>);
  }

  stats(): Observable<TaskStats> {
    return this.api.get<TaskStats>('/tasks/stats');
  }

  get(id: number): Observable<Task> {
    return this.api.get<Task>(`/tasks/${id}`);
  }

  create(request: TaskRequest): Observable<Task> {
    return this.api.post<Task>('/tasks', request);
  }

  update(id: number, request: TaskRequest): Observable<Task> {
    return this.api.put<Task>(`/tasks/${id}`, request);
  }

  changeStatus(id: number, status: TaskStatus, sortOrder?: number): Observable<Task> {
    return this.api.patch<Task>(`/tasks/${id}/status`, { status, sortOrder });
  }

  reorder(orderedIds: number[], status?: TaskStatus): Observable<void> {
    return this.api.post<void>('/tasks/reorder', { orderedIds, status });
  }

  delete(id: number): Observable<void> {
    return this.api.delete(`/tasks/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);

  summary(filter: DashboardFilter): Observable<DashboardSummary> {
    return this.api.get<DashboardSummary>('/dashboard/summary', filter as Record<string, unknown>);
  }
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly api = inject(ApiService);

  guests(filter: ReportFilter): Observable<GuestReport> {
    return this.api.get<GuestReport>('/reports/guests', filter as Record<string, unknown>);
  }

  gifts(filter: ReportFilter): Observable<GiftReport> {
    return this.api.get<GiftReport>('/reports/gifts', filter as Record<string, unknown>);
  }

  expenses(filter: ReportFilter): Observable<ExpenseReport> {
    return this.api.get<ExpenseReport>('/reports/expenses', filter as Record<string, unknown>);
  }

  financial(filter: ReportFilter): Observable<FinancialReport> {
    return this.api.get<FinancialReport>('/reports/financial', filter as Record<string, unknown>);
  }

  export(filter: ReportFilter): Observable<string> {
    return this.api.download('/reports/export', filter as Record<string, unknown>, 'bao-cao-dam-cuoi.xlsx');
  }
}

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  private readonly api = inject(ApiService);

  list(entityType: AttachmentEntityType, entityId: number): Observable<Attachment[]> {
    return this.api.get<Attachment[]>('/attachments', { entityType, entityId });
  }

  upload(entityType: AttachmentEntityType, entityId: number, file: File, description?: string | null): Observable<Attachment> {
    const form = new FormData();
    form.append('file', file);
    return this.api.upload<Attachment>('/attachments', form, { entityType, entityId, description });
  }

  download(attachment: Attachment): Observable<string> {
    return this.api.download(`/attachments/${attachment.id}/download`, undefined, attachment.originalName);
  }

  blob(attachment: Attachment): Observable<Blob> {
    return this.api.blob(`/attachments/${attachment.id}/download`, { inline: true });
  }

  delete(id: number): Observable<void> {
    return this.api.delete(`/attachments/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class SystemService {
  private readonly api = inject(ApiService);

  search(q: string, limit = 6): Observable<SearchResponse> {
    return this.api.get<SearchResponse>('/search', { q, limit });
  }

  reminders(): Observable<ReminderSummary> {
    return this.api.get<ReminderSummary>('/reminders');
  }

  auditLogs(filter: { entityType?: string; entityId?: number; action?: string; performedBy?: string; from?: string | null; to?: string | null; page?: number; size?: number }): Observable<PageResponse<AuditLog>> {
    return this.api.get<PageResponse<AuditLog>>('/audit-logs', filter as Record<string, unknown>);
  }

  backupInfo(): Observable<BackupInfo> {
    return this.api.get<BackupInfo>('/backup/info');
  }

  exportBackup(): Observable<string> {
    return this.api.download('/backup/export', undefined, 'wedding-backup.json');
  }

  restoreBackup(file: File): Observable<RestoreResult> {
    const form = new FormData();
    form.append('file', file);
    return this.api.upload<RestoreResult>('/backup/restore', form, { confirm: true });
  }

  // users
  users(): Observable<UserInfo[]> {
    return this.api.get<UserInfo[]>('/users');
  }

  createUser(request: { username: string; password: string; fullName: string; role: UserRole }): Observable<UserInfo> {
    return this.api.post<UserInfo>('/users', request);
  }

  updateUser(id: number, request: { fullName: string; role: UserRole; enabled: boolean }): Observable<UserInfo> {
    return this.api.put<UserInfo>(`/users/${id}`, request);
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.api.post<void>(`/users/${id}/reset-password`, { newPassword });
  }

  deleteUser(id: number): Observable<void> {
    return this.api.delete(`/users/${id}`);
  }
}
