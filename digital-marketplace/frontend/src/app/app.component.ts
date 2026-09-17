import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header style="background:#e75480;color:#fff;padding:14px 20px;display:flex;justify-content:space-between;align-items:center;">
      <a routerLink="/products" style="color:#fff;text-decoration:none;font-size:20px;font-weight:bold;">🎂 CardBazaar</a>
      <nav style="display:flex;gap:16px;align-items:center;">
        <a routerLink="/products" style="color:#fff;">Cards</a>
        @if (auth.isLoggedIn()) {
          <a routerLink="/my-purchases" style="color:#fff;">My Purchases</a>
          <button class="btn secondary" (click)="auth.logout()">Logout</button>
        } @else {
          <a routerLink="/login" style="color:#fff;">Login</a>
          <a routerLink="/register" style="color:#fff;">Register</a>
        }
      </nav>
    </header>
    <main class="container">
      <router-outlet></router-outlet>
    </main>
  `
})
export class AppComponent {
  constructor(public auth: AuthService) {}
}
