import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

/** Shows a toast when the backend is unreachable or returns a server error. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notify = inject(NotificationService);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        if (err.status === 0) {
          notify.error('Không kết nối được tới máy chủ. Vui lòng kiểm tra backend đang chạy.');
        } else if (err.status >= 500) {
          notify.error('Máy chủ gặp lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.');
        }
      }
      return throwError(() => err);
    })
  );
};
