import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card" style="max-width:400px;margin:40px auto;">
      <h2>Login</h2>
      <input type="email" placeholder="Email" [(ngModel)]="email" name="email">
      <input type="password" placeholder="Password" [(ngModel)]="password" name="password">
      @if (error) { <p class="error">{{ error }}</p> }
      <button class="btn" (click)="submit()" [disabled]="loading">
        {{ loading ? 'Logging in...' : 'Login' }}
      </button>
      <p>New here? <a routerLink="/register">Create an account</a></p>
    </div>
  `
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  submit() {
    this.error = '';
    this.loading = true;
    this.auth.login(this.email, this.password).subscribe({
      next: () => { this.loading = false; this.router.navigate(['/products']); },
      error: (err) => { this.loading = false; this.error = err.error?.error || 'Login failed'; }
    });
  }
}
