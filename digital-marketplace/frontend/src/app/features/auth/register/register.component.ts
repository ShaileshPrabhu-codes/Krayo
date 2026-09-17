import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card" style="max-width:400px;margin:40px auto;">
      <h2>Create account</h2>
      <input placeholder="Full name" [(ngModel)]="fullName" name="fullName">
      <input type="email" placeholder="Email" [(ngModel)]="email" name="email">
      <input type="password" placeholder="Password (min 8 chars)" [(ngModel)]="password" name="password">
      <select [(ngModel)]="countryCode" name="countryCode" style="width:100%;padding:10px;margin-bottom:12px;">
        <option value="IN">India (₹)</option>
        <option value="OTHER">Rest of world ($)</option>
      </select>
      @if (error) { <p class="error">{{ error }}</p> }
      <button class="btn" (click)="submit()" [disabled]="loading">
        {{ loading ? 'Creating...' : 'Register' }}
      </button>
      <p>Already have an account? <a routerLink="/login">Login</a></p>
    </div>
  `
})
export class RegisterComponent {
  fullName = '';
  email = '';
  password = '';
  countryCode = 'IN';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  submit() {
    this.error = '';
    this.loading = true;
    this.auth.register(this.email, this.password, this.fullName, this.countryCode).subscribe({
      next: () => { this.loading = false; this.router.navigate(['/products']); },
      error: (err) => { this.loading = false; this.error = err.error?.error || 'Registration failed'; }
    });
  }
}
