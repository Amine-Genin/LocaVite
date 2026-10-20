import { Injectable, inject, signal, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, throwError } from 'rxjs';

export interface UserResponse {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  roles: string[];
  createdAt: string;
}

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserResponse;
}

export interface JwtPayload {
  sub?: string;
  roles?: string[];
  role?: string;
  iat?: number;
  exp?: number;
  [key: string]: unknown;
}

export const ACCESS_KEY = 'accessToken';
export const REFRESH_KEY = 'refreshToken';
export const USER_KEY = 'currentUser';
export const REMEMBER_EMAIL_KEY = 'locavite_remembered_email';

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeJwt(token);
  if (!payload || typeof payload.exp !== 'number') {
    return false;
  }
  return payload.exp * 1000 <= Date.now();
}

function loadInitialUser(): UserResponse | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
  private http = inject(HttpClient);
  private router = inject(Router);

  private refreshTimer: ReturnType<typeof setTimeout> | null = null;

  isLoggedIn = signal(!!localStorage.getItem(ACCESS_KEY));
  currentUser = signal<UserResponse | null>(loadInitialUser());

  constructor() {
    if (this.isAuthenticated()) {
      this.startAutoRefresh();
    }
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  login(email: string, password: string, rememberMe = false): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(
      tap((res) => {
        this.saveTokens(res);
        if (rememberMe) {
          localStorage.setItem(REMEMBER_EMAIL_KEY, email);
        } else {
          localStorage.removeItem(REMEMBER_EMAIL_KEY);
        }
        this.startAutoRefresh(res.expiresIn);
      }),
    );
  }

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>('/api/auth/register', request);
  }

  refreshToken(): Observable<AuthResponse> {
    const token = this.getRefreshToken();
    if (!token) {
      return throwError(() => new Error('Aucun refresh token disponible'));
    }

    return this.http.post<AuthResponse>('/api/auth/refresh', { refreshToken: token }).pipe(
      tap((res) => {
        this.saveTokens(res);
        this.startAutoRefresh(res.expiresIn);
      }),
    );
  }

  logout(): void {
    this.stopAutoRefresh();
    const token = this.getRefreshToken();
    if (token) {
      this.http.post('/api/auth/logout', { refreshToken: token }).subscribe({ error: () => {} });
    }
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.isLoggedIn.set(false);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    return !isTokenExpired(token);
  }

  getToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  getCurrentUser(): UserResponse | null {
    return this.currentUser();
  }

  getUserRoles(): string[] {
    const user = this.currentUser();
    if (user?.roles && user.roles.length > 0) {
      return user.roles.map((r) => r.replace(/^ROLE_/, '').toUpperCase());
    }

    const token = this.getToken();
    if (token) {
      const payload = decodeJwt(token);
      if (payload?.roles && Array.isArray(payload.roles)) {
        return payload.roles.map((r) => String(r).replace(/^ROLE_/, '').toUpperCase());
      }
      if (payload?.role && typeof payload.role === 'string') {
        return [payload.role.replace(/^ROLE_/, '').toUpperCase()];
      }
    }

    return [];
  }

  hasRole(role: string): boolean {
    if (!role) return false;
    const target = role.replace(/^ROLE_/, '').toUpperCase();
    return this.getUserRoles().includes(target);
  }

  hasAnyRole(roles: string[]): boolean {
    if (!roles || roles.length === 0) return true;
    return roles.some((role) => this.hasRole(role));
  }

  startAutoRefresh(expiresIn?: number): void {
    this.stopAutoRefresh();

    let delayMs: number | null = null;

    if (expiresIn && expiresIn > 0) {
      const totalMs = expiresIn > 1_000_000 ? expiresIn : expiresIn * 1000;
      delayMs = Math.max(totalMs - 60_000, 5_000);
    } else {
      const token = this.getToken();
      if (token) {
        const payload = decodeJwt(token);
        if (payload && typeof payload.exp === 'number') {
          const remainingMs = payload.exp * 1000 - Date.now();
          if (remainingMs > 0) {
            delayMs = Math.max(remainingMs - 60_000, 5_000);
          }
        }
      }
    }

    if (delayMs !== null && delayMs > 0) {
      this.refreshTimer = setTimeout(() => {
        if (this.getRefreshToken()) {
          this.refreshToken().subscribe({
            error: () => {
              this.logout();
            },
          });
        }
      }, delayMs);
    }
  }

  stopAutoRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  get accessToken(): string | null {
    return this.getToken();
  }

  get rememberedEmail(): string {
    return localStorage.getItem(REMEMBER_EMAIL_KEY) || '';
  }

  private saveTokens(res: AuthResponse): void {
    if (res.accessToken) {
      localStorage.setItem(ACCESS_KEY, res.accessToken);
    }
    if (res.refreshToken) {
      localStorage.setItem(REFRESH_KEY, res.refreshToken);
    }
    if (res.user) {
      localStorage.setItem(USER_KEY, JSON.stringify(res.user));
      this.currentUser.set(res.user);
    }
    this.isLoggedIn.set(true);
  }
}
