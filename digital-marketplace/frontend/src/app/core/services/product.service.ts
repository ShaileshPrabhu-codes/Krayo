import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  constructor(private http: HttpClient) {}

  list(countryCode: string): Observable<Product[]> {
    return this.http.get<Product[]>(`${environment.apiBaseUrl}/products`, { params: { countryCode } });
  }

  get(id: number, countryCode: string): Observable<Product> {
    return this.http.get<Product>(`${environment.apiBaseUrl}/products/${id}`, { params: { countryCode } });
  }
}
