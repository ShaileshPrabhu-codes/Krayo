export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  role: string;
}

export interface Product {
  id: number;
  sku: string;
  title: string;
  description: string;
  theme: string;
  version: string;
  price: number;      // minor units (paise/cents)
  currency: string;   // INR | USD
  previewImageUrl: string;
}

export interface CreateOrderResponse {
  orderNumber: string;
  gateway: 'RAZORPAY' | 'STRIPE';
  frontendToken: string;
  publicKey: string;
  amount: number;
  currency: string;
}

export interface OrderStatusResponse {
  orderNumber: string;
  status: 'CREATED' | 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED';
  message: string;
}

export interface Download {
  id: number;
  format: string;
  downloadUrl: string;
  createdAt: string;
}
