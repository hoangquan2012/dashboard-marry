import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'Đóng', { duration: 3500, panelClass: 'snack-success' });
  }

  info(message: string): void {
    this.snackBar.open(message, 'Đóng', { duration: 4000, panelClass: 'snack-info' });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Đóng', { duration: 6000, panelClass: 'snack-error' });
  }

  /** Shows a friendly message for any HTTP error coming from the backend. */
  httpError(err: unknown, fallback = 'Đã xảy ra lỗi, vui lòng thử lại'): void {
    this.error(extractMessage(err, fallback));
  }
}

export function extractMessage(err: unknown, fallback = 'Đã xảy ra lỗi, vui lòng thử lại'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Không kết nối được tới máy chủ. Vui lòng kiểm tra backend đang chạy.';
    }
    const body = err.error as ApiError | undefined;
    if (body && typeof body === 'object') {
      if (body.fieldErrors && Object.keys(body.fieldErrors).length > 0) {
        return Object.values(body.fieldErrors).join('; ');
      }
      if (body.message) {
        return body.message;
      }
    }
    if (err.status === 401 || err.status === 403) return 'Không thực hiện được thao tác này';
    if (err.status === 404) return 'Không tìm thấy dữ liệu yêu cầu';
    if (err.status === 413) return 'Tệp tải lên quá lớn';
    return fallback;
  }
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}

export function apiErrorOf(err: unknown): ApiError | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
    return err.error as ApiError;
  }
  return null;
}
