import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateOrderResponse, OrderStatusResponse, Download } from '../models/models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private http: HttpClient) {}

  createOrder(productId: number, recipientName: string, customMessage: string): Observable<CreateOrderResponse> {
    return this.http.post<CreateOrderResponse>(`${environment.apiBaseUrl}/orders`, { productId, recipientName, customMessage });
  }

  // Poll this - never trust a client-side gateway redirect as proof of payment.
  getStatus(orderNumber: string): Observable<OrderStatusResponse> {
    return this.http.get<OrderStatusResponse>(`${environment.apiBaseUrl}/orders/${orderNumber}/status`);
  }

  myDownloads(): Observable<Download[]> {
    return this.http.get<Download[]>(`${environment.apiBaseUrl}/downloads`);
  }
}
