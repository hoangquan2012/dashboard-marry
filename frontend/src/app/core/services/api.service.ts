import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Thin wrapper around HttpClient used by every feature service:
 * builds query params from plain objects (skipping empty values) and handles file downloads.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  readonly baseUrl = environment.apiUrl;

  get<T>(path: string, params?: Record<string, unknown>): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`, { params: this.params(params) });
  }

  post<T>(path: string, body?: unknown, params?: Record<string, unknown>): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body ?? {}, { params: this.params(params) });
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body);
  }

  patch<T>(path: string, body?: unknown, params?: Record<string, unknown>): Observable<T> {
    return this.http.patch<T>(`${this.baseUrl}${path}`, body ?? {}, { params: this.params(params) });
  }

  delete<T = void>(path: string, params?: Record<string, unknown>): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`, { params: this.params(params) });
  }

  upload<T>(path: string, formData: FormData, params?: Record<string, unknown>): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, formData, { params: this.params(params) });
  }

  /** Downloads a file and triggers the browser "save as" dialog. */
  download(path: string, params?: Record<string, unknown>, fallbackName = 'download'): Observable<string> {
    return this.http
      .get(`${this.baseUrl}${path}`, { params: this.params(params), responseType: 'blob', observe: 'response' })
      .pipe(
        map((res) => {
          const disposition = res.headers.get('Content-Disposition') ?? '';
          const name = parseFileName(disposition) ?? fallbackName;
          saveBlob(res.body as Blob, name);
          return name;
        })
      );
  }

  blob(path: string, params?: Record<string, unknown>): Observable<Blob> {
    return this.http.get(`${this.baseUrl}${path}`, { params: this.params(params), responseType: 'blob' });
  }

  params(obj?: Record<string, unknown>): HttpParams {
    let params = new HttpParams();
    if (!obj) return params;
    for (const [key, value] of Object.entries(obj)) {
      if (value === null || value === undefined || value === '') continue;
      if (Array.isArray(value)) {
        value.forEach((v) => (params = params.append(key, String(v))));
      } else {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}

export function parseFileName(disposition: string): string | null {
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  if (utf8) {
    try {
      return decodeURIComponent(utf8[1]);
    } catch {
      return utf8[1];
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition);
  return plain ? plain[1] : null;
}

export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
