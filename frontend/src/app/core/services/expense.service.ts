import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DebtSummary,
  Expense,
  ExpenseCategory,
  ExpenseCategoryRequest,
  ExpenseFilter,
  ExpenseRequest,
  ExpenseStats,
  PageResponse,
  PaymentRequest
} from '../models/models';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly api = inject(ApiService);

  categories(): Observable<ExpenseCategory[]> {
    return this.api.get<ExpenseCategory[]>('/expense-categories');
  }

  createCategory(request: ExpenseCategoryRequest): Observable<ExpenseCategory> {
    return this.api.post<ExpenseCategory>('/expense-categories', request);
  }

  updateCategory(id: number, request: ExpenseCategoryRequest): Observable<ExpenseCategory> {
    return this.api.put<ExpenseCategory>(`/expense-categories/${id}`, request);
  }

  updateBudgets(budgets: Record<number, number>): Observable<ExpenseCategory[]> {
    return this.api.put<ExpenseCategory[]>('/expense-categories/budgets', budgets);
  }

  deleteCategory(id: number): Observable<void> {
    return this.api.delete(`/expense-categories/${id}`);
  }

  list(filter: ExpenseFilter): Observable<PageResponse<Expense>> {
    return this.api.get<PageResponse<Expense>>('/expenses', filter as Record<string, unknown>);
  }

  stats(filter?: Partial<ExpenseFilter>): Observable<ExpenseStats> {
    return this.api.get<ExpenseStats>('/expenses/stats', filter as Record<string, unknown>);
  }

  debts(vendorId?: number | null): Observable<DebtSummary> {
    return this.api.get<DebtSummary>('/expenses/debts', { vendorId });
  }

  get(id: number): Observable<Expense> {
    return this.api.get<Expense>(`/expenses/${id}`);
  }

  create(request: ExpenseRequest): Observable<Expense> {
    return this.api.post<Expense>('/expenses', request);
  }

  update(id: number, request: ExpenseRequest): Observable<Expense> {
    return this.api.put<Expense>(`/expenses/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.api.delete(`/expenses/${id}`);
  }

  addPayment(id: number, request: PaymentRequest): Observable<Expense> {
    return this.api.post<Expense>(`/expenses/${id}/payments`, request);
  }

  deletePayment(id: number, paymentId: number): Observable<Expense> {
    return this.api.delete<Expense>(`/expenses/${id}/payments/${paymentId}`);
  }

  export(filter: ExpenseFilter): Observable<string> {
    const { page, size, sortBy, direction, from, to, ...rest } = filter;
    return this.api.download('/expenses/export', rest as Record<string, unknown>, 'chi-phi-dam-cuoi.xlsx');
  }
}
