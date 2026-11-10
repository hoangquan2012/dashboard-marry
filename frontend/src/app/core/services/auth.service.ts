import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, UserInfo } from '../models/models';

const TOKEN_KEY = 'wm.token';
const USER_KEY = 'wm.user';

/**
 * JWT session handling. The token is kept in localStorage so the owner stays logged in
 * on the reception-desk tablet; everything else lives in the database.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly userSignal = signal<UserInfo | null>(readUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isLoggedIn = computed(() => !!this.tokenSignal() && !this.isExpired(this.tokenSignal()!));
  readonly isAdmin = computed(() => this.userSignal()?.role === 'ADMIN');

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password }).pipe(
      tap((res) => {
        localStorage.setItem(TOKEN_KEY, res.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify(res.user));
        this.tokenSignal.set(res.accessToken);
        this.userSignal.set(res.user);
      })
    );
  }

  refreshProfile(): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${environment.apiUrl}/auth/me`).pipe(
      tap((user) => {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.userSignal.set(user);
      })
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/change-password`, { currentPassword, newPassword });
  }

  logout(redirect = true): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    if (redirect) {
      this.router.navigate(['/login']);
    }
  }

  private isExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}

function readUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserInfo) : null;
  } catch {
    return null;
  }
}
