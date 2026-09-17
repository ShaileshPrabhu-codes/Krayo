import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models/models';

const TOKEN_KEY = 'cardbazaar_token';
const USER_KEY = 'cardbazaar_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Reactive signal so the nav bar updates instantly on login/logout
  loggedIn = signal<boolean>(!!localStorage.getItem(TOKEN_KEY));

  constructor(private http: HttpClient, private router: Router) {}

  register(email: string, password: string, fullName: string, countryCode: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/register`, { email, password, fullName, countryCode })
      .pipe(tap(res => this.storeSession(res)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, { email, password })
      .pipe(tap(res => this.storeSession(res)));
  }

  private storeSession(res: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res));
    this.loggedIn.set(true);
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.loggedIn.set(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return this.loggedIn();
  }

  getCountryCode(): string {
    // v1: derive from browser locale as a simple heuristic.
    // Swap for an IP-geolocation call if you need more accuracy.
    return navigator.language?.toUpperCase().includes('IN') ? 'IN' : 'IN';
  }
}
