import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product } from '../../../core/models/models';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Birthday Cards - v1.0 Collection</h2>
    <div class="grid">
      @for (p of products; track p.id) {
        <div class="card">
          @if (p.previewImageUrl) {
            <img [src]="p.previewImageUrl" style="width:100%;border-radius:6px;margin-bottom:8px;" alt="{{p.title}}">
          }
          <h3>{{ p.title }}</h3>
          <p style="color:#777;font-size:14px;">{{ p.theme }}</p>
          <p><strong>{{ formatPrice(p) }}</strong></p>
          <button class="btn" (click)="pick(p)">Personalize & Buy</button>
        </div>
      }
      @if (!products.length) { <p>No cards published yet.</p> }
    </div>
  `
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];

  constructor(private productService: ProductService, private auth: AuthService, private router: Router) {}

  ngOnInit() {
    this.productService.list(this.auth.getCountryCode()).subscribe(res => this.products = res);
  }

  formatPrice(p: Product): string {
    const symbol = p.currency === 'INR' ? '₹' : '$';
    const amount = p.currency === 'INR' ? p.price / 100 : p.price / 100;
    return `${symbol}${amount.toFixed(2)}`;
  }

  pick(p: Product) {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }
    this.router.navigate(['/products', p.id, 'customize']);
  }
}
