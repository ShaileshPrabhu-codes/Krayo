import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { OrderService } from '../../../core/services/order.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product } from '../../../core/models/models';

@Component({
  selector: 'app-product-customize',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (product) {
      <div class="card" style="max-width:500px;margin:20px auto;">
        <h2>{{ product.title }}</h2>
        @if (product.previewImageUrl) {
          <img [src]="product.previewImageUrl" style="width:100%;border-radius:6px;margin-bottom:12px;">
        }
        <label>Recipient's name</label>
        <input [(ngModel)]="recipientName" placeholder="e.g. Priya">
        <label>Your message</label>
        <textarea rows="4" [(ngModel)]="customMessage" placeholder="Wishing you a wonderful birthday!"></textarea>
        @if (error) { <p class="error">{{ error }}</p> }
        <button class="btn" (click)="proceed()" [disabled]="loading">
          {{ loading ? 'Preparing checkout...' : 'Proceed to Pay' }}
        </button>
      </div>
    }
  `
})
export class ProductCustomizeComponent implements OnInit {
  product?: Product;
  recipientName = '';
  customMessage = '';
  error = '';
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private orderService: OrderService,
    private auth: AuthService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.productService.get(id, this.auth.getCountryCode()).subscribe(p => this.product = p);
  }

  proceed() {
    if (!this.recipientName || !this.customMessage) {
      this.error = 'Please fill in both fields';
      return;
    }
    this.error = '';
    this.loading = true;
    this.orderService.createOrder(this.product!.id, this.recipientName, this.customMessage).subscribe({
      next: (order) => {
        this.loading = false;
        // Pass the gateway session details via router state so checkout doesn't need
        // to re-create the order (idempotency is still enforced server-side either way).
        this.router.navigate(['/checkout', order.orderNumber], { state: { order } });
      },
      error: (err) => { this.loading = false; this.error = err.error?.error || 'Could not start checkout'; }
    });
  }
}
