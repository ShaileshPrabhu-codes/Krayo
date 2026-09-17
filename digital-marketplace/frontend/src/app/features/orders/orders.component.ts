import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../core/services/order.service';
import { Download } from '../../core/models/models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>My Purchases</h2>
    <div class="grid">
      @for (d of downloads; track d.id) {
        <div class="card">
          <p>Format: {{ d.format.toUpperCase() }}</p>
          <p style="font-size:13px;color:#777;">Purchased {{ d.createdAt | date }}</p>
          <a [href]="d.downloadUrl" target="_blank" class="btn" style="text-decoration:none;display:inline-block;">
            Download HD
          </a>
        </div>
      }
      @if (!downloads.length) { <p>No purchases yet - go grab a card!</p> }
    </div>
  `
})
export class OrdersComponent implements OnInit {
  downloads: Download[] = [];

  constructor(private orderService: OrderService) {}

  ngOnInit() {
    this.orderService.myDownloads().subscribe(res => this.downloads = res);
  }
}
