import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeVi from '@angular/common/locales/vi';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_PAGINATOR_DEFAULT_OPTIONS, MatPaginatorIntl } from '@angular/material/paginator';
import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { routes } from './app.routes';
import { errorInterceptor } from './core/interceptors/auth.interceptor';
import { VN_DATE_FORMATS, VnDateAdapter } from './core/utils/vn-date-adapter';
import { ViPaginatorIntl } from './core/utils/vi-paginator-intl';

registerLocaleData(localeVi, 'vi');

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
      withViewTransitions()
    ),
    provideHttpClient(withInterceptors([errorInterceptor])),
    provideCharts(withDefaultRegisterables()),
    { provide: LOCALE_ID, useValue: 'vi' },
    { provide: MAT_DATE_LOCALE, useValue: 'vi-VN' },
    { provide: DateAdapter, useClass: VnDateAdapter },
    { provide: MAT_DATE_FORMATS, useValue: VN_DATE_FORMATS },
    { provide: MatPaginatorIntl, useClass: ViPaginatorIntl },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline', subscriptSizing: 'dynamic' } },
    { provide: MAT_PAGINATOR_DEFAULT_OPTIONS, useValue: { pageSize: 20, pageSizeOptions: [10, 20, 50, 100], showFirstLastButtons: true } },
    { provide: MAT_DIALOG_DEFAULT_OPTIONS, useValue: { hasBackdrop: true, maxWidth: '96vw', autoFocus: 'first-tabbable' } }
  ]
};
