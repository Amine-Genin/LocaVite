import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: unknown; // à typer avec UserResponse
}

const ACCESS_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  isLoggedIn = signal(!!localStorage.getItem(ACCESS_KEY));

  // Vérifiez les champs de LoginRequest (email ou username ?)
  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(
      tap((res) => {
        localStorage.setItem(ACCESS_KEY, res.accessToken);
        localStorage.setItem(REFRESH_KEY, res.refreshToken);
        this.isLoggedIn.set(true);
      }),
    );
  }

  logout() {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    this.http.post('/api/auth/logout', refreshToken ? { refreshToken } : null).subscribe({ error: () => {} });
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.isLoggedIn.set(false);
    this.router.navigate(['/login']);
  }

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }
}