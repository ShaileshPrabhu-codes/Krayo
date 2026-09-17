import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { CreateOrderResponse } from '../../core/models/models';

declare const Razorpay: any;

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card" style="max-width:480px;margin:30px auto;text-align:center;">
      <h2>Checkout</h2>
      @if (order) {
        <p>Amount: <strong>{{ order.currency === 'INR' ? '₹' : '$' }}{{ (order.amount / 100).toFixed(2) }}</strong></p>
      }

      @switch (status) {
        @case ('idle') {
          <button class="btn" (click)="pay()">Pay Now</button>
        }
        @case ('processing') {
          <p>Opening secure payment window...</p>
        }
        @case ('verifying') {
          <p>Verifying your payment, please wait...</p>
        }
        @case ('success') {
          <p class="success">✅ Payment successful!</p>
          <button class="btn" (click)="goToPurchases()">View my card</button>
        }
        @case ('failed') {
          <p class="error">❌ Payment failed or was cancelled.</p>
          <button class="btn" (click)="pay()">Try again</button>
        }
      }
      @if (errorMsg) { <p class="error">{{ errorMsg }}</p> }
    </div>
  `
})
export class CheckoutComponent implements OnInit, OnDestroy {
  order?: CreateOrderResponse;
  status: 'idle' | 'processing' | 'verifying' | 'success' | 'failed' = 'idle';
  errorMsg = '';
  private pollHandle: any;

  constructor(private route: ActivatedRoute, private router: Router, private orderService: OrderService) {}

  ngOnInit() {
    const nav = this.router.getCurrentNavigation?.() ?? null;
    this.order = (history.state && history.state['order']) || nav?.extras?.state?.['order'];
    if (!this.order) {
      this.errorMsg = 'Checkout session expired. Please go back and pick your card again.';
    }
  }

  ngOnDestroy() {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  pay() {
    if (!this.order) return;
    this.status = 'processing';

    if (this.order.gateway === 'RAZORPAY') {
      this.loadScript('https://checkout.razorpay.com/v1/checkout.js').then(() => {
        const rzp = new Razorpay({
          key: this.order!.publicKey,
          order_id: this.order!.frontendToken,
          amount: this.order!.amount,
          currency: this.order!.currency,
          name: 'CardBazaar',
          description: 'Personalized Birthday Card',
          // NOTE: this handler firing does NOT mean the payment is confirmed - it only
          // means the browser flow completed. The real confirmation is the server-side
          // webhook, so we always poll order status next rather than trusting this alone.
          handler: () => this.startVerifying(),
          modal: { ondismiss: () => { this.status = 'idle'; } },
          theme: { color: '#e75480' }
        });
        rzp.open();
      });
    } else {
      // STRIPE: for a production build, load @stripe/stripe-js and confirmPayment()
      // with this.order.frontendToken (the PaymentIntent client_secret) using Stripe
      // Elements. Kept as a placeholder call here to keep this scaffold framework-light.
      this.loadScript('https://js.stripe.com/v3/').then(() => {
        this.errorMsg = 'Stripe Elements UI goes here - see README for the wiring steps.';
        this.startVerifying();
      });
    }
  }

  private startVerifying() {
    this.status = 'verifying';
    // Poll our backend, which only flips to PAID after a signature-verified webhook.
    this.pollHandle = setInterval(() => {
      this.orderService.getStatus(this.order!.orderNumber).subscribe(res => {
        if (res.status === 'PAID') {
          clearInterval(this.pollHandle);
          this.status = 'success';
        } else if (res.status === 'FAILED') {
          clearInterval(this.pollHandle);
          this.status = 'failed';
        }
        // else keep polling while PENDING
      });
    }, 2000);
  }

  goToPurchases() {
    this.router.navigate(['/my-purchases']);
  }

  private loadScript(src: string): Promise<void> {
    return new Promise((resolve) => {
      if (document.querySelector(`script[src="${src}"]`)) { resolve(); return; }
      const script = document.createElement('script');
      script.src = src;
      script.onload = () => resolve();
      document.body.appendChild(script);
    });
  }
}
