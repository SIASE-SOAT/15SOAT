import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(localStorage.getItem('siase_token'));

  readonly token = this._token.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());

  login(body: LoginRequest) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, body).pipe(
      tap(({ token }) => {
        localStorage.setItem('siase_token', token);
        this._token.set(token);
      })
    );
  }

  register(body: RegisterRequest) {
    return this.http.post<void>(`${environment.apiUrl}/auth/registrar`, body);
  }

  logout() {
    localStorage.removeItem('siase_token');
    this._token.set(null);
    this.router.navigate(['/login']);
  }
}
